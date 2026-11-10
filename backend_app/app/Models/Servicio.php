<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Servicio afectado (catálogo de servicios ITIL). */
class Servicio extends Model
{
    protected $table = 'servicios';
    public $incrementing = false;       // ids fijos del catálogo
    protected $keyType = 'int';
    public $timestamps = false;

    protected $fillable = ['id', 'codigo', 'nombre', 'categoria', 'descripcion', 'activo'];
    protected $casts = ['activo' => 'boolean'];

    public function tickets(): HasMany
    {
        return $this->hasMany(Ticket::class, 'servicio_id');
    }

    public function scopeActivos(Builder $q): Builder
    {
        return $q->where('activo', true);
    }
}
