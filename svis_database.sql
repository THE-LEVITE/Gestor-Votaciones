-- =============================================================================
-- SISTEMA DE VOTACIONES Y ENCUESTAS INSTITUCIONALES SEGURAS (SVIS)
-- SCRIPT SQL DE DEFINICIÓN DE BASE DE DATOS Y DATOS DE PRUEBA
-- Compatible con phpMyAdmin / MySQL 5.7+ / MySQL 8.0+
-- Motor: InnoDB (Garantía estricta de transacciones ACID y Row-Level Locking)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `svis_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `svis_db`;

-- Desactivar temporalmente revisión de claves foráneas para recreación limpia
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `votos`;
DROP TABLE IF EXISTS `tokens_otp`;
DROP TABLE IF EXISTS `opciones`;
DROP TABLE IF EXISTS `encuestas`;
DROP TABLE IF EXISTS `usuarios`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 1. TABLA: usuarios
-- Almacena los usuarios de la institución (Administradores y Aprendices/Votantes).
-- -----------------------------------------------------------------------------
CREATE TABLE `usuarios` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `documento` VARCHAR(20) NOT NULL UNIQUE,
    `nombre_completo` VARCHAR(120) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `rol` ENUM('ADMIN', 'VOTANTE') NOT NULL DEFAULT 'VOTANTE',
    `estado` ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    `fecha_registro` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_usuario_documento` (`documento`),
    INDEX `idx_usuario_rol` (`rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. TABLA: encuestas
-- Define las jornadas electorales y consultas democráticas institucionales.
-- -----------------------------------------------------------------------------
CREATE TABLE `encuestas` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `titulo` VARCHAR(200) NOT NULL,
    `descripcion` TEXT NULL,
    `estado` ENUM('ACTIVA', 'CERRADA') NOT NULL DEFAULT 'ACTIVA',
    `fecha_creacion` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `fecha_cierre` DATETIME NULL,
    INDEX `idx_encuesta_estado` (`estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. TABLA: opciones
-- Candidatos u opciones de respuesta para cada encuesta.
-- Incluye votos_conteo para el incremento atómico (+1).
-- -----------------------------------------------------------------------------
CREATE TABLE `opciones` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `encuesta_id` INT NOT NULL,
    `nombre` VARCHAR(150) NOT NULL,
    `descripcion` TEXT NULL,
    `votos_conteo` INT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_opciones_encuesta` 
        FOREIGN KEY (`encuesta_id`) REFERENCES `encuestas` (`id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX `idx_opciones_encuesta` (`encuesta_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. TABLA: tokens_otp
-- Control de credenciales de un solo uso (One-Time Password).
-- REGLA 1 (Unicidad): Restricción UNIQUE(encuesta_id, usuario_id).
-- -----------------------------------------------------------------------------
CREATE TABLE `tokens_otp` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `encuesta_id` INT NOT NULL,
    `usuario_id` INT NOT NULL,
    `token` VARCHAR(64) NOT NULL UNIQUE,
    `estado` ENUM('DISPONIBLE', 'USADO', 'EXPIRADO') NOT NULL DEFAULT 'DISPONIBLE',
    `fecha_expiracion` DATETIME NOT NULL,
    `fecha_uso` DATETIME NULL,
    `fecha_creacion` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_tokens_encuesta` 
        FOREIGN KEY (`encuesta_id`) REFERENCES `encuestas` (`id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_tokens_usuario` 
        FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    -- REGLA 1: Un usuario solo puede tener un único token generado por encuesta
    CONSTRAINT `uk_encuesta_usuario` UNIQUE (`encuesta_id`, `usuario_id`),
    INDEX `idx_token_hash` (`token`),
    INDEX `idx_token_estado` (`estado`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. TABLA: votos
-- REGLA 4 (Secreto Absoluto del Voto):
-- Prohibido registrar id del usuario o del token.
-- Solo contiene id, encuesta_id, opcion_id y fecha.
-- -----------------------------------------------------------------------------
CREATE TABLE `votos` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `encuesta_id` INT NOT NULL,
    `opcion_id` INT NOT NULL,
    `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_votos_encuesta` 
        FOREIGN KEY (`encuesta_id`) REFERENCES `encuestas` (`id`) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_votos_opcion` 
        FOREIGN KEY (`opcion_id`) REFERENCES `opciones` (`id`) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX `idx_votos_encuesta` (`encuesta_id`),
    INDEX `idx_votos_opcion` (`opcion_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- DATOS DE PRUEBA (SEED DATA)
-- =============================================================================

-- Contraseñas en texto plano / hash de demostración (admin123 y estudiante123)
INSERT INTO `usuarios` (`id`, `documento`, `nombre_completo`, `email`, `password`, `rol`, `estado`) VALUES
(1, '1000000001', 'Prof. Osman Aranguren (Comité Electoral)', 'admin@institucion.edu', 'admin123', 'ADMIN', 'ACTIVO'),
(2, '1020304001', 'Laura Camila Restrepo', 'laura.restrepo@institucion.edu', 'estudiante123', 'VOTANTE', 'ACTIVO'),
(3, '1020304002', 'Juan Sebastian Morales', 'juan.morales@institucion.edu', 'estudiante123', 'VOTANTE', 'ACTIVO'),
(4, '1020304003', 'Valentina Rios Duarte', 'valentina.rios@institucion.edu', 'estudiante123', 'VOTANTE', 'ACTIVO'),
(5, '1020304004', 'Andres Felipe Martinez', 'andres.martinez@institucion.edu', 'estudiante123', 'VOTANTE', 'ACTIVO'),
(6, '1020304005', 'Daniela Sofia Castro', 'daniela.castro@institucion.edu', 'estudiante123', 'VOTANTE', 'ACTIVO');

-- Encuesta 1: Activa (Elección de Representante Estudiantil 2026)
INSERT INTO `encuestas` (`id`, `titulo`, `descripcion`, `estado`, `fecha_creacion`) VALUES
(1, 'Elección Representante Estudiantil 2026', 'Jornada democrática institucional para elegir el vocero estudiantil ante el Consejo Directivo.', 'ACTIVA', CURRENT_TIMESTAMP);

-- Opciones para la Encuesta 1
INSERT INTO `opciones` (`id`, `encuesta_id`, `nombre`, `descripcion`, `votos_conteo`) VALUES
(1, 1, 'Andrea Gómez - Lista 01 (Liderazgo & Innovación)', 'Propuestas: Modernización de laboratorios y bienestar integral.', 0),
(2, 1, 'Carlos Mendoza - Lista 02 (Transparencia & Acción)', 'Propuestas: Auditoría estudiantil y fortalecimiento de convenios.', 0),
(3, 1, 'Voto en Blanco', 'Opción institucional de inconformidad democrática.', 0);

-- Encuesta 2: Encuesta Cerrada de Demostración con Votos y Resultados
INSERT INTO `encuestas` (`id`, `titulo`, `descripcion`, `estado`, `fecha_creacion`, `fecha_cierre`) VALUES
(2, 'Consulta Institucional: Reforma de Horarios Nocturnos', 'Evaluación de propuesta de cambio de franja horaria para formación nocturna.', 'CERRADA', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO `opciones` (`id`, `encuesta_id`, `nombre`, `descripcion`, `votos_conteo`) VALUES
(4, 2, 'A favor (6:00 PM a 10:00 PM)', 'Iniciar 30 minutos antes para optimizar transporte.', 2),
(5, 2, 'En contra (Mantener 6:30 PM a 10:30 PM)', 'Permanecer en el horario habitual.', 1);

-- Votos anónimos para la Encuesta 2 (Cumpliendo REGLA 4: sin usuario ni token)
INSERT INTO `votos` (`encuesta_id`, `opcion_id`, `fecha`) VALUES
(2, 4, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 4, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 5, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- Tokens OTP de prueba para la Encuesta 1 (Generados con vigencia de 24 horas)
-- Token preasignado al usuario 2 (Laura Restrepo) para pruebas inmediatas
INSERT INTO `tokens_otp` (`encuesta_id`, `usuario_id`, `token`, `estado`, `fecha_expiracion`) VALUES
(1, 2, 'SVIS-DEMO-LAURA-77A1F9', 'DISPONIBLE', DATE_ADD(NOW(), INTERVAL 24 HOUR)),
(1, 3, 'SVIS-DEMO-JUAN-33C4E2', 'DISPONIBLE', DATE_ADD(NOW(), INTERVAL 24 HOUR)),
(1, 4, 'SVIS-DEMO-VALENTINA-99B8D1', 'DISPONIBLE', DATE_ADD(NOW(), INTERVAL 24 HOUR));
