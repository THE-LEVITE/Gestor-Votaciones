<?php
/**
 * SISTEMA DE VOTACIONES Y ENCUESTAS INSTITUCIONALES SEGURAS (SVIS)
 * Archivo de Configuración y Cliente REST cURL
 * 
 * Seguridad:
 * - NO CONTIENE credenciales de Base de Datos MySQL.
 * - Toda la persistencia y lógica transaccional es delegada al backend Java.
 */

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

// URL base del backend Java desplegado en Tomcat
// Por defecto en Tomcat: http://localhost:8080/svis-api/api
define('API_BASE_URL', getenv('API_BASE_URL') ?: 'http://localhost:8080/svis-api/api');

/**
 * Cliente HTTP basado en cURL para invocar el Backend Java de manera segura y desacoplada.
 *
 * @param string $method GET, POST, PUT, DELETE
 * @param string $endpoint Ruta relativa (ej: '/encuestas/activas', '/votos/emitir')
 * @param mixed $data Arreglo asociativo o string que se enviará como JSON
 * @return array ['status' => int, 'data' => array|null, 'raw' => string, 'error' => string|null]
 */
function callApi($method, $endpoint, $data = null) {
    $url = rtrim(API_BASE_URL, '/') . '/' . ltrim($endpoint, '/');
    $ch = curl_init($url);

    $headers = [
        'Content-Type: application/json; charset=UTF-8',
        'Accept: application/json'
    ];

    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, strtoupper($method));
    curl_setopt($ch, CURLOPT_TIMEOUT, 15);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);

    if ($data !== null && in_array(strtoupper($method), ['POST', 'PUT', 'PATCH'])) {
        $jsonPayload = is_string($data) ? $data : json_encode($data, JSON_UNESCAPED_UNICODE);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonPayload);
        $headers[] = 'Content-Length: ' . strlen($jsonPayload);
    }

    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    curl_close($ch);

    if ($response === false) {
        return [
            'status' => 0,
            'data'   => null,
            'raw'    => '',
            'error'  => 'Error de comunicación con el backend SVIS: ' . $curlError
        ];
    }

    $decoded = json_decode($response, true);

    return [
        'status' => $httpCode,
        'data'   => $decoded,
        'raw'    => $response,
        'error'  => ($httpCode >= 400) ? ($decoded['error'] ?? 'Error HTTP ' . $httpCode) : null
    ];
}

/**
 * Valida si el usuario actual ha iniciado sesión en PHP.
 */
function isLoggedIn() {
    return isset($_SESSION['usuario']) && !empty($_SESSION['usuario']['id']);
}

/**
 * Exige autenticación y opcionalmente un rol específico ('ADMIN' o 'VOTANTE').
 */
function requireAuth($rolRequerido = null) {
    if (!isLoggedIn()) {
        header('Location: ../login.php');
        exit;
    }

    if ($rolRequerido !== null) {
        $rolActual = $_SESSION['usuario']['rol'] ?? '';
        if ($rolActual !== $rolRequerido) {
            if ($rolActual === 'ADMIN') {
                header('Location: ../admin/dashboard.php');
            } else {
                header('Location: ../votante/votar.php');
            }
            exit;
        }
    }
}

/**
 * Retorna la información del usuario en sesión activa.
 */
function getLoggedUser() {
    return $_SESSION['usuario'] ?? null;
}
