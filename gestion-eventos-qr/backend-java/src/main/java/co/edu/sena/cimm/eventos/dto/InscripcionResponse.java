package co.edu.sena.cimm.eventos.dto;

import java.time.LocalDateTime;

public class InscripcionResponse {
    public String token;
    public String estado;
    public String mensaje;
    public String eventoNombre;
    public LocalDateTime eventoFecha;
    public String eventoLugar;
    public String asistenteNombre;
    public String qrUrl; // ruta relativa del QR en el backend (informativa)
}
