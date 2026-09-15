package co.edu.svis.dto;

/**
 * Excepción para accesos no autorizados por estado de cuenta (mapea a HTTP 403 Forbidden).
 */
public class ProhibidoException extends RuntimeException {
    public ProhibidoException(String mensaje) {
        super(mensaje);
    }
}
