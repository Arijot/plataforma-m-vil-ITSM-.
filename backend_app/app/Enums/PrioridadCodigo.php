<?php

namespace App\Enums;

enum PrioridadCodigo: string
{
    case Critica = 'CRITICA';
    case Alta    = 'ALTA';
    case Media   = 'MEDIA';
    case Baja    = 'BAJA';

    public function id(): int
    {
        return match ($this) {
            self::Critica => 1,
            self::Alta    => 2,
            self::Media   => 3,
            self::Baja    => 4,
        };
    }
}
