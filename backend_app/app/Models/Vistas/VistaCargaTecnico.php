<?php

namespace App\Models\Vistas;

/** v_carga_tecnicos: carga activa por técnico (HU-19, HU-23). */
class VistaCargaTecnico extends VistaModelo
{
    protected $table = 'v_carga_tecnicos';
    protected $primaryKey = 'tecnico_id';

    protected $casts = [
        'tecnico_id'    => 'integer',
        'nivel_soporte' => 'integer',
        'asignados'     => 'integer',
        'en_proceso'    => 'integer',
        'carga_activa'  => 'integer',
    ];
}
