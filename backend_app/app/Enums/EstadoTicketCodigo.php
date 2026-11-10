<?php

namespace App\Enums;

enum EstadoTicketCodigo: string
{
    case Abierto    = 'ABIERTO';
    case Asignado   = 'ASIGNADO';
    case EnProceso  = 'EN_PROCESO';
    case Resuelto   = 'RESUELTO';
    case Cerrado    = 'CERRADO';
    case Cancelado  = 'CANCELADO';

    public function id(): int
    {
        return match ($this) {
            self::Abierto   => 1,
            self::Asignado  => 2,
            self::EnProceso => 3,
            self::Resuelto  => 4,
            self::Cerrado   => 5,
            self::Cancelado => 6,
        };
    }

    public function esFinal(): bool
    {
        return in_array($this, [self::Cerrado, self::Cancelado], true);
    }

    public function etiqueta(): string
    {
        return match ($this) {
            self::EnProceso => 'En proceso',
            default         => ucfirst(strtolower($this->value)),
        };
    }

    /** Estados "vivos" (el ticket aún cuenta para el SLA). */
    public static function enCurso(): array
    {
        return [self::Abierto, self::Asignado, self::EnProceso];
    }
}
