<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

/** Correlativo TCK-AAAA-NNNN. Lo administra el trigger fn_tickets_bi: solo lectura. */
class SecuenciaTicket extends Model
{
    protected $table = 'secuencias_ticket';
    protected $primaryKey = 'anio';
    public $incrementing = false;
    protected $keyType = 'int';
    public $timestamps = false;

    protected $guarded = ['*'];
    protected $casts = ['anio' => 'integer', 'ultimo' => 'integer'];
}
