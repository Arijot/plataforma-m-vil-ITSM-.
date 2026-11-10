<?php

namespace App\Enums;

enum TipoNotificacion: string
{
    case NuevoTicket        = 'NUEVO_TICKET';
    case CambioEstado       = 'CAMBIO_ESTADO';
    case TicketAsignado     = 'TICKET_ASIGNADO';
    case TicketReabierto    = 'TICKET_REABIERTO';
    case NuevoMensaje       = 'NUEVO_MENSAJE';
    case SlaPorVencer       = 'SLA_POR_VENCER';
    case EncuestaDisponible = 'ENCUESTA_DISPONIBLE';
}
