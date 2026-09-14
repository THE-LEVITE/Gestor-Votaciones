package co.edu.svis.servlet;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.util.TokenGenerator;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet REST para la generación masiva y consulta de Tokens de Un Solo Uso (OTP).
 * Satisface la REGLA 1 (Unicidad en Base de Datos):
 * Cada usuario solo puede tener un único token generado por cada encuesta.
 *
 * Endpoints:
 * - POST /api/tokens/generar         -> [ADMIN] Genera OTPs masivos para aprendices registrados con TTL
 * - GET  /api/tokens/usuario         -> [VOTANTE] Consulta el token asignado al usuario para una encuesta
 * - GET  /api/tokens/padron          -> [ADMIN] Lista el padrón electoral y estado de tokens de la encuesta
 */
@WebServlet(name = "TokenServlet", urlPatterns = {"/api/tokens/*"})
public class TokenServlet extends BaseApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/usuario")) {
            consultarTokenUsuario(req, resp);
        } else if (pathInfo != null && pathInfo.equals("/padron")) {
            consultarPadronElectoral(req, resp);
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ruta de tokens no encontrada");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo != null && pathInfo.equals("/generar")) {
            generarPadronTokens(req, resp);
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no válido para POST");
        }
    }

    /**
     * Genera tokens masivos con TTL para todos los aprendices/votantes activos.
     * REGLA 1: La restricción UNIQUE(encuesta_id, usuario_id) protege contra duplicados.
     */
    private void generarPadronTokens(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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

        if (!json.has("encuestaId")) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Parámetro 'encuestaId' obligatorio");
            return;
        }

        int encuestaId = json.get("encuestaId").getAsInt();
        int ttlMinutos = json.has("ttlMinutos") ? json.get("ttlMinutos").getAsInt() : 1440; // Default 24 horas

        if (ttlMinutos <= 0) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "El TTL en minutos debe ser mayor a 0");
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = ahora.plusMinutes(ttlMinutos);
        Timestamp timestampExpiracion = Timestamp.valueOf(expiracion);

        String sqlVotantes = "SELECT u.id FROM usuarios u " +
                             "WHERE u.rol = 'VOTANTE' AND u.estado = 'ACTIVO' " +
                             "AND u.id NOT IN (SELECT t.usuario_id FROM tokens_otp t WHERE t.encuesta_id = ?)";

        String sqlInsertToken = "INSERT INTO tokens_otp (encuesta_id, usuario_id, token, estado, fecha_expiracion, fecha_creacion) " +
                                "VALUES (?, ?, ?, 'DISPONIBLE', ?, CURRENT_TIMESTAMP)";

        int tokensGenerados = 0;
        Connection conn = null;

        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false); // Transacción para consistencia

            // 1. Obtener los votantes elegibles que aún no tienen token para esta encuesta
            List<Integer> usuariosElegibles = new ArrayList<>();
            try (PreparedStatement psVotantes = conn.prepareStatement(sqlVotantes)) {
                psVotantes.setInt(1, encuestaId);
                try (ResultSet rs = psVotantes.executeQuery()) {
                    while (rs.next()) {
                        usuariosElegibles.add(rs.getInt("id"));
                    }
                }
            }

            // 2. Generar e insertar tokens aleatorios con SecureRandom
            if (!usuariosElegibles.isEmpty()) {
                try (PreparedStatement psInsert = conn.prepareStatement(sqlInsertToken)) {
                    for (int usuarioId : usuariosElegibles) {
                        String tokenOtp = TokenGenerator.generarToken(); // Cryptographically Secure
                        psInsert.setInt(1, encuestaId);
                        psInsert.setInt(2, usuarioId);
                        psInsert.setString(3, tokenOtp);
                        psInsert.setTimestamp(4, timestampExpiracion);
                        psInsert.addBatch();
                        tokensGenerados++;
                    }
                    psInsert.executeBatch();
                }
            }

            conn.commit();

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("mensaje", "Padrón electoral y tokens generados con éxito");
            responseData.put("encuestaId", encuestaId);
            responseData.put("tokensGenerados", tokensGenerados);
            responseData.put("ttlMinutos", ttlMinutos);
            responseData.put("expiracion", expiracion.toString());

            writeJson(resp, HttpServletResponse.SC_OK, responseData);

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignorar */ }
            }
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al generar tokens OTP: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { /* ignorar */ }
            }
        }
    }

    /**
     * Permite al estudiante consultar su token asignado para votar en la encuesta activa.
     */
    private void consultarTokenUsuario(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String usuarioIdStr = req.getParameter("usuarioId");
        String encuestaIdStr = req.getParameter("encuestaId");

        if (usuarioIdStr == null || encuestaIdStr == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Parámetros 'usuarioId' y 'encuestaId' requeridos");
            return;
        }

        int usuarioId = Integer.parseInt(usuarioIdStr);
        int encuestaId = Integer.parseInt(encuestaIdStr);

        String sql = "SELECT id, token, estado, fecha_expiracion, fecha_uso " +
                     "FROM tokens_otp WHERE encuesta_id = ? AND usuario_id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, encuestaId);
            ps.setInt(2, usuarioId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> tokenInfo = new HashMap<>();
                    tokenInfo.put("id", rs.getInt("id"));
                    tokenInfo.put("token", rs.getString("token"));
                    tokenInfo.put("estado", rs.getString("estado"));
                    tokenInfo.put("fechaExpiracion", rs.getTimestamp("fecha_expiracion").toString());
                    Timestamp uso = rs.getTimestamp("fecha_uso");
                    tokenInfo.put("fechaUso", uso != null ? uso.toString() : null);

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("success", true);
                    responseData.put("tokenInfo", tokenInfo);

                    writeJson(resp, HttpServletResponse.SC_OK, responseData);
                } else {
                    writeError(resp, HttpServletResponse.SC_NOT_FOUND, "No existe un token asignado para este usuario en esta encuesta");
                }
            }

        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos: " + e.getMessage());
        }
    }

    /**
     * Lista para el Administrador el padrón y estado de cada token OTP.
     */
    private void consultarPadronElectoral(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String encuestaIdStr = req.getParameter("encuestaId");
        if (encuestaIdStr == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Parámetro 'encuestaId' requerido");
            return;
        }

        int encuestaId = Integer.parseInt(encuestaIdStr);

        String sql = "SELECT t.id, t.token, t.estado, t.fecha_expiracion, t.fecha_uso, " +
                     "u.id AS usuario_id, u.documento, u.nombre_completo, u.email " +
                     "FROM tokens_otp t " +
                     "INNER JOIN usuarios u ON t.usuario_id = u.id " +
                     "WHERE t.encuesta_id = ? " +
                     "ORDER BY u.nombre_completo ASC";

        List<Map<String, Object>> padron = new ArrayList<>();

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, encuestaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", rs.getInt("id"));
                    item.put("token", rs.getString("token"));
                    item.put("estado", rs.getString("estado"));
                    item.put("fechaExpiracion", rs.getTimestamp("fecha_expiracion").toString());
                    Timestamp uso = rs.getTimestamp("fecha_uso");
                    item.put("fechaUso", uso != null ? uso.toString() : null);

                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("id", rs.getInt("usuario_id"));
                    usuario.put("documento", rs.getString("documento"));
                    usuario.put("nombreCompleto", rs.getString("nombre_completo"));
                    usuario.put("email", rs.getString("email"));
                    item.put("usuario", usuario);

                    padron.add(item);
                }
            }

            writeJson(resp, HttpServletResponse.SC_OK, padron);

        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al consultar padrón: " + e.getMessage());
        }
    }
}
