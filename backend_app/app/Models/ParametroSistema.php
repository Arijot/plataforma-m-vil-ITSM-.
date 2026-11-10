<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Facades\Cache;

class ParametroSistema extends Model
{
    protected $table = 'parametros_sistema';
    protected $primaryKey = 'clave';
    public $incrementing = false;
    protected $keyType = 'string';
    public $timestamps = false;

    protected $fillable = ['clave', 'valor', 'descripcion'];

    protected static function booted(): void
    {
        static::saved(fn (self $p) => Cache::forget("parametro:{$p->clave}"));
    }

    /** ParametroSistema::valor('plazo_reapertura_horas', 72) */
    public static function valor(string $clave, mixed $porDefecto = null): mixed
    {
        return Cache::remember(
            "parametro:{$clave}", 300,
            fn () => static::query()->whereKey($clave)->value('valor')
        ) ?? $porDefecto;
    }

    public static function entero(string $clave, int $porDefecto = 0): int
    {
        return (int) static::valor($clave, $porDefecto);
    }
}
