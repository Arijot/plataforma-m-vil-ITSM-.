<?php
// app/Services/FcmService.php
//
// Instalación:  composer require kreait/laravel-firebase
// .env:         FIREBASE_CREDENTIALS=storage/app/firebase-credentials.json
//               (JSON de cuenta de servicio: Firebase Console > Configuración > Cuentas de servicio)

namespace App\Services;

use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Kreait\Firebase\Contract\Messaging;
use Kreait\Firebase\Exception\Messaging\NotFound;
use Kreait\Firebase\Messaging\AndroidConfig;
use Kreait\Firebase\Messaging\CloudMessage;

class FcmService
{
    public function __construct(private Messaging $messaging)
    {
    }

    /**
     * Envía por FCM todas las notificaciones pendientes (enviada_fcm = 0) de un ticket.
     * Las filas las crean los triggers de la BD y los sp_notificar del controlador.
     */
    public function enviarPendientes(int $ticketId): void
    {
        $pendientes = DB::table('notificaciones as n')
            ->leftJoin('tickets as t', 't.id', '=', 'n.ticket_id')
            ->where('n.ticket_id', $ticketId)
            ->where('n.enviada_fcm', 0)
            ->get(['n.id', 'n.usuario_id', 'n.tipo', 'n.titulo', 'n.cuerpo', 't.codigo as ticket_codigo']);

        foreach ($pendientes as $n) {
            $tokens = DB::table('dispositivos_fcm')
                ->where('usuario_id', $n->usuario_id)
                ->where('activo', 1)
                ->pluck('token_fcm');

            foreach ($tokens as $token) {
                try {
                    // Mensaje solo de datos: la app arma la notificación (funciona en 1er y 2do plano)
                    $message = CloudMessage::withTarget('token', $token)
                        ->withData([
                            'notificacion_id' => (string) $n->id,
                            'ticket_codigo'   => (string) $n->ticket_codigo,
                            'tipo'            => (string) $n->tipo,
                            'titulo'          => (string) $n->titulo,
                            'cuerpo'          => (string) $n->cuerpo,
                        ])
                        ->withAndroidConfig(AndroidConfig::fromArray(['priority' => 'high']));

                    $this->messaging->send($message);
                } catch (NotFound $e) {
                    // Token ya no existe: se desactiva
                    DB::table('dispositivos_fcm')->where('token_fcm', $token)->update(['activo' => 0]);
                } catch (\Throwable $e) {
                    Log::warning('FCM error: ' . $e->getMessage());
                }
            }

            DB::table('notificaciones')->where('id', $n->id)->update(['enviada_fcm' => 1]);
        }
    }
}