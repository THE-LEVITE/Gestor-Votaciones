package co.edu.svis.dto;

/**
 * DTO para la petición de inicio de sesión.
 */
public class LoginRequest {
    public String identificador;
    public String email;
    public String password;

    public String getIdentificadorEfectivo() {
        if (identificador != null && !identificador.trim().isEmpty()) {
            return identificador.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            return email.trim();
        }
        return "";
    }
}
