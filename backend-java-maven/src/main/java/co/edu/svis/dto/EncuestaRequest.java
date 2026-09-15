package co.edu.svis.dto;

import com.google.gson.JsonElement;
import java.util.List;

/**
 * DTO para la creación de una nueva encuesta institucional.
 */
public class EncuestaRequest {
    public String titulo;
    public String descripcion;
    public List<JsonElement> opciones;
}
