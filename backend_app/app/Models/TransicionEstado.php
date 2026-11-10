<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Tabla con clave compuesta (origen, destino): modelo de solo lectura. */
class TransicionEstado extends Model
{
    protected $table = 'transiciones_estado';
    protected $primaryKey = null;
    public $incrementing = false;
    public $timestamps = false;

    protected $fillable = ['estado_origen_id', 'estado_destino_id', 'descripcion'];

    public function origen(): BelongsTo
    {
        return $this->belongsTo(EstadoTicket::class, 'estado_origen_id');
    }

    public function destino(): BelongsTo
    {
        return $this->belongsTo(EstadoTicket::class, 'estado_destino_id');
    }
}
