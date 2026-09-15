package co.edu.svis.model;

import java.sql.Timestamp;

/**
 * Entidad del dominio que representa un Token de Un Solo Uso (OTP) asignado a un usuario para una encuesta.
 */
public class TokenOtp {

    private Integer id;
    private Integer encuestaId;
    private Integer usuarioId;
    private String token;
    private String estado;
    private Timestamp fechaExpiracion;
    private Timestamp fechaUso;
    private Timestamp fechaCreacion;
    private Usuario usuario;

    public TokenOtp() {
    }

    public TokenOtp(Integer id, Integer encuestaId, Integer usuarioId, String token, 
                    String estado, Timestamp fechaExpiracion, Timestamp fechaUso, Timestamp fechaCreacion) {
        this.id = id;
        this.encuestaId = encuestaId;
        this.usuarioId = usuarioId;
        this.token = token;
        this.estado = estado;
        this.fechaExpiracion = fechaExpiracion;
        this.fechaUso = fechaUso;
        this.fechaCreacion = fechaCreacion;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEncuestaId() { return encuestaId; }
    public void setEncuestaId(Integer encuestaId) { this.encuestaId = encuestaId; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Timestamp getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(Timestamp fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }

    public Timestamp getFechaUso() { return fechaUso; }
    public void setFechaUso(Timestamp fechaUso) { this.fechaUso = fechaUso; }

    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
