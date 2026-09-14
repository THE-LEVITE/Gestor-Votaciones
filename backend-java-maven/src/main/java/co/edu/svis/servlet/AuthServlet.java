package co.edu.svis.servlet;

import co.edu.svis.config.ConexionBD;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet para autenticación institucional de usuarios (Administradores y Aprendices).
 * Permite que el Frontend PHP verifique credenciales sin exponer la conexión a MySQL.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/login"})
public class AuthServlet extends BaseApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readBody(req);
        if (body == null || body.trim().isEmpty()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de solicitud requerido");
            return;
        }

        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "JSON mal formado");
            return;
        }

        String identificador = json.has("identificador") ? json.get("identificador").getAsString().trim() : 
                               (json.has("email") ? json.get("email").getAsString().trim() : "");
        String password = json.has("password") ? json.get("password").getAsString().trim() : "";

        if (identificador.isEmpty() || password.isEmpty()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador (correo o documento) y contraseña requeridos");
            return;
        }

        String sql = "SELECT id, documento, nombre_completo, email, password, rol, estado " +
                     "FROM usuarios WHERE (email = ? OR documento = ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identificador);
            ps.setString(2, identificador);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String dbPass = rs.getString("password");
                    String estado = rs.getString("estado");

                    if (!"ACTIVO".equalsIgnoreCase(estado)) {
                        writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Usuario inactivo en el sistema");
                        return;
                    }

                    // Validación de contraseña
                    if (dbPass.equals(password)) {
                        Map<String, Object> usuario = new HashMap<>();
                        usuario.put("id", rs.getInt("id"));
                        usuario.put("documento", rs.getString("documento"));
                        usuario.put("nombreCompleto", rs.getString("nombre_completo"));
                        usuario.put("email", rs.getString("email"));
                        usuario.put("rol", rs.getString("rol"));

                        Map<String, Object> responseData = new HashMap<>();
                        responseData.put("success", true);
                        responseData.put("mensaje", "Autenticación exitosa");
                        responseData.put("usuario", usuario);

                        writeJson(resp, HttpServletResponse.SC_OK, responseData);
                        return;
                    }
                }
            }

            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Credenciales inválidas");

        } catch (SQLException e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error de base de datos: " + e.getMessage());
        }
    }
}
