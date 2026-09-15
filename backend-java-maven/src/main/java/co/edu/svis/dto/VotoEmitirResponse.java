package co.edu.svis.dto;

/**
 * DTO para la respuesta de emisión de voto confirmada con comprobante digital anónimo.
 */
public class VotoEmitirResponse {
    public boolean success;
    public String mensaje;
    public String reciboDigital;
    public String fechaEmision;
    public int encuestaId;

    public VotoEmitirResponse() {
    }

    public VotoEmitirResponse(boolean success, String mensaje, String reciboDigital, String fechaEmision, int encuestaId) {
        this.success = success;
        this.mensaje = mensaje;
        this.reciboDigital = reciboDigital;
        this.fechaEmision = fechaEmision;
        this.encuestaId = encuestaId;
    }
}
