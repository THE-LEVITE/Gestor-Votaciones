package co.edu.svis.dto;

/**
 * Excepción para tokens OTP que han vencido su vigencia TTL (mapea a HTTP 410 Gone).
 */
public class ExpiradoException extends RuntimeException {
    public ExpiradoException(String mensaje) {
        super(mensaje);
    }
}
