<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Area extends Model
{
    protected $table = 'areas';
    public $timestamps = false;

    protected $fillable = ['nombre', 'tipo', 'activo'];
    protected $casts = ['activo' => 'boolean'];

    public function usuarios(): HasMany
    {
        return $this->hasMany(Usuario::class, 'area_id');
    }

    public function scopeActivas(Builder $q): Builder
    {
        return $q->where('activo', true);
    }
}
