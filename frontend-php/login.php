<?php
require_once __DIR__ . '/config.php';

if (isLoggedIn()) {
    $user = getLoggedUser();
    header('Location: ' . ($user['rol'] === 'ADMIN' ? 'admin/dashboard.php' : 'votante/votar.php'));
    exit;
}

$errorMsg = '';
$identificador = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $identificador = trim($_POST['identificador'] ?? '');
    $password = trim($_POST['password'] ?? '');

    if (empty($identificador) || empty($password)) {
        $errorMsg = 'Por favor ingrese su correo institucional / documento y contraseña.';
    } else {
        // Invocación desacoplada al backend Java
        $apiResponse = callApi('POST', '/auth/login', [
            'identificador' => $identificador,
            'password'      => $password
        ]);

        if ($apiResponse['status'] === 200 && isset($apiResponse['data']['usuario'])) {
            $_SESSION['usuario'] = $apiResponse['data']['usuario'];
            $rol = $_SESSION['usuario']['rol'];
            header('Location: ' . ($rol === 'ADMIN' ? 'admin/dashboard.php' : 'votante/votar.php'));
            exit;
        } else {
            $errorMsg = $apiResponse['error'] ?? 'Credenciales inválidas o servicio no disponible.';
        }
    }
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SVIS - Acceso Institucional Seguro</title>
    <link rel="stylesheet" href="assets/css/style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
</head>
<body class="bg-gradient-auth">
    <div class="auth-wrapper">
        <div class="auth-card">
            <div class="auth-header">
                <div class="brand-badge">SVIS Institucional</div>
                <h1>Sistema de Votaciones y Encuestas</h1>
                <p>Tu espacio para elegir y participar en las decisiones de la comunidad de forma fácil, transparente y confidencial.</p>
            </div>

            <?php if (!empty($errorMsg)): ?>
                <div class="alert alert-danger" role="alert">
                    <span class="alert-icon">⚠️</span>
                    <div><?= htmlspecialchars($errorMsg) ?></div>
                </div>
            <?php endif; ?>

            <form method="POST" action="login.php" class="auth-form">
                <div class="form-group">
                    <label for="identificador">Correo Institucional o Documento</label>
                    <input type="text" id="identificador" name="identificador" 
                           placeholder="ej: admin@institucion.edu o 1020304001" 
                           value="<?= htmlspecialchars($identificador) ?>" required autofocus>
                </div>

                <div class="form-group">
                    <label for="password">Contraseña</label>
                    <input type="password" id="password" name="password" placeholder="••••••••" required>
                </div>

                <button type="submit" class="btn btn-primary btn-block">
                    Ingresar a la Plataforma
                </button>
            </form>

            <div class="demo-accounts">
                <p class="demo-title">Accesos de demostración rápidos:</p>
                <div class="demo-buttons-grid">
                    <button type="button" class="btn btn-sm btn-outline" 
                            onclick="setCreds('admin@institucion.edu', 'admin123')">
                        👑 Administrador
                    </button>
                    <button type="button" class="btn btn-sm btn-outline" 
                            onclick="setCreds('laura.restrepo@institucion.edu', 'estudiante123')">
                        🎓 Estudiante: Laura
                    </button>
                    <button type="button" class="btn btn-sm btn-outline" 
                            onclick="setCreds('juan.morales@institucion.edu', 'estudiante123')">
                        🎓 Estudiante: Juan
                    </button>
                </div>
            </div>

            <div class="auth-footer">
                <small>🔒 Tu voto es personal, único y totalmente confidencial.</small>
            </div>
        </div>
    </div>

    <script>
        function setCreds(user, pass) {
            document.getElementById('identificador').value = user;
            document.getElementById('password').value = pass;
        }
    </script>
</body>
</html>
