<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

// Exigir rol de Administrador
require_admin();

$titulo = 'Administrar eventos';
$msg = null;
$err = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $payload = [
        'nombre'      => trim($_POST['nombre'] ?? ''),
        'descripcion' => trim($_POST['descripcion'] ?? ''),
        'lugar'       => trim($_POST['lugar'] ?? ''),
        'fecha'       => $_POST['fecha'] ?? null,   // datetime-local: yyyy-MM-ddTHH:mm
        'cupoMaximo'  => (int) ($_POST['cupoMaximo'] ?? 0),
    ];
    $r = api()->post('/eventos', $payload);
    if ($r['ok']) {
        $msg = 'Evento "' . $payload['nombre'] . '" programado correctamente.';
    } else {
        $err = ($r['data']['mensaje'] ?? null) ?: ($r['error'] ?? 'No se pudo crear el evento');
    }
}

$lista = api()->get('/eventos');
require __DIR__ . '/includes/header.php';
?>
<h1>Panel de Administración · Eventos</h1>
<p class="muted">Programa nuevos eventos y gestiona los cupos y la lista de asistentes.</p>

<?php if ($msg): ?><div class="alert ok"><?= h($msg) ?></div><?php endif; ?>
<?php if ($err): ?><div class="alert error"><?= h($err) ?></div><?php endif; ?>

<div class="cols">
    <section>
        <h2>Nuevo evento</h2>
        <form method="post" class="form">
            <label>Nombre del evento *
                <input type="text" name="nombre" required maxlength="120" placeholder="Ej: Conferencia de IA aplicada">
            </label>
            <label>Descripción
                <textarea name="descripcion" rows="3" maxlength="400" placeholder="Detalles de los temas a tratar..."></textarea>
            </label>
            <label>Lugar
                <input type="text" name="lugar" maxlength="120" placeholder="Ej: Auditorio Central CIMM">
            </label>
            <label>Fecha y hora
                <input type="datetime-local" name="fecha">
            </label>
            <label>Cupo máximo *
                <input type="number" name="cupoMaximo" min="1" value="50" required>
            </label>
            <button class="btn" type="submit">Guardar y Publicar</button>
        </form>
    </section>

    <section>
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
            <h2 style="margin: 0;">Eventos programados</h2>
            <a href="asistentes.php" class="btn small ghost">Ver todos los asistentes</a>
        </div>

        <?php if (!$lista['ok']): ?>
            <div class="alert error"><?= h($lista['error'] ?? 'No se pudo cargar la lista') ?></div>
        <?php elseif (empty($lista['data'])): ?>
            <p>Aún no hay eventos programados.</p>
        <?php else: ?>
            <table class="tabla">
                <thead>
                <tr>
                    <th>#</th>
                    <th>Nombre</th>
                    <th>Fecha</th>
                    <th>Cupo</th>
                    <th>Acciones</th>
                </tr>
                </thead>
                <tbody>
                <?php foreach ($lista['data'] as $e): ?>
                    <tr>
                        <td><?= (int) $e['id'] ?></td>
                        <td><strong><?= h($e['nombre']) ?></strong><br><small class="muted"><?= h($e['lugar'] ?? '') ?></small></td>
                        <td><?= h(fmtFecha($e['fecha'] ?? null)) ?></td>
                        <td><?= (int) $e['inscritos'] ?> / <?= (int) $e['cupoMaximo'] ?></td>
                        <td>
                            <a class="btn small" href="asistentes.php?evento=<?= (int) $e['id'] ?>">
                                👥 Asistentes (<?= (int) $e['inscritos'] ?>)
                            </a>
                        </td>
                    </tr>
                <?php endforeach; ?>
                </tbody>
            </table>
        <?php endif; ?>
    </section>
</div>
<?php require __DIR__ . '/includes/footer.php'; ?>
