package co.edu.svis.dto;

/**
 * Excepción para errores de validación o reglas de negocio (mapea a HTTP 400).
 */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
