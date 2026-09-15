package co.edu.svis.model;

/**
 * Entidad del dominio que representa una opción o candidatura dentro de una encuesta.
 */
public class Opcion {

    private Integer id;
    private Integer encuestaId;
    private String nombre;
    private String descripcion;
    private int votosConteo;
    private double porcentaje;

    public Opcion() {
    }

    public Opcion(Integer id, Integer encuestaId, String nombre, String descripcion, int votosConteo) {
        this.id = id;
        this.encuestaId = encuestaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.votosConteo = votosConteo;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getEncuestaId() { return encuestaId; }
    public void setEncuestaId(Integer encuestaId) { this.encuestaId = encuestaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getVotosConteo() { return votosConteo; }
    public void setVotosConteo(int votosConteo) { this.votosConteo = votosConteo; }

    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }
}
