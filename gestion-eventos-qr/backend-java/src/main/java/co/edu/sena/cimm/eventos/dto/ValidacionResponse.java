package co.edu.sena.cimm.eventos.dto;

import java.time.LocalDateTime;

public class ValidacionResponse {
    public boolean valido;
    public String estado;
    public String mensaje;
    public String asistenteNombre;
    public String documento;
    public String eventoNombre;
    public LocalDateTime eventoFecha;
    public LocalDateTime fechaValidacion;
}
