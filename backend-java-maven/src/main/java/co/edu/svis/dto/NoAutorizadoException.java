package co.edu.svis.dto;

/**
 * Excepción para credenciales inválidas (mapea a HTTP 401 Unauthorized).
 */
public class NoAutorizadoException extends RuntimeException {
    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
