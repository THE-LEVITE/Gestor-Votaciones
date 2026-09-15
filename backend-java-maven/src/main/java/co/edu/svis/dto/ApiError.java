package co.edu.svis.dto;

/**
 * DTO estándar para respuestas de error de la API.
 */
public class ApiError {
    public String error;
    public int status;

    public ApiError() {
    }

    public ApiError(String error, int status) {
        this.error = error;
        this.status = status;
    }
}
