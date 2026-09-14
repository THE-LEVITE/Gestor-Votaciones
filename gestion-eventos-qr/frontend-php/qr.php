<?php
/**
 * Proxy del codigo QR: pide la imagen PNG al backend Java y la reenvia al
 * navegador. Asi el navegador nunca llama directo al backend (mismo origen).
 */
require_once __DIR__ . '/config.php';

$token = $_GET['token'] ?? '';
if ($token === '') {
    http_response_code(400);
    header('Content-Type: text/plain; charset=utf-8');
    echo 'Falta el token';
    exit;
}

$url = API_BASE_URL . '/qr/' . rawurlencode($token);
$ch  = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_TIMEOUT, 15);
$body  = curl_exec($ch);
$code  = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$ctype = curl_getinfo($ch, CURLINFO_CONTENT_TYPE) ?: 'image/png';
curl_close($ch);

if ($body !== false && $code >= 200 && $code < 300) {
    header('Content-Type: ' . $ctype);
    echo $body;
} else {
    http_response_code($code ?: 502);
    header('Content-Type: text/plain; charset=utf-8');
    echo 'No se pudo obtener el QR';
}
