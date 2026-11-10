<?php

namespace App\Models;

use App\Enums\TipoNotificacion;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * Bandeja de notificaciones (HU-09, HU-21). Las crean los triggers.
 * Un job de cola puede leer pendientesFcm() y enviarlas por Firebase Cloud Messaging.
 */
class Notificacion extends Model
{
    protected $table = 'notificaciones';

    const CREATED_AT = 'creado_en';
    const UPDATED_AT = null;

    protected $fillable = ['usuario_id', 'ticket_id', 'tipo', 'titulo', 'cuerpo', 'leida', 'enviada_fcm', 'leida_en'];
    protected $casts = [
        'tipo'        => TipoNotificacion::class,
        'leida'       => 'boolean',
        'enviada_fcm' => 'boolean',
        'leida_en'    => 'datetime',
    ];

    public function usuario(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'usuario_id');
    }

    public function ticket(): BelongsTo
    {
        return $this->belongsTo(Ticket::class, 'ticket_id');
    }

    public function scopeNoLeidas(Builder $q): Builder
    {
        return $q->where('leida', false);
    }

    public function scopePendientesFcm(Builder $q): Builder
    {
        return $q->where('enviada_fcm', false)->orderBy('id');
    }

    public function marcarComoLeida(): bool
    {
        return $this->leida ? true : $this->forceFill(['leida' => true, 'leida_en' => now()])->save();
    }

    public function marcarEnviadaFcm(): bool
    {
        return $this->forceFill(['enviada_fcm' => true])->save();
    }

    public static function marcarTodasLeidas(Usuario $usuario): int
    {
        return static::where('usuario_id', $usuario->id)->where('leida', false)
            ->update(['leida' => true, 'leida_en' => now()]);
    }
}
