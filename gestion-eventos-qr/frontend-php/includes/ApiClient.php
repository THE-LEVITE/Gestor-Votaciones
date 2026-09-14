<?php
/**
 * Cliente HTTP minimalista para consumir el API REST del backend Java.
 * Usa cURL (incluido en PHP) para no depender de librerias externas.
 */
class ApiClient
{
    private $base;

    public function __construct($base)
    {
        $this->base = rtrim($base, '/');
    }

    public function get($path)
    {
        return $this->send('GET', $path, null);
    }

    public function post($path, array $data)
    {
        return $this->send('POST', $path, $data);
    }

    private function send($method, $path, $data)
    {
        $ch = curl_init($this->base . $path);
        $headers = ['Accept: application/json'];

        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);

        if ($data !== null) {
            $json = json_encode($data);
            curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
            $headers[] = 'Content-Type: application/json';
        }

        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

        $raw  = curl_exec($ch);
        $err  = curl_error($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($raw === false) {
            return [
                'ok'     => false,
                'status' => 0,
                'data'   => null,
                'error'  => 'No se pudo conectar con el backend (' . $err . '). '
                          . 'Verifica que Tomcat este corriendo en ' . $this->base,
            ];
        }

        return [
            'ok'     => ($code >= 200 && $code < 300),
            'status' => $code,
            'data'   => json_decode($raw, true),
            'error'  => null,
            'raw'    => $raw,
        ];
    }
}

/** Instancia unica del cliente. */
function api()
{
    static $cliente = null;
    if ($cliente === null) {
        $cliente = new ApiClient(API_BASE_URL);
    }
    return $cliente;
}

/** Formatea una fecha ISO (yyyy-MM-ddTHH:mm:ss) a dd/mm/YYYY HH:mm. */
function fmtFecha($iso)
{
    if (!$iso) {
        return 'Por definir';
    }
    try {
        $d = new DateTime($iso);
        return $d->format('d/m/Y H:i');
    } catch (Exception $e) {
        return $iso;
    }
}

/** Escape corto para HTML. */
function h($v)
{
    return htmlspecialchars((string) $v, ENT_QUOTES, 'UTF-8');
}
