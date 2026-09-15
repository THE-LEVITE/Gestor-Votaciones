package co.edu.svis.dto;

/**
 * DTO para la solicitud de emisión de voto anónimo.
 */
public class VotoEmitirRequest {
    public Integer encuestaId;
    public Integer opcionId;
    public String token;
}
