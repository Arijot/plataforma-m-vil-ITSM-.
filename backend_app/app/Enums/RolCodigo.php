<?php

namespace App\Enums;

enum RolCodigo: string
{
    case UsuarioFinal     = 'USUARIO_FINAL';
    case TecnicoTi        = 'TECNICO_TI';
    case AdministradorTi  = 'ADMINISTRADOR_TI';

    public function id(): int
    {
        return match ($this) {
            self::UsuarioFinal    => 1,
            self::TecnicoTi       => 2,
            self::AdministradorTi => 3,
        };
    }
}
