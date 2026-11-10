<?php

namespace App\Models;

use App\Enums\TipoEventoLog;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Historial inmutable (HU-14). Lo escriben los triggers; aquí solo se consulta. */
class TicketLog extends Model
{
    protected $table = 'ticket_logs';

    const CREATED_AT = 'creado_en';
    const UPDATED_AT = null;

    protected $guarded = ['*'];
    protected $casts = ['tipo_evento' => TipoEventoLog::class];

    protected static function booted(): void
    {
        static::updating(fn () => false);
        static::deleting(fn () => false);
    }

    public function ticket(): BelongsTo          { return $this->belongsTo(Ticket::class, 'ticket_id'); }
    public function usuario(): BelongsTo         { return $this->belongsTo(Usuario::class, 'usuario_id'); }
    public function estadoAnterior(): BelongsTo  { return $this->belongsTo(EstadoTicket::class, 'estado_anterior_id'); }
    public function estadoNuevo(): BelongsTo     { return $this->belongsTo(EstadoTicket::class, 'estado_nuevo_id'); }
    public function tecnicoAnterior(): BelongsTo { return $this->belongsTo(Usuario::class, 'tecnico_anterior_id'); }
    public function tecnicoNuevo(): BelongsTo    { return $this->belongsTo(Usuario::class, 'tecnico_nuevo_id'); }
    public function prioridadAnterior(): BelongsTo { return $this->belongsTo(Prioridad::class, 'prioridad_anterior_id'); }
    public function prioridadNueva(): BelongsTo  { return $this->belongsTo(Prioridad::class, 'prioridad_nueva_id'); }
}
