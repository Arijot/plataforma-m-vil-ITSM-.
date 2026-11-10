<?php

namespace App\Models\Vistas;

/** v_reporte_sla: cumplimiento por servicio y prioridad (HU-13). */
class VistaReporteSla extends VistaModelo
{
    protected $table = 'v_reporte_sla';
    protected $primaryKey = null;

    protected $casts = [
        'tickets_resueltos'          => 'integer',
        'dentro_sla'                 => 'integer',
        'pct_cumplimiento_sla'       => 'float',
        'min_prom_primera_respuesta' => 'float',
        'horas_prom_resolucion'      => 'float',
    ];
}
