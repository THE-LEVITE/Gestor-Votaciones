<?php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/auth.php';
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><?= isset($titulo) ? h($titulo) : 'Gestion de Eventos QR' ?> · CIMM</title>
    <link rel="stylesheet" href="assets/css/styles.css">
</head>
<body>
<header class="topbar">
    <div class="brand">
        <a href="index.php" style="color: inherit; text-decoration: none; display: flex; align-items: center; gap: 0.6rem;">
            <span class="logo">CIMM</span>
            <span>Gestion de Eventos <small>· Entrada por QR</small></span>
        </a>
    </div>
    <nav>
        <a href="index.php">Eventos</a>
        <?php if (is_admin()): ?>
            <a href="eventos_admin.php">Programar Eventos</a>
            <a href="asistentes.php">Ver Asistentes</a>
            <a href="validar.php">Validar QR</a>
        <?php elseif (is_logged_in()): ?>
            <a href="mis_entradas.php">Mis Entradas / QR</a>
        <?php else: ?>
            <a href="consultar_qr.php">Consultar QR</a>
        <?php endif; ?>

        <?php if (is_logged_in()): ?>
            <?php $u = current_user(); ?>
            <span class="user-tag <?= is_admin() ? 'tag-admin' : 'tag-user' ?>">
                👤 <?= h($u['nombre_completo'] ?? $u['username']) ?>
                <span class="role-pill"><?= h($u['rol']) ?></span>
            </span>
            <a href="logout.php" class="link-logout">Cerrar sesión</a>
        <?php else: ?>
            <a href="login.php" class="nav-btn">Iniciar Sesión</a>
            <a href="registro.php" class="nav-btn primary">Registrarse</a>
        <?php endif; ?>
    </nav>
</header>
<main class="container">
