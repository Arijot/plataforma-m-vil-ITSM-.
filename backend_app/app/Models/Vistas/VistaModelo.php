<?php

namespace App\Models\Vistas;

use Illuminate\Database\Eloquent\Model;
use LogicException;

/** Base para las vistas SQL de reportes: solo lectura. */
abstract class VistaModelo extends Model
{
    public $timestamps = false;
    public $incrementing = false;
    protected $guarded = ['*'];

    public function save(array $options = []): bool
    {
        throw new LogicException(static::class . ' es una vista de solo lectura.');
    }

    public function delete(): ?bool
    {
        throw new LogicException(static::class . ' es una vista de solo lectura.');
    }
}
