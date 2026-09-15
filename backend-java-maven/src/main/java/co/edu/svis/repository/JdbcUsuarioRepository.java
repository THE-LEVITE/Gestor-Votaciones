package co.edu.svis.repository;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC para {@link UsuarioRepository}.
 */
public class JdbcUsuarioRepository implements UsuarioRepository {

    @Override
    public Optional<Usuario> findByIdentificador(String identificador) {
        String sql = "SELECT id, documento, nombre_completo, email, password, rol, estado " +
                     "FROM usuarios WHERE (email = ? OR documento = ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, identificador);
            ps.setString(2, identificador);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setDocumento(rs.getString("documento"));
                    u.setNombreCompleto(rs.getString("nombre_completo"));
                    u.setEmail(rs.getString("email"));
                    u.setPassword(rs.getString("password"));
                    u.setRol(rs.getString("rol"));
                    u.setEstado(rs.getString("estado"));
                    return Optional.of(u);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando usuario por identificador: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Integer> findIdsVotantesActivosSinToken(int encuestaId, Connection conn) {
        String sql = "SELECT u.id FROM usuarios u " +
                     "WHERE u.rol = 'VOTANTE' AND u.estado = 'ACTIVO' " +
                     "AND u.id NOT IN (SELECT t.usuario_id FROM tokens_otp t WHERE t.encuesta_id = ?)";

        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando votantes activos sin token: " + e.getMessage(), e);
        }
        return ids;
    }
}
