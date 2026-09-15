package co.edu.svis.dto;

import java.util.Map;

/**
 * DTO para la consulta individual de token asignado al estudiante.
 */
public class TokenUsuarioResponse {
    public boolean success;
    public Map<String, Object> tokenInfo;

    public TokenUsuarioResponse() {
    }

    public TokenUsuarioResponse(boolean success, Map<String, Object> tokenInfo) {
        this.success = success;
        this.tokenInfo = tokenInfo;
    }
}
