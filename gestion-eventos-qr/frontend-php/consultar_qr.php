<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

$pdo = db();
$documento = trim($_GET['documento'] ?? ($_POST['documento'] ?? ''));
$entradas  = [];
$buscado   = false;

if ($documento !== '') {
    $buscado = true;
    $stmt = $pdo->prepare('
        SELECT 
            i.id AS inscripcion_id,
            i.token,
            i.estado,
            i.fecha_inscripcion,
            i.fecha_validacion,
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

$titulo = 'Consultar mi Código QR';
require __DIR__ . '/includes/header.php';
?>

<div style="max-width: 680px; margin: 1.5rem auto;">
    <div style="text-align: center; margin-bottom: 2rem;">
        <h1 style="margin-bottom: 0.5rem;">Consulta tu Entrada con Código QR</h1>
        <p class="muted">Ingresa tu número de documento de identidad para recuperar tus entradas e ingresar al evento.</p>

        <form method="get" class="form-inline" style="display: flex; gap: 0.5rem; justify-content: center; margin-top: 1.5rem;">
            <input type="text" name="documento" value="<?= h($documento) ?>" required placeholder="Escribe tu número de documento..." style="width: 280px; padding: 0.7rem; font-size: 1rem;">
            <button type="submit" class="btn">Buscar mi QR</button>
        </form>
    </div>

    <?php if ($buscado): ?>
        <?php if (empty($entradas)): ?>
            <div class="card" style="text-align: center; padding: 2.5rem;">
                <p style="font-size: 1.1rem; color: var(--err); margin-bottom: 0.5rem;">No se encontraron entradas registradas</p>
                <p class="muted">No hay inscripciones asociadas al documento <strong><?= h($documento) ?></strong>.</p>
                <a href="index.php" class="btn small" style="margin-top: 1rem;">Ver eventos disponibles</a>
            </div>
        <?php else: ?>
            <h2 style="margin-bottom: 1rem;">Entradas encontradas (<?= count($entradas) ?>)</h2>
            <div style="display: flex; flex-direction: column; gap: 1.5rem;">
                <?php foreach ($entradas as $ent): ?>
                    <?php $isValidada = ($ent['estado'] === 'VALIDADA'); ?>
                    <div class="card ticket" style="margin: 0;">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.8rem;">
                            <span class="badge <?= $isValidada ? 'ok' : 'pending' ?>">
                                <?= $isValidada ? '✓ Ingreso Validado' : '⏳ Pendiente de Ingreso' ?>
                            </span>
                            <small class="muted">Inscrito: <?= h(fmtFecha($ent['fecha_inscripcion'])) ?></small>
                        </div>

                        <h3 style="margin: 0 0 0.5rem; color: var(--sena-dark);"><?= h($ent['evento_nombre']) ?></h3>

                        <div style="background: #fafafa; border: 1px dashed var(--border); border-radius: 8px; padding: 1.5rem; text-align: center; margin: 1rem 0;">
                            <img src="qr.php?token=<?= h($ent['token']) ?>" alt="QR de Ingreso" width="240" height="240" style="max-width: 100%; height: auto; display: block; margin: 0 auto;">
                            <p style="margin: 0.8rem 0 0; font-size: 0.85rem; color: #555;">
                                Muestra este código en la entrada del evento<br>
                                Token: <code><?= h($ent['token']) ?></code>
                            </p>
                        </div>

                        <ul class="meta">
                            <li><strong>Asistente:</strong> <?= h($ent['nombre_completo']) ?></li>
                            <li><strong>Documento:</strong> <?= h($ent['documento']) ?></li>
                            <li><strong>Fecha:</strong> <?= h(fmtFecha($ent['evento_fecha'] ?? null)) ?></li>
                            <li><strong>Lugar:</strong> <?= h($ent['evento_lugar'] ?? 'Por definir') ?></li>
                        </ul>

                        <div style="display: flex; gap: 0.5rem; margin-top: 1.2rem;">
                            <a href="qr.php?token=<?= h($ent['token']) ?>" target="_blank" class="btn ghost small" style="flex: 1; text-align: center;">Descargar imagen QR</a>
                            <button type="button" class="btn small" style="flex: 1;" onclick="window.print()">Imprimir Ticket</button>
                        </div>
                    </div>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
    <?php endif; ?>
</div>

<?php require __DIR__ . '/includes/footer.php'; ?>
