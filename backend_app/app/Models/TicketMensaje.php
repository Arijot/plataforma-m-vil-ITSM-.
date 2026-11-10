<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Chat usuario<->técnico (HU-10) y notas internas (HU-08, es_interno = true). */
class TicketMensaje extends Model
{
    protected $table = 'ticket_mensajes';

    const CREATED_AT = 'creado_en';
    const UPDATED_AT = null;

    protected $fillable = ['ticket_id', 'autor_id', 'contenido', 'es_interno'];
    protected $casts = ['es_interno' => 'boolean'];

    public function ticket(): BelongsTo
    {
        return $this->belongsTo(Ticket::class, 'ticket_id');
    }

    public function autor(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'autor_id');
    }

    public function scopePublicos(Builder $q): Builder
    {
        return $q->where('es_interno', false);
    }

    public function scopeInternos(Builder $q): Builder
    {
        return $q->where('es_interno', true);
    }

    public function scopeVisiblesPara(Builder $q, Usuario $u): Builder
    {
        return $u->esUsuarioFinal() ? $q->publicos() : $q;
    }
}
