<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

class EstadoTicket extends Model
{
    protected $table = 'estados_ticket';
    public $incrementing = false;
    protected $keyType = 'int';
    public $timestamps = false;

    protected $fillable = ['id', 'codigo', 'nombre', 'orden', 'es_final'];
    protected $casts = ['orden' => 'integer', 'es_final' => 'boolean'];

    public function tickets(): HasMany
    {
        return $this->hasMany(Ticket::class, 'estado_id');
    }

    /** Estados a los que se puede pasar desde este (tabla transiciones_estado). */
    public function destinosPermitidos(): BelongsToMany
    {
        return $this->belongsToMany(
            self::class, 'transiciones_estado', 'estado_origen_id', 'estado_destino_id'
        )->withPivot('descripcion');
    }

    public function puedePasarA(self|int $destino): bool
    {
        $id = $destino instanceof self ? $destino->id : $destino;

        return $this->destinosPermitidos()->whereKey($id)->exists();
    }
}
