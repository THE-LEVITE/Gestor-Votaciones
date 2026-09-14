<?php
require_once __DIR__ . '/../config.php';
requireAuth('VOTANTE');

$currentUser = getLoggedUser();
$feedbackSuccess = null;
$feedbackError = '';
$receiptData = null;

// =============================================================================
// PROCESAMIENTO DEL SUFRAGIO (VÍA BACKEND JAVA)
// =============================================================================
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $encuestaId = (int)($_POST['encuesta_id'] ?? 0);
    $opcionId = (int)($_POST['opcion_id'] ?? 0);
    $tokenOtp = trim($_POST['token_otp'] ?? '');

    if ($encuestaId <= 0 || $opcionId <= 0 || empty($tokenOtp)) {
        $feedbackError = 'Por favor selecciona una opción antes de continuar.';
    } else {
        // Enviar voto al servicio de votación
        $res = callApi('POST', '/votos/emitir', [
            'encuestaId' => $encuestaId,
            'opcionId'   => $opcionId,
            'token'      => $tokenOtp
        ]);

        if ($res['status'] === 200) {
            $feedbackSuccess = '¡Excelente! Tu voto ha sido registrado con éxito.';
            $receiptData = [
                'recibo' => $res['data']['reciboDigital'] ?? 'N/A',
                'fecha'  => $res['data']['fechaEmision'] ?? date('Y-m-d H:i:s'),
                'encuestaId' => $encuestaId
            ];
        } elseif ($res['status'] === 409) {
            $feedbackError = '⚠️ Ya has votado en esta encuesta. Cada persona tiene derecho a votar una sola vez.';
        } else {
            $feedbackError = $res['error'] ?? 'No pudimos procesar tu voto. Por favor intenta nuevamente.';
        }
    }
}

// Consultar encuestas activas habilitadas
$encuestasRes = callApi('GET', '/encuestas/activas');
$encuestasActivas = ($encuestasRes['status'] === 200 && is_array($encuestasRes['data'])) ? $encuestasRes['data'] : [];

// Encuesta actual seleccionada en la vista
$selectedEncuestaId = isset($_GET['encuesta']) ? (int)$_GET['encuesta'] : (!empty($encuestasActivas) ? (int)$encuestasActivas[0]['id'] : 0);

$encuestaActual = null;
foreach ($encuestasActivas as $e) {
    if ((int)$e['id'] === $selectedEncuestaId) {
        $encuestaActual = $e;
        break;
    }
}

// Consultar el estado del Token OTP del aprendiz para la encuesta activa
$tokenInfo = null;
if ($selectedEncuestaId > 0 && $currentUser) {
    $tokenRes = callApi('GET', "/tokens/usuario?usuarioId={$currentUser['id']}&encuestaId={$selectedEncuestaId}");
    if ($tokenRes['status'] === 200 && isset($tokenRes['data']['tokenInfo'])) {
        $tokenInfo = $tokenRes['data']['tokenInfo'];
    }
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SVIS - Votación Institucional</title>
    <link rel="stylesheet" href="../assets/css/style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
</head>
<body>
    <header class="navbar">
        <div class="navbar-container">
            <div class="navbar-brand">
                <span class="logo-shield">🗳️</span>
                <div>
                    <span class="brand-title">SVIS - Cabina de Votación</span>
                    <span class="badge-role badge-votante">Estudiante</span>
                </div>
            </div>
            <div class="navbar-user">
                <span class="user-name">🎓 <?= htmlspecialchars($currentUser['nombreCompleto']) ?> (Doc: <?= htmlspecialchars($currentUser['documento']) ?>)</span>
                <a href="../logout.php" class="btn btn-sm btn-outline-danger">Cerrar Sesión</a>
            </div>
        </div>
    </header>

    <div class="main-container max-w-4xl">
        <!-- Notificaciones de Alerta -->
        <?php if (!empty($feedbackSuccess)): ?>
            <div class="alert alert-success">
                <span class="alert-icon">🎉</span>
                <div>
                    <strong><?= htmlspecialchars($feedbackSuccess) ?></strong>
                    <p class="mt-1">¡Gracias por participar! Tu elección ha sido guardada de forma segura y confidencial.</p>
                </div>
            </div>
        <?php endif; ?>

        <?php if (!empty($feedbackError)): ?>
            <div class="alert alert-danger">
                <span class="alert-icon">⚠️</span>
                <div><?= htmlspecialchars($feedbackError) ?></div>
            </div>
        <?php endif; ?>

        <!-- Tarjeta de Comprobante Digital Anónimo -->
        <?php if ($receiptData): ?>
            <div class="receipt-card">
                <div class="receipt-header">
                    <span class="receipt-badge">COMPROBANTE DE PARTICIPACIÓN</span>
                    <h3>Comprobante Digital de Voto</h3>
                </div>
                <div class="receipt-body">
                    <p class="receipt-hash-label">Tu código de confirmación de voto:</p>
                    <div class="receipt-hash">
                        <code><?= htmlspecialchars($receiptData['recibo']) ?></code>
                    </div>
                    <div class="receipt-details">
                        <span><strong>Fecha y Hora:</strong> <?= htmlspecialchars($receiptData['fecha']) ?></span>
                        <span>🔒 <strong>Privacidad asegurada:</strong> Este código demuestra que participaste, sin revelar por quién o qué votaste.</span>
                    </div>
                </div>
            </div>
        <?php endif; ?>

        <!-- Pestañas de Jornadas Electorales Disponibles -->
        <?php if (count($encuestasActivas) > 1): ?>
            <div class="survey-tabs">
                <?php foreach ($encuestasActivas as $ea): ?>
                    <a href="?encuesta=<?= $ea['id'] ?>" 
                       class="tab-item <?= ((int)$ea['id'] === $selectedEncuestaId) ? 'tab-active' : '' ?>">
                        <?= htmlspecialchars($ea['titulo']) ?>
                    </a>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>

        <?php if (!$encuestaActual): ?>
            <div class="card text-center p-5">
                <h3>No hay elecciones o consultas activas en este momento.</h3>
                <p class="text-muted mt-2">Te avisaremos cuando haya una nueva votación disponible.</p>
            </div>
        <?php else: ?>
            <div class="card voting-card">
                <div class="voting-header">
                    <div class="flex-between">
                        <span class="badge badge-success">VOTACIÓN EN CURSO</span>
                        <small class="text-muted">Consulta #<?= $encuestaActual['id'] ?></small>
                    </div>
                    <h2 class="mt-2"><?= htmlspecialchars($encuestaActual['titulo']) ?></h2>
                    <p class="text-muted mt-1"><?= htmlspecialchars($encuestaActual['descripcion']) ?></p>
                </div>

                <div class="voting-body">
                    <!-- Validación Visual de Token y Bloqueo si ya votó -->
                    <?php if (!$tokenInfo): ?>
                        <div class="alert alert-warning">
                            <span class="alert-icon">⚠️</span>
                            <div>
                                <strong>Aún no tienes un pase de votación asignado:</strong> Tu usuario todavía no está habilitado para esta consulta. Por favor comunícate con el administrador para solicitar tu pase.
                            </div>
                        </div>
                    <?php elseif ($tokenInfo['estado'] === 'USADO'): ?>
                        <!-- Mensaje claro cuando ya se votó -->
                        <div class="locked-survey-banner">
                            <div class="locked-icon">🔒</div>
                            <div class="locked-content">
                                <h3>¡Ya has participado en esta votación!</h3>
                                <p>Tu voto fue registrado exitosamente. Para asegurar una votación justa e igualitaria, cada participante cuenta con una única oportunidad de voto.</p>
                                <div class="token-burned-meta mt-2">
                                    <span>Pase utilizado: <code><?= htmlspecialchars($tokenInfo['token']) ?></code></span>
                                    <span>Estado: <strong class="badge badge-secondary">Completado</strong></span>
                                </div>
                            </div>
                        </div>
                    <?php elseif ($tokenInfo['estado'] === 'EXPIRADO'): ?>
                        <div class="alert alert-danger">
                            <span class="alert-icon">⌛</span>
                            <div>
                                <strong>Plazo vencido:</strong> El tiempo límite para emitir tu voto con este código ha finalizado.
                            </div>
                        </div>
                    <?php else: ?>
                        <!-- Estado DISPONIBLE: El estudiante puede votar -->
                        <div class="token-banner">
                            <div class="token-banner-icon">🔑</div>
                            <div class="token-banner-info">
                                <span class="token-banner-label">Tu Clave de Votación (Pase Único):</span>
                                <div class="token-display">
                                    <code id="otpTokenDisplay"><?= htmlspecialchars($tokenInfo['token']) ?></code>
                                </div>
                                <small class="text-muted">Disponible hasta: <?= htmlspecialchars($tokenInfo['fechaExpiracion']) ?></small>
                            </div>
                        </div>

                        <form method="POST" action="votar.php?encuesta=<?= $encuestaActual['id'] ?>" id="formVoto" class="mt-4">
                            <input type="hidden" name="encuesta_id" value="<?= $encuestaActual['id'] ?>">
                            <input type="hidden" name="token_otp" value="<?= htmlspecialchars($tokenInfo['token']) ?>">
                            
                            <h3 class="mb-3">Elige tu opción de preferencia:</h3>

                            <div class="options-grid">
                                <?php foreach ($encuestaActual['opciones'] as $opc): ?>
                                    <label class="option-card" for="opcion_<?= $opc['id'] ?>">
                                        <input type="radio" name="opcion_id" id="opcion_<?= $opc['id'] ?>" 
                                               value="<?= $opc['id'] ?>" required>
                                        <div class="option-card-content">
                                            <div class="option-card-radio"></div>
                                            <div class="option-card-info">
                                                <h4 class="option-card-name"><?= htmlspecialchars($opc['nombre']) ?></h4>
                                                <?php if (!empty($opc['descripcion'])): ?>
                                                    <p class="option-card-desc"><?= htmlspecialchars($opc['descripcion']) ?></p>
                                                <?php endif; ?>
                                            </div>
                                        </div>
                                    </label>
                                <?php endforeach; ?>
                            </div>

                            <div class="voting-confirm-box mt-4">
                                <p class="confirm-notice">
                                    🔒 <strong>Tu voto es 100% secreto y seguro:</strong> Tu nombre nunca se asocia con la opción que elijas. Al confirmar, tu voto se sumará al conteo general inmediatamente.
                                </p>
                                <button type="submit" class="btn btn-primary btn-lg btn-block mt-3" id="btnEmitirVoto"
                                        onclick="return confirm('¿Confirmas tu elección? Recuerda que solo puedes votar una vez.');">
                                    🗳️ Confirmar y Enviar Mi Voto
                                </button>
                            </div>
                        </form>
                    <?php endif; ?>
                </div>
            </div>
        <?php endif; ?>
    </div>
</body>
</html>
