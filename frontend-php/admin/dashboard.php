<?php
require_once __DIR__ . '/../config.php';
requireAuth('ADMIN');

$currentUser = getLoggedUser();
$feedbackSuccess = '';
$feedbackError = '';

// =============================================================================
// PROCESAMIENTO DE ACCIONES ADMINISTRATIVAS (VÍA BACKEND JAVA)
// =============================================================================
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';

    // 1. Crear nueva encuesta y opciones
    if ($action === 'crear_encuesta') {
        $titulo = trim($_POST['titulo'] ?? '');
        $descripcion = trim($_POST['descripcion'] ?? '');
        $opcionesRaw = $_POST['opciones'] ?? [];

        $opciones = [];
        foreach ($opcionesRaw as $opc) {
            $nom = trim($opc['nombre'] ?? '');
            if (!empty($nom)) {
                $opciones[] = [
                    'nombre'      => $nom,
                    'descripcion' => trim($opc['descripcion'] ?? '')
                ];
            }
        }

        if (empty($titulo) || count($opciones) < 2) {
            $feedbackError = 'Debe indicar el título y al menos dos opciones para la votación.';
        } else {
            $res = callApi('POST', '/encuestas', [
                'titulo'      => $titulo,
                'descripcion' => $descripcion,
                'opciones'    => $opciones
            ]);

            if ($res['status'] === 201) {
                $feedbackSuccess = 'Encuesta creada exitosamente con ' . count($opciones) . ' opciones.';
            } else {
                $feedbackError = $res['error'] ?? 'Error al crear la encuesta en el backend.';
            }
        }
    }

    // 2. Generar Padrón y Tokens OTP masivos con TTL
    if ($action === 'generar_tokens') {
        $encuestaId = (int)($_POST['encuesta_id'] ?? 0);
        $ttlMinutos = (int)($_POST['ttl_minutos'] ?? 1440);

        if ($encuestaId <= 0) {
            $feedbackError = 'Seleccione una encuesta válida.';
        } else {
            $res = callApi('POST', '/tokens/generar', [
                'encuestaId' => $encuestaId,
                'ttlMinutos' => $ttlMinutos
            ]);

            if ($res['status'] === 200) {
                $tokensGen = $res['data']['tokensGenerados'] ?? 0;
                $feedbackSuccess = "¡Pases generados con éxito! Se crearon {$tokensGen} códigos para los estudiantes (disponibles por {$ttlMinutos} min).";
            } else {
                $feedbackError = $res['error'] ?? 'No se pudieron generar los pases en este momento.';
            }
        }
    }

    // 3. Apertura y Cierre de Encuesta
    if ($action === 'cambiar_estado') {
        $encuestaId = (int)($_POST['encuesta_id'] ?? 0);
        $nuevoEstado = trim($_POST['nuevo_estado'] ?? '');

        if ($encuestaId > 0 && in_array($nuevoEstado, ['ACTIVA', 'CERRADA'])) {
            $res = callApi('PUT', "/encuestas/{$encuestaId}/estado", [
                'estado' => $nuevoEstado
            ]);

            if ($res['status'] === 200) {
                $estadoTexto = ($nuevoEstado === 'ACTIVA') ? 'abierta para votar' : 'finalizada';
                $feedbackSuccess = "La votación #{$encuestaId} ahora se encuentra: {$estadoTexto}.";
            } else {
                $feedbackError = $res['error'] ?? 'No se pudo actualizar el estado de la encuesta.';
            }
        }
    }
}

// Consultar todas las encuestas
$encuestasRes = callApi('GET', '/encuestas');
$encuestas = ($encuestasRes['status'] === 200 && is_array($encuestasRes['data'])) ? $encuestasRes['data'] : [];

// Encuesta seleccionada para visualizar resultados en tiempo real
$selectedEncuestaId = isset($_GET['ver_resultados']) ? (int)$_GET['ver_resultados'] : (!empty($encuestas) ? (int)$encuestas[0]['id'] : 0);

$resultadosData = null;
if ($selectedEncuestaId > 0) {
    $resResultados = callApi('GET', "/encuestas/{$selectedEncuestaId}/resultados");
    if ($resResultados['status'] === 200) {
        $resultadosData = $resResultados['data'];
    }
}

// Consultar padrón para la encuesta seleccionada si se solicita
$padronData = null;
$verPadronId = isset($_GET['ver_padron']) ? (int)$_GET['ver_padron'] : 0;
if ($verPadronId > 0) {
    $resPadron = callApi('GET', "/tokens/padron?encuestaId={$verPadronId}");
    if ($resPadron['status'] === 200) {
        $padronData = $resPadron['data'];
    }
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SVIS - Panel de Administración</title>
    <link rel="stylesheet" href="../assets/css/style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
</head>
<body>
    <!-- Barra de Navegación Superior -->
    <header class="navbar">
        <div class="navbar-container">
            <div class="navbar-brand">
                <span class="logo-shield">🛡️</span>
                <div>
                    <span class="brand-title">SVIS - Panel de Control</span>
                    <span class="badge-role badge-admin">Administrador</span>
                </div>
            </div>
            <div class="navbar-user">
                <span class="user-name">👤 <?= htmlspecialchars($currentUser['nombreCompleto']) ?></span>
                <a href="../logout.php" class="btn btn-sm btn-outline-danger">Cerrar Sesión</a>
            </div>
        </div>
    </header>

    <div class="main-container">
        <!-- Notificaciones de Éxito / Error -->
        <?php if (!empty($feedbackSuccess)): ?>
            <div class="alert alert-success">
                <span class="alert-icon">✅</span>
                <div><?= htmlspecialchars($feedbackSuccess) ?></div>
            </div>
        <?php endif; ?>
        <?php if (!empty($feedbackError)): ?>
            <div class="alert alert-danger">
                <span class="alert-icon">⚠️</span>
                <div><?= htmlspecialchars($feedbackError) ?></div>
            </div>
        <?php endif; ?>

        <!-- Métricas Rápidas -->
        <div class="dashboard-stats-grid">
            <div class="stat-card">
                <div class="stat-label">Total de Encuestas</div>
                <div class="stat-value"><?= count($encuestas) ?></div>
                <div class="stat-sub">Votaciones creadas</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Encuestas Activas</div>
                <div class="stat-value text-success">
                    <?= count(array_filter($encuestas, fn($e) => ($e['estado'] ?? '') === 'ACTIVA')) ?>
                </div>
                <div class="stat-sub">Abiertas para votar</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Votos Registrados</div>
                <div class="stat-value text-primary">
                    <?= array_sum(array_column($encuestas, 'totalVotos')) ?>
                </div>
                <div class="stat-sub">Participación protegida</div>
            </div>
            <div class="stat-card">
                <div class="stat-label">Pases Generados</div>
                <div class="stat-value text-warning">
                    <?= array_sum(array_column($encuestas, 'totalTokens')) ?>
                </div>
                <div class="stat-sub">1 pase único por estudiante</div>
            </div>
        </div>

        <div class="dashboard-split">
            <!-- Columna Izquierda: Gestión de Encuestas -->
            <div class="dashboard-left">
                <div class="card">
                    <div class="card-header flex-between">
                        <h2>📋 Votaciones y Consultas</h2>
                        <button class="btn btn-primary btn-sm" onclick="toggleModal('modalCrearEncuesta')">
                            ➕ Nueva Votación
                        </button>
                    </div>
                    <div class="card-body">
                        <?php if (empty($encuestas)): ?>
                            <p class="empty-text">No hay votaciones registradas aún. ¡Crea la primera!</p>
                        <?php else: ?>
                            <div class="survey-list">
                                <?php foreach ($encuestas as $e): ?>
                                    <div class="survey-item <?= ($e['id'] === $selectedEncuestaId) ? 'active-border' : '' ?>">
                                        <div class="survey-info">
                                            <div class="flex-align-center gap-2">
                                                <h3 class="survey-title"><?= htmlspecialchars($e['titulo']) ?></h3>
                                                <span class="badge <?= $e['estado'] === 'ACTIVA' ? 'badge-success' : 'badge-secondary' ?>">
                                                    <?= $e['estado'] === 'ACTIVA' ? 'Abierta' : 'Cerrada' ?>
                                                </span>
                                            </div>
                                            <p class="survey-desc"><?= htmlspecialchars($e['descripcion']) ?></p>
                                            <div class="survey-meta">
                                                <span>🗳️ <?= $e['totalVotos'] ?> Votos</span> | 
                                                <span>🔑 <?= $e['totalTokens'] ?> Pases</span> | 
                                                <span>📅 <?= substr($e['fechaCreacion'], 0, 10) ?></span>
                                            </div>
                                        </div>

                                        <div class="survey-actions">
                                            <!-- Ver Resultados en tiempo real -->
                                            <a href="?ver_resultados=<?= $e['id'] ?>" class="btn btn-sm btn-outline">
                                                📊 Ver Resultados
                                            </a>

                                            <!-- Ver Estudiantes y Pases -->
                                            <a href="?ver_padron=<?= $e['id'] ?>&ver_resultados=<?= $e['id'] ?>" class="btn btn-sm btn-outline">
                                                👥 Ver Estudiantes
                                            </a>

                                            <!-- Generar Pases Modal Trigger -->
                                            <button class="btn btn-sm btn-warning" 
                                                    onclick="abrirModalTokens(<?= $e['id'] ?>, '<?= htmlspecialchars($e['titulo'], ENT_QUOTES) ?>')">
                                                🔑 Crear Pases
                                            </button>

                                            <!-- Alternar Estado -->
                                            <form method="POST" style="display:inline;" onsubmit="return confirm('¿Confirma cambio de estado de la encuesta?');">
                                                <input type="hidden" name="action" value="cambiar_estado">
                                                <input type="hidden" name="encuesta_id" value="<?= $e['id'] ?>">
                                                <?php if ($e['estado'] === 'ACTIVA'): ?>
                                                    <input type="hidden" name="nuevo_estado" value="CERRADA">
                                                    <button type="submit" class="btn btn-sm btn-danger">🔒 Finalizar</button>
                                                <?php else: ?>
                                                    <input type="hidden" name="nuevo_estado" value="ACTIVA">
                                                    <button type="submit" class="btn btn-sm btn-success">🔓 Abrir</button>
                                                <?php endif; ?>
                                            </form>
                                        </div>
                                    </div>
                                <?php endforeach; ?>
                            </div>
                        <?php endif; ?>
                    </div>
                </div>

                <!-- Tabla de Padrón Electoral si está seleccionada -->
                <?php if ($padronData !== null): ?>
                    <div class="card mt-4">
                        <div class="card-header flex-between">
                            <h2>👥 Participantes y Pases de Votación (#<?= $verPadronId ?>)</h2>
                            <a href="?ver_resultados=<?= $selectedEncuestaId ?>" class="btn btn-sm btn-outline">✕ Cerrar Lista</a>
                        </div>
                        <div class="card-body">
                            <p class="mb-2 text-muted">
                                💡 <strong>Privacidad garantizada:</strong> Esta lista permite revisar quién tiene su pase listo y quién ya participó. Por confidencialidad, nunca se registra la opción elegida por ninguna persona.
                            </p>
                            <div class="table-responsive">
                                <table class="data-table">
                                    <thead>
                                        <tr>
                                            <th>Estudiante</th>
                                            <th>Documento</th>
                                            <th>Código de Pase</th>
                                            <th>Estado</th>
                                            <th>Vigencia</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php if (empty($padronData)): ?>
                                            <tr><td colspan="5" class="text-center">No se han generado pases para esta votación todavía.</td></tr>
                                        <?php else: ?>
                                            <?php foreach ($padronData as $p): ?>
                                                <tr>
                                                    <td><?= htmlspecialchars($p['usuario']['nombreCompleto'] ?? '') ?></td>
                                                    <td><?= htmlspecialchars($p['usuario']['documento'] ?? '') ?></td>
                                                    <td><code><?= htmlspecialchars($p['token']) ?></code></td>
                                                    <td>
                                                        <?php if ($p['estado'] === 'DISPONIBLE'): ?>
                                                            <span class="badge badge-success">Listo para votar</span>
                                                        <?php elseif ($p['estado'] === 'USADO'): ?>
                                                            <span class="badge badge-secondary">Ya votó</span>
                                                        <?php else: ?>
                                                            <span class="badge badge-danger">Vencido</span>
                                                        <?php endif; ?>
                                                    </td>
                                                    <td><small><?= $p['fechaExpiracion'] ?></small></td>
                                                </tr>
                                            <?php endforeach; ?>
                                        <?php endif; ?>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                <?php endif; ?>
            </div>

            <!-- Columna Derecha: Gráficas y Resultados en Tiempo Real -->
            <div class="dashboard-right">
                <div class="card">
                    <div class="card-header">
                        <h2>📊 Resultados en Tiempo Real</h2>
                    </div>
                    <div class="card-body">
                        <?php if ($resultadosData): ?>
                            <div class="results-header">
                                <h3><?= htmlspecialchars($resultadosData['titulo']) ?></h3>
                                <div class="badge <?= $resultadosData['estado'] === 'ACTIVA' ? 'badge-success' : 'badge-secondary' ?>">
                                    <?= $resultadosData['estado'] === 'ACTIVA' ? 'Abierta' : 'Finalizada' ?>
                                </div>
                            </div>
                            <div class="results-total-badge">
                                Total de votos registrados: <strong><?= $resultadosData['totalVotos'] ?></strong>
                            </div>

                            <!-- 1. Primera Gráfica: Barras Horizontales con Porcentajes -->
                            <div class="results-bars-container">
                                <?php foreach ($resultadosData['opciones'] as $opc): ?>
                                    <div class="option-result-card">
                                        <div class="flex-between mb-1">
                                            <span class="option-result-name"><?= htmlspecialchars($opc['nombre']) ?></span>
                                            <span class="option-result-score">
                                                <strong><?= $opc['votos'] ?> <?= $opc['votos'] == 1 ? 'voto' : 'votos' ?></strong> (<?= $opc['porcentaje'] ?>%)
                                            </span>
                                        </div>
                                        <div class="progress-bar-bg">
                                            <div class="progress-bar-fill" style="width: <?= $opc['porcentaje'] ?>%;"></div>
                                        </div>
                                        <?php if (!empty($opc['descripcion'])): ?>
                                            <small class="text-muted"><?= htmlspecialchars($opc['descripcion']) ?></small>
                                        <?php endif; ?>
                                    </div>
                                <?php endforeach; ?>
                            </div>

                            <!-- 2. GRÁFICA DE PASTEL (SVG puro) -->
                            <?php
                            // Paleta de colores para el pastel
                            $pieColors = ['#3b82f6','#10b981','#f59e0b','#8b5cf6','#ec4899','#38bdf8','#f97316','#84cc16'];
                            $totalVotos = (int)($resultadosData['totalVotos'] ?? 0);

                            // Radio y centro del SVG viewBox 200×200
                            $cx = 100; $cy = 100; $r = 80;
                            $segments = [];
                            $angleStart = -90; // 12 en punto

                            foreach ($resultadosData['opciones'] as $i => $opc) {
                                $pct   = (float)$opc['porcentaje'];
                                $sweep = ($pct / 100) * 360;
                                $angleEnd = $angleStart + $sweep;

                                // ── Caso especial: segmento ocupa el círculo completo (100 %) ──
                                // Un path SVG con start=end es inválido y no renderiza nada.
                                // Marcamos el segmento como círculo completo y lo dibujamos con <circle>.
                                $isFullCircle = ($pct >= 99.9);

                                $path = '';
                                if ($totalVotos > 0 && $pct > 0 && !$isFullCircle) {
                                    $x1 = $cx + $r * cos(deg2rad($angleStart));
                                    $y1 = $cy + $r * sin(deg2rad($angleStart));
                                    $x2 = $cx + $r * cos(deg2rad($angleEnd));
                                    $y2 = $cy + $r * sin(deg2rad($angleEnd));
                                    $largeArc = $sweep > 180 ? 1 : 0;
                                    $path = "M {$cx},{$cy} L {$x1},{$y1} A {$r},{$r} 0 {$largeArc},1 {$x2},{$y2} Z";
                                }

                                // Punto medio del arco — posición de la etiqueta
                                // Usamos r*0.68 para quedar más al centro del anillo visible
                                $midAngle = $angleStart + $sweep / 2;
                                $labelR   = $r * 0.68;
                                $lx = $cx + $labelR * cos(deg2rad($midAngle));
                                $ly = $cy + $labelR * sin(deg2rad($midAngle));

                                // Formato del porcentaje: sin decimales si son .00
                                $pctFmt = (fmod($pct, 1) == 0) ? (int)$pct . '%' : $pct . '%';

                                $segments[] = [
                                    'color'        => $pieColors[$i % count($pieColors)],
                                    'path'         => $path,
                                    'isFullCircle' => $isFullCircle,
                                    'pct'          => $pct,
                                    'pctFmt'       => $pctFmt,
                                    'votos'        => $opc['votos'],
                                    'nombre'       => $opc['nombre'],
                                    'labelX'       => round($lx, 2),
                                    'labelY'       => round($ly, 2),
                                    // Mostrar etiqueta sólo si el segmento es lo bastante ancho (≥18%)
                                    // y no es círculo completo (en ese caso va en el centro)
                                    'showLabel'    => ($pct >= 18 && !$isFullCircle),
                                ];
                                $angleStart = $angleEnd;
                            }
                            ?>
                            <div class="pie-chart-container">
                                <h4 class="chart-title-sub">🥧 Distribución de Votos</h4>
                                <div class="pie-chart-layout">
                                    <!-- SVG Pastel -->
                                    <div class="pie-svg-wrap">
                                        <?php if ($totalVotos === 0): ?>
                                            <!-- Sin votos aún: círculo vacío -->
                                            <svg viewBox="0 0 200 200" class="pie-svg">
                                                <circle cx="100" cy="100" r="80" fill="#e2e8f0"/>
                                                <text x="100" y="105" text-anchor="middle" fill="#94a3b8" font-size="13" font-family="Inter,sans-serif">Sin votos</text>
                                            </svg>
                                        <?php else: ?>
                                            <svg viewBox="0 0 200 200" class="pie-svg">
                                                <defs>
                                                    <filter id="pieShadow" x="-10%" y="-10%" width="120%" height="120%">
                                                        <feDropShadow dx="0" dy="2" stdDeviation="4" flood-color="rgba(0,0,0,0.18)"/>
                                                    </filter>
                                                </defs>
                                                <g filter="url(#pieShadow)">
                                                <?php foreach ($segments as $seg): ?>
                                                    <?php if ($seg['isFullCircle']): ?>
                                                        <!-- Círculo completo: dibujamos un <circle> de radio completo -->
                                                        <circle cx="<?= $cx ?>" cy="<?= $cy ?>" r="<?= $r ?>"
                                                                fill="<?= $seg['color'] ?>"
                                                                class="pie-slice"
                                                                data-label="<?= htmlspecialchars($seg['nombre']) ?>"
                                                                data-pct="<?= $seg['pctFmt'] ?>"
                                                                data-votos="<?= $seg['votos'] ?> <?= $seg['votos'] == 1 ? 'voto' : 'votos' ?>"/>
                                                    <?php elseif ($seg['path']): ?>
                                                        <path d="<?= $seg['path'] ?>"
                                                              fill="<?= $seg['color'] ?>"
                                                              class="pie-slice"
                                                              data-label="<?= htmlspecialchars($seg['nombre']) ?>"
                                                              data-pct="<?= $seg['pctFmt'] ?>"
                                                              data-votos="<?= $seg['votos'] ?> <?= $seg['votos'] == 1 ? 'voto' : 'votos' ?>"/>
                                                    <?php endif; ?>
                                                <?php endforeach; ?>
                                                </g>
                                                <!-- Círculo interior donut -->
                                                <circle cx="100" cy="100" r="38" fill="white" class="pie-inner"/>
                                                <!-- Texto central -->
                                                <text x="100" y="96"  text-anchor="middle" fill="#1e293b" font-size="14" font-weight="700" font-family="Inter,sans-serif"><?= $totalVotos ?></text>
                                                <text x="100" y="111" text-anchor="middle" fill="#64748b" font-size="8.5" font-family="Inter,sans-serif">VOTOS</text>
                                                <!-- Etiquetas de % dentro de los segmentos -->
                                                <?php foreach ($segments as $seg): ?>
                                                    <?php if ($seg['showLabel']): ?>
                                                        <text x="<?= $seg['labelX'] ?>" y="<?= $seg['labelY'] + 3.5 ?>"
                                                              text-anchor="middle"
                                                              fill="white"
                                                              font-size="7.5"
                                                              font-weight="700"
                                                              font-family="Inter,sans-serif"><?= $seg['pctFmt'] ?></text>
                                                    <?php endif; ?>
                                                <?php endforeach; ?>
                                            </svg>
                                        <?php endif; ?>
                                    </div>
                                    <!-- Leyenda -->
                                    <div class="pie-legend">
                                        <?php foreach ($segments as $seg): ?>
                                            <div class="pie-legend-item">
                                                <span class="pie-legend-dot" style="background:<?= $seg['color'] ?>;"></span>
                                                <div class="pie-legend-text">
                                                    <span class="pie-legend-name"><?= htmlspecialchars($seg['nombre']) ?></span>
                                                    <span class="pie-legend-val"><?= $seg['pct'] ?>% · <?= $seg['votos'] ?> <?= $seg['votos'] == 1 ? 'voto' : 'votos' ?></span>
                                                </div>
                                            </div>
                                        <?php endforeach; ?>
                                    </div>
                                </div>
                            </div>

                            <!-- 3. Gráfica comparativa de participación en todas las encuestas -->
                            <?php if (count($encuestas) > 1): ?>
                                <div class="surveys-overview-card">
                                    <h4 class="chart-title-sub">📈 Participación y Votos por Consulta</h4>
                                    <?php 
                                    $maxVotos = max(1, ...array_map(fn($item) => (int)($item['totalVotos'] ?? 0), $encuestas));
                                    foreach ($encuestas as $encItem): 
                                        $votosEnc = (int)($encItem['totalVotos'] ?? 0);
                                        $pctRelativo = round(($votosEnc / $maxVotos) * 100);
                                    ?>
                                        <div class="survey-metric-row">
                                            <div class="flex-between mb-1">
                                                <span style="font-size: 0.82rem; font-weight: 600;"><?= htmlspecialchars($encItem['titulo']) ?></span>
                                                <span style="font-size: 0.78rem;" class="text-muted"><?= $votosEnc ?> <?= $votosEnc == 1 ? 'voto' : 'votos' ?></span>
                                            </div>
                                            <div class="progress-bar-bg" style="height: 8px;">
                                                <div class="progress-bar-fill" style="width: <?= $pctRelativo ?>%; background: linear-gradient(90deg, #6366f1 0%, #4f46e5 100%);"></div>
                                            </div>
                                        </div>
                                    <?php endforeach; ?>
                                </div>
                            <?php endif; ?>

                            <div class="audit-note">
                                <small>🔒 <strong>Resultados al instante:</strong> Esta información se actualiza automáticamente conforme los estudiantes participan, con total transparencia y confidencialidad.</small>
                            </div>
                        <?php else: ?>
                            <p class="empty-text">Elige una encuesta para ver sus resultados y gráficas.</p>
                        <?php endif; ?>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal: Crear Nueva Encuesta -->
    <div id="modalCrearEncuesta" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3>➕ Crear Nueva Consulta o Votación</h3>
                <span class="modal-close" onclick="toggleModal('modalCrearEncuesta')">&times;</span>
            </div>
            <form method="POST" action="dashboard.php">
                <input type="hidden" name="action" value="crear_encuesta">
                <div class="modal-body">
                    <div class="form-group">
                        <label for="titulo">Título de la Votación o Consulta</label>
                        <input type="text" id="titulo" name="titulo" required placeholder="ej: Elección de Representantes Estudiantiles">
                    </div>
                    <div class="form-group">
                        <label for="descripcion">Descripción o Instrucciones para los Votantes</label>
                        <textarea id="descripcion" name="descripcion" rows="2" placeholder="Explica de qué trata esta votación y las orientaciones generales..."></textarea>
                    </div>

                    <div class="form-group">
                        <label>Opciones o Candidaturas Disponibles</label>
                        <div id="opcionesContainer">
                            <div class="option-row">
                                <input type="text" name="opciones[0][nombre]" placeholder="Nombre de Opción / Candidato 1" required>
                                <input type="text" name="opciones[0][descripcion]" placeholder="Breve descripción o propuesta">
                            </div>
                            <div class="option-row">
                                <input type="text" name="opciones[1][nombre]" placeholder="Nombre de Opción / Candidato 2" required>
                                <input type="text" name="opciones[1][descripcion]" placeholder="Breve descripción o propuesta">
                            </div>
                            <div class="option-row">
                                <input type="text" name="opciones[2][nombre]" value="Voto en Blanco" placeholder="Voto en Blanco" required>
                                <input type="text" name="opciones[2][descripcion]" value="Opción de inconformidad democrática" placeholder="Descripción">
                            </div>
                        </div>
                        <button type="button" class="btn btn-sm btn-outline mt-2" onclick="agregarOpcion()">
                            ➕ Agregar Otra Opción
                        </button>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline" onclick="toggleModal('modalCrearEncuesta')">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Crear Votación</button>
                </div>
            </form>
        </div>
    </div>

    <!-- Modal: Generar Padrón de Tokens OTP -->
    <div id="modalGenerarTokens" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h3>🔑 Generar Pases de Votación</h3>
                <span class="modal-close" onclick="toggleModal('modalGenerarTokens')">&times;</span>
            </div>
            <form method="POST" action="dashboard.php">
                <input type="hidden" name="action" value="generar_tokens">
                <input type="hidden" id="tokenEncuestaId" name="encuesta_id" value="">
                <div class="modal-body">
                    <p id="tokenEncuestaTitulo" class="mb-3 font-semibold"></p>
                    <p class="text-muted mb-3">
                        Este proceso crea un código de votación único para cada estudiante activo que aún no tenga uno. Esto garantiza que cada participante tenga exactamente una oportunidad de votar.
                    </p>
                    <div class="form-group">
                        <label for="ttl_minutos">¿Cuánto tiempo tendrán disponible para votar?</label>
                        <select id="ttl_minutos" name="ttl_minutos" class="form-control">
                            <option value="60">1 Hora (60 minutos)</option>
                            <option value="120">2 Horas (120 minutos)</option>
                            <option value="480">8 Horas (Jornada del día)</option>
                            <option value="1440" selected>24 Horas (1 Día completo)</option>
                            <option value="2880">48 Horas (2 Días)</option>
                        </select>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-outline" onclick="toggleModal('modalGenerarTokens')">Cancelar</button>
                    <button type="submit" class="btn btn-warning">Generar Pases Únicos</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        let opcionCount = 3;
        function agregarOpcion() {
            const container = document.getElementById('opcionesContainer');
            const div = document.createElement('div');
            div.className = 'option-row';
            div.innerHTML = `
                <input type="text" name="opciones[${opcionCount}][nombre]" placeholder="Nombre Candidato / Opción ${opcionCount + 1}" required>
                <input type="text" name="opciones[${opcionCount}][descripcion]" placeholder="Breve propuesta o lema">
            `;
            container.appendChild(div);
            opcionCount++;
        }

        function toggleModal(id) {
            const m = document.getElementById(id);
            if (m.style.display === 'block') {
                m.style.display = 'none';
            } else {
                m.style.display = 'block';
            }
        }

        function abrirModalTokens(id, titulo) {
            document.getElementById('tokenEncuestaId').value = id;
            document.getElementById('tokenEncuestaTitulo').textContent = "Encuesta Seleccionada: " + titulo;
            toggleModal('modalGenerarTokens');
        }

        window.onclick = function(event) {
            if (event.target.classList.contains('modal')) {
                event.target.style.display = 'none';
            }
        }
    </script>
</body>
</html>
