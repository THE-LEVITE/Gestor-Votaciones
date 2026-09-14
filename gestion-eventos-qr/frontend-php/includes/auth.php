<?php
require_once __DIR__ . '/../config.php';

if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

/**
 * Obtiene la conexion PDO a la base de datos MySQL.
 */
function db()
{
    static $pdo = null;
    if ($pdo === null) {
        $dsn = 'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4';
        $pdo = new PDO($dsn, DB_USER, DB_PASS, [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
    }
    return $pdo;
}

/**
 * Retorna los datos del usuario autenticado o null.
 */
function current_user()
{
    return $_SESSION['user'] ?? null;
}

/**
 * Verifica si hay una sesion iniciada.
 */
function is_logged_in()
{
    return !empty($_SESSION['user']);
}

/**
 * Verifica si el usuario actual tiene rol de Administrador.
 */
function is_admin()
{
    $u = current_user();
    return $u && ($u['rol'] ?? '') === 'ADMIN';
}

/**
 * Exige rol de Administrador. Si no lo tiene, redirige a login.
 */
function require_admin()
{
    if (!is_admin()) {
        header('Location: login.php?msg=admin_required');
        exit;
    }
}

/**
 * Exige iniciar sesion. Si no esta autenticado, redirige a login.
 */
function require_login()
{
    if (!is_logged_in()) {
        header('Location: login.php?msg=login_required');
        exit;
    }
}

/**
 * Intenta autenticar un usuario por nombre de usuario o documento.
 */
function login($identificador, $password)
{
    $pdo = db();
    $stmt = $pdo->prepare('SELECT * FROM usuario WHERE username = ? OR documento = ? LIMIT 1');
    $stmt->execute([$identificador, $identificador]);
    $u = $stmt->fetch();

    if ($u && password_verify($password, $u['password'])) {
        unset($u['password']);
        $_SESSION['user'] = $u;
        return ['ok' => true, 'user' => $u];
    }
    return ['ok' => false, 'error' => 'Usuario o contraseña incorrectos'];
}

/**
 * Cierra la sesion actual.
 */
function logout()
{
    $_SESSION['user'] = null;
    unset($_SESSION['user']);
    session_destroy();
}

/**
 * Registra un nuevo usuario con rol USUARIO.
 */
function registrar_usuario($username, $password, $nombre, $documento, $email, $telefono)
{
    $pdo = db();

    // Validar username unico
    $st = $pdo->prepare('SELECT id FROM usuario WHERE username = ? LIMIT 1');
    $st->execute([$username]);
    if ($st->fetch()) {
        return ['ok' => false, 'error' => 'El nombre de usuario ya esta en uso'];
    }

    // Validar documento unico si fue ingresado
    if (!empty($documento)) {
        $st = $pdo->prepare('SELECT id FROM usuario WHERE documento = ? LIMIT 1');
        $st->execute([$documento]);
        if ($st->fetch()) {
            return ['ok' => false, 'error' => 'El numero de documento ya esta registrado'];
        }
    }

    $hash = password_hash($password, PASSWORD_BCRYPT);
    $ins = $pdo->prepare('INSERT INTO usuario (username, password, nombre_completo, documento, email, telefono, rol) VALUES (?, ?, ?, ?, ?, ?, "USUARIO")');
    $ins->execute([$username, $hash, $nombre, $documento, $email, $telefono]);

    return ['ok' => true];
}
