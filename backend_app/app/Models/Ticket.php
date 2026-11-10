<?php

namespace App\Models;

use App\Enums\EstadoTicketCodigo;
use App\Enums\PrioridadCodigo;
use App\Support\ContextoBD;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Database\Eloquent\Relations\HasOne;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use Throwable;

/**
 * Ticket ITSM.
 *
 * IMPORTANTE: la base de datos (triggers) genera el código TCK-AAAA-NNNN, el estado
 * inicial, los límites de SLA, las fechas de cada etapa, el historial (ticket_logs)
 * y las notificaciones. Por eso los cambios sensibles se hacen con los métodos de
 * este modelo (registrar, asignarA, resolver, cerrar...) y no con asignación masiva.
 */
class Ticket extends Model
{
    protected $table = 'tickets';

    const CREATED_AT = 'fecha_creacion';
    const UPDATED_AT = 'actualizado_en';

    /** Solo lo que el usuario final puede enviar al registrar (HU-03). */
    protected $fillable = ['titulo', 'descripcion', 'servicio_id', 'prioridad_id', 'solicitante_id', 'ubicacion'];

    protected $casts = [
        'veces_reabierto'         => 'integer',
        'fecha_asignacion'        => 'datetime',
        'fecha_primera_respuesta' => 'datetime',
        'fecha_inicio_atencion'   => 'datetime',
        'fecha_resolucion'        => 'datetime',
        'fecha_cierre'            => 'datetime',
        'fecha_cancelacion'       => 'datetime',
        'fecha_limite_respuesta'  => 'datetime',
        'fecha_limite_resolucion' => 'datetime',
        'fecha_ultima_reapertura' => 'datetime',
    ];

    /** Permite /tickets/TCK-2026-0001 en las rutas (HU-18). */
    public function getRouteKeyName(): string
    {
        return 'codigo';
    }

    // =====================================================================
    // Relaciones
    // =====================================================================
    public function servicio(): BelongsTo         { return $this->belongsTo(Servicio::class, 'servicio_id'); }
    public function prioridad(): BelongsTo        { return $this->belongsTo(Prioridad::class, 'prioridad_id'); }
    public function prioridadOriginal(): BelongsTo { return $this->belongsTo(Prioridad::class, 'prioridad_original_id'); }
    public function estado(): BelongsTo           { return $this->belongsTo(EstadoTicket::class, 'estado_id'); }
    public function solicitante(): BelongsTo      { return $this->belongsTo(Usuario::class, 'solicitante_id'); }
    public function tecnico(): BelongsTo          { return $this->belongsTo(Usuario::class, 'tecnico_id'); }

    public function adjuntos(): HasMany { return $this->hasMany(TicketAttachment::class, 'ticket_id'); }
    public function logs(): HasMany     { return $this->hasMany(TicketLog::class, 'ticket_id')->orderBy('creado_en')->orderBy('id'); }
    public function mensajes(): HasMany { return $this->hasMany(TicketMensaje::class, 'ticket_id')->orderBy('creado_en'); }

    public function mensajesPublicos(): HasMany
    {
        return $this->mensajes()->where('es_interno', false);
    }

    public function encuesta(): HasOne
    {
        return $this->hasOne(SatisfactionSurvey::class, 'ticket_id');
    }

    public function notificaciones(): HasMany
    {
        return $this->hasMany(Notificacion::class, 'ticket_id');
    }

    /** El usuario final NO debe ver las notas internas del técnico (HU-08). */
    public function mensajesVisiblesPara(Usuario $usuario): HasMany
    {
        return $usuario->esUsuarioFinal() ? $this->mensajesPublicos() : $this->mensajes();
    }

    // =====================================================================
    // Accessors
    // =====================================================================
    public function getEstadoCodigoAttribute(): EstadoTicketCodigo
    {
        return collect(EstadoTicketCodigo::cases())->first(fn ($c) => $c->id() === (int) $this->estado_id);
    }

    public function getEstaFinalizadoAttribute(): bool
    {
        return $this->estado_codigo->esFinal();
    }

    /** TRUE/FALSE según SLA de resolución; NULL si el ticket fue cancelado. */
    public function getSlaVencidoAttribute(): ?bool
    {
        return match ($this->estado_codigo) {
            EstadoTicketCodigo::Cancelado => null,
            EstadoTicketCodigo::Resuelto,
            EstadoTicketCodigo::Cerrado   => $this->fecha_resolucion?->gt($this->fecha_limite_resolucion),
            default                       => now()->gt($this->fecha_limite_resolucion),
        };
    }

    /** Minutos que faltan para vencer el SLA (negativo = ya venció). Solo tickets en curso (HU-22). */
    public function getMinutosRestantesSlaAttribute(): ?int
    {
        if (! in_array($this->estado_codigo, EstadoTicketCodigo::enCurso(), true)) {
            return null;
        }

        return (int) now()->diffInMinutes($this->fecha_limite_resolucion, false);
    }

    public function getPuedeCancelarAttribute(): bool   // HU-16
    {
        return $this->estado_codigo === EstadoTicketCodigo::Abierto;
    }

    public function getPuedeReabrirAttribute(): bool    // HU-17
    {
        if ($this->estado_codigo !== EstadoTicketCodigo::Resuelto || ! $this->fecha_resolucion) {
            return false;
        }
        $horas = ParametroSistema::entero('plazo_reapertura_horas', 72);

        return now()->lte($this->fecha_resolucion->copy()->addHours($horas));
    }

    // =====================================================================
    // Scopes
    // =====================================================================
    public function scopeConEstado(Builder $q, EstadoTicketCodigo|array $estados): Builder
    {
        $ids = collect(is_array($estados) ? $estados : [$estados])->map(fn ($e) => $e->id())->all();

        return $q->whereIn('estado_id', $ids);
    }

    public function scopeNoFinalizados(Builder $q): Builder
    {
        return $q->whereNotIn('estado_id', [EstadoTicketCodigo::Cerrado->id(), EstadoTicketCodigo::Cancelado->id()]);
    }

    public function scopeConPrioridad(Builder $q, PrioridadCodigo $p): Builder
    {
        return $q->where('prioridad_id', $p->id());
    }

    public function scopeDeSolicitante(Builder $q, Usuario|int $u): Builder
    {
        return $q->where('solicitante_id', $u instanceof Usuario ? $u->id : $u);
    }

    public function scopeAsignadosA(Builder $q, Usuario|int $u): Builder   // HU-19
    {
        return $q->where('tecnico_id', $u instanceof Usuario ? $u->id : $u);
    }

    public function scopeSinAsignar(Builder $q): Builder
    {
        return $q->whereNull('tecnico_id')->conEstado(EstadoTicketCodigo::Abierto);
    }

    public function scopeSlaVencido(Builder $q): Builder
    {
        return $q->conEstado(EstadoTicketCodigo::enCurso())->where('fecha_limite_resolucion', '<', now());
    }

    /** Búsqueda por código o título (HU-18). */
    public function scopeBuscar(Builder $q, ?string $texto): Builder
    {
        if (blank($texto)) {
            return $q;
        }

        return $q->where(fn (Builder $w) => $w
            ->where('codigo', 'ilike', "%{$texto}%")
            ->orWhere('titulo', 'ilike', "%{$texto}%"));
    }

    /** Lo que cada rol puede listar: HU-04 (propios), HU-19 (asignados), HU-05 (todos). */
    public function scopeVisiblePara(Builder $q, Usuario $u): Builder
    {
        return match (true) {
            $u->esAdministrador() => $q,
            $u->esTecnico()       => $q->asignadosA($u),
            default               => $q->deSolicitante($u),
        };
    }

    // =====================================================================
    // Operaciones de negocio (cada una corre con el actor informado a la BD)
    // =====================================================================

    /** HU-03: registrar incidencia. Devuelve el ticket con código y SLA ya calculados. */
    public static function registrar(array $datos, ?Usuario $actor = null): static
    {
        $actorId = $actor?->id ?? ($datos['solicitante_id'] ?? null);

        return ContextoBD::como($actorId, fn () => static::create($datos)->refresh());
    }

    /** HU-06 / HU-23: asignar o reasignar técnico (el trigger pasa a ASIGNADO si estaba ABIERTO). */
    public function asignarA(Usuario|int $tecnico, ?Usuario $actor = null): static
    {
        return $this->aplicar($actor, ['tecnico_id' => $tecnico instanceof Usuario ? $tecnico->id : $tecnico]);
    }

    /** HU-07: el técnico inicia la atención. */
    public function iniciarAtencion(?Usuario $actor = null): static
    {
        return $this->aplicar($actor, ['estado_id' => EstadoTicketCodigo::EnProceso->id()]);
    }

    /** HU-07 + HU-20: resolver registrando la solución aplicada. */
    public function resolver(string $solucion, ?Usuario $actor = null): static
    {
        return $this->aplicar($actor, [
            'estado_id'         => EstadoTicketCodigo::Resuelto->id(),
            'solucion_aplicada' => $solucion,
        ]);
    }

    /** HU-11: cerrar (habilita la encuesta CSAT). */
    public function cerrar(?Usuario $actor = null): static
    {
        return $this->aplicar($actor, ['estado_id' => EstadoTicketCodigo::Cerrado->id()]);
    }

    /** HU-16: cancelar un ticket que sigue ABIERTO. */
    public function cancelar(?Usuario $actor = null): static
    {
        return $this->aplicar($actor, ['estado_id' => EstadoTicketCodigo::Cancelado->id()]);
    }

    /** HU-17: reabrir un ticket RESUELTO dentro del plazo (pasa a EN_PROCESO). */
    public function reabrir(?Usuario $actor = null): static
    {
        return $this->aplicar($actor, ['estado_id' => EstadoTicketCodigo::EnProceso->id()]);
    }

    /** HU-24: corregir la prioridad (recalcula el SLA en la BD). */
    public function cambiarPrioridad(PrioridadCodigo|int $prioridad, ?Usuario $actor = null): static
    {
        return $this->aplicar($actor, [
            'prioridad_id' => $prioridad instanceof PrioridadCodigo ? $prioridad->id() : $prioridad,
        ]);
    }

    /** HU-08 (interno) y HU-10 (chat): publicar un mensaje en el ticket. */
    public function enviarMensaje(Usuario $autor, string $contenido, bool $interno = false): TicketMensaje
    {
        return ContextoBD::como($autor->id, fn () => $this->mensajes()->create([
            'autor_id'   => $autor->id,
            'contenido'  => $contenido,
            'es_interno' => $interno,
        ]));
    }

    /** HU-15: adjuntar una captura (JPG/PNG, máx. 5 MB; la BD valida mime y tamaño). */
    public function adjuntarImagen(Usuario $autor, UploadedFile $archivo): TicketAttachment
    {
        $ruta = $archivo->storeAs(
            "tickets/{$this->codigo}", Str::uuid() . '.' . $archivo->extension(), 'public'
        );

        try {
            return ContextoBD::como($autor->id, fn () => $this->adjuntos()->create([
                'subido_por'          => $autor->id,
                'nombre_archivo'      => $archivo->getClientOriginalName(),
                'ruta_almacenamiento' => $ruta,
                'mime_type'           => $archivo->getMimeType(),
                'tamano_bytes'        => $archivo->getSize(),
            ]));
        } catch (Throwable $e) {
            Storage::disk('public')->delete($ruta);   // no dejar archivos huérfanos
            throw $e;
        }
    }

    /** HU-12: calificar la atención (solo el solicitante, ticket CERRADO). */
    public function calificar(Usuario $usuario, int $calificacion, ?string $comentario = null): SatisfactionSurvey
    {
        return ContextoBD::como($usuario->id, fn () => $this->encuesta()->create([
            'usuario_id'   => $usuario->id,
            'calificacion' => $calificacion,
            'comentario'   => $comentario,
        ]));
    }

    /** Aplica un cambio dentro del contexto de auditoría y recarga lo que calcularon los triggers. */
    private function aplicar(?Usuario $actor, array $cambios): static
    {
        ContextoBD::como($actor?->id, fn () => $this->forceFill($cambios)->save());

        return $this->refresh();
    }
}
