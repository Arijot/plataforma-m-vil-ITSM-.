<?php

namespace App\Http\Controllers;

use App\Models\Usuario;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Validation\Rule;
use Throwable;

/**
 * CRUD de usuarios (solo ADMINISTRADOR_TI).
 * Toda la lógica de datos vive en stored procedures (ver sql/usuarios_crud.sql).
 */
class UsuarioController extends Controller
{
    private const ROL_ADMIN = 'ADMINISTRADOR_TI';

    // ------------------------------------------------------------------
    // LISTAR  GET /api/usuarios?buscar=&rol_id=&activo=&page=&per_page=
    // ------------------------------------------------------------------
    public function index(Request $request): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        $request->validate([
            'buscar'   => 'nullable|string|max:100',
            'rol_id'   => 'nullable|integer',
            'activo'   => 'nullable|in:0,1',
            'page'     => 'nullable|integer|min:1',
            'per_page' => 'nullable|integer|min:1|max:200',
        ]);

        try {
            $perPage = (int) $request->query('per_page', 50);
            $page    = (int) $request->query('page', 1);

            $rows = DB::select('CALL sp_usuarios_listar(?, ?, ?, ?, ?)', [
                $request->filled('buscar') ? trim($request->query('buscar')) : null,
                $request->filled('rol_id') ? (int) $request->query('rol_id') : null,
                $request->filled('activo') ? (int) $request->query('activo') : null,
                $perPage,
                ($page - 1) * $perPage,
            ]);

            $total = count($rows) > 0 ? (int) $rows[0]->total_registros : 0;

            return response()->json([
                'data'     => array_map(fn ($r) => $this->formatear($r), $rows),
                'total'    => $total,
                'page'     => $page,
                'per_page' => $perPage,
            ], 200);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    // ------------------------------------------------------------------
    // DETALLE  GET /api/usuarios/{id}
    // ------------------------------------------------------------------
    public function show(Request $request, int $id): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        try {
            $usuario = $this->obtener($id);
            if (!$usuario) {
                return $this->noEncontrado();
            }
            return response()->json($usuario, 200);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    // ------------------------------------------------------------------
    // CREAR  POST /api/usuarios
    // ------------------------------------------------------------------
    public function store(Request $request): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        $request->validate([
            'nombres'       => 'required|string|max:80',
            'apellidos'     => 'required|string|max:80',
            'email'         => 'required|string|email|max:150|unique:usuarios,email',
            'password'      => 'required|string|min:6|max:72',
            'rol_id'        => 'required|integer|exists:roles,id',
            'area_id'       => 'nullable|integer|exists:areas,id',
            'cargo'         => 'nullable|string|max:80',
            'telefono'      => 'nullable|string|max:20',
            'nivel_soporte' => 'nullable|integer|between:1,3',
            'genero'        => 'nullable|integer|in:1,2',
            'activo'        => 'nullable|boolean',
        ]);

        try {
            $res = DB::select('CALL sp_usuario_crear(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)', [
                $request->rol_id,
                $request->area_id,
                $request->nombres,
                $request->apellidos,
                $request->email,
                Hash::make($request->password),
                $request->cargo,
                $request->telefono,
                $request->nivel_soporte,
                $request->genero,
                $request->has('activo') ? (int) $request->boolean('activo') : 1,
            ]);

            $nuevoId = (int) $res[0]->id;

            return response()->json([
                'message' => 'Usuario creado exitosamente',
                'user'    => $this->obtener($nuevoId),
            ], 201);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    // ------------------------------------------------------------------
    // ACTUALIZAR  PUT /api/usuarios/{id}
    // password es opcional: si viene, se cambia.
    // ------------------------------------------------------------------
    public function update(Request $request, int $id): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        $request->validate([
            'nombres'       => 'required|string|max:80',
            'apellidos'     => 'required|string|max:80',
            'email'         => ['required', 'string', 'email', 'max:150',
                                Rule::unique('usuarios', 'email')->ignore($id)],
            'password'      => 'nullable|string|min:6|max:72',
            'rol_id'        => 'required|integer|exists:roles,id',
            'area_id'       => 'nullable|integer|exists:areas,id',
            'cargo'         => 'nullable|string|max:80',
            'telefono'      => 'nullable|string|max:20',
            'nivel_soporte' => 'nullable|integer|between:1,3',
            'genero'        => 'nullable|integer|in:1,2',
            'activo'        => 'nullable|boolean',
        ]);

        try {
            $actual = $this->obtener($id);
            if (!$actual) {
                return $this->noEncontrado();
            }

            $activo = $request->has('activo') ? (int) $request->boolean('activo') : (int) $actual->activo;

            // Un admin no puede quitarse su propio rol ni desactivarse
            if ($id === (int) $request->user()->id) {
                if ((int) $request->rol_id !== (int) $actual->rol_id || $activo === 0) {
                    return response()->json([
                        'message'    => 'No puedes cambiar tu propio rol ni desactivar tu propia cuenta',
                        'error_code' => 'USR-003',
                    ], 422);
                }
            }

            DB::statement('CALL sp_usuario_actualizar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)', [
                $id,
                $request->rol_id,
                $request->area_id,
                $request->nombres,
                $request->apellidos,
                $request->email,
                $request->cargo,
                $request->telefono,
                $request->nivel_soporte,
                $request->genero,
                $activo,
            ]);

            if ($request->filled('password')) {
                DB::statement('CALL sp_usuario_cambiar_password(?, ?)', [
                    $id,
                    Hash::make($request->password),
                ]);
                $this->revocarTokens($id);
            }

            if ($activo === 0) {
                $this->revocarTokens($id);
            }

            return response()->json([
                'message' => 'Usuario actualizado exitosamente',
                'user'    => $this->obtener($id),
            ], 200);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    // ------------------------------------------------------------------
    // ACTIVAR/DESACTIVAR  PATCH /api/usuarios/{id}/estado   {activo: bool}
    // ------------------------------------------------------------------
    public function cambiarEstado(Request $request, int $id): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        $request->validate(['activo' => 'required|boolean']);

        try {
            $activo = (int) $request->boolean('activo');

            if ($id === (int) $request->user()->id && $activo === 0) {
                return response()->json([
                    'message'    => 'No puedes desactivar tu propia cuenta',
                    'error_code' => 'USR-003',
                ], 422);
            }

            DB::statement('CALL sp_usuario_cambiar_estado(?, ?)', [$id, $activo]);

            if ($activo === 0) {
                $this->revocarTokens($id);
            }

            return response()->json([
                'message' => $activo ? 'Usuario activado' : 'Usuario desactivado',
                'user'    => $this->obtener($id),
            ], 200);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    // ------------------------------------------------------------------
    // ELIMINAR  DELETE /api/usuarios/{id}
    // ------------------------------------------------------------------
    public function destroy(Request $request, int $id): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }

        try {
            if ($id === (int) $request->user()->id) {
                return response()->json([
                    'message'    => 'No puedes eliminar tu propia cuenta',
                    'error_code' => 'USR-003',
                ], 422);
            }

            if (!$this->obtener($id)) {
                return $this->noEncontrado();
            }

            $this->revocarTokens($id);
            DB::statement('CALL sp_usuario_eliminar(?)', [$id]);

            return response()->json(['message' => 'Usuario eliminado exitosamente'], 200);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    // ------------------------------------------------------------------
    // CATÁLOGOS  GET /api/catalogos/roles  |  GET /api/catalogos/areas
    // ------------------------------------------------------------------
    public function roles(Request $request): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }
        try {
            return response()->json(DB::select('CALL sp_roles_listar()'), 200);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    public function areas(Request $request): JsonResponse
    {
        if ($denegado = $this->soloAdmin($request)) {
            return $denegado;
        }
        try {
            return response()->json(DB::select('CALL sp_areas_listar()'), 200);
        } catch (Throwable $e) {
            return $this->error($e);
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /** Devuelve una respuesta 403 si el usuario autenticado no es administrador. */
    private function soloAdmin(Request $request): ?JsonResponse
    {
        $codigo = DB::table('roles')->where('id', $request->user()->rol_id)->value('codigo');

        if ($codigo !== self::ROL_ADMIN) {
            return response()->json([
                'message'    => 'No tienes permisos para gestionar usuarios',
                'error_code' => 'USR-001',
            ], 403);
        }
        return null;
    }

    private function obtener(int $id): ?object
    {
        $rows = DB::select('CALL sp_usuario_obtener(?)', [$id]);
        return count($rows) ? $this->formatear($rows[0]) : null;
    }

    /** Normaliza tipos para que Android (Gson) los lea sin problemas. */
    private function formatear(object $r): object
    {
        $r->id            = (int) $r->id;
        $r->rol_id        = (int) $r->rol_id;
        $r->area_id       = $r->area_id !== null ? (int) $r->area_id : null;
        $r->nivel_soporte = $r->nivel_soporte !== null ? (int) $r->nivel_soporte : null;
        $r->genero        = $r->genero !== null ? (int) $r->genero : null;
        $r->activo        = (bool) $r->activo;
        unset($r->total_registros);
        return $r;
    }

    private function revocarTokens(int $id): void
    {
        $u = Usuario::find($id);
        if ($u) {
            $u->tokens()->delete();
        }
    }

    private function noEncontrado(): JsonResponse
    {
        return response()->json([
            'message'    => 'Usuario no encontrado',
            'error_code' => 'USR-004',
        ], 404);
    }

    /**
     * Traduce errores de BD:
     *  - 1644 = SIGNAL de los SP (regla de negocio)  -> 422 con el mensaje del SP
     *  - 1062 = duplicado                            -> 422
     *  - 1451 = FK                                   -> 409
     */
    private function error(Throwable $e): JsonResponse
    {
        if ($e instanceof QueryException) {
            $codigo  = $e->errorInfo[1] ?? null;
            $mensaje = $e->errorInfo[2] ?? '';

            if ($codigo === 1644) {
                $status = str_contains($mensaje, 'no existe') && str_contains($mensaje, 'usuario') ? 404 : 422;
                if (str_contains($mensaje, 'Desactívelo')) {
                    $status = 409;
                }
                return response()->json(['message' => $mensaje, 'error_code' => 'USR-002'], $status);
            }
            if ($codigo === 1062) {
                return response()->json([
                    'message'    => 'Ya existe un usuario con ese correo',
                    'error_code' => 'USR-002',
                ], 422);
            }
            if ($codigo === 1451) {
                return response()->json([
                    'message'    => 'El usuario tiene registros asociados; desactívelo en lugar de eliminarlo',
                    'error_code' => 'USR-002',
                ], 409);
            }
        }

        report($e);
        return response()->json([
            'message'    => 'Error de comunicación con el servicio de usuarios',
            'error_code' => 'USR-005',
        ], 500);
    }
}