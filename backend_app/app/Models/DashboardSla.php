<?php

namespace App\Models;

use Carbon\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * "Modelo" del dashboard SLA. No es Eloquent: no hay tabla propia, todo sale de
 * stored procedures (sp_dash_*). Aquí vive el cálculo de periodos y porcentajes
 * para que el controller quede delgado.
 */
class DashboardSla
{
    public const PERIODOS = ['semana', 'mes', 'anio'];

    /** Rango [desde, hasta) del periodo actual y del anterior (para la variación). */
    public static function rango(string $periodo): array
    {
        $ahora = Carbon::now();

        switch ($periodo) {
            case 'semana':
                $desde = $ahora->copy()->startOfWeek();
                $hasta = $desde->copy()->addWeek();
                $pDesde = $desde->copy()->subWeek();
                break;
            case 'anio':
                $desde = $ahora->copy()->startOfYear();
                $hasta = $desde->copy()->addYear();
                $pDesde = $desde->copy()->subYear();
                break;
            default: // mes
                $desde = $ahora->copy()->startOfMonth();
                $hasta = $desde->copy()->addMonth();
                $pDesde = $desde->copy()->subMonth();
        }

        return [
            'desde'          => $desde->toDateTimeString(),
            'hasta'          => $hasta->toDateTimeString(),
            'previo_desde'   => $pDesde->toDateTimeString(),
            'previo_hasta'   => $desde->toDateTimeString(),
        ];
    }

    /** Arma todo lo que pinta la pantalla del dashboard. */
    public static function resumen(string $periodo, ?int $servicioId, ?int $prioridadId): array
    {
        $r = self::rango($periodo);

        $actual = self::kpis($r['desde'], $r['hasta'], $servicioId, $prioridadId);
        $previo = self::kpis($r['previo_desde'], $r['previo_hasta'], $servicioId, $prioridadId);

        $variacion = ($actual['cumplimiento'] !== null && $previo['cumplimiento'] !== null)
            ? round($actual['cumplimiento'] - $previo['cumplimiento'], 1)
            : null;

        return [
            'periodo' => $periodo,
            'desde'   => $r['desde'],
            'hasta'   => $r['hasta'],
            'meta'    => self::meta(),
            'kpis'    => $actual + ['variacion' => $variacion],
            'por_prioridad' => self::porPrioridad($r['desde'], $r['hasta'], $servicioId, $prioridadId),
            'por_servicio'  => self::porServicio($r['desde'], $r['hasta'], $servicioId, $prioridadId),
            'filtros' => [
                'servicios'   => DB::table('servicios')->where('activo', 1)->orderBy('nombre')->get(['id', 'nombre']),
                'prioridades' => DB::table('prioridades')->orderBy('nivel')->get(['id', 'nombre']),
            ],
        ];
    }

    public static function meta(): int
    {
        $row = DB::selectOne("SELECT fn_param('meta_sla_pct') AS v");
        return (int) ($row->v ?? 90);
    }

    public static function kpis(string $desde, string $hasta, ?int $servicioId, ?int $prioridadId): array
    {
        $k = DB::select('CALL sp_dash_kpis(?, ?, ?, ?)', [$desde, $hasta, $servicioId, $prioridadId])[0];

        $dentro   = (int) $k->dentro_sla;
        $evaluados = (int) $k->resueltos + (int) $k->abiertos_vencidos;

        return [
            'total_tickets'     => (int) $k->total_tickets,
            'evaluados'         => $evaluados,
            'dentro_sla'        => $dentro,
            'fuera_sla'         => $evaluados - $dentro,
            'resueltos_tarde'   => (int) $k->resueltos_tarde,
            'abiertos_vencidos' => (int) $k->abiertos_vencidos,
            'mttr_min'          => $k->mttr_min !== null ? (int) $k->mttr_min : null,
            'cumplimiento'      => self::pct($dentro, $evaluados),
        ];
    }

    public static function porPrioridad(string $desde, string $hasta, ?int $servicioId, ?int $prioridadId): array
    {
        return array_map(fn ($r) => [
            'id'           => (int) $r->id,
            'codigo'       => $r->codigo,
            'nombre'       => $r->nombre,
            'color_hex'    => $r->color_hex,
            'total'        => (int) $r->total,
            'evaluados'    => (int) $r->evaluados,
            'dentro_sla'   => (int) $r->dentro_sla,
            'cumplimiento' => self::pct((int) $r->dentro_sla, (int) $r->evaluados),
        ], DB::select('CALL sp_dash_prioridad(?, ?, ?, ?)', [$desde, $hasta, $servicioId, $prioridadId]));
    }

    public static function porServicio(string $desde, string $hasta, ?int $servicioId, ?int $prioridadId): array
    {
        return array_map(fn ($r) => [
            'id'           => (int) $r->id,
            'nombre'       => $r->nombre,
            'total'        => (int) $r->total,
            'evaluados'    => (int) $r->evaluados,
            'dentro_sla'   => (int) $r->dentro_sla,
            'cumplimiento' => self::pct((int) $r->dentro_sla, (int) $r->evaluados),
        ], DB::select('CALL sp_dash_servicio(?, ?, ?, ?)', [$desde, $hasta, $servicioId, $prioridadId]));
    }

    public static function vencidos(string $desde, string $hasta, ?int $servicioId, ?int $prioridadId): array
    {
        return DB::select('CALL sp_dash_vencidos(?, ?, ?, ?)', [$desde, $hasta, $servicioId, $prioridadId]);
    }

    /** % con 1 decimal; null cuando no hay tickets evaluados (la app muestra "—"). */
    private static function pct(int $parte, int $total): ?float
    {
        return $total > 0 ? round(100 * $parte / $total, 1) : null;
    }
}