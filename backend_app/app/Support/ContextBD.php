<?php

namespace App\Support;

use Carbon\CarbonInterface;
use Closure;
use Illuminate\Support\Facades\DB;

/**
 * Informa a los triggers de MySQL/MariaDB QUIÉN actúa y CUÁNDO ocurre el evento.
 *
 * Los triggers de soporte_db leen dos variables de sesión:
 *   @app_usuario_id    -> usuario_id de ticket_logs (trg_tickets_ai / trg_tickets_au)
 *   @app_fecha_evento  -> creado_en de logs y fechas de etapa (IFNULL(..., CURRENT_TIMESTAMP))
 *
 * Las variables @usuario de MySQL viven en la CONEXIÓN, no en la transacción. Si no se
 * limpian, el actor de una petición se "filtra" a la siguiente cuando la conexión se
 * reutiliza (Octane, PDO persistente, workers de colas). Por eso aquí se restauran SIEMPRE
 * en un `finally`.
 *
 * Uso:
 *   ContextoBD::como($actor->id, fn () => $ticket->save());
 *   ContextoBD::como($actor->id, fn () => ..., now()->subHour());   // fecha de evento explícita
 */
final class ContextoBD
{
    public const VAR_USUARIO = '@app_usuario_id';
    public const VAR_FECHA   = '@app_fecha_evento';

    /** Profundidad de anidación: solo el bloque más externo deja las variables en NULL. */
    private static int $nivel = 0;

    /**
     * @template T
     * @param  int|string|null       $usuarioId  null = sin actor (el trigger usará el solicitante o NULL).
     * @param  Closure():T           $callback
     * @param  CarbonInterface|null  $fecha      Por defecto now() de Laravel, para que los logs usen el
     *                                           mismo reloj que fecha_creacion/actualizado_en (y respeten
     *                                           Carbon::setTestNow() en los tests).
     * @return T
     */
    public static function como(int|string|null $usuarioId, Closure $callback, ?CarbonInterface $fecha = null): mixed
    {
        if (! self::soportado()) {
            return $callback();   // p. ej. SQLite en tests: no hay triggers que alimentar
        }

        // Valores del bloque exterior (null si es el más externo)
        $anterior = self::$nivel > 0 ? self::leer() : [null, null];

        self::$nivel++;
        try {
            self::escribir(
                $usuarioId === null ? null : (string) $usuarioId,
                ($fecha ?? now())->format('Y-m-d H:i:s'),
            );

            return $callback();
        } finally {
            self::$nivel--;

            try {
                self::escribir($anterior[0], $anterior[1]);
            } catch (\Throwable) {
                // Si la conexión murió no hay nada que limpiar; no ocultar la excepción original.
            }
        }
    }

    /** Actor que verán los triggers ahora mismo (null si no hay contexto). */
    public static function actual(): ?int
    {
        if (! self::soportado()) {
            return null;
        }

        $valor = self::leer()[0];

        return $valor === null ? null : (int) $valor;
    }

    private static function soportado(): bool
    {
        return in_array(DB::connection()->getDriverName(), ['mysql', 'mariadb'], true);
    }

    /** @return array{0: ?string, 1: ?string} */
    private static function leer(): array
    {
        // useReadPdo = false: las variables de sesión están en la conexión de ESCRITURA.
        $fila = DB::selectOne(
            'select ' . self::VAR_USUARIO . ' as usuario, ' . self::VAR_FECHA . ' as fecha', [], false
        );

        return [
            isset($fila->usuario) ? (string) $fila->usuario : null,
            isset($fila->fecha)   ? (string) $fila->fecha   : null,
        ];
    }

    private static function escribir(?string $usuario, ?string $fecha): void
    {
        // statement() usa la conexión de escritura; valores siempre por binding.
        DB::statement('set ' . self::VAR_USUARIO . ' = ?, ' . self::VAR_FECHA . ' = ?', [$usuario, $fecha]);
    }
}