<?php
// app/Events/MensajeEnviado.php
namespace App\Events;

use Illuminate\Broadcasting\InteractsWithSockets;
use Illuminate\Broadcasting\PrivateChannel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcastNow;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

// ShouldBroadcastNow = se emite al instante, sin necesitar queue worker
class MensajeEnviado implements ShouldBroadcastNow
{
    use Dispatchable, InteractsWithSockets, SerializesModels;

    public function __construct(
        public string $codigo,
        public array $mensaje,
        public bool $interno = false
    ) {
    }

    /** Las notas internas viajan por un canal aparte al que el usuario final NO puede suscribirse */
    public function broadcastOn(): array
    {
        return [new PrivateChannel($this->interno
            ? "ticket.{$this->codigo}.interno"
            : "ticket.{$this->codigo}")];
    }

    public function broadcastAs(): string
    {
        return 'mensaje.nuevo';
    }

    public function broadcastWith(): array
    {
        return ['mensaje' => $this->mensaje];
    }
}