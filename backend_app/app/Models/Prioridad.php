<?php

namespace App\Models;

use Carbon\CarbonInterface;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Prioridad extends Model
{
    protected $table = 'prioridades';
    public $incrementing = false;
    protected $keyType = 'int';
    public $timestamps = false;

    protected $fillable = [
        'id', 'codigo', 'nombre', 'nivel',
        'minutos_respuesta', 'minutos_resolucion', 'color_hex',
    ];
    protected $casts = [
        'nivel'              => 'integer',
        'minutos_respuesta'  => 'integer',
        'minutos_resolucion' => 'integer',
    ];

    public function tickets(): HasMany
    {
        return $this->hasMany(Ticket::class, 'prioridad_id');
    }

    /** Fecha límite de resolución (SLA) si el ticket se creara en $desde. */
    public function limiteResolucion(CarbonInterface $desde): CarbonInterface
    {
        return $desde->copy()->addMinutes($this->minutos_resolucion);
    }

    public function limiteRespuesta(CarbonInterface $desde): CarbonInterface
    {
        return $desde->copy()->addMinutes($this->minutos_respuesta);
    }

    public function getHorasResolucionAttribute(): float
    {
        return round($this->minutos_resolucion / 60, 2);
    }
}
