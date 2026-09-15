package co.edu.svis.dto;

/**
 * Excepción para recursos no encontrados (mapea a HTTP 404).
 */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
