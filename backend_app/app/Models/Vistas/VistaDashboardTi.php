<?php

namespace App\Models\Vistas;

/** v_dashboard_ti: conteo de tickets por estado y prioridad (panel de control TI). */
class VistaDashboardTi extends VistaModelo
{
    protected $table = 'v_dashboard_ti';
    protected $primaryKey = null;

    protected $casts = ['orden' => 'integer', 'nivel' => 'integer', 'total' => 'integer'];
}
