<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

if (is_logged_in()) {
    header('Location: ' . (is_admin() ? 'eventos_admin.php' : 'mis_entradas.php'));
    exit;
}

$error = null;
$mensaje = null;

if (isset($_GET['msg'])) {
    if ($_GET['msg'] === 'admin_required') {
        $error = 'Acceso restringido: Debes iniciar sesion como Administrador.';
    } elseif ($_GET['msg'] === 'login_required') {
        $error = 'Por favor inicia sesion para acceder a esta seccion.';
    } elseif ($_GET['msg'] === 'registrado') {
        $mensaje = 'Registro exitoso. Ya puedes iniciar sesion con tu cuenta.';
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $usuario = trim($_POST['usuario'] ?? '');
    $password = $_POST['password'] ?? '';

    if ($usuario === '' || $password === '') {
        $error = 'Por favor ingresa tu usuario y contraseña.';
    } else {
        $res = login($usuario, $password);
        if ($res['ok']) {
            if (is_admin()) {
                header('Location: eventos_admin.php');
            } else {
                header('Location: mis_entradas.php');
            }
            exit;
        } else {
            $error = $res['error'];
        }
    }
}

$titulo = 'Iniciar Sesion';
require __DIR__ . '/includes/header.php';
?>

<div style="max-width: 440px; margin: 2rem auto;">
    <div class="card" style="padding: 2rem;">
        <h2 style="margin-top: 0; text-align: center;">Iniciar Sesión</h2>
        <p class="muted" style="text-align: center; margin-bottom: 1.5rem;">
            Ingresa a tu cuenta de <strong>Administrador</strong> o <strong>Usuario</strong>
        </p>

        <?php if ($mensaje): ?>
            <div class="alert ok"><?= h($mensaje) ?></div>
        <?php endif; ?>

        <?php if ($error): ?>
            <div class="alert error"><?= h($error) ?></div>
        <?php endif; ?>

        <form method="post" class="form" autocomplete="off">
            <label>Usuario o No. de Documento
                <input type="text" name="usuario" required autofocus placeholder="Ej: admin o tu documento">
            </label>

            <label>Contraseña
                <input type="password" name="password" required placeholder="Tu contraseña">
            </label>

            <button class="btn" type="submit" style="width: 100%; margin-top: 0.5rem;">Entrar</button>
        </form>

        <div style="margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid var(--border); font-size: 0.9rem; text-align: center;">
            <p style="margin: 0.3rem 0;">¿No tienes cuenta de usuario? <a href="registro.php"><strong>Registrarse aquí</strong></a></p>
            <p style="margin: 0.3rem 0;"><a href="consultar_qr.php">¿Solo deseas consultar tu QR sin iniciar sesión?</a></p>
        </div>

        <div style="margin-top: 1.2rem; background: #f9fafb; border: 1px dashed var(--border); border-radius: 6px; padding: 0.8rem; font-size: 0.82rem; color: #555;">
            <strong>Credenciales de Administrador por defecto:</strong><br>
            Usuario: <code>admin</code> | Contraseña: <code>admin123</code>
        </div>
    </div>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>
