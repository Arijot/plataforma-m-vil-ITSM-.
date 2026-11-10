<?php

namespace App\Http\Controllers;

use App\Models\DashboardSla;
use Carbon\Carbon;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Style\Alignment;
use PhpOffice\PhpSpreadsheet\Style\Fill;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;
use Symfony\Component\HttpFoundation\StreamedResponse;
use Throwable;

/**
 * Dashboard de SLA (solo ADMINISTRADOR_TI, rol_id = 3).
 *   GET /api/dashboard/sla?periodo=semana|mes|anio&servicio_id=&prioridad_id=
 *   GET /api/dashboard/sla/exportar?...mismos filtros...   -> .xlsx
 *
 * Requiere:  composer require phpoffice/phpspreadsheet
 */
class DashboardController extends Controller
{
    private const MORADO = '7E57C2';

    public function sla(Request $request): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        [$periodo, $servicioId, $prioridadId] = $this->filtros($request);

        try {
            return response()->json(DashboardSla::resumen($periodo, $servicioId, $prioridadId), 200);
        } catch (Throwable $e) {
            report($e);
            return response()->json([
                'message' => 'Error al calcular el dashboard',
                'error'   => $e->getMessage(),
            ], 500);
        }
    }

    public function exportar(Request $request)
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        [$periodo, $servicioId, $prioridadId] = $this->filtros($request);

        try {
            $datos    = DashboardSla::resumen($periodo, $servicioId, $prioridadId);
            $vencidos = DashboardSla::vencidos($datos['desde'], $datos['hasta'], $servicioId, $prioridadId);

            $libro   = $this->construirExcel($datos, $vencidos, $request->user());
            $archivo = 'reporte_sla_' . $periodo . '_' . Carbon::now()->format('Ymd_His') . '.xlsx';

            return new StreamedResponse(function () use ($libro) {
                (new Xlsx($libro))->save('php://output');
            }, 200, [
                'Content-Type'        => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                'Content-Disposition' => 'attachment; filename="' . $archivo . '"',
                'Cache-Control'       => 'max-age=0, no-cache, must-revalidate',
            ]);
        } catch (Throwable $e) {
            report($e);
            return response()->json([
                'message' => 'No se pudo generar el reporte',
                'error'   => $e->getMessage(),
            ], 500);
        }
    }

    // ------------------------------------------------------------------ Excel

    private function construirExcel(array $d, array $vencidos, $usuario): Spreadsheet
    {
        $k = $d['kpis'];
        $libro = new Spreadsheet();

        // ---- Hoja 1: Resumen
        $h = $libro->getActiveSheet()->setTitle('Resumen');
        $h->setCellValue('A1', 'Reporte ejecutivo de SLA');
        $h->getStyle('A1')->getFont()->setBold(true)->setSize(16);
        $h->setCellValue('A2', 'Periodo: ' . substr($d['desde'], 0, 10) . ' al ' . substr(Carbon::parse($d['hasta'])->subSecond()->toDateString(), 0, 10)
            . '  (' . $d['periodo'] . ')');
        $h->setCellValue('A3', 'Generado: ' . Carbon::now()->format('Y-m-d H:i') . '  por ' . $usuario->nombres . ' ' . $usuario->apellidos);

        $filas = [
            ['Indicador', 'Valor'],
            ['Cumplimiento global (%)', $k['cumplimiento']],
            ['Meta (%)', $d['meta']],
            ['Variación vs periodo anterior (pp)', $k['variacion']],
            ['Tickets creados en el periodo', $k['total_tickets']],
            ['Tickets evaluados', $k['evaluados']],
            ['Atendidos dentro de SLA', $k['dentro_sla']],
            ['Fuera de SLA (total)', $k['fuera_sla']],
            ['   · Resueltos tarde', $k['resueltos_tarde']],
            ['   · Abiertos ya vencidos', $k['abiertos_vencidos']],
            ['MTTR (horas)', $k['mttr_min'] !== null ? round($k['mttr_min'] / 60, 2) : null],
        ];
        $h->fromArray($filas, null, 'A5');
        $this->encabezado($h, 'A5:B5');
        $h->getColumnDimension('A')->setWidth(44);
        $h->getColumnDimension('B')->setWidth(18);
        $h->getStyle('B6:B15')->getAlignment()->setHorizontal(Alignment::HORIZONTAL_RIGHT);

        // ---- Hoja 2: por prioridad
        $this->hojaTabla($libro, 'Por prioridad',
            ['Prioridad', 'Tickets', 'Evaluados', 'Dentro SLA', 'Cumplimiento %'],
            array_map(fn ($p) => [$p['nombre'], $p['total'], $p['evaluados'], $p['dentro_sla'], $p['cumplimiento']], $d['por_prioridad']),
            $d['meta'], 'E');

        // ---- Hoja 3: por servicio
        $this->hojaTabla($libro, 'Por servicio',
            ['Servicio', 'Tickets', 'Evaluados', 'Dentro SLA', 'Cumplimiento %'],
            array_map(fn ($s) => [$s['nombre'], $s['total'], $s['evaluados'], $s['dentro_sla'], $s['cumplimiento']], $d['por_servicio']),
            $d['meta'], 'E');

        // ---- Hoja 4: detalle para auditoría
        $this->hojaTabla($libro, 'Fuera de SLA (auditoría)',
            ['Código', 'Título', 'Servicio', 'Prioridad', 'Estado', 'Técnico', 'Creado', 'Límite SLA', 'Resuelto', 'Exceso (h)', 'Tipo'],
            array_map(fn ($v) => [
                $v->codigo, $v->titulo, $v->servicio, $v->prioridad, $v->estado, $v->tecnico ?: 'Sin asignar',
                $v->fecha_creacion, $v->fecha_limite_resolucion, $v->fecha_resolucion ?: '—',
                round(((int) $v->minutos_exceso) / 60, 1), $v->tipo,
            ], $vencidos));

        $libro->setActiveSheetIndex(0);
        return $libro;
    }

    private function hojaTabla(Spreadsheet $libro, string $titulo, array $cabecera, array $filas, ?int $meta = null, ?string $colPct = null): void
    {
        $h = $libro->createSheet()->setTitle(mb_substr($titulo, 0, 31));
        $h->fromArray($cabecera, null, 'A1');
        $ultima = chr(ord('A') + count($cabecera) - 1);
        $this->encabezado($h, "A1:{$ultima}1");

        if ($filas) {
            $h->fromArray($filas, null, 'A2');
        }

        // Si hay meta, el % que no la alcanza se pinta en rojo
        if ($meta !== null && $colPct !== null) {
            foreach ($filas as $i => $fila) {
                $pct = $fila[count($fila) - 1];
                if ($pct !== null && $pct < $meta) {
                    $h->getStyle($colPct . ($i + 2))->getFont()->getColor()->setRGB('C62828');
                }
            }
        }

        foreach (range('A', $ultima) as $col) {
            $h->getColumnDimension($col)->setAutoSize(true);
        }
        $h->freezePane('A2');
    }

    private function encabezado($hoja, string $rango): void
    {
        $hoja->getStyle($rango)->applyFromArray([
            'font' => ['bold' => true, 'color' => ['rgb' => 'FFFFFF']],
            'fill' => ['fillType' => Fill::FILL_SOLID, 'startColor' => ['rgb' => self::MORADO]],
        ]);
    }

    // ------------------------------------------------------------------ helpers

    private function soloAdmin(Request $request): ?JsonResponse
    {
        if ((int) $request->user()->rol_id !== 3) {
            return response()->json(['message' => 'Solo el administrador de TI puede ver el dashboard'], 403);
        }
        return null;
    }

    /** @return array{0:string,1:?int,2:?int} */
    private function filtros(Request $request): array
    {
        $request->validate([
            'periodo'      => 'nullable|string|in:' . implode(',', DashboardSla::PERIODOS),
            'servicio_id'  => 'nullable|integer|exists:servicios,id',
            'prioridad_id' => 'nullable|integer|exists:prioridades,id',
        ]);

        return [
            $request->query('periodo', 'mes'),
            $request->filled('servicio_id') ? (int) $request->query('servicio_id') : null,
            $request->filled('prioridad_id') ? (int) $request->query('prioridad_id') : null,
        ];
    }
}