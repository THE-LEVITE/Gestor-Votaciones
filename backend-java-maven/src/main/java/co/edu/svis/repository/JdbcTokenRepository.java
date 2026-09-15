package co.edu.svis.repository;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.model.TokenOtp;
import co.edu.svis.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC para {@link TokenRepository}.
 */
public class JdbcTokenRepository implements TokenRepository {

    @Override
    public void insertBatch(List<TokenOtp> tokens, Connection conn) {
        String sql = "INSERT INTO tokens_otp (encuesta_id, usuario_id, token, estado, fecha_expiracion, fecha_creacion) " +
                     "VALUES (?, ?, ?, 'DISPONIBLE', ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TokenOtp t : tokens) {
                ps.setInt(1, t.getEncuestaId());
                ps.setInt(2, t.getUsuarioId());
                ps.setString(3, t.getToken());
                ps.setTimestamp(4, t.getFechaExpiracion());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar tokens en lote: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<TokenOtp> findByUsuarioAndEncuesta(int usuarioId, int encuestaId) {
        String sql = "SELECT id, token, estado, fecha_expiracion, fecha_uso " +
                     "FROM tokens_otp WHERE encuesta_id = ? AND usuario_id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, encuestaId);
            ps.setInt(2, usuarioId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TokenOtp t = new TokenOtp();
                    t.setId(rs.getInt("id"));
                    t.setEncuestaId(encuestaId);
                    t.setUsuarioId(usuarioId);
                    t.setToken(rs.getString("token"));
                    t.setEstado(rs.getString("estado"));
                    t.setFechaExpiracion(rs.getTimestamp("fecha_expiracion"));
                    t.setFechaUso(rs.getTimestamp("fecha_uso"));
                    return Optional.of(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando token del usuario: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<TokenOtp> findPadronByEncuesta(int encuestaId) {
        String sql = "SELECT t.id, t.token, t.estado, t.fecha_expiracion, t.fecha_uso, " +
                     "u.id AS usuario_id, u.documento, u.nombre_completo, u.email " +
                     "FROM tokens_otp t " +
                     "INNER JOIN usuarios u ON t.usuario_id = u.id " +
                     "WHERE t.encuesta_id = ? " +
                     "ORDER BY u.nombre_completo ASC";

        List<TokenOtp> padron = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, encuestaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TokenOtp t = new TokenOtp();
                    t.setId(rs.getInt("id"));
                    t.setEncuestaId(encuestaId);
                    t.setToken(rs.getString("token"));
                    t.setEstado(rs.getString("estado"));
                    t.setFechaExpiracion(rs.getTimestamp("fecha_expiracion"));
                    t.setFechaUso(rs.getTimestamp("fecha_uso"));

                    Usuario u = new Usuario();
                    u.setId(rs.getInt("usuario_id"));
                    u.setDocumento(rs.getString("documento"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setEmail(rs.getString("email"));
                    t.setUsuario(u);

                    padron.add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando padrón electoral: " + e.getMessage(), e);
        }
        return padron;
    }

    @Override
    public Optional<TokenOtp> findByTokenForUpdate(String token, Connection conn) {
        String sql = "SELECT id, encuesta_id, estado, fecha_expiracion " +
                     "FROM tokens_otp WHERE token = ? FOR UPDATE";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TokenOtp t = new TokenOtp();
                    t.setId(rs.getInt("id"));
                    t.setEncuestaId(rs.getInt("encuesta_id"));
                    t.setToken(token);
                    t.setEstado(rs.getString("estado"));
                    t.setFechaExpiracion(rs.getTimestamp("fecha_expiracion"));
                    return Optional.of(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al bloquear token con FOR UPDATE: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public int quemarToken(int tokenId, Connection conn) {
        String sql = "UPDATE tokens_otp SET estado = 'USADO', fecha_uso = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al quemar token OTP: " + e.getMessage(), e);
        }
    }

    @Override
    public void marcarExpirado(int tokenId, Connection conn) {
        String sql = "UPDATE tokens_otp SET estado = 'EXPIRADO' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al marcar token expirado: " + e.getMessage(), e);
        }
    }
}
