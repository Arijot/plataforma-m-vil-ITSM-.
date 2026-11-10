<?php

namespace App\Models;

use App\Enums\RolCodigo;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Notifications\Notifiable;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Laravel\Sanctum\HasApiTokens;

class Usuario extends Authenticatable
{
    use HasApiTokens, HasFactory, Notifiable;

    protected $table = 'usuarios';

    public const CREATED_AT = 'creado_en';
    public const UPDATED_AT = 'actualizado_en';

    protected $fillable = [
        'rol_id', 'area_id', 'nombres', 'apellidos', 'email', 'sso_subject',
        'password_hash', 'cargo', 'telefono', 'nivel_soporte', 'activo',
    ];

    protected $hidden = ['password_hash', 'sso_subject'];

    protected $casts = [
        'activo'            => 'boolean',
        'nivel_soporte'     => 'integer',
        'intentos_fallidos' => 'integer',
        'bloqueado_hasta'   => 'datetime',
        'ultimo_login'      => 'datetime',
    ];

    public function getAuthPassword(): ?string
    {
        return $this->password_hash;
    }

    public function getAuthPasswordName(): string   // Laravel 11+
    {
        return 'password_hash';
    }

    public function getRememberTokenName(): string  // no existe remember_token
    {
        return '';
    }

    public function establecerPassword(string $plana): static
    {
        $this->password_hash = Hash::make($plana);

        return $this;
    }

    // ---------- Relaciones ----------
    public function rol(): BelongsTo
    {
        return $this->belongsTo(Rol::class, 'rol_id');
    }

    public function area(): BelongsTo
    {
        return $this->belongsTo(Area::class, 'area_id');
    }

    public function ticketsSolicitados(): HasMany
    {
        return $this->hasMany(Ticket::class, 'solicitante_id');
    }

    public function ticketsAsignados(): HasMany
    {
        return $this->hasMany(Ticket::class, 'tecnico_id');
    }

    public function mensajes(): HasMany
    {
        return $this->hasMany(TicketMensaje::class, 'autor_id');
    }

    public function dispositivos(): HasMany
    {
        return $this->hasMany(DispositivoFcm::class, 'usuario_id');
    }

    public function notificaciones(): HasMany
    {
        return $this->hasMany(Notificacion::class, 'usuario_id')->latest('creado_en');
    }

    public function notificacionesNoLeidas(): HasMany
    {
        return $this->notificaciones()->where('leida', false);
    }

    public function sesiones(): HasMany
    {
        return $this->hasMany(AuditoriaSesion::class, 'usuario_id')->latest('creado_en');
    }

    // ---------- Accessors ----------
    public function getNombreCompletoAttribute(): string
    {
        return trim("{$this->nombres} {$this->apellidos}");
    }

    public function tieneRol(RolCodigo $rol): bool
    {
        return $this->rol_id === $rol->id();
    }

    public function esUsuarioFinal(): bool  { return $this->tieneRol(RolCodigo::UsuarioFinal); }
    public function esTecnico(): bool       { return $this->tieneRol(RolCodigo::TecnicoTi); }
    public function esAdministrador(): bool { return $this->tieneRol(RolCodigo::AdministradorTi); }

    public function estaBloqueado(): bool
    {
        return $this->bloqueado_hasta !== null && $this->bloqueado_hasta->isFuture();
    }

    // ---------- Scopes ----------
    public function scopeActivos(Builder $q): Builder
    {
        return $q->where('activo', true);
    }

    public function scopeConRol(Builder $q, RolCodigo $rol): Builder
    {
        return $q->where('rol_id', $rol->id());
    }

    public function scopeTecnicos(Builder $q): Builder
    {
        return $q->activos()->conRol(RolCodigo::TecnicoTi);
    }

    public function scopeAdministradores(Builder $q): Builder
    {
        return $q->activos()->conRol(RolCodigo::AdministradorTi);
    }

    public function registrarLoginOk(?string $ip = null, ?string $dispositivo = null): void
    {
        DB::select('select fn_registrar_login_ok(?, ?, ?)', [$this->id, $ip, $dispositivo]);
    }

    /** @return bool TRUE si la cuenta quedó bloqueada tras este intento. */
    public static function registrarLoginFallido(string $email, ?string $ip = null, ?string $dispositivo = null): bool
    {
        return (bool) DB::selectOne(
            'select fn_registrar_login_fallido(?, ?, ?) as bloqueada', [$email, $ip, $dispositivo]
        )->bloqueada;
    }

    public function registrarLogout(?string $ip = null, ?string $dispositivo = null): void
    {
        DB::select('select fn_registrar_logout(?, ?, ?)', [$this->id, $ip, $dispositivo]);
    }

    /** HU-25: activar / desactivar acceso. */
    public function desactivar(): bool
    {
        return $this->forceFill(['activo' => false])->save();
    }

    public function activar(): bool
    {
        return $this->forceFill(['activo' => true, 'intentos_fallidos' => 0, 'bloqueado_hasta' => null])->save();
    }
}
