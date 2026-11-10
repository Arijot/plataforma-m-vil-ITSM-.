<?php

namespace App\Http\Controllers;

use Carbon\Carbon;
use Exception;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

/**
 * Lecturas -> vista v_notificaciones.
 * Escrituras -> procedimientos sp_notif_marcar_leida / sp_notif_leer_todas (ver notificaciones_db.sql).
 */
class NotificacionController extends Controller
{
    /** GET /notificaciones?page=1  -> paginadas de 15, más recientes primero */
    public function index(Request $request)
    {
        $request->validate(['page' => 'sometimes|integer|min:1']);

        try {
            $uid = (int) $request->user()->id;

            $pagina = DB::table('v_notificaciones')
                ->where('usuario_id', $uid)
                ->orderByDesc('creado_en')
                ->orderByDesc('id')
                ->paginate(15, ['id', 'ticket_id', 'ticket_codigo', 'tipo', 'titulo', 'cuerpo', 'leida', 'creado_en']);

            return response()->json([
                'data' => $pagina->items(),
                'current_page' => $pagina->currentPage(),
                'last_page' => $pagina->lastPage(),
                'total' => $pagina->total(),
                'per_page' => $pagina->perPage(),
                'no_leidas' => $this->contarNoLeidas($uid),
            ], 200);
        } catch (Exception $e) {
            return response()->json(['message' => 'Error al obtener las notificaciones', 'error' => $e->getMessage()], 500);
        }
    }

    /** GET /notificaciones/no-leidas -> {"count": N}  (indicador del footer) */
    public function noLeidas(Request $request)
    {
        try {
            return response()->json(['count' => $this->contarNoLeidas((int) $request->user()->id)], 200);
        } catch (Exception $e) {
            return response()->json(['message' => 'Error al contar notificaciones', 'error' => $e->getMessage()], 500);
        }
    }

    /** POST /notificaciones/{id}/leida */
    public function marcarLeida(Request $request, int $id)
    {
        try {
            $uid = (int) $request->user()->id;

            $existe = DB::table('v_notificaciones')->where('id', $id)->where('usuario_id', $uid)->exists();
            if (!$existe) {
                return response()->json(['message' => 'La notificación no existe'], 404);
            }

            DB::statement('CALL sp_notif_marcar_leida(?, ?, ?)', [$uid, $id, Carbon::now()->toDateTimeString()]);

            return response()->json(['message' => 'Notificación leída'], 200);
        } catch (Exception $e) {
            return response()->json(['message' => 'Error al marcar la notificación', 'error' => $e->getMessage()], 500);
        }
    }

    /** POST /notificaciones/leer-todas */
    public function leerTodas(Request $request)
    {
        try {
            DB::statement('CALL sp_notif_leer_todas(?, ?)', [(int) $request->user()->id, Carbon::now()->toDateTimeString()]);

            return response()->json(['message' => 'Todas las notificaciones fueron marcadas como leídas'], 200);
        } catch (Exception $e) {
            return response()->json(['message' => 'Error al marcar las notificaciones', 'error' => $e->getMessage()], 500);
        }
    }

    private function contarNoLeidas(int $uid): int
    {
        return DB::table('v_notificaciones')->where('usuario_id', $uid)->where('leida', 0)->count();
    }
}