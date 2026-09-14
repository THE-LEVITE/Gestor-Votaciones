package co.edu.sena.cimm.eventos.dto;

public class ApiError {
    public String error;
    public String mensaje;

    public ApiError(String error, String mensaje) {
        this.error = error;
        this.mensaje = mensaje;
    }
}
