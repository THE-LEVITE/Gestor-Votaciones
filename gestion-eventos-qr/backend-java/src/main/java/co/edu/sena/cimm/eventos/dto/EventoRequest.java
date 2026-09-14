package co.edu.sena.cimm.eventos.dto;

import java.time.LocalDateTime;

public class EventoRequest {
    public String nombre;
    public String descripcion;
    public String lugar;
    public LocalDateTime fecha;
    public int cupoMaximo;
}
