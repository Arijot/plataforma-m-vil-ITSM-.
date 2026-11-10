<?php

namespace App\Models\Vistas;

/** v_tickets_detalle: listado enriquecido con SLA (HU-05, HU-22). */
class VistaTicketDetalle extends VistaModelo
{
    protected $table = 'v_tickets_detalle';
    protected $primaryKey = 'id';

    protected $casts = [
        'sla_vencido'             => 'boolean',
        'veces_reabierto'         => 'integer',
        'fecha_creacion'          => 'datetime',
        'fecha_asignacion'        => 'datetime',
        'fecha_primera_respuesta' => 'datetime',
        'fecha_resolucion'        => 'datetime',
        'fecha_cierre'            => 'datetime',
        'fecha_limite_respuesta'  => 'datetime',
        'fecha_limite_resolucion' => 'datetime',
        // tiempo_restante_sla llega como intervalo de PostgreSQL (texto: "1 day 02:03:04")
    ];
}
