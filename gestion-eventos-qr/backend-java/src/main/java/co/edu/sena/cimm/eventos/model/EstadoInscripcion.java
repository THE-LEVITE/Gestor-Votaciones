package co.edu.sena.cimm.eventos.model;

/**
 * Ciclo de vida de una inscripcion (mini "maquina de estados").
 * PENDIENTE -> VALIDADA (al escanear el QR en la puerta)
 * PENDIENTE -> CANCELADA (opcional, si se anula la inscripcion)
 */
public enum EstadoInscripcion {
    PENDIENTE("Inscripcion registrada, pendiente de ingreso"),
    VALIDADA("Ingreso validado en puerta"),
    CANCELADA("Inscripcion cancelada");

    private final String descripcion;

    EstadoInscripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
