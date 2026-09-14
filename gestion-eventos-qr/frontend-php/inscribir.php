<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

$titulo    = 'Inscripción a Evento';
$eventoId  = isset($_GET['evento']) ? (int) $_GET['evento'] : 0;
$resultado = null;
$error     = null;

$user = current_user();

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $payload = [
        'eventoId'       => (int) ($_POST['eventoId'] ?? 0),
        'nombreCompleto' => trim($_POST['nombreCompleto'] ?? ''),
        'documento'      => trim($_POST['documento'] ?? ''),
        'email'          => trim($_POST['email'] ?? ''),
        'telefono'       => trim($_POST['telefono'] ?? ''),
    ];
    $eventoId = $payload['eventoId'];
    $r = api()->post('/inscripciones', $payload);
    if ($r['ok']) {
        $resultado = $r['data'];
    } else {
        $error = ($r['data']['mensaje'] ?? null) ?: ($r['error'] ?? 'No se pudo completar la inscripcion');
    }
}

// Datos del evento para el encabezado del formulario
$evento = null;
if ($eventoId && !$resultado) {
    $er = api()->get('/eventos/' . $eventoId);
    if ($er['ok']) {
        $evento = $er['data'];
    }
}

require __DIR__ . '/includes/header.php';
?>
<?php if ($resultado): ?>
    <div class="ticket">
        <h1>Inscripción confirmada</h1>
        <p class="alert ok"><?= h($resultado['mensaje']) ?></p>
        <div class="qr-box">
            <img src="qr.php?token=<?= h($resultado['token']) ?>" alt="Codigo QR de ingreso" width="300" height="300">
        </div>
        <dl class="detalle">
            <dt>Asistente</dt><dd><?= h($resultado['asistenteNombre']) ?></dd>
            <dt>Evento</dt><dd><?= h($resultado['eventoNombre']) ?></dd>
            <dt>Fecha</dt><dd><?= h(fmtFecha($resultado['eventoFecha'] ?? null)) ?></dd>
            <dt>Lugar</dt><dd><?= h($resultado['eventoLugar'] ?? 'Por definir') ?></dd>
            <dt>Código Token</dt><dd><code><?= h($resultado['token']) ?></code></dd>
        </dl>
        <div class="acciones">
            <button class="btn" onclick="window.print()">Imprimir / Guardar Ticket</button>
            <?php if (is_logged_in()): ?>
                <a class="btn" href="mis_entradas.php">Ver Mis Entradas</a>
            <?php else: ?>
                <a class="btn ghost" href="consultar_qr.php?documento=<?= urlencode($payload['documento'] ?? '') ?>">Guardar enlace de consulta</a>
            <?php endif; ?>
            <a class="btn ghost" href="index.php">Volver a eventos</a>
        </div>
    </div>
<?php else: ?>
    <h1>Formulario de inscripción</h1>
    <?php if ($error): ?>
        <div class="alert error"><?= h($error) ?></div>
    <?php endif; ?>

    <?php if (!$eventoId): ?>
        <div class="alert error">No se indicó un evento. <a href="index.php">Elige uno aquí</a>.</div>
    <?php else: ?>
        <?php if ($evento): ?>
            <div class="card resumen" style="margin-bottom: 1.5rem;">
                <h3 style="margin-top: 0;"><?= h($evento['nombre']) ?></h3>
                <p class="muted"><?= h(fmtFecha($evento['fecha'] ?? null)) ?> · <?= h($evento['lugar'] ?? '') ?></p>
                <p style="margin-bottom: 0;">Cupos disponibles: <strong><?= (int) $evento['cupoDisponible'] ?></strong></p>
            </div>
        <?php endif; ?>

        <div class="card" style="max-width: 600px;">
            <form method="post" class="form" autocomplete="off">
                <input type="hidden" name="eventoId" value="<?= (int) $eventoId ?>">

                <label>Nombre completo *
                    <input type="text" name="nombreCompleto" required maxlength="120" value="<?= h($_POST['nombreCompleto'] ?? ($user['nombre_completo'] ?? '')) ?>" placeholder="Ej: Osman Aranguren">
                </label>

                <label>Documento de identidad *
                    <input type="text" name="documento" required maxlength="20" value="<?= h($_POST['documento'] ?? ($user['documento'] ?? '')) ?>" placeholder="Ej: 1020304050">
                </label>

                <label>Correo electrónico
                    <input type="email" name="email" maxlength="120" value="<?= h($_POST['email'] ?? ($user['email'] ?? '')) ?>" placeholder="correo@ejemplo.com">
                </label>

                <label>Teléfono
                    <input type="tel" name="telefono" maxlength="20" value="<?= h($_POST['telefono'] ?? ($user['telefono'] ?? '')) ?>" placeholder="Ej: 3101234567">
                </label>

                <button class="btn" type="submit" style="margin-top: 0.5rem;">Confirmar inscripción</button>
            </form>
        </div>
    <?php endif; ?>
<?php endif; ?>
<?php require __DIR__ . '/includes/footer.php'; ?>
