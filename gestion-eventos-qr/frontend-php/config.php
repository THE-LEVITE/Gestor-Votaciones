<?php
/**
 * Configuracion del frontend PHP.
 * API_BASE_URL apunta al backend Java desplegado en Tomcat.
 */
define('API_BASE_URL', getenv('API_BASE_URL') ?: 'http://localhost:8080/api');
define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_NAME', getenv('DB_NAME') ?: 'eventos_qr');
define('DB_USER', getenv('DB_USER') ?: 'root');
define('DB_PASS', getenv('DB_PASS') ?: '');
