-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Versión del servidor:         8.4.3 - MySQL Community Server - GPL
-- SO del servidor:              Win64
-- HeidiSQL Versión:             12.8.0.6908
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Volcando estructura de base de datos para soporte_db
CREATE DATABASE IF NOT EXISTS `soporte_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `soporte_db`;

-- Volcando estructura para tabla soporte_db.areas
CREATE TABLE IF NOT EXISTS `areas` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nombre` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `nombre` (`nombre`),
  CONSTRAINT `areas_chk_1` CHECK ((`tipo` in (_utf8mb4'ASISTENCIAL',_utf8mb4'ADMINISTRATIVA',_utf8mb4'APOYO',_utf8mb4'TI')))
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.auditoria_sesiones
CREATE TABLE IF NOT EXISTS `auditoria_sesiones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint DEFAULT NULL,
  `email_intento` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `evento` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_origen` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dispositivo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detalle` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `ix_auditoria_usuario` (`usuario_id`,`creado_en` DESC),
  CONSTRAINT `fk_aud_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `auditoria_sesiones_chk_1` CHECK ((`evento` in (_utf8mb4'LOGIN_OK',_utf8mb4'LOGIN_FALLIDO',_utf8mb4'LOGOUT',_utf8mb4'BLOQUEO_CUENTA',_utf8mb4'TOKEN_EXPIRADO')))
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.cache
CREATE TABLE IF NOT EXISTS `cache` (
  `key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiration` int NOT NULL,
  PRIMARY KEY (`key`),
  KEY `cache_expiration_index` (`expiration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.cache_locks
CREATE TABLE IF NOT EXISTS `cache_locks` (
  `key` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expiration` int NOT NULL,
  PRIMARY KEY (`key`),
  KEY `cache_locks_expiration_index` (`expiration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.dispositivos_fcm
CREATE TABLE IF NOT EXISTS `dispositivos_fcm` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `token_fcm` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plataforma` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ANDROID',
  `modelo` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `registrado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ultimo_uso` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token_fcm` (`token_fcm`),
  KEY `ix_dispositivos_usuario` (`usuario_id`),
  CONSTRAINT `fk_fcm_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE,
  CONSTRAINT `dispositivos_fcm_chk_1` CHECK ((`plataforma` = _utf8mb4'ANDROID'))
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.estados_ticket
CREATE TABLE IF NOT EXISTS `estados_ticket` (
  `id` smallint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `orden` smallint NOT NULL,
  `es_final` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.failed_jobs
CREATE TABLE IF NOT EXISTS `failed_jobs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `connection` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `queue` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `exception` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `failed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `failed_jobs_uuid_unique` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para función soporte_db.fn_es_ultimo_admin
DELIMITER //
CREATE FUNCTION `fn_es_ultimo_admin`(p_id BIGINT) RETURNS tinyint(1)
    READS SQL DATA
BEGIN
    DECLARE v_es_admin INT DEFAULT 0;
    DECLARE v_otros INT DEFAULT 0;

    SELECT COUNT(*) INTO v_es_admin
    FROM usuarios u JOIN roles r ON r.id = u.rol_id
    WHERE u.id = p_id AND u.activo = 1 AND r.codigo = 'ADMINISTRADOR_TI';

    IF v_es_admin = 0 THEN
        RETURN 0;
    END IF;

    SELECT COUNT(*) INTO v_otros
    FROM usuarios u JOIN roles r ON r.id = u.rol_id
    WHERE u.id <> p_id AND u.activo = 1 AND r.codigo = 'ADMINISTRADOR_TI';

    RETURN IF(v_otros = 0, 1, 0);
END//
DELIMITER ;

-- Volcando estructura para función soporte_db.fn_param
DELIMITER //
CREATE FUNCTION `fn_param`(p_clave VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci) RETURNS int
    READS SQL DATA
BEGIN
    DECLARE v_val INT;
    SELECT CAST(valor AS SIGNED) INTO v_val
    FROM parametros_sistema
    WHERE clave = p_clave COLLATE utf8mb4_unicode_ci
    LIMIT 1;
    RETURN v_val;
END//
DELIMITER ;

-- Volcando estructura para tabla soporte_db.jobs
CREATE TABLE IF NOT EXISTS `jobs` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `queue` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payload` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `attempts` tinyint unsigned NOT NULL,
  `reserved_at` int unsigned DEFAULT NULL,
  `available_at` int unsigned NOT NULL,
  `created_at` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `jobs_queue_index` (`queue`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.job_batches
CREATE TABLE IF NOT EXISTS `job_batches` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_jobs` int NOT NULL,
  `pending_jobs` int NOT NULL,
  `failed_jobs` int NOT NULL,
  `failed_job_ids` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `options` mediumtext COLLATE utf8mb4_unicode_ci,
  `cancelled_at` int DEFAULT NULL,
  `created_at` int NOT NULL,
  `finished_at` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.migrations
CREATE TABLE IF NOT EXISTS `migrations` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `migration` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `batch` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.notificaciones
CREATE TABLE IF NOT EXISTS `notificaciones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint NOT NULL,
  `ticket_id` bigint DEFAULT NULL,
  `tipo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `titulo` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cuerpo` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `leida` tinyint(1) NOT NULL DEFAULT '0',
  `enviada_fcm` tinyint(1) NOT NULL DEFAULT '0',
  `creado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `leida_en` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_not_tic` (`ticket_id`),
  KEY `ix_notif_usuario` (`usuario_id`,`leida`,`creado_en` DESC),
  KEY `ix_notif_pendientes_fcm` (`id`,`enviada_fcm`),
  CONSTRAINT `fk_not_tic` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_not_usu` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE,
  CONSTRAINT `notificaciones_chk_1` CHECK ((`tipo` in (_utf8mb4'NUEVO_TICKET',_utf8mb4'CAMBIO_ESTADO',_utf8mb4'TICKET_ASIGNADO',_utf8mb4'TICKET_REABIERTO',_utf8mb4'NUEVO_MENSAJE',_utf8mb4'SLA_POR_VENCER',_utf8mb4'ENCUESTA_DISPONIBLE')))
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.parametros_sistema
CREATE TABLE IF NOT EXISTS `parametros_sistema` (
  `clave` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valor` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`clave`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.password_reset_tokens
CREATE TABLE IF NOT EXISTS `password_reset_tokens` (
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.personal_access_tokens
CREATE TABLE IF NOT EXISTS `personal_access_tokens` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tokenable_type` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tokenable_id` bigint unsigned NOT NULL,
  `name` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `abilities` text COLLATE utf8mb4_unicode_ci,
  `last_used_at` timestamp NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `personal_access_tokens_token_unique` (`token`),
  KEY `personal_access_tokens_tokenable_type_tokenable_id_index` (`tokenable_type`,`tokenable_id`),
  KEY `personal_access_tokens_expires_at_index` (`expires_at`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.prioridades
CREATE TABLE IF NOT EXISTS `prioridades` (
  `id` smallint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nivel` smallint NOT NULL,
  `minutos_respuesta` int NOT NULL,
  `minutos_resolucion` int NOT NULL,
  `color_hex` char(7) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`),
  UNIQUE KEY `nivel` (`nivel`),
  CONSTRAINT `chk_min_res` CHECK ((`minutos_resolucion` >= `minutos_respuesta`)),
  CONSTRAINT `prioridades_chk_1` CHECK ((`nivel` between 1 and 4)),
  CONSTRAINT `prioridades_chk_2` CHECK ((`minutos_respuesta` > 0)),
  CONSTRAINT `prioridades_chk_3` CHECK ((`minutos_resolucion` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.roles
CREATE TABLE IF NOT EXISTS `roles` (
  `id` smallint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.satisfaction_surveys
CREATE TABLE IF NOT EXISTS `satisfaction_surveys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint NOT NULL,
  `usuario_id` bigint NOT NULL,
  `calificacion` smallint NOT NULL,
  `comentario` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ticket_id` (`ticket_id`),
  KEY `fk_sat_usu` (`usuario_id`),
  CONSTRAINT `fk_sat_tic` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sat_usu` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `satisfaction_surveys_chk_1` CHECK ((`calificacion` between 1 and 5))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.secuencias_ticket
CREATE TABLE IF NOT EXISTS `secuencias_ticket` (
  `anio` int NOT NULL,
  `ultimo` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`anio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.servicios
CREATE TABLE IF NOT EXISTS `servicios` (
  `id` smallint NOT NULL,
  `codigo` varchar(15) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `categoria` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`),
  CONSTRAINT `servicios_chk_1` CHECK ((`categoria` in (_utf8mb4'APLICACIONES',_utf8mb4'HARDWARE',_utf8mb4'REDES',_utf8mb4'ACCESOS',_utf8mb4'INFRAESTRUCTURA',_utf8mb4'BIOMEDICO',_utf8mb4'OTROS')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.sessions
CREATE TABLE IF NOT EXISTS `sessions` (
  `id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint unsigned DEFAULT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` text COLLATE utf8mb4_unicode_ci,
  `payload` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_activity` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sessions_user_id_index` (`user_id`),
  KEY `sessions_last_activity_index` (`last_activity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para procedimiento soporte_db.sp_actualizar_perfil
DELIMITER //
CREATE PROCEDURE `sp_actualizar_perfil`(
    IN p_id BIGINT,
    IN p_nombres VARCHAR(80), 
    IN p_apellidos VARCHAR(80),
    IN p_telefono VARCHAR(20),
    IN p_genero INT
)
BEGIN
    UPDATE usuarios 
    SET nombres = p_nombres, 
        apellidos = p_apellidos, 
        telefono = p_telefono,
        genero = p_genero,
        actualizado_en = CURRENT_TIMESTAMP
    WHERE id = p_id;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_adjunto
DELIMITER //
CREATE PROCEDURE `sp_adjunto`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_autor BIGINT,
    IN p_archivo TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_bytes INT, IN p_ts DATETIME)
BEGIN
    INSERT INTO ticket_attachments (ticket_id, subido_por, nombre_archivo, ruta_almacenamiento, mime_type, tamano_bytes, creado_en)
    SELECT id, p_autor, p_archivo, CONCAT('tickets/', codigo, '/', p_archivo),
           CASE WHEN p_archivo LIKE '%.png' THEN 'image/png' ELSE 'image/jpeg' END, p_bytes, p_ts
    FROM tickets WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_areas_listar
DELIMITER //
CREATE PROCEDURE `sp_areas_listar`()
BEGIN
    SELECT id, nombre, tipo FROM areas WHERE activo = 1 ORDER BY nombre;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_asignar
DELIMITER //
CREATE PROCEDURE `sp_asignar`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_tecnico BIGINT, IN p_ts DATETIME, IN p_actor BIGINT)
BEGIN
    SET @app_usuario_id = p_actor;
    SET @app_fecha_evento = p_ts;
    UPDATE tickets SET tecnico_id = p_tecnico WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_crear_ticket
DELIMITER //
CREATE PROCEDURE `sp_crear_ticket`(IN p_codigo VARCHAR(20), IN p_titulo TEXT, IN p_desc TEXT, IN p_servicio INT, IN p_prioridad INT, IN p_solicitante BIGINT, IN p_ubicacion TEXT, IN p_ts DATETIME)
BEGIN
    SET @app_usuario_id = p_solicitante;
    SET @app_fecha_evento = p_ts;
    INSERT INTO tickets (codigo, titulo, descripcion, servicio_id, prioridad_id, solicitante_id, ubicacion, fecha_creacion, estado_id)
    VALUES (p_codigo, p_titulo, p_desc, p_servicio, p_prioridad, p_solicitante, p_ubicacion, p_ts, 1);
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_crear_usuario_auth
DELIMITER //
CREATE PROCEDURE `sp_crear_usuario_auth`(
    IN p_rol_id SMALLINT, 
    IN p_area_id INT, 
    IN p_nombres VARCHAR(80), 
    IN p_apellidos VARCHAR(80), 
    IN p_email VARCHAR(150), 
    IN p_password_hash VARCHAR(100),
    IN p_cargo VARCHAR(80),
    IN p_telefono VARCHAR(20),
    IN p_genero INT
)
BEGIN
    INSERT INTO usuarios (rol_id, area_id, nombres, apellidos, email, password_hash, cargo, telefono, genero, activo) 
    VALUES (p_rol_id, p_area_id, p_nombres, p_apellidos, p_email, p_password_hash, p_cargo, p_telefono, p_genero, 1);
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_dash_kpis
DELIMITER //
CREATE PROCEDURE `sp_dash_kpis`(IN p_desde DATETIME, IN p_hasta DATETIME, IN p_servicio INT, IN p_prioridad INT)
BEGIN
    SELECT
        COUNT(*)                                                    AS total_tickets,
        COALESCE(SUM(x.es_res), 0)                                  AS resueltos,
        COALESCE(SUM(x.es_res AND x.a_tiempo), 0)                   AS dentro_sla,
        COALESCE(SUM(x.es_res AND NOT x.a_tiempo), 0)               AS resueltos_tarde,
        COALESCE(SUM(x.abierto_vencido), 0)                         AS abiertos_vencidos,
        ROUND(AVG(CASE WHEN x.es_res AND x.fecha_resolucion IS NOT NULL
                       THEN TIMESTAMPDIFF(MINUTE, x.fecha_creacion, x.fecha_resolucion) END), 0) AS mttr_min
    FROM (
        SELECT t.fecha_creacion, t.fecha_resolucion,
               (e.codigo IN ('RESUELTO','CERRADO'))                                         AS es_res,
               COALESCE(t.fecha_resolucion <= t.fecha_limite_resolucion, 0)                 AS a_tiempo,
               (e.codigo IN ('ABIERTO','ASIGNADO','EN_PROCESO') AND NOW() > t.fecha_limite_resolucion) AS abierto_vencido
        FROM tickets t
        JOIN estados_ticket e ON e.id = t.estado_id
        WHERE t.fecha_creacion >= p_desde AND t.fecha_creacion < p_hasta
          AND e.codigo <> 'CANCELADO'
          AND (p_servicio  IS NULL OR t.servicio_id  = p_servicio)
          AND (p_prioridad IS NULL OR t.prioridad_id = p_prioridad)
    ) x;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_dash_prioridad
DELIMITER //
CREATE PROCEDURE `sp_dash_prioridad`(IN p_desde DATETIME, IN p_hasta DATETIME, IN p_servicio INT, IN p_prioridad INT)
BEGIN
    SELECT p.id, p.codigo, p.nombre, p.color_hex, p.nivel,
           COUNT(e.id) AS total,
           COALESCE(SUM(CASE WHEN e.codigo IN ('RESUELTO','CERRADO')
                              OR (e.codigo IN ('ABIERTO','ASIGNADO','EN_PROCESO') AND NOW() > t.fecha_limite_resolucion)
                             THEN 1 ELSE 0 END), 0) AS evaluados,
           COALESCE(SUM(CASE WHEN e.codigo IN ('RESUELTO','CERRADO') AND t.fecha_resolucion <= t.fecha_limite_resolucion
                             THEN 1 ELSE 0 END), 0) AS dentro_sla
    FROM prioridades p
    LEFT JOIN tickets t ON t.prioridad_id = p.id
         AND t.fecha_creacion >= p_desde AND t.fecha_creacion < p_hasta
         AND (p_servicio IS NULL OR t.servicio_id = p_servicio)
    LEFT JOIN estados_ticket e ON e.id = t.estado_id AND e.codigo <> 'CANCELADO'
    WHERE (p_prioridad IS NULL OR p.id = p_prioridad)
    GROUP BY p.id, p.codigo, p.nombre, p.color_hex, p.nivel
    ORDER BY p.nivel;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_dash_servicio
DELIMITER //
CREATE PROCEDURE `sp_dash_servicio`(IN p_desde DATETIME, IN p_hasta DATETIME, IN p_servicio INT, IN p_prioridad INT)
BEGIN
    SELECT s.id, s.nombre,
           COUNT(e.id) AS total,
           COALESCE(SUM(CASE WHEN e.codigo IN ('RESUELTO','CERRADO')
                              OR (e.codigo IN ('ABIERTO','ASIGNADO','EN_PROCESO') AND NOW() > t.fecha_limite_resolucion)
                             THEN 1 ELSE 0 END), 0) AS evaluados,
           COALESCE(SUM(CASE WHEN e.codigo IN ('RESUELTO','CERRADO') AND t.fecha_resolucion <= t.fecha_limite_resolucion
                             THEN 1 ELSE 0 END), 0) AS dentro_sla
    FROM servicios s
    LEFT JOIN tickets t ON t.servicio_id = s.id
         AND t.fecha_creacion >= p_desde AND t.fecha_creacion < p_hasta
         AND (p_prioridad IS NULL OR t.prioridad_id = p_prioridad)
    LEFT JOIN estados_ticket e ON e.id = t.estado_id AND e.codigo <> 'CANCELADO'
    WHERE s.activo = 1 AND (p_servicio IS NULL OR s.id = p_servicio)
    GROUP BY s.id, s.nombre
    HAVING COUNT(e.id) > 0
    ORDER BY total DESC, s.nombre
    LIMIT 8;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_dash_vencidos
DELIMITER //
CREATE PROCEDURE `sp_dash_vencidos`(IN p_desde DATETIME, IN p_hasta DATETIME, IN p_servicio INT, IN p_prioridad INT)
BEGIN
    SELECT t.codigo, t.titulo, s.nombre AS servicio, p.nombre AS prioridad, e.nombre AS estado,
           CONCAT(u.nombres, ' ', u.apellidos) AS tecnico,
           t.fecha_creacion, t.fecha_limite_resolucion, t.fecha_resolucion,
           TIMESTAMPDIFF(MINUTE, t.fecha_limite_resolucion, COALESCE(t.fecha_resolucion, NOW())) AS minutos_exceso,
           CASE WHEN e.codigo IN ('RESUELTO','CERRADO') THEN 'Resuelto fuera de SLA' ELSE 'Abierto vencido' END AS tipo
    FROM tickets t
    JOIN servicios s ON s.id = t.servicio_id
    JOIN prioridades p ON p.id = t.prioridad_id
    JOIN estados_ticket e ON e.id = t.estado_id
    LEFT JOIN usuarios u ON u.id = t.tecnico_id
    WHERE t.fecha_creacion >= p_desde AND t.fecha_creacion < p_hasta
      AND e.codigo <> 'CANCELADO'
      AND (p_servicio  IS NULL OR t.servicio_id  = p_servicio)
      AND (p_prioridad IS NULL OR t.prioridad_id = p_prioridad)
      AND ( (e.codigo IN ('RESUELTO','CERRADO') AND t.fecha_resolucion > t.fecha_limite_resolucion)
         OR (e.codigo IN ('ABIERTO','ASIGNADO','EN_PROCESO') AND NOW() > t.fecha_limite_resolucion) )
    ORDER BY minutos_exceso DESC;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_encuesta
DELIMITER //
CREATE PROCEDURE `sp_encuesta`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_nota INT, IN p_comentario TEXT, IN p_ts DATETIME)
BEGIN
    INSERT INTO satisfaction_surveys (ticket_id, usuario_id, calificacion, comentario, creado_en)
    SELECT id, solicitante_id, p_nota, p_comentario, p_ts FROM tickets WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_mover
DELIMITER //
CREATE PROCEDURE `sp_mover`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_estado VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_ts DATETIME, IN p_actor BIGINT, IN p_solucion TEXT)
BEGIN
    SET @app_usuario_id = p_actor;
    SET @app_fecha_evento = p_ts;
    UPDATE tickets
       SET estado_id = (SELECT id FROM estados_ticket WHERE codigo = p_estado),
           solucion_aplicada = COALESCE(p_solucion, solucion_aplicada)
     WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_msg
DELIMITER //
CREATE PROCEDURE `sp_msg`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_autor BIGINT, IN p_texto TEXT, IN p_interno BOOLEAN, IN p_ts DATETIME)
BEGIN
    INSERT INTO ticket_mensajes (ticket_id, autor_id, contenido, es_interno, creado_en)
    SELECT id, p_autor, p_texto, p_interno, p_ts FROM tickets WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_notificar
DELIMITER //
CREATE PROCEDURE `sp_notificar`(IN p_usuario BIGINT, IN p_ticket BIGINT, IN p_tipo VARCHAR(20),
                              IN p_titulo VARCHAR(120), IN p_cuerpo TEXT, IN p_ts DATETIME)
BEGIN
    INSERT INTO notificaciones (usuario_id, ticket_id, tipo, titulo, cuerpo, creado_en)
    SELECT u.id, p_ticket, p_tipo, LEFT(p_titulo, 120), LEFT(p_cuerpo, 300), p_ts
    FROM usuarios u
    WHERE u.id = p_usuario AND u.activo = TRUE;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_notif_leer_ticket
DELIMITER //
CREATE PROCEDURE `sp_notif_leer_ticket`(IN p_usuario BIGINT, IN p_ticket BIGINT, IN p_ts DATETIME)
BEGIN
    UPDATE notificaciones
       SET leida = 1, leida_en = p_ts
     WHERE usuario_id = p_usuario AND ticket_id = p_ticket AND leida = 0;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_notif_leer_todas
DELIMITER //
CREATE PROCEDURE `sp_notif_leer_todas`(IN p_usuario BIGINT, IN p_ts DATETIME)
BEGIN
    UPDATE notificaciones
       SET leida = 1, leida_en = p_ts
     WHERE usuario_id = p_usuario AND leida = 0;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_notif_marcar_leida
DELIMITER //
CREATE PROCEDURE `sp_notif_marcar_leida`(IN p_usuario BIGINT, IN p_id BIGINT, IN p_ts DATETIME)
BEGIN
    UPDATE notificaciones
       SET leida = 1, leida_en = p_ts
     WHERE id = p_id AND usuario_id = p_usuario AND leida = 0;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_obtener_detalle_ticket
DELIMITER //
CREATE PROCEDURE `sp_obtener_detalle_ticket`(
    IN p_codigo_o_id VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci)
BEGIN
    DECLARE v_ticket_id BIGINT DEFAULT NULL;

    IF p_codigo_o_id REGEXP '^[0-9]+$' THEN
        SELECT id INTO v_ticket_id FROM tickets
        WHERE id = CAST(p_codigo_o_id AS UNSIGNED) LIMIT 1;
    ELSE
        SELECT id INTO v_ticket_id FROM tickets
        WHERE codigo = p_codigo_o_id COLLATE utf8mb4_unicode_ci LIMIT 1;
    END IF;

    IF v_ticket_id IS NOT NULL THEN
        SELECT * FROM v_tickets_detalle WHERE id = v_ticket_id;

        SELECT id, subido_por, nombre_archivo, ruta_almacenamiento, mime_type, tamano_bytes, creado_en
        FROM ticket_attachments WHERE ticket_id = v_ticket_id ORDER BY creado_en ASC;

        SELECT m.id, m.autor_id,
               CONCAT(u.nombres, ' ', u.apellidos) AS autor_nombre,
               r.codigo AS autor_rol,
               m.contenido, m.es_interno, m.creado_en
        FROM ticket_mensajes m
        JOIN usuarios u ON u.id = m.autor_id
        JOIN roles r ON r.id = u.rol_id
        WHERE m.ticket_id = v_ticket_id
        ORDER BY m.id ASC;

        SELECT l.id, l.tipo_evento, l.detalle, l.creado_en,
               CONCAT(u.nombres, ' ', u.apellidos) AS realizado_por
        FROM ticket_logs l
        LEFT JOIN usuarios u ON u.id = l.usuario_id
        WHERE l.ticket_id = v_ticket_id
        ORDER BY l.creado_en DESC, l.id DESC;

        SELECT calificacion, comentario, creado_en
        FROM satisfaction_surveys WHERE ticket_id = v_ticket_id LIMIT 1;
    ELSE
        SELECT * FROM v_tickets_detalle WHERE 1 = 0;
    END IF;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_priorizar
DELIMITER //
CREATE PROCEDURE `sp_priorizar`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_prioridad INT, IN p_ts DATETIME, IN p_actor BIGINT)
BEGIN
    SET @app_usuario_id = p_actor;
    SET @app_fecha_evento = p_ts;
    UPDATE tickets SET prioridad_id = p_prioridad WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_reabrir
DELIMITER //
CREATE PROCEDURE `sp_reabrir`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_actor BIGINT,
    IN p_motivo TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_ts DATETIME)
BEGIN
    SET @app_usuario_id = p_actor;
    SET @app_fecha_evento = p_ts;

    UPDATE tickets
       SET estado_id  = (SELECT id FROM estados_ticket WHERE codigo = 'ABIERTO'),
           tecnico_id = NULL
     WHERE codigo = p_codigo;

    CALL sp_msg(p_codigo, p_actor, CONCAT('Ticket reabierto por el usuario. Motivo: ', p_motivo), FALSE, p_ts);
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_registrar_login_fallido
DELIMITER //
CREATE PROCEDURE `sp_registrar_login_fallido`(
    IN p_email VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_ip VARCHAR(45), IN p_dispositivo VARCHAR(150))
BEGIN
    DECLARE v_id BIGINT;
    DECLARE v_intentos INT;
    DECLARE v_ts DATETIME DEFAULT IFNULL(@app_fecha_evento, CURRENT_TIMESTAMP);
    DECLARE v_max_intentos INT;
    DECLARE v_minutos_bloqueo INT;

    SELECT id, intentos_fallidos + 1 INTO v_id, v_intentos
    FROM usuarios WHERE LOWER(email) = LOWER(p_email);

    IF v_id IS NOT NULL THEN
        UPDATE usuarios SET intentos_fallidos = v_intentos WHERE id = v_id;

        INSERT INTO auditoria_sesiones (usuario_id, email_intento, evento, ip_origen, dispositivo, creado_en)
        VALUES (v_id, p_email, 'LOGIN_FALLIDO', p_ip, p_dispositivo, v_ts);

        SET v_max_intentos = fn_param('max_intentos_login');

        IF v_intentos >= v_max_intentos THEN
            SET v_minutos_bloqueo = fn_param('minutos_bloqueo');

            UPDATE usuarios SET bloqueado_hasta = DATE_ADD(v_ts, INTERVAL v_minutos_bloqueo MINUTE)
            WHERE id = v_id;

            INSERT INTO auditoria_sesiones (usuario_id, email_intento, evento, ip_origen, dispositivo, detalle, creado_en)
            VALUES (v_id, p_email, 'BLOQUEO_CUENTA', p_ip, p_dispositivo, CONCAT('Bloqueo por ', v_intentos, ' intentos fallidos'), v_ts);
        END IF;
    ELSE
        INSERT INTO auditoria_sesiones (email_intento, evento, ip_origen, dispositivo, creado_en)
        VALUES (p_email, 'LOGIN_FALLIDO', p_ip, p_dispositivo, v_ts);
    END IF;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_registrar_login_ok
DELIMITER //
CREATE PROCEDURE `sp_registrar_login_ok`(IN p_usuario BIGINT, IN p_ip VARCHAR(45), IN p_dispositivo VARCHAR(150))
BEGIN
    DECLARE v_ts DATETIME DEFAULT IFNULL(@app_fecha_evento, CURRENT_TIMESTAMP);
    
    UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL, ultimo_login = v_ts
    WHERE id = p_usuario;
    
    INSERT INTO auditoria_sesiones (usuario_id, email_intento, evento, ip_origen, dispositivo, creado_en)
    SELECT id, email, 'LOGIN_OK', p_ip, p_dispositivo, v_ts FROM usuarios WHERE id = p_usuario;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_registrar_logout
DELIMITER //
CREATE PROCEDURE `sp_registrar_logout`(IN p_usuario BIGINT, IN p_ip VARCHAR(45), IN p_dispositivo VARCHAR(150))
BEGIN
    DECLARE v_ts DATETIME DEFAULT IFNULL(@app_fecha_evento, CURRENT_TIMESTAMP);
    
    INSERT INTO auditoria_sesiones (usuario_id, email_intento, evento, ip_origen, dispositivo, creado_en)
    SELECT id, email, 'LOGOUT', p_ip, p_dispositivo, v_ts FROM usuarios WHERE id = p_usuario;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_roles_listar
DELIMITER //
CREATE PROCEDURE `sp_roles_listar`()
BEGIN
    SELECT id, codigo, nombre FROM roles ORDER BY id;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_set_complejidad
DELIMITER //
CREATE PROCEDURE `sp_set_complejidad`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_complejidad VARCHAR(10), IN p_ts DATETIME, IN p_actor BIGINT)
BEGIN
    SET @app_usuario_id = p_actor;
    SET @app_fecha_evento = p_ts;
    UPDATE tickets SET complejidad = p_complejidad WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_set_plazo
DELIMITER //
CREATE PROCEDURE `sp_set_plazo`(
    IN p_codigo VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_dias INT, IN p_ts DATETIME, IN p_actor BIGINT)
BEGIN
    SET @app_usuario_id = p_actor;
    SET @app_fecha_evento = p_ts;
    UPDATE tickets
       SET plazo_dias = IF(p_dias IS NULL OR p_dias <= 0, NULL, p_dias),
           fecha_limite_tecnico = IF(p_dias IS NULL OR p_dias <= 0, NULL, DATE_ADD(p_ts, INTERVAL p_dias DAY))
     WHERE codigo = p_codigo;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_usuarios_listar
DELIMITER //
CREATE PROCEDURE `sp_usuarios_listar`(
    IN p_buscar VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_rol_id SMALLINT,
    IN p_activo TINYINT,
    IN p_limit  INT,
    IN p_offset INT
)
BEGIN
    SELECT v.*, COUNT(*) OVER() AS total_registros
    FROM v_usuarios_detalle v
    WHERE (p_buscar IS NULL OR p_buscar = ''
           OR CONCAT_WS(' ', v.nombres, v.apellidos, v.email, IFNULL(v.cargo,''))
              LIKE CONCAT('%', p_buscar, '%'))
      AND (p_rol_id IS NULL OR v.rol_id = p_rol_id)
      AND (p_activo IS NULL OR v.activo = p_activo)
    ORDER BY v.apellidos ASC, v.nombres ASC
    LIMIT p_limit OFFSET p_offset;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_usuario_actualizar
DELIMITER //
CREATE PROCEDURE `sp_usuario_actualizar`(
    IN p_id            BIGINT,
    IN p_rol_id        SMALLINT,
    IN p_area_id       INT,
    IN p_nombres       VARCHAR(80)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_apellidos     VARCHAR(80)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_email         VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_cargo         VARCHAR(80)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_telefono      VARCHAR(20)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_nivel_soporte SMALLINT,
    IN p_genero        INT,
    IN p_activo        TINYINT
)
BEGIN
    DECLARE v_rol_codigo VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
    DECLARE v_email VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_nivel SMALLINT DEFAULT NULL;

    SET v_email = LOWER(TRIM(p_email));

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id = p_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no existe';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM roles WHERE id = p_rol_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El rol indicado no existe';
    END IF;
    SELECT codigo INTO v_rol_codigo FROM roles WHERE id = p_rol_id;

    IF p_area_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM areas WHERE id = p_area_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El área indicada no existe';
    END IF;

    IF p_genero IS NOT NULL AND p_genero NOT IN (1, 2) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Género inválido (1 = hombre, 2 = mujer)';
    END IF;

    IF EXISTS (SELECT 1 FROM usuarios WHERE LOWER(email) = v_email AND id <> p_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe otro usuario con ese correo';
    END IF;

    -- No dejar el sistema sin administradores
    IF fn_es_ultimo_admin(p_id) = 1
       AND (v_rol_codigo <> 'ADMINISTRADOR_TI' OR IFNULL(p_activo, 1) = 0) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede quitar el rol ni desactivar al último administrador';
    END IF;

    IF v_rol_codigo = 'TECNICO_TI' THEN
        SET v_nivel = IFNULL(p_nivel_soporte, 1);
    END IF;

    UPDATE usuarios
    SET rol_id         = p_rol_id,
        area_id        = p_area_id,
        nombres        = TRIM(p_nombres),
        apellidos      = TRIM(p_apellidos),
        email          = v_email,
        cargo          = NULLIF(TRIM(p_cargo), ''),
        telefono       = NULLIF(TRIM(p_telefono), ''),
        nivel_soporte  = v_nivel,
        genero         = p_genero,
        activo         = IFNULL(p_activo, activo),
        actualizado_en = CURRENT_TIMESTAMP
    WHERE id = p_id;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_usuario_cambiar_estado
DELIMITER //
CREATE PROCEDURE `sp_usuario_cambiar_estado`(IN p_id BIGINT, IN p_activo TINYINT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id = p_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no existe';
    END IF;

    IF p_activo = 0 AND fn_es_ultimo_admin(p_id) = 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede desactivar al último administrador';
    END IF;

    UPDATE usuarios
    SET activo            = p_activo,
        intentos_fallidos = IF(p_activo = 1, 0, intentos_fallidos),
        bloqueado_hasta   = IF(p_activo = 1, NULL, bloqueado_hasta),
        actualizado_en    = CURRENT_TIMESTAMP
    WHERE id = p_id;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_usuario_cambiar_password
DELIMITER //
CREATE PROCEDURE `sp_usuario_cambiar_password`(
    IN p_id BIGINT,
    IN p_password_hash VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id = p_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no existe';
    END IF;

    UPDATE usuarios
    SET password_hash     = p_password_hash,
        intentos_fallidos = 0,
        bloqueado_hasta   = NULL,
        actualizado_en    = CURRENT_TIMESTAMP
    WHERE id = p_id;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_usuario_crear
DELIMITER //
CREATE PROCEDURE `sp_usuario_crear`(
    IN p_rol_id        SMALLINT,
    IN p_area_id       INT,
    IN p_nombres       VARCHAR(80)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_apellidos     VARCHAR(80)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_email         VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_password_hash VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_cargo         VARCHAR(80)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_telefono      VARCHAR(20)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    IN p_nivel_soporte SMALLINT,
    IN p_genero        INT,
    IN p_activo        TINYINT
)
BEGIN
    DECLARE v_rol_codigo VARCHAR(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL;
    DECLARE v_email VARCHAR(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    DECLARE v_nivel SMALLINT DEFAULT NULL;

    SET v_email = LOWER(TRIM(p_email));

    IF NOT EXISTS (SELECT 1 FROM roles WHERE id = p_rol_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El rol indicado no existe';
    END IF;
    SELECT codigo INTO v_rol_codigo FROM roles WHERE id = p_rol_id;

    IF p_area_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM areas WHERE id = p_area_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El área indicada no existe';
    END IF;

    IF p_genero IS NOT NULL AND p_genero NOT IN (1, 2) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Género inválido (1 = hombre, 2 = mujer)';
    END IF;

    IF EXISTS (SELECT 1 FROM usuarios WHERE LOWER(email) = v_email) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un usuario con ese correo';
    END IF;

    -- nivel_soporte solo aplica a técnicos (1..3)
    IF v_rol_codigo = 'TECNICO_TI' THEN
        SET v_nivel = IFNULL(p_nivel_soporte, 1);
    END IF;

    INSERT INTO usuarios
        (rol_id, area_id, nombres, apellidos, email, password_hash,
         cargo, telefono, nivel_soporte, genero, activo)
    VALUES
        (p_rol_id, p_area_id, TRIM(p_nombres), TRIM(p_apellidos), v_email, p_password_hash,
         NULLIF(TRIM(p_cargo), ''), NULLIF(TRIM(p_telefono), ''), v_nivel, p_genero,
         IFNULL(p_activo, 1));

    SELECT LAST_INSERT_ID() AS id;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_usuario_eliminar
DELIMITER //
CREATE PROCEDURE `sp_usuario_eliminar`(IN p_id BIGINT)
BEGIN
    DECLARE EXIT HANDLER FOR 1451
    BEGIN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se puede eliminar: el usuario tiene historial (tickets, sesiones, etc.). Desactívelo en su lugar';
    END;

    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id = p_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no existe';
    END IF;

    IF fn_es_ultimo_admin(p_id) = 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede eliminar al último administrador';
    END IF;

    DELETE FROM usuarios WHERE id = p_id;
END//
DELIMITER ;

-- Volcando estructura para procedimiento soporte_db.sp_usuario_obtener
DELIMITER //
CREATE PROCEDURE `sp_usuario_obtener`(IN p_id BIGINT)
BEGIN
    SELECT * FROM v_usuarios_detalle WHERE id = p_id;
END//
DELIMITER ;

-- Volcando estructura para tabla soporte_db.tickets
CREATE TABLE IF NOT EXISTS `tickets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `titulo` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `servicio_id` smallint NOT NULL,
  `prioridad_id` smallint NOT NULL,
  `prioridad_original_id` smallint NOT NULL,
  `estado_id` smallint NOT NULL,
  `solicitante_id` bigint NOT NULL,
  `tecnico_id` bigint DEFAULT NULL,
  `ubicacion` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fecha_creacion` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_asignacion` timestamp NULL DEFAULT NULL,
  `fecha_primera_respuesta` timestamp NULL DEFAULT NULL,
  `fecha_inicio_atencion` timestamp NULL DEFAULT NULL,
  `fecha_resolucion` timestamp NULL DEFAULT NULL,
  `fecha_cierre` timestamp NULL DEFAULT NULL,
  `fecha_cancelacion` timestamp NULL DEFAULT NULL,
  `fecha_limite_respuesta` timestamp NOT NULL,
  `fecha_limite_resolucion` timestamp NOT NULL,
  `solucion_aplicada` text COLLATE utf8mb4_unicode_ci,
  `veces_reabierto` smallint NOT NULL DEFAULT '0',
  `fecha_ultima_reapertura` timestamp NULL DEFAULT NULL,
  `actualizado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `complejidad` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plazo_dias` smallint DEFAULT NULL,
  `fecha_limite_tecnico` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `codigo` (`codigo`),
  KEY `fk_tic_prio_o` (`prioridad_original_id`),
  KEY `ix_tickets_estado` (`estado_id`),
  KEY `ix_tickets_solicitante` (`solicitante_id`,`fecha_creacion` DESC),
  KEY `ix_tickets_tecnico` (`tecnico_id`,`estado_id`),
  KEY `ix_tickets_prioridad` (`prioridad_id`),
  KEY `ix_tickets_servicio` (`servicio_id`),
  KEY `ix_tickets_creacion` (`fecha_creacion` DESC),
  CONSTRAINT `fk_tic_est` FOREIGN KEY (`estado_id`) REFERENCES `estados_ticket` (`id`),
  CONSTRAINT `fk_tic_prio` FOREIGN KEY (`prioridad_id`) REFERENCES `prioridades` (`id`),
  CONSTRAINT `fk_tic_prio_o` FOREIGN KEY (`prioridad_original_id`) REFERENCES `prioridades` (`id`),
  CONSTRAINT `fk_tic_serv` FOREIGN KEY (`servicio_id`) REFERENCES `servicios` (`id`),
  CONSTRAINT `fk_tic_sol` FOREIGN KEY (`solicitante_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_tic_tec` FOREIGN KEY (`tecnico_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `chk_fechas_cie` CHECK (((`fecha_cierre` is null) or (`fecha_cierre` >= `fecha_creacion`))),
  CONSTRAINT `chk_fechas_res` CHECK (((`fecha_resolucion` is null) or (`fecha_resolucion` >= `fecha_creacion`))),
  CONSTRAINT `chk_tic_complejidad` CHECK (((`complejidad` is null) or (`complejidad` in (_utf8mb4'BAJA',_utf8mb4'MEDIA',_utf8mb4'ALTA')))),
  CONSTRAINT `chk_tic_plazo` CHECK (((`plazo_dias` is null) or (`plazo_dias` between 1 and 90)))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.ticket_attachments
CREATE TABLE IF NOT EXISTS `ticket_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint NOT NULL,
  `subido_por` bigint NOT NULL,
  `nombre_archivo` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ruta_almacenamiento` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tamano_bytes` int NOT NULL,
  `creado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_att_user` (`subido_por`),
  KEY `ix_attachments_ticket` (`ticket_id`),
  CONSTRAINT `fk_att_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_att_user` FOREIGN KEY (`subido_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ticket_attachments_chk_1` CHECK ((`mime_type` in (_utf8mb4'image/jpeg',_utf8mb4'image/png'))),
  CONSTRAINT `ticket_attachments_chk_2` CHECK ((`tamano_bytes` between 1 and 5242880))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.ticket_logs
CREATE TABLE IF NOT EXISTS `ticket_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint NOT NULL,
  `usuario_id` bigint DEFAULT NULL,
  `tipo_evento` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `estado_anterior_id` smallint DEFAULT NULL,
  `estado_nuevo_id` smallint DEFAULT NULL,
  `tecnico_anterior_id` bigint DEFAULT NULL,
  `tecnico_nuevo_id` bigint DEFAULT NULL,
  `prioridad_anterior_id` smallint DEFAULT NULL,
  `prioridad_nueva_id` smallint DEFAULT NULL,
  `detalle` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `creado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_log_usu` (`usuario_id`),
  KEY `fk_log_est_a` (`estado_anterior_id`),
  KEY `fk_log_est_n` (`estado_nuevo_id`),
  KEY `fk_log_tec_a` (`tecnico_anterior_id`),
  KEY `fk_log_tec_n` (`tecnico_nuevo_id`),
  KEY `fk_log_pri_a` (`prioridad_anterior_id`),
  KEY `fk_log_pri_n` (`prioridad_nueva_id`),
  KEY `ix_logs_ticket` (`ticket_id`,`creado_en`),
  CONSTRAINT `fk_log_est_a` FOREIGN KEY (`estado_anterior_id`) REFERENCES `estados_ticket` (`id`),
  CONSTRAINT `fk_log_est_n` FOREIGN KEY (`estado_nuevo_id`) REFERENCES `estados_ticket` (`id`),
  CONSTRAINT `fk_log_pri_a` FOREIGN KEY (`prioridad_anterior_id`) REFERENCES `prioridades` (`id`),
  CONSTRAINT `fk_log_pri_n` FOREIGN KEY (`prioridad_nueva_id`) REFERENCES `prioridades` (`id`),
  CONSTRAINT `fk_log_tec_a` FOREIGN KEY (`tecnico_anterior_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_log_tec_n` FOREIGN KEY (`tecnico_nuevo_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_log_tic` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_log_usu` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ticket_logs_chk_1` CHECK ((`tipo_evento` in (_utf8mb4'CREACION',_utf8mb4'CAMBIO_ESTADO',_utf8mb4'ASIGNACION',_utf8mb4'REASIGNACION',_utf8mb4'CAMBIO_PRIORIDAD',_utf8mb4'REAPERTURA',_utf8mb4'CANCELACION',_utf8mb4'CAMBIO_COMPLEJIDAD',_utf8mb4'CAMBIO_PLAZO')))
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.ticket_mensajes
CREATE TABLE IF NOT EXISTS `ticket_mensajes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint NOT NULL,
  `autor_id` bigint NOT NULL,
  `contenido` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `es_interno` tinyint(1) NOT NULL DEFAULT '0',
  `creado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_msg_aut` (`autor_id`),
  KEY `ix_mensajes_ticket` (`ticket_id`,`creado_en`),
  CONSTRAINT `fk_msg_aut` FOREIGN KEY (`autor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_msg_tic` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.transiciones_estado
CREATE TABLE IF NOT EXISTS `transiciones_estado` (
  `estado_origen_id` smallint NOT NULL,
  `estado_destino_id` smallint NOT NULL,
  `descripcion` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`estado_origen_id`,`estado_destino_id`),
  KEY `fk_trans_destino` (`estado_destino_id`),
  CONSTRAINT `fk_trans_destino` FOREIGN KEY (`estado_destino_id`) REFERENCES `estados_ticket` (`id`),
  CONSTRAINT `fk_trans_origen` FOREIGN KEY (`estado_origen_id`) REFERENCES `estados_ticket` (`id`),
  CONSTRAINT `chk_trans_dif` CHECK ((`estado_origen_id` <> `estado_destino_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.users
CREATE TABLE IF NOT EXISTS `users` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email_verified_at` timestamp NULL DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remember_token` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `users_email_unique` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para tabla soporte_db.usuarios
CREATE TABLE IF NOT EXISTS `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rol_id` smallint NOT NULL,
  `area_id` int DEFAULT NULL,
  `nombres` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `apellidos` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sso_subject` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password_hash` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cargo` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `telefono` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nivel_soporte` tinyint(1) DEFAULT '1',
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `intentos_fallidos` smallint NOT NULL DEFAULT '0',
  `bloqueado_hasta` timestamp NULL DEFAULT NULL,
  `ultimo_login` timestamp NULL DEFAULT NULL,
  `creado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `actualizado_en` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `genero` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_usuarios_email` (`email`),
  UNIQUE KEY `sso_subject` (`sso_subject`),
  KEY `ix_usuarios_area` (`area_id`),
  CONSTRAINT `fk_usu_area` FOREIGN KEY (`area_id`) REFERENCES `areas` (`id`),
  CONSTRAINT `fk_usu_rol` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `usuarios_chk_1` CHECK ((`nivel_soporte` between 1 and 3))
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- La exportación de datos fue deseleccionada.

-- Volcando estructura para vista soporte_db.v_carga_tecnicos
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_carga_tecnicos` (
	`tecnico_id` BIGINT NOT NULL,
	`tecnico` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`nivel_soporte` SMALLINT NULL,
	`asignados` DECIMAL(23,0) NULL,
	`en_proceso` DECIMAL(23,0) NULL,
	`carga_activa` DECIMAL(23,0) NULL
) ENGINE=MyISAM;

-- Volcando estructura para vista soporte_db.v_csat
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_csat` (
	`mes` VARCHAR(1) NULL COLLATE 'utf8mb4_general_ci',
	`servicio` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`encuestas` BIGINT NOT NULL,
	`csat_promedio` DECIMAL(8,2) NULL,
	`pct_satisfechos` DECIMAL(28,1) NULL
) ENGINE=MyISAM;

-- Volcando estructura para vista soporte_db.v_dashboard_ti
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_dashboard_ti` (
	`orden` SMALLINT NOT NULL,
	`estado` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`nivel` SMALLINT NOT NULL,
	`prioridad` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`total` BIGINT NOT NULL
) ENGINE=MyISAM;

-- Volcando estructura para vista soporte_db.v_estados_ticket
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_estados_ticket` (
	`id` SMALLINT NOT NULL,
	`codigo` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`nombre` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`orden` SMALLINT NOT NULL,
	`es_final` TINYINT(1) NOT NULL
) ENGINE=MyISAM;

-- Volcando estructura para vista soporte_db.v_notificaciones
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_notificaciones` (
	`id` BIGINT NOT NULL,
	`usuario_id` BIGINT NOT NULL,
	`ticket_id` BIGINT NULL,
	`ticket_codigo` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`tipo` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`titulo` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`cuerpo` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`leida` TINYINT(1) NOT NULL,
	`creado_en` TIMESTAMP NOT NULL,
	`leida_en` TIMESTAMP NULL
) ENGINE=MyISAM;

-- Volcando estructura para vista soporte_db.v_reporte_sla
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_reporte_sla` (
	`servicio` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`prioridad` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`tickets_resueltos` BIGINT NOT NULL,
	`dentro_sla` DECIMAL(23,0) NULL,
	`pct_cumplimiento_sla` DECIMAL(28,1) NULL,
	`min_prom_primera_respuesta` DECIMAL(22,1) NULL,
	`horas_prom_resolucion` DECIMAL(23,2) NULL
) ENGINE=MyISAM;

-- Volcando estructura para vista soporte_db.v_tickets_detalle
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_tickets_detalle` (
	`id` BIGINT NOT NULL,
	`codigo` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`titulo` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`descripcion` TEXT NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`servicio` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`categoria` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`prioridad` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`prioridad_nivel` SMALLINT NOT NULL,
	`color_hex` CHAR(7) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`estado` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`solicitante_id` BIGINT NOT NULL,
	`solicitante` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`area_solicitante` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`ubicacion` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`tecnico_id` BIGINT NULL,
	`tecnico` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`fecha_creacion` TIMESTAMP NOT NULL,
	`fecha_asignacion` TIMESTAMP NULL,
	`fecha_primera_respuesta` TIMESTAMP NULL,
	`fecha_resolucion` TIMESTAMP NULL,
	`fecha_cierre` TIMESTAMP NULL,
	`fecha_limite_respuesta` TIMESTAMP NOT NULL,
	`fecha_limite_resolucion` TIMESTAMP NOT NULL,
	`tiempo_restante_sla` BIGINT NULL,
	`sla_vencido` INT NULL,
	`veces_reabierto` SMALLINT NOT NULL,
	`solucion_aplicada` TEXT NULL COLLATE 'utf8mb4_unicode_ci',
	`estado_nombre` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`estado_orden` SMALLINT NOT NULL,
	`estado_final` TINYINT(1) NOT NULL,
	`complejidad` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`plazo_dias` SMALLINT NULL,
	`fecha_limite_tecnico` TIMESTAMP NULL,
	`tiempo_restante_tecnico` BIGINT NULL
) ENGINE=MyISAM;

-- Volcando estructura para vista soporte_db.v_usuarios_detalle
-- Creando tabla temporal para superar errores de dependencia de VIEW
CREATE TABLE `v_usuarios_detalle` (
	`id` BIGINT NOT NULL,
	`rol_id` SMALLINT NOT NULL,
	`rol_codigo` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`rol_nombre` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`area_id` INT NULL,
	`area_nombre` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`nombres` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`apellidos` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`nombre_completo` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`email` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_unicode_ci',
	`cargo` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`telefono` VARCHAR(1) NULL COLLATE 'utf8mb4_unicode_ci',
	`nivel_soporte` SMALLINT NULL,
	`activo` TINYINT(1) NOT NULL,
	`genero` INT NULL,
	`foto_tipo` VARCHAR(1) NOT NULL COLLATE 'utf8mb4_general_ci',
	`intentos_fallidos` SMALLINT NOT NULL,
	`bloqueado_hasta` TIMESTAMP NULL,
	`ultimo_login` TIMESTAMP NULL,
	`creado_en` TIMESTAMP NOT NULL,
	`actualizado_en` TIMESTAMP NOT NULL
) ENGINE=MyISAM;

-- Volcando estructura para disparador soporte_db.trg_attachments_bi
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_attachments_bi` BEFORE INSERT ON `ticket_attachments` FOR EACH ROW BEGIN
    DECLARE v_final BOOLEAN;
    
    SELECT e.es_final INTO v_final
    FROM tickets t JOIN estados_ticket e ON e.id = t.estado_id
    WHERE t.id = NEW.ticket_id;
    
    IF v_final THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se pueden adjuntar archivos a un ticket finalizado';
    END IF;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Volcando estructura para disparador soporte_db.trg_mensajes_bi
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_mensajes_bi` BEFORE INSERT ON `ticket_mensajes` FOR EACH ROW BEGIN
    DECLARE v_estado_id INT;
    DECLARE v_solicitante_id BIGINT;
    DECLARE v_tecnico_id BIGINT;
    DECLARE v_codigo VARCHAR(20);
    DECLARE v_es_final BOOLEAN;
    DECLARE v_rol VARCHAR(30);
    
    SELECT t.estado_id, t.solicitante_id, t.tecnico_id, t.codigo, e.es_final 
    INTO v_estado_id, v_solicitante_id, v_tecnico_id, v_codigo, v_es_final
    FROM tickets t JOIN estados_ticket e ON e.id = t.estado_id
    WHERE t.id = NEW.ticket_id;
    
    SELECT r.codigo INTO v_rol FROM usuarios u JOIN roles r ON r.id = u.rol_id WHERE u.id = NEW.autor_id;
    
    IF v_es_final THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El ticket ya está finalizado; no admite mensajes';
    END IF;
    
    IF v_rol = 'USUARIO_FINAL' AND (NEW.es_interno OR NEW.autor_id <> v_solicitante_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario final solo puede escribir mensajes públicos en sus propios tickets';
    END IF;
    
    IF v_rol = 'TECNICO_TI' AND (v_tecnico_id IS NULL OR v_tecnico_id <> NEW.autor_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo el técnico asignado puede escribir en este ticket';
    END IF;
    
    IF NOT NEW.es_interno THEN
        IF NEW.autor_id = v_solicitante_id AND v_tecnico_id IS NOT NULL THEN
            -- Se notifica dentro del Trigger posterior para no romper reglas de tabla mutante, pero en MySQL 
            -- insert a otra tabla en Before Trigger es válido
            INSERT INTO notificaciones (usuario_id, ticket_id, tipo, titulo, cuerpo, creado_en)
            VALUES (v_tecnico_id, NEW.ticket_id, 'NUEVO_MENSAJE', CONCAT('Nuevo mensaje en ', v_codigo), NEW.contenido, NEW.creado_en);
        ELSEIF NEW.autor_id <> v_solicitante_id THEN
            INSERT INTO notificaciones (usuario_id, ticket_id, tipo, titulo, cuerpo, creado_en)
            VALUES (v_solicitante_id, NEW.ticket_id, 'NUEVO_MENSAJE', CONCAT('Nuevo mensaje en ', v_codigo), NEW.contenido, NEW.creado_en);
        END IF;
    END IF;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Volcando estructura para disparador soporte_db.trg_surveys_bi
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_surveys_bi` BEFORE INSERT ON `satisfaction_surveys` FOR EACH ROW BEGIN
    DECLARE v_estado VARCHAR(15);
    DECLARE v_solicitante_id BIGINT;

    SELECT e.codigo, t.solicitante_id INTO v_estado, v_solicitante_id
    FROM tickets t JOIN estados_ticket e ON e.id = t.estado_id
    WHERE t.id = NEW.ticket_id;

    IF v_estado NOT IN ('RESUELTO','CERRADO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La encuesta solo se habilita cuando el ticket está RESUELTO o CERRADO';
    END IF;

    IF NEW.usuario_id <> v_solicitante_id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo el solicitante puede calificar el ticket';
    END IF;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Volcando estructura para disparador soporte_db.trg_tickets_ai
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_tickets_ai` AFTER INSERT ON `tickets` FOR EACH ROW BEGIN
    DECLARE v_actor BIGINT DEFAULT IFNULL(@app_usuario_id, NEW.solicitante_id);
    
    INSERT INTO ticket_logs (ticket_id, usuario_id, tipo_evento, estado_nuevo_id, prioridad_nueva_id, detalle, creado_en)
    VALUES (NEW.id, v_actor, 'CREACION', NEW.estado_id, NEW.prioridad_id, 'Ticket registrado', NEW.fecha_creacion);

    -- Notificar admins
    INSERT INTO notificaciones (usuario_id, ticket_id, tipo, titulo, cuerpo, creado_en)
    SELECT u.id, NEW.id, 'NUEVO_TICKET', CONCAT('Nuevo ticket ', NEW.codigo), NEW.titulo, NEW.fecha_creacion
    FROM usuarios u JOIN roles r ON r.id = u.rol_id
    WHERE r.codigo = 'ADMINISTRADOR_TI' AND u.activo = TRUE;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Volcando estructura para disparador soporte_db.trg_tickets_au
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_tickets_au` AFTER UPDATE ON `tickets` FOR EACH ROW BEGIN
    DECLARE v_ts DATETIME DEFAULT IFNULL(@app_fecha_evento, CURRENT_TIMESTAMP);
    DECLARE v_actor BIGINT DEFAULT @app_usuario_id;
    DECLARE v_old_codigo VARCHAR(15);
    DECLARE v_new_codigo VARCHAR(15);
    DECLARE v_tipo VARCHAR(20);
    DECLARE v_nombre VARCHAR(160);

    IF NEW.tecnico_id IS NOT NULL AND (OLD.tecnico_id IS NULL OR NEW.tecnico_id <> OLD.tecnico_id) THEN
        SELECT CONCAT(nombres, ' ', apellidos) INTO v_nombre FROM usuarios WHERE id = NEW.tecnico_id;
        INSERT INTO ticket_logs (ticket_id, usuario_id, tipo_evento, tecnico_anterior_id, tecnico_nuevo_id, detalle, creado_en)
        VALUES (NEW.id, v_actor, IF(OLD.tecnico_id IS NULL, 'ASIGNACION', 'REASIGNACION'), OLD.tecnico_id, NEW.tecnico_id, CONCAT('Asignado a ', v_nombre), v_ts);

        CALL sp_notificar(NEW.tecnico_id, NEW.id, 'TICKET_ASIGNADO', CONCAT('Se te asignó ', NEW.codigo), NEW.titulo, v_ts);
    END IF;

    IF NEW.tecnico_id IS NULL AND OLD.tecnico_id IS NOT NULL THEN
        INSERT INTO ticket_logs (ticket_id, usuario_id, tipo_evento, tecnico_anterior_id, tecnico_nuevo_id, detalle, creado_en)
        VALUES (NEW.id, v_actor, 'REASIGNACION', OLD.tecnico_id, NULL, 'Técnico retirado al reabrir el ticket', v_ts);
    END IF;

    IF NEW.estado_id <> OLD.estado_id THEN
        SELECT codigo INTO v_old_codigo FROM estados_ticket WHERE id = OLD.estado_id;
        SELECT codigo INTO v_new_codigo FROM estados_ticket WHERE id = NEW.estado_id;

        SET v_tipo = CASE
            WHEN v_new_codigo = 'CANCELADO' THEN 'CANCELACION'
            WHEN v_old_codigo = 'RESUELTO' AND v_new_codigo IN ('EN_PROCESO','ABIERTO') THEN 'REAPERTURA'
            ELSE 'CAMBIO_ESTADO' END;

        INSERT INTO ticket_logs (ticket_id, usuario_id, tipo_evento, estado_anterior_id, estado_nuevo_id, detalle, creado_en)
        VALUES (NEW.id, v_actor, v_tipo, OLD.estado_id, NEW.estado_id, CONCAT(v_old_codigo, ' -> ', v_new_codigo), v_ts);

        IF v_actor IS NULL OR v_actor <> NEW.solicitante_id THEN
            CALL sp_notificar(
                NEW.solicitante_id, NEW.id,
                IF(v_new_codigo = 'RESUELTO', 'ENCUESTA_DISPONIBLE', 'CAMBIO_ESTADO'),
                CONCAT(NEW.codigo, ': ', REPLACE(v_new_codigo, '_', ' ')),
                IF(v_new_codigo = 'RESUELTO',
                   'Tu ticket fue resuelto. Califica la atención o reábrelo si el problema persiste.',
                   NEW.titulo),
                v_ts
            );
        END IF;

        IF v_tipo = 'REAPERTURA' THEN
            -- al técnico que lo había resuelto (si el ticket quedó sin técnico, se usa el anterior)
            CALL sp_notificar(COALESCE(NEW.tecnico_id, OLD.tecnico_id), NEW.id, 'TICKET_REABIERTO',
                              CONCAT(NEW.codigo, ' fue reabierto'), NEW.titulo, v_ts);
            -- y a los administradores, que deben reasignarlo
            INSERT INTO notificaciones (usuario_id, ticket_id, tipo, titulo, cuerpo, creado_en)
            SELECT u.id, NEW.id, 'TICKET_REABIERTO', CONCAT(NEW.codigo, ' fue reabierto'), LEFT(NEW.titulo, 300), v_ts
            FROM usuarios u JOIN roles r ON r.id = u.rol_id
            WHERE r.codigo = 'ADMINISTRADOR_TI' AND u.activo = TRUE;
        END IF;
    END IF;

    IF NEW.prioridad_id <> OLD.prioridad_id THEN
        INSERT INTO ticket_logs (ticket_id, usuario_id, tipo_evento, prioridad_anterior_id, prioridad_nueva_id, detalle, creado_en)
        VALUES (NEW.id, v_actor, 'CAMBIO_PRIORIDAD', OLD.prioridad_id, NEW.prioridad_id, 'Prioridad reclasificada', v_ts);
    END IF;

    IF NOT (NEW.complejidad <=> OLD.complejidad) THEN
        INSERT INTO ticket_logs (ticket_id, usuario_id, tipo_evento, detalle, creado_en)
        VALUES (NEW.id, v_actor, 'CAMBIO_COMPLEJIDAD',
                CONCAT('Complejidad: ', IFNULL(OLD.complejidad, 'sin definir'), ' -> ', IFNULL(NEW.complejidad, 'sin definir')), v_ts);
    END IF;

    IF NOT (NEW.fecha_limite_tecnico <=> OLD.fecha_limite_tecnico) AND NEW.estado_id = OLD.estado_id THEN
        INSERT INTO ticket_logs (ticket_id, usuario_id, tipo_evento, detalle, creado_en)
        VALUES (NEW.id, v_actor, 'CAMBIO_PLAZO',
                IF(NEW.plazo_dias IS NULL, 'Plazo del técnico retirado', CONCAT('Plazo del técnico: ', NEW.plazo_dias, ' día(s)')), v_ts);
    END IF;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Volcando estructura para disparador soporte_db.trg_tickets_bi
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_tickets_bi` BEFORE INSERT ON `tickets` FOR EACH ROW BEGIN
    DECLARE v_anio INT;
    DECLARE v_n INT;
    DECLARE v_min_resp INT;
    DECLARE v_min_res INT;
    
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id = NEW.solicitante_id AND activo = TRUE) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El solicitante no existe o está inactivo';
    END IF;
    
    SET v_anio = YEAR(NEW.fecha_creacion);
    
    INSERT INTO secuencias_ticket (anio, ultimo) VALUES (v_anio, 1)
    ON DUPLICATE KEY UPDATE ultimo = ultimo + 1;
    
    SELECT ultimo INTO v_n FROM secuencias_ticket WHERE anio = v_anio;
    
    SET NEW.codigo = CONCAT('TCK-', v_anio, '-', LPAD(v_n, 4, '0'));
    SET NEW.estado_id = (SELECT id FROM estados_ticket WHERE codigo = 'ABIERTO');
    SET NEW.prioridad_original_id = NEW.prioridad_id;
    SET NEW.tecnico_id = NULL;
    SET NEW.veces_reabierto = 0;
    
    SELECT minutos_respuesta, minutos_resolucion INTO v_min_resp, v_min_res
    FROM prioridades WHERE id = NEW.prioridad_id;
    
    SET NEW.fecha_limite_respuesta  = DATE_ADD(NEW.fecha_creacion, INTERVAL v_min_resp MINUTE);
    SET NEW.fecha_limite_resolucion = DATE_ADD(NEW.fecha_creacion, INTERVAL v_min_res MINUTE);
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Volcando estructura para disparador soporte_db.trg_tickets_bu
SET @OLDTMP_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
DELIMITER //
CREATE TRIGGER `trg_tickets_bu` BEFORE UPDATE ON `tickets` FOR EACH ROW BEGIN
    DECLARE v_ts DATETIME DEFAULT IFNULL(@app_fecha_evento, CURRENT_TIMESTAMP);
    DECLARE v_old_codigo VARCHAR(15);
    DECLARE v_new_codigo VARCHAR(15);
    DECLARE v_min_resp INT;
    DECLARE v_min_res INT;
    DECLARE v_plazo_reapertura INT;

    SELECT codigo INTO v_old_codigo FROM estados_ticket WHERE id = OLD.estado_id;

    IF v_old_codigo IN ('CERRADO','CANCELADO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El ticket está CERRADO o CANCELADO y no admite modificaciones';
    END IF;

    IF NEW.codigo <> OLD.codigo OR NEW.solicitante_id <> OLD.solicitante_id OR NEW.fecha_creacion <> OLD.fecha_creacion THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Código, solicitante y fecha de creación son inmodificables';
    END IF;

    -- Retirar técnico: solo permitido al reabrir un ticket RESUELTO (vuelve a ABIERTO)
    IF NEW.tecnico_id IS NULL AND OLD.tecnico_id IS NOT NULL THEN
        IF NOT (v_old_codigo = 'RESUELTO'
                AND NEW.estado_id = (SELECT id FROM estados_ticket WHERE codigo = 'ABIERTO')) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede quitar el técnico; use reasignación';
        END IF;

    -- Asignación / reasignación de técnico
    ELSEIF NEW.tecnico_id IS NOT NULL AND (OLD.tecnico_id IS NULL OR NEW.tecnico_id <> OLD.tecnico_id) THEN
        IF NOT EXISTS (SELECT 1 FROM usuarios u JOIN roles r ON r.id = u.rol_id
                       WHERE u.id = NEW.tecnico_id AND u.activo = TRUE AND r.codigo = 'TECNICO_TI') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no es un técnico de TI activo';
        END IF;

        IF v_old_codigo = 'ABIERTO' AND NEW.estado_id = OLD.estado_id THEN
            SET NEW.estado_id = (SELECT id FROM estados_ticket WHERE codigo = 'ASIGNADO');
        END IF;
    END IF;

    -- Cambio de estado
    IF NEW.estado_id <> OLD.estado_id THEN
        SELECT codigo INTO v_new_codigo FROM estados_ticket WHERE id = NEW.estado_id;

        IF NOT EXISTS (SELECT 1 FROM transiciones_estado
                       WHERE estado_origen_id = OLD.estado_id AND estado_destino_id = NEW.estado_id) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transición no permitida';
        END IF;

        IF v_new_codigo = 'ASIGNADO' THEN
            IF NEW.tecnico_id IS NULL THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Debe indicar un técnico para pasar a ASIGNADO';
            END IF;
            SET NEW.fecha_asignacion = v_ts;

        ELSEIF v_new_codigo IN ('EN_PROCESO','ABIERTO') THEN
            IF v_old_codigo = 'RESUELTO' THEN
                -- REAPERTURA (a EN_PROCESO por el técnico, o a ABIERTO por el usuario final)
                SET v_plazo_reapertura = IFNULL(fn_param('plazo_reapertura_horas'), 48);
                IF v_ts > DATE_ADD(OLD.fecha_resolucion, INTERVAL v_plazo_reapertura HOUR) THEN
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Venció el plazo para reabrir el ticket';
                END IF;
                SET NEW.veces_reabierto = OLD.veces_reabierto + 1;
                SET NEW.fecha_ultima_reapertura = v_ts;
                SET NEW.fecha_resolucion = NULL;
                IF v_new_codigo = 'ABIERTO' THEN
                    -- vuelve a la cola del admin: sin técnico y sin plazo
                    SET NEW.plazo_dias = NULL;
                    SET NEW.fecha_limite_tecnico = NULL;
                END IF;
            ELSEIF v_new_codigo = 'EN_PROCESO' THEN
                SET NEW.fecha_inicio_atencion = IFNULL(OLD.fecha_inicio_atencion, v_ts);
                SET NEW.fecha_primera_respuesta = IFNULL(OLD.fecha_primera_respuesta, v_ts);
            END IF;

        ELSEIF v_new_codigo = 'RESUELTO' THEN
            IF NEW.solucion_aplicada IS NULL OR TRIM(NEW.solucion_aplicada) = '' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Debe registrar la solución aplicada para resolver el ticket';
            END IF;
            SET NEW.fecha_resolucion = v_ts;

        ELSEIF v_new_codigo = 'CERRADO' THEN
            SET NEW.fecha_cierre = v_ts;

        ELSEIF v_new_codigo = 'CANCELADO' THEN
            SET NEW.fecha_cancelacion = v_ts;
        END IF;
    END IF;

    -- Cambio de prioridad
    IF NEW.prioridad_id <> OLD.prioridad_id THEN
        SELECT minutos_respuesta, minutos_resolucion INTO v_min_resp, v_min_res FROM prioridades WHERE id = NEW.prioridad_id;
        SET NEW.fecha_limite_respuesta  = DATE_ADD(NEW.fecha_creacion, INTERVAL v_min_resp MINUTE);
        SET NEW.fecha_limite_resolucion = DATE_ADD(NEW.fecha_creacion, INTERVAL v_min_res MINUTE);
    END IF;
END//
DELIMITER ;
SET SQL_MODE=@OLDTMP_SQL_MODE;

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_carga_tecnicos`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_carga_tecnicos` AS select `u`.`id` AS `tecnico_id`,concat(`u`.`nombres`,' ',`u`.`apellidos`) AS `tecnico`,`u`.`nivel_soporte` AS `nivel_soporte`,sum((case when (`e`.`codigo` = 'ASIGNADO') then 1 else 0 end)) AS `asignados`,sum((case when (`e`.`codigo` = 'EN_PROCESO') then 1 else 0 end)) AS `en_proceso`,sum((case when (`e`.`codigo` in ('ASIGNADO','EN_PROCESO')) then 1 else 0 end)) AS `carga_activa` from (((`usuarios` `u` join `roles` `r` on(((`r`.`id` = `u`.`rol_id`) and (`r`.`codigo` = 'TECNICO_TI')))) left join `tickets` `t` on((`t`.`tecnico_id` = `u`.`id`))) left join `estados_ticket` `e` on((`e`.`id` = `t`.`estado_id`))) where (`u`.`activo` = true) group by `u`.`id`,`u`.`nombres`,`u`.`apellidos`,`u`.`nivel_soporte` order by `carga_activa` desc;

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_csat`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_csat` AS select date_format(`ss`.`creado_en`,'%Y-%m-01') AS `mes`,`s`.`nombre` AS `servicio`,count(0) AS `encuestas`,round(avg(`ss`.`calificacion`),2) AS `csat_promedio`,round(((100.0 * sum((case when (`ss`.`calificacion` >= 4) then 1 else 0 end))) / count(0)),1) AS `pct_satisfechos` from ((`satisfaction_surveys` `ss` join `tickets` `t` on((`t`.`id` = `ss`.`ticket_id`))) join `servicios` `s` on((`s`.`id` = `t`.`servicio_id`))) group by date_format(`ss`.`creado_en`,'%Y-%m-01'),`s`.`nombre` order by date_format(`ss`.`creado_en`,'%Y-%m-01'),`s`.`nombre`;

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_dashboard_ti`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_dashboard_ti` AS select `e`.`orden` AS `orden`,`e`.`codigo` AS `estado`,`p`.`nivel` AS `nivel`,`p`.`codigo` AS `prioridad`,count(`t`.`id`) AS `total` from ((`estados_ticket` `e` join `prioridades` `p`) left join `tickets` `t` on(((`t`.`estado_id` = `e`.`id`) and (`t`.`prioridad_id` = `p`.`id`)))) group by `e`.`orden`,`e`.`codigo`,`p`.`nivel`,`p`.`codigo` order by `e`.`orden`,`p`.`nivel`;

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_estados_ticket`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_estados_ticket` AS select `estados_ticket`.`id` AS `id`,`estados_ticket`.`codigo` AS `codigo`,`estados_ticket`.`nombre` AS `nombre`,`estados_ticket`.`orden` AS `orden`,`estados_ticket`.`es_final` AS `es_final` from `estados_ticket`;

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_notificaciones`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_notificaciones` AS select `n`.`id` AS `id`,`n`.`usuario_id` AS `usuario_id`,`n`.`ticket_id` AS `ticket_id`,`t`.`codigo` AS `ticket_codigo`,`n`.`tipo` AS `tipo`,`n`.`titulo` AS `titulo`,`n`.`cuerpo` AS `cuerpo`,`n`.`leida` AS `leida`,`n`.`creado_en` AS `creado_en`,`n`.`leida_en` AS `leida_en` from (`notificaciones` `n` left join `tickets` `t` on((`t`.`id` = `n`.`ticket_id`)));

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_reporte_sla`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_reporte_sla` AS select `s`.`nombre` AS `servicio`,`p`.`codigo` AS `prioridad`,count(0) AS `tickets_resueltos`,sum((case when (`t`.`fecha_resolucion` <= `t`.`fecha_limite_resolucion`) then 1 else 0 end)) AS `dentro_sla`,round(((100.0 * sum((case when (`t`.`fecha_resolucion` <= `t`.`fecha_limite_resolucion`) then 1 else 0 end))) / count(0)),1) AS `pct_cumplimiento_sla`,round(avg(timestampdiff(MINUTE,`t`.`fecha_creacion`,`t`.`fecha_primera_respuesta`)),1) AS `min_prom_primera_respuesta`,round((avg(timestampdiff(SECOND,`t`.`fecha_creacion`,`t`.`fecha_resolucion`)) / 3600),2) AS `horas_prom_resolucion` from (((`tickets` `t` join `servicios` `s` on((`s`.`id` = `t`.`servicio_id`))) join `prioridades` `p` on((`p`.`id` = `t`.`prioridad_id`))) join `estados_ticket` `e` on((`e`.`id` = `t`.`estado_id`))) where (`e`.`codigo` in ('RESUELTO','CERRADO')) group by `s`.`nombre`,`p`.`codigo`,`p`.`nivel` order by `s`.`nombre`,`p`.`nivel`;

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_tickets_detalle`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_tickets_detalle` AS select `t`.`id` AS `id`,`t`.`codigo` AS `codigo`,`t`.`titulo` AS `titulo`,`t`.`descripcion` AS `descripcion`,`s`.`nombre` AS `servicio`,`s`.`categoria` AS `categoria`,`p`.`codigo` AS `prioridad`,`p`.`nivel` AS `prioridad_nivel`,`p`.`color_hex` AS `color_hex`,`e`.`codigo` AS `estado`,`t`.`solicitante_id` AS `solicitante_id`,concat(`sol`.`nombres`,' ',`sol`.`apellidos`) AS `solicitante`,`a`.`nombre` AS `area_solicitante`,`t`.`ubicacion` AS `ubicacion`,`t`.`tecnico_id` AS `tecnico_id`,concat(`tec`.`nombres`,' ',`tec`.`apellidos`) AS `tecnico`,`t`.`fecha_creacion` AS `fecha_creacion`,`t`.`fecha_asignacion` AS `fecha_asignacion`,`t`.`fecha_primera_respuesta` AS `fecha_primera_respuesta`,`t`.`fecha_resolucion` AS `fecha_resolucion`,`t`.`fecha_cierre` AS `fecha_cierre`,`t`.`fecha_limite_respuesta` AS `fecha_limite_respuesta`,`t`.`fecha_limite_resolucion` AS `fecha_limite_resolucion`,(case when (`e`.`codigo` in ('ABIERTO','ASIGNADO','EN_PROCESO')) then timestampdiff(MINUTE,now(),`t`.`fecha_limite_resolucion`) end) AS `tiempo_restante_sla`,(case when (`e`.`codigo` in ('RESUELTO','CERRADO')) then (`t`.`fecha_resolucion` > `t`.`fecha_limite_resolucion`) when (`e`.`codigo` = 'CANCELADO') then NULL else (now() > `t`.`fecha_limite_resolucion`) end) AS `sla_vencido`,`t`.`veces_reabierto` AS `veces_reabierto`,`t`.`solucion_aplicada` AS `solucion_aplicada`,`e`.`nombre` AS `estado_nombre`,`e`.`orden` AS `estado_orden`,`e`.`es_final` AS `estado_final`,`t`.`complejidad` AS `complejidad`,`t`.`plazo_dias` AS `plazo_dias`,`t`.`fecha_limite_tecnico` AS `fecha_limite_tecnico`,(case when ((`e`.`codigo` in ('ASIGNADO','EN_PROCESO')) and (`t`.`fecha_limite_tecnico` is not null)) then timestampdiff(MINUTE,now(),`t`.`fecha_limite_tecnico`) end) AS `tiempo_restante_tecnico` from ((((((`tickets` `t` join `servicios` `s` on((`s`.`id` = `t`.`servicio_id`))) join `prioridades` `p` on((`p`.`id` = `t`.`prioridad_id`))) join `estados_ticket` `e` on((`e`.`id` = `t`.`estado_id`))) join `usuarios` `sol` on((`sol`.`id` = `t`.`solicitante_id`))) left join `areas` `a` on((`a`.`id` = `sol`.`area_id`))) left join `usuarios` `tec` on((`tec`.`id` = `t`.`tecnico_id`)));

-- Eliminando tabla temporal y crear estructura final de VIEW
DROP TABLE IF EXISTS `v_usuarios_detalle`;
CREATE ALGORITHM=UNDEFINED SQL SECURITY DEFINER VIEW `v_usuarios_detalle` AS select `u`.`id` AS `id`,`u`.`rol_id` AS `rol_id`,`r`.`codigo` AS `rol_codigo`,`r`.`nombre` AS `rol_nombre`,`u`.`area_id` AS `area_id`,`a`.`nombre` AS `area_nombre`,`u`.`nombres` AS `nombres`,`u`.`apellidos` AS `apellidos`,concat(`u`.`nombres`,' ',`u`.`apellidos`) AS `nombre_completo`,`u`.`email` AS `email`,`u`.`cargo` AS `cargo`,`u`.`telefono` AS `telefono`,`u`.`nivel_soporte` AS `nivel_soporte`,`u`.`activo` AS `activo`,`u`.`genero` AS `genero`,(case `u`.`genero` when 1 then 'hombre' when 2 then 'mujer' else 'sin_foto' end) AS `foto_tipo`,`u`.`intentos_fallidos` AS `intentos_fallidos`,`u`.`bloqueado_hasta` AS `bloqueado_hasta`,`u`.`ultimo_login` AS `ultimo_login`,`u`.`creado_en` AS `creado_en`,`u`.`actualizado_en` AS `actualizado_en` from ((`usuarios` `u` join `roles` `r` on((`r`.`id` = `u`.`rol_id`))) left join `areas` `a` on((`a`.`id` = `u`.`area_id`)));

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
