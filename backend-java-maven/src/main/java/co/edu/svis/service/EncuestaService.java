package co.edu.svis.service;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.dto.EncuestaRequest;
import co.edu.svis.dto.EncuestaResultadosResponse;
import co.edu.svis.dto.NegocioException;
import co.edu.svis.dto.RecursoNoEncontradoException;
import co.edu.svis.model.Encuesta;
import co.edu.svis.model.Opcion;
import co.edu.svis.repository.EncuestaRepository;
import co.edu.svis.repository.OpcionRepository;
import co.edu.svis.repository.VotoRepository;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de lógica de negocio para la gestión de encuestas institucionales y sus resultados.
 */
public class EncuestaService {

    private final EncuestaRepository encuestaRepository;
    private final OpcionRepository opcionRepository;
    private final VotoRepository votoRepository;

    public EncuestaService(EncuestaRepository encuestaRepository,
                           OpcionRepository opcionRepository,
                           VotoRepository votoRepository) {
        this.encuestaRepository = encuestaRepository;
        this.opcionRepository = opcionRepository;
        this.votoRepository = votoRepository;
    }

    public List<Encuesta> listarTodas() {
        return encuestaRepository.findAll();
    }

    public List<Encuesta> listarActivasConOpciones() {
        List<Encuesta> activas = encuestaRepository.findActivas();
        for (Encuesta e : activas) {
            e.setOpciones(opcionRepository.findByEncuestaId(e.getId()));
        }
        return activas;
    }

    public Encuesta obtenerPorId(int id) {
        return encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encuesta no encontrada"));
    }

    public EncuestaResultadosResponse obtenerResultados(int id) {
        Encuesta encuesta = encuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Encuesta no encontrada"));

        int totalVotos = votoRepository.countByEncuestaId(id);
        List<Opcion> opcionesDb = opcionRepository.findByEncuestaId(id);

        List<Map<String, Object>> opcionesResult = new ArrayList<>();
        for (Opcion opc : opcionesDb) {
            Map<String, Object> map = new HashMap<>();
            int votos = opc.getVotosConteo();
            map.put("id", opc.getId());
            map.put("nombre", opc.getNombre());
            map.put("descripcion", opc.getDescripcion());
            map.put("votos", votos);

            double porcentaje = totalVotos > 0 ? ((double) votos / totalVotos) * 100.0 : 0.0;
            map.put("porcentaje", Math.round(porcentaje * 100.0) / 100.0);
            opcionesResult.add(map);
        }

        return new EncuestaResultadosResponse(
                encuesta.getId(),
                encuesta.getTitulo(),
                encuesta.getDescripcion(),
                encuesta.getEstado(),
                totalVotos,
                opcionesResult
        );
    }

    public int crearEncuesta(EncuestaRequest req) {
        if (req == null) {
            throw new NegocioException("Cuerpo JSON requerido");
        }

        String titulo = req.titulo != null ? req.titulo.trim() : "";
        String descripcion = req.descripcion != null ? req.descripcion.trim() : "";

        if (titulo.isEmpty() || req.opciones == null || req.opciones.size() < 2) {
            throw new NegocioException("El título y al menos dos opciones son obligatorios.");
        }

        List<Opcion> listaOpciones = new ArrayList<>();
        for (JsonElement elem : req.opciones) {
            String nombre = "";
            String desc = "";
            if (elem.isJsonObject()) {
                JsonObject o = elem.getAsJsonObject();
                nombre = o.has("nombre") ? o.get("nombre").getAsString().trim() : "";
                desc = o.has("descripcion") ? o.get("descripcion").getAsString().trim() : "";
            } else {
                nombre = elem.getAsString().trim();
            }

            if (!nombre.isEmpty()) {
                Opcion op = new Opcion();
                op.setNombre(nombre);
                op.setDescripcion(desc);
                listaOpciones.add(op);
            }
        }

        if (listaOpciones.size() < 2) {
            throw new NegocioException("Debe ingresar al menos dos opciones válidas.");
        }

        Connection conn = null;
        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            Encuesta e = new Encuesta();
            e.setTitulo(titulo);
            e.setDescripcion(descripcion);

            int encuestaId = encuestaRepository.insert(e, conn);
            opcionRepository.insertBatch(encuestaId, listaOpciones, conn);

            conn.commit();
            return encuestaId;

        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            throw new RuntimeException("Error al crear encuesta: " + ex.getMessage(), ex);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignore) {}
            }
        }
    }

    public void cambiarEstado(int id, String nuevoEstado) {
        if (nuevoEstado == null) {
            throw new NegocioException("Estado requerido");
        }
        String estadoNorm = nuevoEstado.toUpperCase().trim();
        if (!estadoNorm.equals("ACTIVA") && !estadoNorm.equals("CERRADA")) {
            throw new NegocioException("Estado debe ser ACTIVA o CERRADA");
        }

        boolean actualizado = encuestaRepository.updateEstado(id, estadoNorm);
        if (!actualizado) {
            throw new RecursoNoEncontradoException("Encuesta no encontrada");
        }
    }
}
