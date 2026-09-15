package co.edu.svis.model;

import java.sql.Timestamp;

/**
 * Entidad del dominio que representa un voto institucional anónimo.
 * Satisface estrictamente la REGLA 4: No almacena usuario_id ni token_id.
 */
public class Voto {

    private Integer id;
    private Integer encuestaId;
    private Integer opcionId;
    private Timestamp fecha;

    public Voto() {
    }

    public Voto(Integer id, Integer encuestaId, Integer opcionId, Timestamp fecha) {
        this.id = id;
        this.encuestaId = encuestaId;
        this.opcionId = opcionId;
        this.fecha = fecha;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEncuestaId() { return encuestaId; }
    public void setEncuestaId(Integer encuestaId) { this.encuestaId = encuestaId; }

    public Integer getOpcionId() { return opcionId; }
    public void setOpcionId(Integer opcionId) { this.opcionId = opcionId; }

    public Timestamp getFecha() { return fecha; }
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }
}
