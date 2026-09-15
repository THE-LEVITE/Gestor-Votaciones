package co.edu.svis.dto;

/**
 * DTO para la respuesta de generación masiva de tokens OTP.
 */
public class TokenGenerarResponse {
    public boolean success;
    public String mensaje;
    public int encuestaId;
    public int tokensGenerados;
    public int ttlMinutos;
    public String expiracion;

    public TokenGenerarResponse() {
    }

    public TokenGenerarResponse(boolean success, String mensaje, int encuestaId, 
                                int tokensGenerados, int ttlMinutos, String expiracion) {
        this.success = success;
        this.mensaje = mensaje;
        this.encuestaId = encuestaId;
        this.tokensGenerados = tokensGenerados;
        this.ttlMinutos = ttlMinutos;
        this.expiracion = expiracion;
    }
}
