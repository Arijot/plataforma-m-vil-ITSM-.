<?php

namespace App\Http\Controllers;

use App\Events\MensajeEnviado;
use App\Events\TicketActualizado;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use App\Models\Servicio;
use App\Models\Prioridad;
use App\Models\Ticket;
use Carbon\Carbon;
use Exception;
use Illuminate\Support\Str;
use App\Services\FcmService;
use Illuminate\Support\Facades\Log;

class TicketController extends Controller
{
    public function __construct(private FcmService $fcm) {}

    /**
     * Retorna listas para llenar spinners/combos en la app móvil
     */
    public function getFormOptions()
    {
        try {
            $servicios = Servicio::where('activo', true)->get(['id', 'codigo', 'nombre', 'categoria']);
            $prioridades = Prioridad::orderBy('nivel', 'asc')->get(['id', 'codigo', 'nombre', 'nivel', 'color_hex']);

            return response()->json([
                'servicios' => $servicios,
                'prioridades' => $prioridades
            ], 200);
        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error al obtener opciones del formulario',
                'error' => $e->getMessage()
            ], 500);
        }
    }

    /**
     * Crea un nuevo ticket usando los Stored Procedures
     */
    public function store(Request $request)
    {
        $request->validate([
            'titulo' => 'required|string|max:150',
            'descripcion' => 'required|string',
            'servicio_id' => 'required|integer|exists:servicios,id',
            'prioridad_id' => 'required|integer|exists:prioridades,id',
            'ubicacion' => 'nullable|string|max:120',
            'archivo' => 'nullable|file|mimes:jpg,jpeg,png|max:5120' // Máx 5MB según la BD
        ]);

        try {
            DB::beginTransaction();

            $usuario = $request->user(); // Usuario autenticado vía Sanctum
            $fechaActual = Carbon::now()->toDateTimeString();

            // Código temporal: El Trigger trg_tickets_bi generará el correlativo TCK-YYYY-XXXX automáticamente[cite: 7]
            $codigoDummy = 'TEMP-' . time();

            // 1. Ejecutar Stored Procedure de creación[cite: 7]
            DB::statement('CALL sp_crear_ticket(?, ?, ?, ?, ?, ?, ?, ?)', [
                $codigoDummy,
                $request->titulo,
                $request->descripcion,
                $request->servicio_id,
                $request->prioridad_id,
                $usuario->id,
                $request->ubicacion,
                $fechaActual
            ]);

            // 2. Obtener el ticket recién creado a través de la vista o tabla usando solicitante y fecha
            $ticketCreado = Ticket::where('solicitante_id', $usuario->id)
                ->orderBy('id', 'desc')
                ->first();

            $codigoGenerado = $ticketCreado->codigo;
            $urlAdjunto = null;

            // 3. Procesar archivo adjunto si fue enviado
            if ($request->hasFile('archivo')) {
                $file = $request->file('archivo');
                $nombreArchivo = time() . '_' . preg_replace('/[^A-Za-z0-9\._-]/', '', $file->getClientOriginalName());
                $tamanoBytes = $file->getSize();

                // Guardar archivo físicamente en public/tickets/{CODIGO}/
                $carpetaDestino = 'tickets/' . $codigoGenerado;
                $rutaFisica = public_path($carpetaDestino);

                if (!file_exists($rutaFisica)) {
                    mkdir($rutaFisica, 0777, true);
                }

                $file->move($rutaFisica, $nombreArchivo);

                // URL completa para guardar en la base de datos
                $urlAdjunto = url($carpetaDestino . '/' . $nombreArchivo);

                // Llamar al Stored Procedure sp_adjunto[cite: 7]
                DB::statement('CALL sp_adjunto(?, ?, ?, ?, ?)', [
                    $codigoGenerado,
                    $usuario->id,
                    $urlAdjunto,
                    $tamanoBytes,
                    $fechaActual
                ]);
            }

            DB::commit();

            return response()->json([
                'message' => 'Ticket registrado exitosamente',
                'codigo' => $codigoGenerado,
                'ticket_id' => $ticketCreado->id,
                'url_adjunto' => $urlAdjunto
            ], 201);

        } catch (Exception $e) {
            DB::rollBack();

            return response()->json([
                'message' => 'Error al registrar el ticket',
                'error' => $e->getMessage()
            ], 500);
        }
    }

    public function myTickets(Request $request)
    {
        try {
            $usuario = $request->user();
            $rol = (int) $usuario->rol_id;

            // ---- Catálogo de estados: la BD (estados_ticket) es la única fuente de verdad ----
            $estados = DB::table('estados_ticket')->orderBy('orden')->get(['id', 'codigo', 'nombre', 'orden', 'es_final']);
            $filtrosEstado = $estados->map(fn ($e) => strtolower($e->codigo))->all();   // abierto, asignado, en_proceso...

            $request->validate([
                'page' => 'sometimes|integer|min:1',
                'filtro' => 'sometimes|string|in:' . implode(',', array_merge(['todos', 'activos', 'sin_asignar', 'asignados'], $filtrosEstado)),
            ]);

            // "Terminados" para la lista = estados finales (es_final = 1) + RESUELTO (ya no requiere trabajo del técnico)
            $terminados = $estados->filter(fn ($e) => (int) $e->es_final === 1 || $e->codigo === 'RESUELTO')
                ->pluck('codigo')->values()->all();

            $q = DB::table('v_tickets_detalle');

            // ---- Alcance por rol ----
            if ($rol === 1) {
                $q->where('solicitante_id', $usuario->id);
            } elseif ($rol === 2) {
                $q->where('tecnico_id', $usuario->id);
            }

            // ---- Filtros rápidos ----
            $filtro = $request->query('filtro', 'todos');
            if ($filtro === 'activos') {
                $q->whereNotIn('estado', $terminados);
            } elseif ($filtro === 'sin_asignar') {
                $q->whereNull('tecnico_id')->whereNotIn('estado', $terminados);
            } elseif ($filtro === 'asignados') {
                $q->whereNotNull('tecnico_id')->whereNotIn('estado', $terminados);
            } elseif ($filtro !== 'todos') {
                // Cualquier estado de la tabla estados_ticket (filtro = código en minúsculas)
                $codigo = optional($estados->first(fn ($e) => strtolower($e->codigo) === $filtro))->codigo;
                if ($codigo) {
                    $q->where('estado', $codigo);
                }
            }

            // ---- Ordenamiento ----
            $ph = implode(',', array_fill(0, count($terminados), '?'));
            $grupo = $rol === 3
                ? "CASE WHEN estado IN ($ph) THEN 3
                        WHEN tecnico_id IS NOT NULL THEN 2
                        ELSE 1 END"
                : "CASE WHEN estado IN ($ph) THEN 2 ELSE 1 END";

            $q->orderByRaw($grupo, $terminados)
                ->orderBy('prioridad_nivel')          // nivel más bajo = más urgente
                ->orderByDesc('fecha_creacion')
                ->orderByDesc('id');

            $columnas = [
                'id', 'codigo', 'titulo', 'servicio', 'prioridad', 'prioridad_nivel', 'color_hex',
                'estado', 'estado_nombre', 'estado_orden', 'estado_final',
                'solicitante', 'tecnico_id', 'tecnico', 'fecha_creacion',
                // notificaciones sin leer de este usuario sobre cada ticket (punto en la lista)
                DB::raw('(SELECT COUNT(*) FROM v_notificaciones n WHERE n.ticket_id = v_tickets_detalle.id'
                    . ' AND n.usuario_id = ' . (int) $usuario->id . ' AND n.leida = 0) AS notif_pendientes'),
            ];

            // El tiempo que le queda al técnico para resolver el ticket SOLO se envía al técnico (rol 2).
            // Para los demás roles la columna ni sale del servidor.
            if ($rol === 2) {
                $columnas[] = 'plazo_dias';
                $columnas[] = 'fecha_limite_tecnico';
                $columnas[] = 'tiempo_restante_tecnico';   // minutos; negativo = vencido
            }

            $pagina = $q->paginate(7, $columnas);

            return response()->json([
                'data' => $pagina->items(),
                'current_page' => $pagina->currentPage(),
                'last_page' => $pagina->lastPage(),
                'total' => $pagina->total(),
                'per_page' => $pagina->perPage(),
                'estados' => $estados,                 // la app arma los chips de filtro con esto
            ], 200);
        } catch (\Illuminate\Validation\ValidationException $e) {
            throw $e;                                  // que Laravel responda el 422 normal
        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error al obtener listado de tickets',
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    /**
     * Detalle completo del ticket (cabecera, adjuntos, chat, historial, encuesta)
     * + estados que el usuario puede elegir + su propio id (para pintar el chat).
     */
    public function show(Request $request, string $idOrCodigo)
    {
        try {
            $usuario = $request->user();

            $pdo = DB::connection()->getPdo();
            $stmt = $pdo->prepare('CALL sp_obtener_detalle_ticket(?)');
            $stmt->execute([$idOrCodigo]);

            $ticketData = $stmt->fetchAll(\PDO::FETCH_OBJ);
            if (empty($ticketData)) {
                $stmt->closeCursor();
                return response()->json(['message' => 'El ticket solicitado no existe'], 404);
            }
            $ticket = $ticketData[0];

            if ($usuario->rol_id == 1 && $ticket->solicitante_id != $usuario->id) {
                $stmt->closeCursor();
                return response()->json(['message' => 'No tienes permiso para consultar este ticket'], 403);
            }
            if ($usuario->rol_id == 2 && $ticket->tecnico_id != $usuario->id) {
                $stmt->closeCursor();
                return response()->json(['message' => 'Solo puedes ver los tickets que se te han asignado'], 403);
            }

            $stmt->nextRowset();
            $adjuntos = $stmt->fetchAll(\PDO::FETCH_OBJ);
            $adjuntosResponse = [
                'tiene_adjuntos' => !empty($adjuntos),
                'mensaje' => empty($adjuntos) ? 'El usuario no dejo archivos adjuntos en el ticket' : null,
                'archivos' => $adjuntos,
            ];

            $stmt->nextRowset();
            $mensajesBrutos = $stmt->fetchAll(\PDO::FETCH_OBJ);
            $mensajes = array_values(array_filter($mensajesBrutos, function ($msg) use ($usuario) {
                return !($usuario->rol_id == 1 && $msg->es_interno == 1);
            }));

            $stmt->nextRowset();
            $historial = $stmt->fetchAll(\PDO::FETCH_OBJ);
            // El historial lo ve ÚNICAMENTE el administrador de TI (rol 3). Nunca sale del servidor para otros roles.
            if ((int) $usuario->rol_id !== 3) {
                $historial = [];
            }

            $stmt->nextRowset();
            $encuestaArray = $stmt->fetchAll(\PDO::FETCH_OBJ);
            $encuesta = !empty($encuestaArray) ? $encuestaArray[0] : null;

            // IMPORTANTE: cerrar el cursor antes de hacer otra consulta en la misma conexión
            $stmt->closeCursor();

            // Estado tomado de estados_ticket (vista): código + nombre + es_final.
            // Así 'estado' siempre es el CÓDIGO, que es lo que usa transiciones_estado.
            $vista = $this->ticketVista($ticket->codigo);
            if ($vista) {
                $ticket->estado = $vista->estado;
                $ticket->estado_nombre = $vista->estado_nombre;
                $ticket->estado_final = (int) $vista->estado_final;
            }

            // Al abrir el ticket, sus notificaciones pendientes (mensajes, cambios de estado...) quedan leídas
            DB::statement('CALL sp_notif_leer_ticket(?, ?, ?)', [$usuario->id, $ticket->id, Carbon::now()->toDateTimeString()]);

            return response()->json([
                'ticket' => $ticket,
                'adjuntos' => $adjuntosResponse,
                'mensajes' => $mensajes,
                'historial' => $historial,
                'encuesta' => $encuesta,
                'estados_permitidos' => $this->estadosPermitidos($usuario, (string) $ticket->estado),
                'acciones' => $this->acciones($usuario, $ticket, $encuesta),
                'mi_usuario_id' => $usuario->id,
            ], 200);
        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error al obtener el detalle del ticket',
                'error' => $e->getMessage(),
            ], 500);
        }
    }

    /** Polling ligero del chat: solo mensajes nuevos + estado/técnico actuales */
    public function mensajes(Request $request, string $codigo)
    {
        $usuario = $request->user();
        $ticket = $this->ticketVista($codigo);
        if (!$ticket) {
            return response()->json(['message' => 'El ticket solicitado no existe'], 404);
        }
        if (!$this->puedeAcceder($usuario, $ticket)) {
            return response()->json(['message' => 'Sin permiso sobre este ticket'], 403);
        }

        $q = DB::table('ticket_mensajes as m')
            ->join('usuarios as u', 'u.id', '=', 'm.autor_id')
            ->join('roles as r', 'r.id', '=', 'u.rol_id')
            ->where('m.ticket_id', $ticket->id)
            ->where('m.id', '>', (int) $request->query('despues_de', 0))
            ->orderBy('m.id')
            ->select('m.id', 'm.autor_id', DB::raw("CONCAT(u.nombres, ' ', u.apellidos) AS autor_nombre"),
                'r.codigo as autor_rol', 'm.contenido', 'm.es_interno', 'm.creado_en');

        if ($usuario->rol_id == 1) {
            $q->where('m.es_interno', 0);
        }

        return response()->json([
            'mensajes' => $q->get(),
            'estado' => $ticket->estado,
            'tecnico_id' => $ticket->tecnico_id,
            'tecnico' => $ticket->tecnico,
        ]);
    }

    /** Enviar mensaje al chat (usuario final, técnico asignado o admin) */
    public function enviarMensaje(Request $request, string $codigo)
    {
        $request->validate([
            'contenido' => 'required|string|max:2000',
            'es_interno' => 'sometimes|boolean',
        ]);

        $usuario = $request->user();
        $ticket = $this->ticketVista($codigo);
        if (!$ticket) {
            return response()->json(['message' => 'El ticket solicitado no existe'], 404);
        }
        if (!$this->puedeAcceder($usuario, $ticket)) {
            return response()->json(['message' => 'Sin permiso sobre este ticket'], 403);
        }

        $interno = $usuario->rol_id == 1 ? false : $request->boolean('es_interno');
        $contenido = trim($request->contenido);
        $ahora = Carbon::now()->toDateTimeString();

        try {
            // El trigger trg_mensajes_bi valida reglas y notifica al otro lado
            DB::statement('CALL sp_msg(?, ?, ?, ?, ?)', [$codigo, $usuario->id, $contenido, $interno ? 1 : 0, $ahora]);

            if (!$interno) {
                $titulo = 'Nuevo mensaje en ' . $ticket->codigo;
                $cuerpo = Str::limit($contenido, 200);

                if ($usuario->id == $ticket->solicitante_id) {
                    // Usuario responde: el trigger ya avisa al técnico; aquí avisamos a los admins
                    $admins = DB::table('usuarios as u')->join('roles as r', 'r.id', '=', 'u.rol_id')
                        ->where('r.codigo', 'ADMINISTRADOR_TI')->where('u.activo', 1)->pluck('u.id');
                    foreach ($admins as $adminId) {
                        DB::statement('CALL sp_notificar(?, ?, ?, ?, ?, ?)',
                            [$adminId, $ticket->id, 'NUEVO_MENSAJE', $titulo, $cuerpo, $ahora]);
                    }
                } elseif ($usuario->rol_id == 3 && $ticket->tecnico_id && $ticket->tecnico_id != $usuario->id) {
                    // Admin escribe: el trigger avisa al usuario; aquí avisamos al técnico
                    DB::statement('CALL sp_notificar(?, ?, ?, ?, ?, ?)',
                        [$ticket->tecnico_id, $ticket->id, 'NUEVO_MENSAJE', $titulo, $cuerpo, $ahora]);
                }
            }
        } catch (\Throwable $e) {
            return response()->json(['message' => $this->mensajeSql($e)], 422);
        }

        $this->emitirMensaje($ticket->id, $usuario->id, $ticket->codigo, $interno);

        $this->push($ticket->id);

        return response()->json(['message' => 'Mensaje enviado'], 201);
    }

    /** Cambiar estado (técnico asignado o admin). Las transiciones las valida la BD. */
    public function cambiarEstado(Request $request, string $codigo)
    {
        $request->validate([
            'estado' => 'required|string|in:ABIERTO,ASIGNADO,EN_PROCESO,RESUELTO,CERRADO,CANCELADO',
            'solucion' => 'nullable|string|max:2000',
        ]);

        $usuario = $request->user();
        $ticket = $this->ticketVista($codigo);
        if (!$ticket) {
            return response()->json(['message' => 'El ticket solicitado no existe'], 404);
        }
        if ($usuario->rol_id == 1 || !$this->puedeAcceder($usuario, $ticket)) {
            return response()->json(['message' => 'No tienes permiso para cambiar el estado'], 403);
        }
        if ($usuario->rol_id == 2 && !in_array($request->estado, ['EN_PROCESO', 'RESUELTO'])) {
            return response()->json(['message' => 'Solo puedes pasar el ticket a En proceso o Resuelto'], 403);
        }

        $solucion = trim((string) $request->solucion);
        if ($request->estado === 'RESUELTO' && $solucion === '') {
            return response()->json(['message' => 'Debes registrar la solución aplicada'], 422);
        }

        try {
            DB::statement('CALL sp_mover(?, ?, ?, ?, ?)', [
                $codigo, $request->estado, Carbon::now()->toDateTimeString(), $usuario->id,
                $solucion !== '' ? $solucion : null,
            ]);
        } catch (\Throwable $e) {
            return response()->json(['message' => $this->mensajeSql($e)], 422);
        }

        $this->emitirActualizacion($codigo);

        $this->push($ticket->id); // el trigger ya notificó al solicitante

        return response()->json(['message' => 'Estado actualizado'], 200);
    }

    /** Asignar / reasignar técnico (solo admin) */
    public function asignar(Request $request, string $codigo)
    {
        $usuario = $request->user();
        if ($usuario->rol_id != 3) {
            return response()->json(['message' => 'Solo el administrador puede asignar técnicos'], 403);
        }
        $request->validate(['tecnico_id' => 'required|integer|exists:usuarios,id']);

        $ticket = $this->ticketVista($codigo);
        if (!$ticket) {
            return response()->json(['message' => 'El ticket solicitado no existe'], 404);
        }

        $ahora = Carbon::now()->toDateTimeString();
        try {
            DB::statement('CALL sp_asignar(?, ?, ?, ?)', [$codigo, $request->tecnico_id, $ahora, $usuario->id]);

            $nombre = DB::table('usuarios')->where('id', $request->tecnico_id)
                ->selectRaw("CONCAT(nombres, ' ', apellidos) AS n")->value('n');

            // El trigger avisa al técnico; aquí avisamos al usuario final quién lo atiende
            DB::statement('CALL sp_notificar(?, ?, ?, ?, ?, ?)', [
                $ticket->solicitante_id, $ticket->id, 'TICKET_ASIGNADO',
                'Técnico asignado a ' . $ticket->codigo, $nombre . ' atenderá tu ticket', $ahora,
            ]);
        } catch (\Throwable $e) {
            return response()->json(['message' => $this->mensajeSql($e)], 422);
        }

        $this->emitirActualizacion($codigo);

        $this->push($ticket->id);

        return response()->json(['message' => 'Técnico asignado'], 200);
    }

    /** Lista de técnicos activos (para el spinner del admin) */
    public function tecnicos(Request $request)
    {
        if ($request->user()->rol_id != 3) {
            return response()->json(['message' => 'No autorizado'], 403);
        }

        return response()->json(
            DB::table('usuarios as u')->join('roles as r', 'r.id', '=', 'u.rol_id')
                ->where('r.codigo', 'TECNICO_TI')->where('u.activo', 1)
                ->orderBy('u.nombres')
                ->get(['u.id', DB::raw("CONCAT(u.nombres, ' ', u.apellidos) AS nombre")])
        );
    }

    /**
     * REABRIR (solo el usuario final dueño del ticket).
     * El ticket vuelve al estado inicial (ABIERTO) y sin técnico; el motivo queda en el chat.
     * Reglas: estado RESUELTO, dentro del plazo (parametro plazo_reapertura_horas) y sin encuesta enviada.
     */
    public function reabrir(Request $request, string $codigo)
    {
        $request->validate(['motivo' => 'required|string|min:5|max:500']);

        $usuario = $request->user();
        $ticket = $this->ticketVista($codigo);
        if (!$ticket) {
            return response()->json(['message' => 'El ticket solicitado no existe'], 404);
        }
        if ((int) $usuario->rol_id !== 1 || $ticket->solicitante_id != $usuario->id) {
            return response()->json(['message' => 'Solo el solicitante puede reabrir su ticket'], 403);
        }
        if ($ticket->estado !== 'RESUELTO') {
            return response()->json(['message' => 'Solo se pueden reabrir tickets en estado Resuelto'], 422);
        }
        if (DB::table('satisfaction_surveys')->where('ticket_id', $ticket->id)->exists()) {
            return response()->json(['message' => 'Ya calificaste este ticket, por lo que ya no se puede reabrir'], 422);
        }
        $limite = $this->limiteReapertura($ticket);
        if ($limite && Carbon::now()->gt($limite)) {
            return response()->json(['message' => 'Venció el plazo para reabrir este ticket'], 422);
        }

        try {
            DB::transaction(function () use ($codigo, $usuario, $request) {
                DB::statement('CALL sp_reabrir(?, ?, ?, ?)', [
                    $codigo, $usuario->id, trim($request->motivo), Carbon::now()->toDateTimeString(),
                ]);
            });
        } catch (\Throwable $e) {
            return response()->json(['message' => $this->mensajeSql($e)], 422);
        }

        $this->emitirMensaje($ticket->id, $usuario->id, $ticket->codigo, false);
        $this->emitirActualizacion($codigo);
        $this->push($ticket->id);   // el trigger ya dejó los avisos para admins y técnico anterior

        return response()->json(['message' => 'Ticket reabierto'], 200);
    }

    /**
     * ENCUESTA de satisfacción (solo el solicitante, ticket RESUELTO o CERRADO, una sola vez).
     */
    public function encuesta(Request $request, string $codigo)
    {
        $request->validate([
            'calificacion' => 'required|integer|between:1,5',
            'comentario' => 'nullable|string|max:500',
        ]);

        $usuario = $request->user();
        $ticket = $this->ticketVista($codigo);
        if (!$ticket) {
            return response()->json(['message' => 'El ticket solicitado no existe'], 404);
        }
        if ((int) $usuario->rol_id !== 1 || $ticket->solicitante_id != $usuario->id) {
            return response()->json(['message' => 'Solo el solicitante puede calificar el ticket'], 403);
        }
        if (!in_array($ticket->estado, ['RESUELTO', 'CERRADO'], true)) {
            return response()->json(['message' => 'La encuesta se habilita cuando el ticket está resuelto'], 422);
        }
        if (DB::table('satisfaction_surveys')->where('ticket_id', $ticket->id)->exists()) {
            return response()->json(['message' => 'Este ticket ya fue calificado'], 422);
        }

        try {
            $comentario = trim((string) $request->comentario);
            DB::statement('CALL sp_encuesta(?, ?, ?, ?)', [
                $codigo,
                (int) $request->calificacion,
                $comentario !== '' ? $comentario : null,
                Carbon::now()->toDateTimeString(),
            ]);
        } catch (\Throwable $e) {
            return response()->json(['message' => $this->mensajeSql($e)], 422);
        }

        return response()->json(['message' => '¡Gracias por tu opinión!'], 201);
    }

    /**
     * GESTIÓN del ticket (SOLO administrador de TI): nivel de complejidad y plazo en días.
     * Body (todo opcional, pero al menos uno):
     *   complejidad: BAJA | MEDIA | ALTA
     *   plazo_dias : 1..90  (0 = quitar plazo). Cuenta desde el momento en que se guarda.
     */
    public function gestion(Request $request, string $codigo)
    {
        $usuario = $request->user();
        if ((int) $usuario->rol_id !== 3) {
            return response()->json(['message' => 'Solo el administrador de TI puede editar la complejidad y el plazo'], 403);
        }

        $request->validate([
            'complejidad' => 'sometimes|string|in:BAJA,MEDIA,ALTA',
            'plazo_dias' => 'sometimes|integer|between:0,90',
        ]);
        if (!$request->has('complejidad') && !$request->has('plazo_dias')) {
            return response()->json(['message' => 'No hay cambios para guardar'], 422);
        }

        $ticket = $this->ticketVista($codigo);
        if (!$ticket) {
            return response()->json(['message' => 'El ticket solicitado no existe'], 404);
        }
        if (in_array($ticket->estado, ['CERRADO', 'CANCELADO'], true)) {
            return response()->json(['message' => 'El ticket está finalizado y no admite cambios'], 422);
        }
        if ($request->has('plazo_dias') && (int) $request->plazo_dias > 0 && !$ticket->tecnico_id) {
            return response()->json(['message' => 'Asigna un técnico antes de fijar un plazo'], 422);
        }

        $ahora = Carbon::now()->toDateTimeString();
        try {
            DB::transaction(function () use ($request, $codigo, $usuario, $ahora) {
                if ($request->has('complejidad')) {
                    DB::statement('CALL sp_set_complejidad(?, ?, ?, ?)', [$codigo, $request->complejidad, $ahora, $usuario->id]);
                }
                if ($request->has('plazo_dias')) {
                    DB::statement('CALL sp_set_plazo(?, ?, ?, ?)', [$codigo, (int) $request->plazo_dias, $ahora, $usuario->id]);
                }
            });

            // Avisar al técnico del nuevo plazo (tipo SLA_POR_VENCER ya existe en notificaciones)
            if ($request->has('plazo_dias') && (int) $request->plazo_dias > 0 && $ticket->tecnico_id) {
                DB::statement('CALL sp_notificar(?, ?, ?, ?, ?, ?)', [
                    $ticket->tecnico_id, $ticket->id, 'SLA_POR_VENCER',
                    'Plazo para ' . $ticket->codigo,
                    'Tienes ' . (int) $request->plazo_dias . ' día(s) para resolver este ticket',
                    $ahora,
                ]);
                $this->push($ticket->id);
            }
        } catch (\Throwable $e) {
            return response()->json(['message' => $this->mensajeSql($e)], 422);
        }

        $this->emitirActualizacion($codigo);

        return response()->json(['message' => 'Cambios guardados'], 200);
    }

    /** Registra/actualiza el token FCM del dispositivo (omite si ya tienes este endpoint) */
    public function guardarTokenFcm(Request $request)
    {
        $request->validate(['token' => 'required|string|max:255', 'modelo' => 'nullable|string|max:80']);

        DB::table('dispositivos_fcm')->updateOrInsert(
            ['token_fcm' => $request->token],
            [
                'usuario_id' => $request->user()->id,
                'plataforma' => 'ANDROID',
                'modelo' => $request->modelo,
                'activo' => 1,
                'ultimo_uso' => Carbon::now(),
            ]
        );

        return response()->json(['message' => 'Token registrado']);
    }

    // ------------------------------ helpers ------------------------------

    /** Hasta cuándo puede el usuario reabrir un ticket resuelto (null si aún no tiene fecha de resolución) */
    private function limiteReapertura($ticket): ?Carbon
    {
        if (empty($ticket->fecha_resolucion)) {
            return null;
        }
        $row = DB::selectOne("SELECT fn_param('plazo_reapertura_horas') AS v");
        $horas = (int) ($row->v ?? 48);

        return Carbon::parse($ticket->fecha_resolucion)->addHours($horas);
    }

    /**
     * Banderas que la app usa para mostrar/ocultar botones y tarjetas del detalle.
     * La complejidad y el plazo solo se envían a técnico y admin.
     */
    private function acciones($usuario, $ticket, $encuesta): array
    {
        $rol = (int) $usuario->rol_id;
        $esDueno = $rol === 1 && $ticket->solicitante_id == $usuario->id;
        $final = in_array($ticket->estado, ['CERRADO', 'CANCELADO'], true);

        $puedeEncuestar = $esDueno && in_array($ticket->estado, ['RESUELTO', 'CERRADO'], true) && !$encuesta;

        $puedeReabrir = false;
        $reabrirHasta = null;
        if ($esDueno && $ticket->estado === 'RESUELTO' && !$encuesta) {
            $limite = $this->limiteReapertura($ticket);
            if ($limite) {
                $reabrirHasta = $limite->toDateTimeString();
                $puedeReabrir = Carbon::now()->lte($limite);
            }
        }

        $verGestion = $rol === 2 || $rol === 3;

        return [
            'puede_encuestar' => $puedeEncuestar,
            'puede_reabrir' => $puedeReabrir,
            'reabrir_hasta' => $reabrirHasta,
            'puede_editar_gestion' => $rol === 3 && !$final,
            'complejidad' => $verGestion ? ($ticket->complejidad ?? null) : null,
            'plazo_dias' => $verGestion ? ($ticket->plazo_dias ?? null) : null,
            'fecha_limite_tecnico' => $verGestion ? ($ticket->fecha_limite_tecnico ?? null) : null,
            'minutos_restantes_tecnico' => $verGestion && isset($ticket->tiempo_restante_tecnico)
                ? (int) $ticket->tiempo_restante_tecnico : null,
        ];
    }

    private function ticketVista(string $codigo)
    {
        return DB::table('v_tickets_detalle')->where('codigo', $codigo)->first();
    }

    private function puedeAcceder($usuario, $ticket): bool
    {
        return match ((int) $usuario->rol_id) {
            3 => true,
            2 => $ticket->tecnico_id == $usuario->id,
            1 => $ticket->solicitante_id == $usuario->id,
            default => false,
        };
    }

    /** Estados destino válidos según transiciones_estado (técnico: solo En proceso / Resuelto) */
    private function estadosPermitidos($usuario, string $estadoActual): array
    {
        if ($usuario->rol_id == 1) {
            return [];
        }

        $q = DB::table('transiciones_estado as tr')
            ->join('estados_ticket as o', 'o.id', '=', 'tr.estado_origen_id')
            ->join('estados_ticket as d', 'd.id', '=', 'tr.estado_destino_id')
            ->where('o.codigo', $estadoActual)
            ->orderBy('d.orden');

        if ($usuario->rol_id == 2) {
            $q->whereIn('d.codigo', ['EN_PROCESO', 'RESUELTO']);
        }

        return $q->get(['d.codigo', 'd.nombre'])->all();
    }

    /** Extrae el texto de los SIGNAL SQLSTATE '45000' de los triggers */
    private function mensajeSql(\Throwable $e): string
    {
        if (preg_match('/1644\s+(.*?)\s+\(Connection:/s', $e->getMessage(), $m)) {
            return $m[1];
        }
        return 'No se pudo completar la operación';
    }

    /** Envía las notificaciones FCM pendientes DESPUÉS de responder a la app (sin lentitud) */
    private function push(int $ticketId): void
    {
        dispatch(fn () => $this->fcm->enviarPendientes($ticketId))->afterResponse();
    }

    private function emitirMensaje(int $ticketId, int $autorId, string $codigo, bool $interno): void
    {
        try {
            $m = DB::table('ticket_mensajes as m')
                ->join('usuarios as u', 'u.id', '=', 'm.autor_id')
                ->join('roles as r', 'r.id', '=', 'u.rol_id')
                ->where('m.ticket_id', $ticketId)
                ->where('m.autor_id', $autorId)
                ->orderByDesc('m.id')
                ->first([
                    'm.id', 'm.autor_id',
                    DB::raw("CONCAT(u.nombres, ' ', u.apellidos) AS autor_nombre"),
                    'r.codigo as autor_rol', 'm.contenido', 'm.es_interno', 'm.creado_en',
                ]);
 
            if ($m) {
                broadcast(new MensajeEnviado($codigo, (array) $m, $interno));
            }
        } catch (\Throwable $e) {
            Log::warning('Broadcast mensaje: ' . $e->getMessage());
        }
    }
 
    /** Avisa a quienes tienen abierto el ticket que cambió el estado o el técnico */
    private function emitirActualizacion(string $codigo): void
    {
        try {
            $t = $this->ticketVista($codigo);
            if ($t) {
                broadcast(new TicketActualizado($codigo, $t->estado, $t->tecnico_id, $t->tecnico));
            }
        } catch (\Throwable $e) {
            Log::warning('Broadcast ticket: ' . $e->getMessage());
        }
    }
}