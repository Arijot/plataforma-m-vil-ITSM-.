<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use App\Models\Usuario;
use Carbon\Carbon;
use Exception;

class AuthController extends Controller
{
    // LOGIN
    public function login(Request $request)
    {
        $request->validate([
            'email' => 'required|email',
            'password' => 'required',
            'dispositivo' => 'nullable|string'
        ]);

        try {
            $user = Usuario::where('email', $request->email)->first();
            $ip = $request->ip();
            $dispositivo = $request->dispositivo ?? 'Mobile App';

            // 1. Validar si el usuario existe y si la contraseña coincide
            if (!$user || !Hash::check($request->password, $user->password_hash)) {
                // Registrar intento fallido
                try {
                    DB::statement('CALL sp_registrar_login_fallido(?, ?, ?)', [
                        $request->email,
                        $ip,
                        $dispositivo
                    ]);
                } catch (Exception $e) {
                    // Ignorar fallos de auditoría secundarios
                }

                // Error de credenciales inválidas para /login
                return response()->json([
                    'message' => 'Credenciales institucionales inválidas o acceso denegado por el dominio',
                    'error_code' => 'SSO-001'
                ], 401);
            }

            // 2. Validar estado del usuario
            if (!$user->activo) {
                return response()->json([
                    'message' => 'Usuario inactivo',
                    'error_code' => 'SSO-002'
                ], 403);
            }

            if ($user->bloqueado_hasta && Carbon::parse($user->bloqueado_hasta)->isFuture()) {
                return response()->json([
                    'message' => 'Cuenta bloqueada temporalmente',
                    'error_code' => 'SSO-003'
                ], 403);
            }

            // 3. Registrar login exitoso
            DB::statement('CALL sp_registrar_login_ok(?, ?, ?)', [
                $user->id,
                $ip,
                $dispositivo
            ]);

            // 4. Generar Token
            $token = $user->createToken('mobile_access_token')->plainTextToken;

            return response()->json([
                'message' => 'Login exitoso',
                'access_token' => $token,
                'user' => $user->fresh()
            ], 200);

        } catch (Exception $e) {
            // Captura cualquier falla de servidor, BD o backend solo en /login
            return response()->json([
                'message' => 'Error de comunicación con el servicio de autenticación',
                'error_code' => 'SSO-005'
            ], 500);
        }
    }

    // REGISTER
    public function register(Request $request)
    {
        $request->validate([
            'nombres' => 'required|string|max:80',
            'apellidos' => 'required|string|max:80',
            'email' => 'required|string|email|max:150|unique:usuarios',
            'password' => 'required|string|min:6',
            'rol_id' => 'required|integer',
            'area_id' => 'nullable|integer',
            'cargo' => 'nullable|string|max:80',
            'telefono' => 'nullable|string|max:20',
            'genero' => 'nullable|integer'
        ]);

        try {
            $hashedPassword = Hash::make($request->password);

            DB::statement('CALL sp_crear_usuario_auth(?, ?, ?, ?, ?, ?, ?, ?, ?)', [
                $request->rol_id,
                $request->area_id,
                $request->nombres,
                $request->apellidos,
                $request->email,
                $hashedPassword,
                $request->cargo,
                $request->telefono,
                $request->genero
            ]);

            $user = Usuario::where('email', $request->email)->first();
            $token = $user->createToken('mobile_access_token')->plainTextToken;

            return response()->json([
                'message' => 'Usuario registrado exitosamente',
                'user' => $user,
                'access_token' => $token
            ], 201);

        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error de comunicación con el servicio de autenticación',
                'error_code' => 'SSO-005'
            ], 500);
        }
    }

    // LOGOUT
    public function logout(Request $request)
    {
        try {
            $user = $request->user();

            DB::statement('CALL sp_registrar_logout(?, ?, ?)', [
                $user->id,
                $request->ip(),
                $request->header('User-Agent') ?? 'Mobile App'
            ]);

            $user->currentAccessToken()->delete();

            return response()->json([
                'message' => 'Sesión cerrada exitosamente'
            ], 200);

        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error de comunicación con el servicio de autenticación',
                'error_code' => 'SSO-005'
            ], 500);
        }
    }

    // PROFILE
    public function profile(Request $request)
    {
        try {
            return response()->json($request->user(), 200);
        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error de comunicación con el servicio de autenticación',
                'error_code' => 'SSO-005'
            ], 500);
        }
    }

    // UPDATE PROFILE
    public function updateProfile(Request $request)
    {
        $user = $request->user();

        $request->validate([
            'nombres' => 'required|string|max:80',
            'apellidos' => 'required|string|max:80',
            'telefono' => 'nullable|string|max:20',
            'genero' => 'nullable|integer'
        ]);

        try {
            DB::statement('CALL sp_actualizar_perfil(?, ?, ?, ?, ?)', [
                $user->id,
                $request->nombres,
                $request->apellidos,
                $request->telefono,
                $request->genero
            ]);

            return response()->json([
                'message' => 'Perfil actualizado exitosamente',
                'user' => $user->fresh()
            ], 200);

        } catch (Exception $e) {
            return response()->json([
                'message' => 'Error de comunicación con el servicio de autenticación',
                'error_code' => 'SSO-005'
            ], 500);
        }
    }
}