package co.edu.svis.servlet;

import co.edu.svis.config.ConexionBD;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet REST para la administración y consulta de encuestas institucionales y sus resultados.
 * Rutas soportadas:
 * - GET  /api/encuestas/activas        -> Lista encuestas con estado ACTIVA y sus opciones
 * - GET  /api/encuestas/{id}/resultados -> Retorna consolidado de votos y porcentajes por opción
 * - GET  /api/encuestas                -> Lista todas las encuestas
 * - POST /api/encuestas                -> [ADMIN] Crea una nueva encuesta con sus opciones en transacción
 * - PUT  /api/encuestas/{id}/estado    -> [ADMIN] Cambia estado entre ACTIVA y CERRADA
 */
@WebServlet(name = "EncuestaServlet", urlPatterns = {"/api/encuestas/*"})
public class EncuestaServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            listarTodasLasEncuestas(resp);
        } else if (pathInfo.equals("/activas")) {
            listarEncuestasActivas(resp);
        } else if (pathInfo.matches("/\\d+/resultados")) {
            String[] parts = pathInfo.split("/");
            int encuestaId = Integer.parseInt(parts[1]);
            obtenerResultadosEncuesta(encuestaId, resp);
        } else if (pathInfo.matches("/\\d+")) {
            int encuestaId = Integer.parseInt(pathInfo.substring(1));
            obtenerDetalleEncuesta(encuestaId, resp);
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso de encuesta no encontrado");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/") && !pathInfo.isEmpty()) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no válido para POST");
            return;
        }

        String body = readBody(req);
        if (body == null || body.trim().isEmpty()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON requerido");
            return;
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "JSON mal formado");
            return;
        }

        String titulo = json.has("titulo") ? json.get("titulo").getAsString().trim() : "";
        String descripcion = json.has("descripcion") ? json.get("descripcion").getAsString().trim() : "";
        JsonArray opcionesJson = json.has("opciones") ? json.getAsJsonArray("opciones") : null;

        if (titulo.isEmpty() || opcionesJson == null || opcionesJson.size() < 2) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "El título y al menos dos opciones son obligatorios.");
            return;
        }

        // Creación atómica de encuesta y sus opciones bajo transacción JDBC
        Connection conn = null;
        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false); // Transacción manual

            int encuestaId;
            String sqlEncuesta = "INSERT INTO encuestas (titulo, descripcion, estado, fecha_creacion) VALUES (?, ?, 'ACTIVA', CURRENT_TIMESTAMP)";
            try (PreparedStatement psEncuesta = conn.prepareStatement(sqlEncuesta, Statement.RETURN_GENERATED_KEYS)) {
                psEncuesta.setString(1, titulo);
                psEncuesta.setString(2, descripcion);
                psEncuesta.executeUpdate();

                try (ResultSet rs = psEncuesta.getGeneratedKeys()) {
                    if (rs.next()) {
                        encuestaId = rs.getInt(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID de la encuesta generada");
                    }
                }
            }

            String sqlOpcion = "INSERT INTO opciones (encuesta_id, nombre, descripcion, votos_conteo) VALUES (?, ?, ?, 0)";
            try (PreparedStatement psOpcion = conn.prepareStatement(sqlOpcion)) {
                for (JsonElement elem : opcionesJson) {
                    String nombreOpcion = "";
                    String descOpcion = "";
                    if (elem.isJsonObject()) {
                        JsonObject o = elem.getAsJsonObject();
                        nombreOpcion = o.has("nombre") ? o.get("nombre").getAsString().trim() : "";
                        descOpcion = o.has("descripcion") ? o.get("descripcion").getAsString().trim() : "";
                    } else {
                        nombreOpcion = elem.getAsString().trim();
                    }

                    if (!nombreOpcion.isEmpty()) {
                        psOpcion.setInt(1, encuestaId);
                        psOpcion.setString(2, nombreOpcion);
                        psOpcion.setString(3, descOpcion);
                        psOpcion.addBatch();
                    }
                }
                psOpcion.executeBatch();
            }

            conn.commit(); // Confirmación atómica

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("mensaje", "Encuesta y opciones creadas exitosamente");
            responseData.put("encuestaId", encuestaId);

            writeJson(resp, HttpServletResponse.SC_CREATED, responseData);

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
            }
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al crear encuesta: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { /* ignorar */ }
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+/estado")) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no válido para PUT. Use /api/encuestas/{id}/estado");
            return;
        }

        int encuestaId = Integer.parseInt(pathInfo.split("/")[1]);
        String body = readBody(req);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        String nuevoEstado = json.has("estado") ? json.get("estado").getAsString().toUpperCase().trim() : "";

        if (!nuevoEstado.equals("ACTIVA") && !nuevoEstado.equals("CERRADA")) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Estado debe ser ACTIVA o CERRADA");
            return;
        }

        String sql = "UPDATE encuestas SET estado = ?, fecha_cierre = " + 
                     (nuevoEstado.equals("CERRADA") ? "CURRENT_TIMESTAMP" : "NULL") + 
                     " WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, encuestaId);
            int filas = ps.executeUpdate();

            if (filas > 0) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("mensaje", "Estado de encuesta actualizado a " + nuevoEstado);
                writeJson(resp, HttpServletResponse.SC_OK, responseData);
            } else {
                writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Encuesta no encontrada");
            }
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos: " + e.getMessage());
        }
    }

    private void listarEncuestasActivas(HttpServletResponse resp) throws IOException {
        String sqlEncuestas = "SELECT id, titulo, descripcion, estado, fecha_creacion FROM encuestas WHERE estado = 'ACTIVA' ORDER BY id DESC";
        String sqlOpciones = "SELECT id, encuesta_id, nombre, descripcion FROM opciones WHERE encuesta_id = ? ORDER BY id ASC";

        List<Map<String, Object>> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement psEnc = conn.prepareStatement(sqlEncuestas);
             ResultSet rsEnc = psEnc.executeQuery()) {

            while (rsEnc.next()) {
                Map<String, Object> enc = new HashMap<>();
                int id = rsEnc.getInt("id");
                enc.put("id", id);
                enc.put("titulo", rsEnc.getString("titulo"));
                enc.put("descripcion", rsEnc.getString("descripcion"));
                enc.put("estado", rsEnc.getString("estado"));
                enc.put("fechaCreacion", rsEnc.getString("fecha_creacion"));

                List<Map<String, Object>> opciones = new ArrayList<>();
                try (PreparedStatement psOpc = conn.prepareStatement(sqlOpciones)) {
                    psOpc.setInt(1, id);
                    try (ResultSet rsOpc = psOpc.executeQuery()) {
                        while (rsOpc.next()) {
                            Map<String, Object> opc = new HashMap<>();
                            opc.put("id", rsOpc.getInt("id"));
                            opc.put("nombre", rsOpc.getString("nombre"));
                            opc.put("descripcion", rsOpc.getString("descripcion"));
                            opciones.add(opc);
                        }
                    }
                }
                enc.put("opciones", opciones);
                lista.add(enc);
            }

            writeJson(resp, HttpServletResponse.SC_OK, lista);

        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al consultar encuestas activas: " + e.getMessage());
        }
    }

    private void listarTodasLasEncuestas(HttpServletResponse resp) throws IOException {
        String sql = "SELECT e.id, e.titulo, e.descripcion, e.estado, e.fecha_creacion, e.fecha_cierre, " +
                     "(SELECT COUNT(*) FROM votos v WHERE v.encuesta_id = e.id) AS total_votos, " +
                     "(SELECT COUNT(*) FROM tokens_otp t WHERE t.encuesta_id = e.id) AS total_tokens " +
                     "FROM encuestas e ORDER BY e.id DESC";

        List<Map<String, Object>> lista = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getInt("id"));
                item.put("titulo", rs.getString("titulo"));
                item.put("descripcion", rs.getString("descripcion"));
                item.put("estado", rs.getString("estado"));
                item.put("fechaCreacion", rs.getString("fecha_creacion"));
                item.put("fechaCierre", rs.getString("fecha_cierre"));
                item.put("totalVotos", rs.getInt("total_votos"));
                item.put("totalTokens", rs.getInt("total_tokens"));
                lista.add(item);
            }

            writeJson(resp, HttpServletResponse.SC_OK, lista);

        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al listar encuestas: " + e.getMessage());
        }
    }

    private void obtenerDetalleEncuesta(int encuestaId, HttpServletResponse resp) throws IOException {
        String sql = "SELECT id, titulo, descripcion, estado, fecha_creacion, fecha_cierre FROM encuestas WHERE id = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> enc = new HashMap<>();
                    enc.put("id", rs.getInt("id"));
                    enc.put("titulo", rs.getString("titulo"));
                    enc.put("descripcion", rs.getString("descripcion"));
                    enc.put("estado", rs.getString("estado"));
                    enc.put("fechaCreacion", rs.getString("fecha_creacion"));
                    enc.put("fechaCierre", rs.getString("fecha_cierre"));
                    writeJson(resp, HttpServletResponse.SC_OK, enc);
                } else {
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Encuesta no encontrada");
                }
            }
        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al obtener detalle: " + e.getMessage());
        }
    }

    private void obtenerResultadosEncuesta(int encuestaId, HttpServletResponse resp) throws IOException {
        String sqlEncuesta = "SELECT id, titulo, descripcion, estado FROM encuestas WHERE id = ?";
        String sqlOpciones = "SELECT id, nombre, descripcion, votos_conteo FROM opciones WHERE encuesta_id = ? ORDER BY votos_conteo DESC, id ASC";
        String sqlTotalVotos = "SELECT COUNT(*) AS total FROM votos WHERE encuesta_id = ?";

        try (Connection conn = ConexionBD.getConnection()) {
            Map<String, Object> resultado = new HashMap<>();

            try (PreparedStatement ps = conn.prepareStatement(sqlEncuesta)) {
                ps.setInt(1, encuestaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Encuesta no encontrada");
                        return;
                    }
                    resultado.put("id", rs.getInt("id"));
                    resultado.put("titulo", rs.getString("titulo"));
                    resultado.put("descripcion", rs.getString("descripcion"));
                    resultado.put("estado", rs.getString("estado"));
                }
            }

            int totalVotos = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlTotalVotos)) {
                ps.setInt(1, encuestaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalVotos = rs.getInt("total");
                    }
                }
            }
            resultado.put("totalVotos", totalVotos);

            List<Map<String, Object>> opciones = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlOpciones)) {
                ps.setInt(1, encuestaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> opc = new HashMap<>();
                        int votosOpcion = rs.getInt("votos_conteo");
                        opc.put("id", rs.getInt("id"));
                        opc.put("nombre", rs.getString("nombre"));
                        opc.put("descripcion", rs.getString("descripcion"));
                        opc.put("votos", votosOpcion);

                        double porcentaje = totalVotos > 0 ? ((double) votosOpcion / totalVotos) * 100.0 : 0.0;
                        opc.put("porcentaje", Math.round(porcentaje * 100.0) / 100.0);
                        opciones.add(opc);
                    }
                }
            }
            resultado.put("opciones", opciones);

            writeJson(resp, HttpServletResponse.SC_OK, resultado);

        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al calcular resultados: " + e.getMessage());
        }
    }
}
