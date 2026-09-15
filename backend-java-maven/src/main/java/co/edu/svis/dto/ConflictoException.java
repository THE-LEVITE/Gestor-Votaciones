package co.edu.svis.dto;

/**
 * Excepción para colisiones transaccionales o tokens ya usados (mapea a HTTP 409 Conflict).
 */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
