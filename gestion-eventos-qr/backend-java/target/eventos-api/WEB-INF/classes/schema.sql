-- =====================================================================
--  Gestion de Eventos QR - Esquema MySQL
--  Ejecutar una sola vez:  mysql -u root -p < schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS eventos_qr
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eventos_qr;

-- Orden de borrado respetando llaves foraneas
DROP TABLE IF EXISTS inscripcion;
DROP TABLE IF EXISTS asistente;
DROP TABLE IF EXISTS evento;

CREATE TABLE evento (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL,
    descripcion  VARCHAR(400),
    lugar        VARCHAR(120),
    fecha        DATETIME,
    cupo_maximo  INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE asistente (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(120) NOT NULL,
    documento       VARCHAR(20)  NOT NULL,
    email           VARCHAR(120),
    telefono        VARCHAR(20)
) ENGINE=InnoDB;

CREATE TABLE inscripcion (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    token             VARCHAR(36) NOT NULL UNIQUE,
    evento_id         BIGINT NOT NULL,
    asistente_id      BIGINT NOT NULL,
    estado            VARCHAR(20) NOT NULL,
    fecha_inscripcion DATETIME NOT NULL,
    fecha_validacion  DATETIME,
    CONSTRAINT fk_insc_evento
        FOREIGN KEY (evento_id) REFERENCES evento(id),
    CONSTRAINT fk_insc_asistente
        FOREIGN KEY (asistente_id) REFERENCES asistente(id),
    INDEX idx_insc_evento (evento_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Datos de ejemplo (equivalentes a los del modo memoria)
-- ---------------------------------------------------------------------
INSERT INTO evento (nombre, descripcion, lugar, fecha, cupo_maximo) VALUES
('Feria de Innovacion CIMM 2026',
 'Muestra de proyectos de aprendices de tecnologia y manufactura.',
 'Auditorio Principal - CIMM, Paipa',
 DATE_ADD(NOW(), INTERVAL 7 DAY), 120),
('Charla: Ingenieria Agentica y Prompt Engineering',
 'Introduccion al estandar PIC 2026 y herramientas de IA generativa.',
 'Sala de Sistemas 2 - CIMM',
 DATE_ADD(NOW(), INTERVAL 14 DAY), 40),
('Taller de Mantenimiento Industrial 4.0',
 'Sensorica, IoT y mantenimiento predictivo en planta.',
 'Taller de Mecatronica - CIMM',
 DATE_ADD(NOW(), INTERVAL 21 DAY), 30);
