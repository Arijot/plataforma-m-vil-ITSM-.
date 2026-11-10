<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Rol extends Model
{
    protected $table = 'roles';
    public $incrementing = false;       // ids fijos: 1, 2, 3
    protected $keyType = 'int';
    public $timestamps = false;

    protected $fillable = ['id', 'codigo', 'nombre', 'descripcion'];

    public function usuarios(): HasMany
    {
        return $this->hasMany(Usuario::class, 'rol_id');
    }
}
