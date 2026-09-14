package co.edu.sena.cimm.eventos.model;

import java.time.LocalDateTime;

public class Evento {

    private Long id;
    private String nombre;
    private String descripcion;
    private String lugar;
    private LocalDateTime fecha;
    private int cupoMaximo;

    public Evento() {
    }

    public Evento(Long id, String nombre, String descripcion, String lugar,
                  LocalDateTime fecha, int cupoMaximo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fecha = fecha;
        this.cupoMaximo = cupoMaximo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public int getCupoMaximo() { return cupoMaximo; }
    public void setCupoMaximo(int cupoMaximo) { this.cupoMaximo = cupoMaximo; }
}
