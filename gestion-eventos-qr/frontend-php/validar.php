<?php
require_once __DIR__ . '/config.php';
require_once __DIR__ . '/includes/ApiClient.php';
require_once __DIR__ . '/includes/auth.php';

// Solo el Administrador o personal de control de acceso puede validar entradas
require_admin();

// Modo API: el escaner (JS) envia el token por POST y respondemos JSON.
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    header('Content-Type: application/json; charset=utf-8');
    $entrada = json_decode(file_get_contents('php://input'), true);
    $token   = $entrada['token'] ?? ($_POST['token'] ?? '');
    $r = api()->post('/validaciones', ['token' => $token]);
    if ($r['ok']) {
        echo json_encode($r['data']);
    } else {
        echo json_encode([
            'valido'  => false,
            'estado'  => 'ERROR',
            'mensaje' => $r['error'] ?? 'Error al validar',
        ]);
    }
    exit;
}

$titulo = 'Validar entrada';
require __DIR__ . '/includes/header.php';
?>
<h1>Validacion de ingreso</h1>
<p class="muted">Escanea el codigo QR del asistente o ingresa el codigo manualmente.</p>

<div class="validador">
    <div id="reader" class="reader"></div>

    <form id="manualForm" class="form-inline">
        <input type="text" id="tokenManual" placeholder="Pegar codigo (token) aqui" autocomplete="off">
        <button class="btn" type="submit">Validar</button>
    </form>

    <div id="resultado" class="resultado oculto">
        <div class="estado" id="estadoBanner"></div>
        <p id="mensaje"></p>
        <ul class="meta" id="detalle"></ul>
    </div>
</div>

<script src="https://unpkg.com/html5-qrcode" defer></script>
<script>
    let ultimoToken = null;
    let bloqueadoHasta = 0;

    async function validar(token) {
        if (!token) return;
        const ahora = Date.now();
        // Evita re-enviar el mismo QR repetidamente mientras la camara lo ve
        if (token === ultimoToken && ahora < bloqueadoHasta) return;
        ultimoToken = token;
        bloqueadoHasta = ahora + 3000;

        try {
            const resp = await fetch('validar.php', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token: token })
            });
            const data = await resp.json();
            mostrar(data);
        } catch (e) {
            mostrar({ valido: false, estado: 'ERROR', mensaje: 'No se pudo contactar el servidor' });
        }
    }

    function mostrar(data) {
        const cont = document.getElementById('resultado');
        const banner = document.getElementById('estadoBanner');
        const mensaje = document.getElementById('mensaje');
        const detalle = document.getElementById('detalle');

        cont.classList.remove('oculto');
        banner.className = 'estado ' + (data.valido ? 'verde' : 'rojo');
        banner.textContent = data.valido ? 'ACCESO AUTORIZADO' : 'ACCESO DENEGADO';
        mensaje.textContent = data.mensaje || '';

        detalle.innerHTML = '';
        const filas = [
            ['Asistente', data.asistenteNombre],
            ['Documento', data.documento],
            ['Evento', data.eventoNombre],
            ['Estado', data.estado]
        ];
        filas.forEach(function (f) {
            if (f[1]) {
                const li = document.createElement('li');
                li.textContent = f[0] + ': ' + f[1];
                detalle.appendChild(li);
            }
        });
    }

    // Envio manual
    document.getElementById('manualForm').addEventListener('submit', function (ev) {
        ev.preventDefault();
        validar(document.getElementById('tokenManual').value.trim());
    });

    // Escaner con camara (si el navegador lo permite)
    window.addEventListener('load', function () {
        if (typeof Html5Qrcode === 'undefined') return;
        const lector = new Html5Qrcode('reader');
        Html5Qrcode.getCameras().then(function (camaras) {
            if (camaras && camaras.length) {
                lector.start(
                    { facingMode: 'environment' },
                    { fps: 10, qrbox: 250 },
                    function (texto) { validar(texto.trim()); }
                ).catch(function () {
                    document.getElementById('reader').innerHTML =
                        '<p class="muted">No se pudo iniciar la camara. Usa el ingreso manual.</p>';
                });
            }
        }).catch(function () {
            document.getElementById('reader').innerHTML =
                '<p class="muted">Camara no disponible. Usa el ingreso manual.</p>';
        });
    });
</script>
<?php require __DIR__ . '/includes/footer.php'; ?>
