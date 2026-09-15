package co.edu.svis.dto;

import java.util.Map;

/**
 * DTO para la respuesta de autenticación exitosa.
 */
public class LoginResponse {
    public boolean success;
    public String mensaje;
    public Map<String, Object> usuario;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String mensaje, Map<String, Object> usuario) {
        this.success = success;
        this.mensaje = mensaje;
        this.usuario = usuario;
    }
}
