<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

require_admin();

$pdo = db();
$eventoFiltro = isset($_GET['evento']) ? (int) $_GET['evento'] : 0;
$busqueda     = trim($_GET['q'] ?? '');

// Obtener lista de eventos para el filtro
$eventos = $pdo->query('SELECT id, nombre FROM evento ORDER BY fecha ASC')->fetchAll();

// Construir consulta de asistentes
$sql = '
    SELECT 
        i.id AS inscripcion_id,
        i.token,
        i.estado,
        i.fecha_inscripcion,
        i.fecha_validacion,
        e.id AS evento_id,
        e.nombre AS evento_nombre,
        e.fecha AS evento_fecha,
        e.lugar AS evento_lugar,
        a.id AS asistente_id,
        a.nombre_completo,
        a.documento,
        a.email,
        a.telefono
    FROM inscripcion i
    JOIN evento e ON i.evento_id = e.id
    JOIN asistente a ON i.asistente_id = a.id
    WHERE 1=1
';
$params = [];

if ($eventoFiltro > 0) {
    $sql .= ' AND e.id = ?';
    $params[] = $eventoFiltro;
}

if ($busqueda !== '') {
    $sql .= ' AND (a.nombre_completo LIKE ? OR a.documento LIKE ? OR i.token LIKE ?)';
    $like = '%' . $busqueda . '%';
    $params[] = $like;
    $params[] = $like;
    $params[] = $like;
}

$sql .= ' ORDER BY i.fecha_inscripcion DESC';
$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$inscripciones = $stmt->fetchAll();

$titulo = 'Listado de Asistentes';
require __DIR__ . '/includes/header.php';
?>

<div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; margin-bottom: 1.5rem;">
    <div>
        <h1 style="margin: 0;">Lista de Asistentes e Inscripciones</h1>
        <p class="muted" style="margin: 0.2rem 0 0;">Visualiza los participantes registrados, su estado de ingreso y códigos QR.</p>
    </div>
    <div>
        <button class="btn ghost small" onclick="window.print()">🖨️ Imprimir Lista</button>
        <a href="eventos_admin.php" class="btn small">➕ Programar Evento</a>
    </div>
</div>

<!-- Filtros -->
<div class="card" style="margin-bottom: 1.5rem; padding: 1rem;">
    <form method="get" class="form-inline" style="display: flex; gap: 0.8rem; flex-wrap: wrap;">
        <select name="evento" style="padding: 0.5rem; border: 1px solid var(--border); border-radius: 4px; flex: 1; min-width: 200px;">
            <option value="0">-- Todos los eventos --</option>
            <?php foreach ($eventos as $ev): ?>
                <option value="<?= (int) $ev['id'] ?>" <?= $eventoFiltro === (int) $ev['id'] ? 'selected' : '' ?>>
                    <?= h($ev['nombre']) ?>
                </option>
            <?php endforeach; ?>
        </select>

        <input type="text" name="q" value="<?= h($busqueda) ?>" placeholder="Buscar por nombre, cédula o token..." style="flex: 1.5; min-width: 220px;">

        <button type="submit" class="btn">Filtrar</button>
        <?php if ($eventoFiltro > 0 || $busqueda !== ''): ?>
            <a href="asistentes.php" class="btn ghost">Limpiar</a>
        <?php endif; ?>
    </form>
</div>

<?php if (empty($inscripciones)): ?>
    <div class="card" style="text-align: center; padding: 2.5rem;">
        <p class="muted" style="font-size: 1.1rem; margin: 0;">No se encontraron asistentes con los filtros seleccionados.</p>
    </div>
<?php else: ?>
    <div class="card" style="overflow-x: auto; padding: 0;">
        <table class="tabla" style="margin: 0;">
            <thead>
                <tr>
                    <th>Asistente</th>
                    <th>Documento</th>
                    <th>Contacto</th>
                    <th>Evento</th>
                    <th>Estado</th>
                    <th>Inscrito</th>
                    <th style="text-align: center;">Código QR</th>
                </tr>
            </thead>
            <tbody>
                <?php foreach ($inscripciones as $i): ?>
                    <?php $isValidada = ($i['estado'] === 'VALIDADA'); ?>
                    <tr>
                        <td>
                            <strong><?= h($i['nombre_completo']) ?></strong>
                        </td>
                        <td><?= h($i['documento']) ?></td>
                        <td>
                            <small>
                                <?= h($i['email'] ?: 'Sin email') ?><br>
                                <?= h($i['telefono'] ?: 'Sin tel.') ?>
                            </small>
                        </td>
                        <td>
                            <strong><?= h($i['evento_nombre']) ?></strong>
                        </td>
                        <td>
                            <span class="badge <?= $isValidada ? 'ok' : 'pending' ?>">
                                <?= $isValidada ? '✓ Validado' : '⏳ Pendiente' ?>
                            </span>
                            <?php if ($i['fecha_validacion']): ?>
                                <br><small class="muted"><?= h(fmtFecha($i['fecha_validacion'])) ?></small>
                            <?php endif; ?>
                        </td>
                        <td><small class="muted"><?= h(fmtFecha($i['fecha_inscripcion'])) ?></small></td>
                        <td style="text-align: center;">
                            <button type="button" class="btn small" onclick="mostrarModalQr('<?= h($i['token']) ?>', '<?= h(addslashes($i['nombre_completo'])) ?>', '<?= h(addslashes($i['evento_nombre'])) ?>')">
                                🔍 Ver QR
                            </button>
                        </td>
                    </tr>
                <?php endforeach; ?>
            </tbody>
        </table>
    </div>
<?php endif; ?>

<!-- Modal para ver el QR -->
<div id="modalQr" class="modal-overlay" style="display: none;">
    <div class="modal-card">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
            <h3 style="margin: 0;" id="modalTitulo">Ticket con Código QR</h3>
            <button type="button" onclick="cerrarModalQr()" style="background: none; border: none; font-size: 1.5rem; cursor: pointer;">&times;</button>
        </div>
        <div style="text-align: center;">
            <p id="modalAsistente" style="font-weight: 600; margin-bottom: 0.2rem;"></p>
            <p id="modalEvento" class="muted" style="margin-top: 0;"></p>
            <div style="margin: 1.5rem 0;">
                <img id="modalQrImg" src="" alt="Codigo QR" width="260" height="260" style="border: 1px solid var(--border); border-radius: 8px; padding: 0.5rem; background: #fff;">
            </div>
            <p style="font-size: 0.85rem; color: #666;">Token: <code id="modalToken"></code></p>
            <div style="display: flex; gap: 0.5rem; justify-content: center; margin-top: 1rem;">
                <a id="modalDescargar" href="" target="_blank" class="btn small ghost">Abrir imagen</a>
                <button type="button" class="btn small" onclick="cerrarModalQr()">Cerrar</button>
            </div>
        </div>
    </div>
</div>

<script>
function mostrarModalQr(token, nombre, evento) {
    document.getElementById('modalTitulo').textContent = 'Código QR de Ingreso';
    document.getElementById('modalAsistente').textContent = nombre;
    document.getElementById('modalEvento').textContent = evento;
    document.getElementById('modalToken').textContent = token;
    
    var qrUrl = 'qr.php?token=' + encodeURIComponent(token);
    document.getElementById('modalQrImg').src = qrUrl;
    document.getElementById('modalDescargar').href = qrUrl;
    
    document.getElementById('modalQr').style.display = 'flex';
}

function cerrarModalQr() {
    document.getElementById('modalQr').style.display = 'none';
}

// Cerrar con Escape o clic fuera
window.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') cerrarModalQr();
});
document.getElementById('modalQr').addEventListener('click', function(e) {
    if (e.target === this) cerrarModalQr();
});
</script>

<?php require __DIR__ . '/includes/footer.php'; ?>
