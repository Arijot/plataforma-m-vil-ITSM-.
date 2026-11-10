<?php
// routes/channels.php  (reemplaza el contenido)

use Illuminate\Support\Facades\Broadcast;
use Illuminate\Support\Facades\DB;

// Misma regla de acceso que usa la API REST
$puedeVer = function ($user, string $codigo): bool {
    $t = DB::table('v_tickets_detalle')->where('codigo', $codigo)->first();
    if (!$t) {
        return false;
    }
    return match ((int) $user->rol_id) {
        3 => true,                                   // Admin: todo
        2 => $t->tecnico_id == $user->id,            // Técnico: solo asignados
        1 => $t->solicitante_id == $user->id,        // Usuario: solo los suyos
        default => false,
    };
};

// Chat público + cambios de estado/técnico
Broadcast::channel('ticket.{codigo}', fn ($user, $codigo) => $puedeVer($user, $codigo));

// Notas internas: solo técnico y admin
Broadcast::channel('ticket.{codigo}.interno',
    fn ($user, $codigo) => $user->rol_id != 1 && $puedeVer($user, $codigo));