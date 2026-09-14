<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

require_login();
$user = current_user();
$pdo = db();

$documento = $user['documento'] ?? '';
$entradas = [];

if ($documento !== '') {
    $stmt = $pdo->prepare('
        SELECT 
            i.id AS inscripcion_id,
            i.token,
            i.estado,
            i.fecha_inscripcion,
            i.fecha_validacion,
            e.id AS evento_id,
            e.nombre AS evento_nombre,
            e.descripcion AS evento_desc,
            e.lugar AS evento_lugar,
            e.fecha AS evento_fecha,
            a.nombre_completo,
            a.documento
        FROM inscripcion i
        JOIN evento e ON i.evento_id = e.id
        JOIN asistente a ON i.asistente_id = a.id
        WHERE a.documento = ?
        ORDER BY e.fecha ASC
    ');
    $stmt->execute([$documento]);
    $entradas = $stmt->fetchAll();
}

$titulo = 'Mis Entradas y Códigos QR';
require __DIR__ . '/includes/header.php';
?>

<div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;">
    <div>
        <h1 style="margin: 0;">Mis Entradas (Códigos QR)</h1>
        <p class="muted" style="margin: 0.2rem 0 0;">
            Hola <strong><?= h($user['nombre_completo']) ?></strong>, aquí tienes tus tickets de acceso para presentar en los eventos.
        </p>
    </div>
    <a href="index.php" class="btn">Explorar más eventos</a>
</div>

<?php if (empty($entradas)): ?>
    <div class="card" style="text-align: center; padding: 3rem 1.5rem;">
        <h3>Aún no tienes inscripciones registradas</h3>
        <p class="muted">No encontramos entradas asociadas a tu documento (<strong><?= h($documento ?: 'No registrado') ?></strong>).</p>
        <p style="margin-top: 1.5rem;">
            <a href="index.php" class="btn">Ver eventos disponibles e inscribirme</a>
        </p>
    </div>
<?php else: ?>
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.5rem;">
        <?php foreach ($entradas as $ent): ?>
            <?php $isValidada = ($ent['estado'] === 'VALIDADA'); ?>
            <div class="card ticket" style="margin: 0; display: flex; flex-direction: column; justify-content: space-between;">
                <div>
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.8rem;">
                        <span class="badge <?= $isValidada ? 'ok' : 'pending' ?>">
                            <?= $isValidada ? '✓ Ingreso Validado' : '⏳ Pendiente de Ingreso' ?>
                        </span>
                        <small class="muted">Inscrito: <?= h(fmtFecha($ent['fecha_inscripcion'])) ?></small>
                    </div>

                    <h3 style="margin: 0 0 0.5rem; color: var(--sena-dark);"><?= h($ent['evento_nombre']) ?></h3>
                    <p class="muted" style="font-size: 0.9rem; margin-bottom: 1rem;"><?= h($ent['evento_desc'] ?? '') ?></p>

                    <div style="background: #fafafa; border: 1px dashed var(--border); border-radius: 8px; padding: 1rem; text-align: center; margin-bottom: 1rem;">
                        <img src="qr.php?token=<?= h($ent['token']) ?>" alt="QR de Ingreso" width="220" height="220" style="max-width: 100%; height: auto; display: block; margin: 0 auto;">
                        <p style="margin: 0.5rem 0 0; font-size: 0.8rem; color: #666;">
                            Presenta este código en la entrada<br>
                            <code><?= h($ent['token']) ?></code>
                        </p>
                    </div>

                    <ul class="meta" style="margin-bottom: 1rem;">
                        <li><strong>Fecha:</strong> <?= h(fmtFecha($ent['evento_fecha'] ?? null)) ?></li>
                        <li><strong>Lugar:</strong> <?= h($ent['evento_lugar'] ?? 'Por definir') ?></li>
                        <li><strong>Asistente:</strong> <?= h($ent['nombre_completo']) ?></li>
                        <li><strong>Documento:</strong> <?= h($ent['documento']) ?></li>
                    </ul>
                </div>

                <div style="display: flex; gap: 0.5rem; margin-top: 1rem;">
                    <a href="qr.php?token=<?= h($ent['token']) ?>" target="_blank" class="btn small ghost" style="flex: 1; text-align: center;">Descargar QR</a>
                    <button type="button" class="btn small" style="flex: 1;" onclick="window.print()">Imprimir Ticket</button>
                </div>
            </div>
        <?php endforeach; ?>
    </div>
<?php endif; ?>

<?php require __DIR__ . '/includes/footer.php'; ?>
