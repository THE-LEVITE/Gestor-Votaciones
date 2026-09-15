package co.edu.svis.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO para la consulta consolidada de resultados y porcentajes de una encuesta.
 */
public class EncuestaResultadosResponse {
    public int id;
    public String titulo;
    public String descripcion;
    public String estado;
    public int totalVotos;
    public List<Map<String, Object>> opciones;

    public EncuestaResultadosResponse() {
    }

    public EncuestaResultadosResponse(int id, String titulo, String descripcion, String estado, 
                                      int totalVotos, List<Map<String, Object>> opciones) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.totalVotos = totalVotos;
        this.opciones = opciones;
    }
}
