package co.edu.svis.servlet;

import co.edu.svis.config.ConexionBD;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet central para el procesamiento transaccional de votos institucionales.
 * Implementa las 4 Reglas Críticas de Integridad:
 * 
 * - REGLA 1: Validación de token asociado a la encuesta.
 * - REGLA 2 (Quema Atómica del OTP): Token pasa de DISPONIBLE a USADO en la misma
 *   transacción JDBC en que se registra el voto. Si falla, se invoca rollback().
 * - REGLA 3 (Aislamiento contra Race Conditions): Mediante SELECT ... FOR UPDATE
 *   (bloqueo pesimista de fila en InnoDB), dos hilos simultáneos se serializan:
 *   el primero responde HTTP 200 OK y el segundo responde HTTP 409 Conflict.
 * - REGLA 4 (Secreto Absoluto del Voto): La tabla votos no almacena id del usuario ni
 *   del token. Solo contiene (id, encuesta_id, opcion_id, fecha). El acumulador de
 *   la opción se incrementa numéricamente (+1).
 */
@WebServlet(name = "VotoServlet", urlPatterns = {"/api/votos/*"})
public class VotoServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/emitir") && !pathInfo.equals("/")) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no encontrado. Use POST /api/votos/emitir");
            return;
        }

        String body = readBody(req);
        if (body == null || body.trim().isEmpty()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON de voto requerido");
            return;
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "JSON mal formado");
            return;
        }

        if (!json.has("encuestaId") || !json.has("opcionId") || !json.has("token")) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Campos 'encuestaId', 'opcionId' y 'token' son obligatorios");
            return;
        }

        int encuestaId = json.get("encuestaId").getAsInt();
        int opcionId = json.get("opcionId").getAsInt();
        String tokenOtp = json.get("token").getAsString().trim();

        if (tokenOtp.isEmpty()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "El token OTP no puede estar vacío");
            return;
        }

        // =====================================================================
        // TRANSACCIÓN JDBC CON AISLAMIENTO Y BLOQUEO PESIMISTA
        // =====================================================================
        Connection conn = null;
        try {
            conn = ConexionBD.getConnection();
            // Desactivar autocommit para control transaccional explícito (ACID)
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // -----------------------------------------------------------------
            // PASO 1: REGLA 3 - Bloqueo Pesimista de Fila (SELECT ... FOR UPDATE)
            // InnoDB adquiere un cerrojo exclusivo (X-Lock) sobre el registro del token.
            // Si dos peticiones compiten por el mismo token, la segunda se suspende
            // hasta que la primera confirme (commit) o aborte (rollback).
            // -----------------------------------------------------------------
            String sqlLockToken = "SELECT id, encuesta_id, estado, fecha_expiracion " +
                                  "FROM tokens_otp WHERE token = ? FOR UPDATE";

            int tokenId;
            int tokenEncuestaId;
            String estadoToken;
            Timestamp fechaExpiracion;

            try (PreparedStatement psLock = conn.prepareStatement(sqlLockToken)) {
                psLock.setString(1, tokenOtp);
                try (ResultSet rs = psLock.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        writeError(resp, HttpServletResponse.SC_NOT_FOUND, "El código de votación no existe o no es válido");
                        return;
                    }
                    tokenId = rs.getInt("id");
                    tokenEncuestaId = rs.getInt("encuesta_id");
                    estadoToken = rs.getString("estado");
                    fechaExpiracion = rs.getTimestamp("fecha_expiracion");
                }
            }

            // -----------------------------------------------------------------
            // PASO 2: Evaluación del Estado bajo Bloqueo Exclusivo
            // Si el hilo competidor ya ejecutó el commit, el segundo hilo leerá 'USADO'.
            // En ese caso, rechazamos inmediatamente con HTTP 409 Conflict.
            // -----------------------------------------------------------------
            if (!"DISPONIBLE".equalsIgnoreCase(estadoToken)) {
                conn.rollback();
                Map<String, Object> conflictResponse = new HashMap<>();
                conflictResponse.put("error", "Ya has participado en esta votación. Cada persona solo puede votar una vez.");
                conflictResponse.put("status", HttpServletResponse.SC_CONFLICT);
                writeJson(resp, HttpServletResponse.SC_CONFLICT, conflictResponse); // HTTP 409
                return;
            }

            // -----------------------------------------------------------------
            // PASO 3: Validación de Expiración (TTL)
            // -----------------------------------------------------------------
            if (fechaExpiracion != null && fechaExpiracion.before(new java.util.Date())) {
                try (PreparedStatement psExp = conn.prepareStatement("UPDATE tokens_otp SET estado = 'EXPIRADO' WHERE id = ?")) {
                    psExp.setInt(1, tokenId);
                    psExp.executeUpdate();
                }
                conn.commit();
                writeError(resp, HttpServletResponse.SC_GONE, "El tiempo para votar con este código ha vencido.");
                return;
            }

            // -----------------------------------------------------------------
            // PASO 4: Validación de correspondencia de encuesta
            // -----------------------------------------------------------------
            if (tokenEncuestaId != encuestaId) {
                conn.rollback();
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Este código de votación no corresponde a esta encuesta.");
                return;
            }

            // -----------------------------------------------------------------
            // PASO 5: Validación de Estado ACTIVO de la encuesta
            // -----------------------------------------------------------------
            String sqlEstadoEncuesta = "SELECT estado FROM encuestas WHERE id = ?";
            try (PreparedStatement psEnc = conn.prepareStatement(sqlEstadoEncuesta)) {
                psEnc.setInt(1, encuestaId);
                try (ResultSet rsEnc = psEnc.executeQuery()) {
                    if (!rsEnc.next() || !"ACTIVA".equalsIgnoreCase(rsEnc.getString("estado"))) {
                        conn.rollback();
                        writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Esta votación ha finalizado o no está activa en este momento.");
                        return;
                    }
                }
            }

            // -----------------------------------------------------------------
            // PASO 6: Validación de Existencia de la Opción en la Encuesta
            // -----------------------------------------------------------------
            String sqlOpcionValida = "SELECT id FROM opciones WHERE id = ? AND encuesta_id = ?";
            try (PreparedStatement psOpc = conn.prepareStatement(sqlOpcionValida)) {
                psOpc.setInt(1, opcionId);
                psOpc.setInt(2, encuestaId);
                try (ResultSet rsOpc = psOpc.executeQuery()) {
                    if (!rsOpc.next()) {
                        conn.rollback();
                        writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "La opción seleccionada no pertenece a esta encuesta.");
                        return;
                    }
                }
            }

            // -----------------------------------------------------------------
            // PASO 7: REGLA 2 - Quema Atómica del Token OTP (DISPONIBLE -> USADO)
            // -----------------------------------------------------------------
            String sqlQuemar = "UPDATE tokens_otp SET estado = 'USADO', fecha_uso = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement psQuemar = conn.prepareStatement(sqlQuemar)) {
                psQuemar.setInt(1, tokenId);
                int filasAfectadas = psQuemar.executeUpdate();
                if (filasAfectadas == 0) {
                    conn.rollback();
                    writeError(resp, HttpServletResponse.SC_CONFLICT, "No fue posible quemar el token OTP");
                    return;
                }
            }

            // -----------------------------------------------------------------
            // PASO 8: REGLA 4 - Registro del Voto Anónimo
            // Estrictamente prohibido almacenar usuario_id o token_id en esta tabla.
            // -----------------------------------------------------------------
            int votoIdGenerado = 0;
            String sqlInsertVoto = "INSERT INTO votos (encuesta_id, opcion_id, fecha) VALUES (?, ?, CURRENT_TIMESTAMP)";
            try (PreparedStatement psVoto = conn.prepareStatement(sqlInsertVoto, Statement.RETURN_GENERATED_KEYS)) {
                psVoto.setInt(1, encuestaId);
                psVoto.setInt(2, opcionId);
                psVoto.executeUpdate();

                try (ResultSet rsKey = psVoto.getGeneratedKeys()) {
                    if (rsKey.next()) {
                        votoIdGenerado = rsKey.getInt(1);
                    }
                }
            }

            // -----------------------------------------------------------------
            // PASO 9: REGLA 4 - Incremento Numérico Atómico de la Opción (+1)
            // -----------------------------------------------------------------
            String sqlInc = "UPDATE opciones SET votos_conteo = votos_conteo + 1 WHERE id = ?";
            try (PreparedStatement psInc = conn.prepareStatement(sqlInc)) {
                psInc.setInt(1, opcionId);
                psInc.executeUpdate();
            }

            // -----------------------------------------------------------------
            // PASO 10: COMMIT DE LA TRANSACCIÓN (Persistencia ACID)
            // Al confirmar, se liberan los cerrojos de fila adquiridos por FOR UPDATE.
            // -----------------------------------------------------------------
            conn.commit();

            // -----------------------------------------------------------------
            // PASO 11: Generación de Comprobante Digital Anónimo (Hash SHA-256)
            // -----------------------------------------------------------------
            String timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String rawReceipt = "SVIS-CERT-" + votoIdGenerado + "-" + encuestaId + "-" + System.nanoTime();
            String reciboHash = calcularSha256(rawReceipt);

            Map<String, Object> respuestaExito = new HashMap<>();
            respuestaExito.put("success", true);
            respuestaExito.put("mensaje", "¡Tu voto ha sido registrado y contabilizado con éxito!");
            respuestaExito.put("reciboDigital", reciboHash);
            respuestaExito.put("fechaEmision", timestampStr);
            respuestaExito.put("encuestaId", encuestaId);

            writeJson(resp, HttpServletResponse.SC_OK, respuestaExito);

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    /* log rollback error */
                }
            }
            // Si ocurre detección de deadlock o lock wait timeout de MySQL
            if (e.getErrorCode() == 1205 || e.getErrorCode() == 1213) {
                writeError(resp, HttpServletResponse.SC_CONFLICT, "Conflicto por colisión de transacciones en bloqueo pesimista");
            } else {
                writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error transaccional al emitir voto: " + e.getMessage());
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* ignorar */
                }
            }
        }
    }

    /**
     * Calcula resumen criptográfico SHA-256 para el comprobante digital anónimo.
     */
    private String calcularSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "RECIBO-" + Long.toHexString(System.currentTimeMillis());
        }
    }
}
