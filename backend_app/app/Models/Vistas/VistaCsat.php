<?php

namespace App\Models\Vistas;

/** v_csat: satisfacción del usuario por mes y servicio (objetivo específico 4). */
class VistaCsat extends VistaModelo
{
    protected $table = 'v_csat';
    protected $primaryKey = null;

    protected $casts = [
        'mes'             => 'date',
        'encuestas'       => 'integer',
        'csat_promedio'   => 'float',
        'pct_satisfechos' => 'float',
    ];
}
