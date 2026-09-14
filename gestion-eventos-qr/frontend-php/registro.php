<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

if (is_logged_in()) {
    header('Location: ' . (is_admin() ? 'eventos_admin.php' : 'mis_entradas.php'));
    exit;
}

$error = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username  = trim($_POST['username'] ?? '');
    $password  = $_POST['password'] ?? '';
    $nombre    = trim($_POST['nombre'] ?? '');
    $documento = trim($_POST['documento'] ?? '');
    $email     = trim($_POST['email'] ?? '');
    $telefono  = trim($_POST['telefono'] ?? '');

    if ($username === '' || $password === '' || $nombre === '' || $documento === '') {
        $error = 'Por favor completa todos los campos obligatorios (*).';
    } elseif (strlen($password) < 4) {
        $error = 'La contraseña debe tener al menos 4 caracteres.';
    } else {
        $res = registrar_usuario($username, $password, $nombre, $documento, $email, $telefono);
        if ($res['ok']) {
            // Auto login tras registro exitoso
            login($username, $password);
            header('Location: mis_entradas.php');
            exit;
        } else {
            $error = $res['error'];
        }
    }
}

$titulo = 'Registro de Usuario';
require __DIR__ . '/includes/header.php';
?>

<div style="max-width: 480px; margin: 1.5rem auto;">
    <div class="card" style="padding: 2rem;">
        <h2 style="margin-top: 0; text-align: center;">Crear Cuenta de Usuario</h2>
        <p class="muted" style="text-align: center; margin-bottom: 1.5rem;">
            Regístrate para gestionar tus inscripciones y descargar tus entradas con QR
        </p>

        <?php if ($error): ?>
            <div class="alert error"><?= h($error) ?></div>
        <?php endif; ?>

        <form method="post" class="form" autocomplete="off">
            <label>Nombre completo *
                <input type="text" name="nombre" required maxlength="120" value="<?= h($_POST['nombre'] ?? '') ?>" placeholder="Ej: Juan Pérez">
            </label>

            <label>Documento de identidad *
                <input type="text" name="documento" required maxlength="20" value="<?= h($_POST['documento'] ?? '') ?>" placeholder="Ej: 1020304050">
            </label>

            <label>Correo electrónico
                <input type="email" name="email" maxlength="120" value="<?= h($_POST['email'] ?? '') ?>" placeholder="juan@correo.com">
            </label>

            <label>Teléfono
                <input type="tel" name="telefono" maxlength="20" value="<?= h($_POST['telefono'] ?? '') ?>" placeholder="Ej: 3101234567">
            </label>

            <label>Nombre de usuario *
                <input type="text" name="username" required maxlength="50" value="<?= h($_POST['username'] ?? '') ?>" placeholder="Ej: juanperez">
            </label>

            <label>Contraseña *
                <input type="password" name="password" required placeholder="Crea una contraseña segura">
            </label>

            <button class="btn" type="submit" style="width: 100%; margin-top: 0.5rem;">Registrarme</button>
        </form>

        <div style="margin-top: 1.5rem; padding-top: 1rem; border-top: 1px solid var(--border); font-size: 0.9rem; text-align: center;">
            ¿Ya tienes una cuenta? <a href="login.php"><strong>Iniciar sesión</strong></a>
        </div>
    </div>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>
