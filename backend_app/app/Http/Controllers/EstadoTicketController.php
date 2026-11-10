<?php
 
namespace App\Http\Controllers;
 
use Exception;
use Illuminate\Support\Facades\DB;
 
class EstadoTicketController extends Controller
{
    /**
     * GET /estados-ticket
     * Catálogo de estados (id, codigo, nombre, orden, es_final) leído de la BD.
     * Lo usa la app para armar los filtros de la lista de tickets.
     */
    public function index()
    {
        try {
            $estados = DB::table('v_estados_ticket')
                ->orderBy('orden')
                ->get(['id', 'codigo', 'nombre', 'orden', 'es_final']);
 
            return response()->json($estados, 200);
        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error al obtener los estados de ticket',
                'error' => $e->getMessage(),
            ], 500);
        }
    }
}