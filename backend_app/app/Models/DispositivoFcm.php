<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Token de Firebase Cloud Messaging por dispositivo Android. */
class DispositivoFcm extends Model
{
    protected $table = 'dispositivos_fcm';

    const CREATED_AT = 'registrado_en';
    const UPDATED_AT = null;

    protected $fillable = ['usuario_id', 'token_fcm', 'plataforma', 'modelo', 'activo', 'ultimo_uso'];
    protected $casts = ['activo' => 'boolean', 'ultimo_uso' => 'datetime'];

    public function usuario(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'usuario_id');
    }

    public function scopeActivos(Builder $q): Builder
    {
        return $q->where('activo', true);
    }
}
