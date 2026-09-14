<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';

$titulo = 'Eventos disponibles';
$res = api()->get('/eventos');

require __DIR__ . '/includes/header.php';
?>
<h1>Eventos disponibles</h1>
<p class="muted">Selecciona un evento e inscribete para recibir tu codigo QR de ingreso.</p>

<?php if (!$res['ok']): ?>
    <div class="alert error"><?= h($res['error'] ?? 'No se pudieron cargar los eventos') ?></div>
<?php elseif (empty($res['data'])): ?>
    <p>No hay eventos publicados por el momento.</p>
<?php else: ?>
    <div class="grid">
        <?php foreach ($res['data'] as $e): ?>
            <?php $disp = (int) $e['cupoDisponible']; ?>
            <article class="card">
                <h3><?= h($e['nombre']) ?></h3>
                <p class="muted"><?= h($e['descripcion'] ?? '') ?></p>
                <ul class="meta">
                    <li>Lugar: <?= h($e['lugar'] ?? 'Por definir') ?></li>
                    <li>Fecha: <?= h(fmtFecha($e['fecha'] ?? null)) ?></li>
                    <li>Cupo: <?= (int) $e['inscritos'] ?>/<?= (int) $e['cupoMaximo'] ?>
                        <span class="badge <?= $disp > 0 ? 'ok' : 'no' ?>">
                            <?= $disp > 0 ? ($disp . ' disponibles') : 'Sin cupos' ?>
                        </span>
                    </li>
                </ul>
                <?php if ($disp > 0): ?>
                    <a class="btn" href="inscribir.php?evento=<?= (int) $e['id'] ?>">Inscribirme</a>
                <?php else: ?>
                    <span class="btn disabled">Sin cupos</span>
                <?php endif; ?>
            </article>
        <?php endforeach; ?>
    </div>
<?php endif; ?>
<?php require __DIR__ . '/includes/footer.php'; ?>
