package co.edu.svis.model;

import java.time.LocalDateTime;

/**
 * Entidad del dominio que representa a un usuario institucional (Administrador o Votante).
 */
public class Usuario {

    private Integer id;
    private String documento;
    private String nombreCompleto;
    private String email;
    private String password;
    private String rol;
    private String estado;
    private LocalDateTime fechaRegistro;

    public Usuario() {
    }

    public Usuario(Integer id, String documento, String nombreCompleto, String email, 
                   String password, String rol, String estado, LocalDateTime fechaRegistro) {
        this.id = id;
        this.documento = documento;
        this.nombreCompleto = nombreCompleto;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
