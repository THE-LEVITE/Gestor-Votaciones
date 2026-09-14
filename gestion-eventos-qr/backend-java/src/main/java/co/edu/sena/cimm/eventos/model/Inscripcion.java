package co.edu.sena.cimm.eventos.model;

import java.time.LocalDateTime;

public class Inscripcion {

    private Long id;
    private String token;              // UUID: clave de negocio que viaja dentro del QR
    private Evento evento;
    private Asistente asistente;
    private EstadoInscripcion estado;
    private LocalDateTime fechaInscripcion;
    private LocalDateTime fechaValidacion; // null hasta que se valida en puerta

    public Inscripcion() {
    }

    public Inscripcion(Long id, String token, Evento evento, Asistente asistente,
                       EstadoInscripcion estado, LocalDateTime fechaInscripcion,
                       LocalDateTime fechaValidacion) {
        this.id = id;
        this.token = token;
        this.evento = evento;
        this.asistente = asistente;
        this.estado = estado;
        this.fechaInscripcion = fechaInscripcion;
        this.fechaValidacion = fechaValidacion;
    }

    public boolean estaPendiente() {
        return estado == EstadoInscripcion.PENDIENTE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public Asistente getAsistente() { return asistente; }
    public void setAsistente(Asistente asistente) { this.asistente = asistente; }

    public EstadoInscripcion getEstado() { return estado; }
    public void setEstado(EstadoInscripcion estado) { this.estado = estado; }

    public LocalDateTime getFechaInscripcion() { return fechaInscripcion; }
    public void setFechaInscripcion(LocalDateTime fechaInscripcion) { this.fechaInscripcion = fechaInscripcion; }

    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
    public void setFechaValidacion(LocalDateTime fechaValidacion) { this.fechaValidacion = fechaValidacion; }
}
