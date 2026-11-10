<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Encuesta de satisfacción CSAT (1-5), una por ticket cerrado (HU-12). */
class SatisfactionSurvey extends Model
{
    protected $table = 'satisfaction_surveys';

    const CREATED_AT = 'creado_en';
    const UPDATED_AT = null;

    protected $fillable = ['ticket_id', 'usuario_id', 'calificacion', 'comentario'];
    protected $casts = ['calificacion' => 'integer'];

    public function ticket(): BelongsTo
    {
        return $this->belongsTo(Ticket::class, 'ticket_id');
    }

    public function usuario(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'usuario_id');
    }

    public function getEsSatisfechoAttribute(): bool
    {
        return $this->calificacion >= 4;
    }
}
