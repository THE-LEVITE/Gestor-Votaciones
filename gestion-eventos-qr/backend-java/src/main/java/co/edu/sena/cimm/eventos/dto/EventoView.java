package co.edu.sena.cimm.eventos.dto;

import java.time.LocalDateTime;

/** Vista de evento para el API: incluye conteo de inscritos y cupo disponible. */
public class EventoView {
    public Long id;
    public String nombre;
    public String descripcion;
    public String lugar;
    public LocalDateTime fecha;
    public int cupoMaximo;
    public long inscritos;
    public long cupoDisponible;
}
