<?php

use App\Http\Controllers\AuthController;
use App\Http\Controllers\DashboardController;
use App\Http\Controllers\EstadoTicketController;
use App\Http\Controllers\NotificacionController;
use App\Http\Controllers\TicketController;
use App\Http\Controllers\UsuarioController;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

Route::post('/register', [AuthController::class, 'register']);
Route::post('/login', [AuthController::class, 'login']);

// Protected Endpoints (Requires Bearer Token)
Route::middleware('auth:sanctum')->group(function () {
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::get('/profile', [AuthController::class, 'profile']);
    Route::put('/profile', [AuthController::class, 'updateProfile']);
});

Route::middleware('auth:sanctum')->group(function () {
    // Opciones para llenar combos/spinners (Servicios y Prioridades)
    Route::get('/tickets/options', [TicketController::class, 'getFormOptions']);

    // Crear un nuevo ticket
    Route::post('/tickets', [TicketController::class, 'store']);

    Route::get('/estados-ticket', [EstadoTicketController::class, 'index']);

    // Listar mis tickets
    Route::get('/tickets/my-tickets', [TicketController::class, 'myTickets']);
    Route::get('/tickets/{idOrCodigo}', [TicketController::class, 'show']);
    Route::get('/tickets/{codigo}/mensajes',  [TicketController::class, 'mensajes']);        // polling del chat
    Route::post('/tickets/{codigo}/mensajes', [TicketController::class, 'enviarMensaje']);
    Route::post('/tickets/{codigo}/estado',   [TicketController::class, 'cambiarEstado']);
    Route::post('/tickets/{codigo}/asignar',  [TicketController::class, 'asignar']);          // solo admin
    Route::get('/tecnicos',                   [TicketController::class, 'tecnicos']);         // solo admin
    Route::post('/fcm-token',                 [TicketController::class, 'guardarTokenFcm']);
    Route::post('/tickets/{codigo}/reabrir',  [TicketController::class, 'reabrir']);
    Route::post('/tickets/{codigo}/encuesta', [TicketController::class, 'encuesta']);
    Route::post('/tickets/{codigo}/gestion',  [TicketController::class, 'gestion']);


    Route::get('/notificaciones', [NotificacionController::class, 'index']); 
    Route::get('/notificaciones/no-leidas', [NotificacionController::class, 'noLeidas']); 
    Route::post('/notificaciones/leer-todas', [NotificacionController::class, 'leerTodas']); 
    Route::post('/notificaciones/{id}/leida', [NotificacionController::class, 'marcarLeida']);
    
    // Catálogos para los combos del formulario (antes de usuarios/{id})
    Route::get('/catalogos/roles', [UsuarioController::class, 'roles']);
    Route::get('/catalogos/areas', [UsuarioController::class, 'areas']);
 
    // CRUD de usuarios (solo ADMINISTRADOR_TI, validado dentro del controller)
    Route::get('/usuarios',                 [UsuarioController::class, 'index']);
    Route::post('/usuarios',                [UsuarioController::class, 'store']);
    Route::get('/usuarios/{id}',            [UsuarioController::class, 'show'])->whereNumber('id');
    Route::put('/usuarios/{id}',            [UsuarioController::class, 'update'])->whereNumber('id');
    Route::patch('/usuarios/{id}/estado',   [UsuarioController::class, 'cambiarEstado'])->whereNumber('id');
    Route::delete('/usuarios/{id}',         [UsuarioController::class, 'destroy'])->whereNumber('id');

    Route::get('/dashboard/sla',          [DashboardController::class, 'sla']);
    Route::get('/dashboard/sla/exportar', [DashboardController::class, 'exportar']);
});