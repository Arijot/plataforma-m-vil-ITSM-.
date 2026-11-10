<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Bitácora de sesiones. Se escribe vía fn_registrar_login_* : solo lectura desde Eloquent. */
class AuditoriaSesion extends Model
{
    protected $table = 'auditoria_sesiones';

    const CREATED_AT = 'creado_en';
    const UPDATED_AT = null;

    protected $fillable = ['usuario_id', 'email_intento', 'evento', 'ip_origen', 'dispositivo', 'detalle'];

    public function usuario(): BelongsTo
    {
        return $this->belongsTo(Usuario::class, 'usuario_id');
    }
}
