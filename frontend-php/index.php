<?php
require_once __DIR__ . '/config.php';

if (!isLoggedIn()) {
    header('Location: login.php');
    exit;
}

$user = getLoggedUser();
if ($user['rol'] === 'ADMIN') {
    header('Location: admin/dashboard.php');
} else {
    header('Location: votante/votar.php');
}
exit;
