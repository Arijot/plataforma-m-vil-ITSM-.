<?php
// app/Events/TicketActualizado.php
namespace App\Events;

use Illuminate\Broadcasting\InteractsWithSockets;
use Illuminate\Broadcasting\PrivateChannel;
use Illuminate\Contracts\Broadcasting\ShouldBroadcastNow;
use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class TicketActualizado implements ShouldBroadcastNow
{
    use Dispatchable, InteractsWithSockets, SerializesModels;

    public function __construct(
        public string $codigo,
        public string $estado,
        public ?int $tecnicoId,
        public ?string $tecnico
    ) {
    }

    public function broadcastOn(): array
    {
        return [new PrivateChannel("ticket.{$this->codigo}")];
    }

    public function broadcastAs(): string
    {
        return 'ticket.actualizado';
    }

    public function broadcastWith(): array
    {
        return [
            'estado' => $this->estado,
            'tecnico_id' => $this->tecnicoId,
            'tecnico' => $this->tecnico,
        ];
    }
}