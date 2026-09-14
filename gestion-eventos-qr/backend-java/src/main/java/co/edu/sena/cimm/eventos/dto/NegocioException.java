package co.edu.sena.cimm.eventos.dto;

/** Error de reglas de negocio -> se traduce a HTTP 400/404 en los servlets. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
