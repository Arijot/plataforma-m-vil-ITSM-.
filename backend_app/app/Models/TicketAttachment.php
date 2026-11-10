<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Support\Facades\Storage;

class TicketAttachment extends Model
{
    protected $table = 'ticket_attachments';

    const CREATED_AT = 'creado_en';
    const UPDATED_AT = null;

    protected $fillable = [
        'ticket_id', 'subido_por', 'nombre_archivo', 'ruta_almacenamiento', 'mime_type', 'tamano_bytes',
    ];
    protected $casts = ['tamano_bytes' => 'integer'];

    public function ticket(): BelongsTo
    {
        return $this->belongsTo(Ticket::class, 'ticket_id');
    }

    public function subidoPor(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'subido_por');
    }
    /*
    public function getUrlAttribute(): string
    {
        return Storage::disk('public')->url($this->ruta_almacenamiento);
    }
    */
    public function getTamanoKbAttribute(): float
    {
        return round($this->tamano_bytes / 1024, 1);
    }
}
