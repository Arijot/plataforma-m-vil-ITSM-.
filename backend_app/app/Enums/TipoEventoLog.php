<?php

namespace App\Enums;

enum TipoEventoLog: string
{
    case Creacion        = 'CREACION';
    case CambioEstado    = 'CAMBIO_ESTADO';
    case Asignacion      = 'ASIGNACION';
    case Reasignacion    = 'REASIGNACION';
    case CambioPrioridad = 'CAMBIO_PRIORIDAD';
    case Reapertura      = 'REAPERTURA';
    case Cancelacion     = 'CANCELACION';
}
