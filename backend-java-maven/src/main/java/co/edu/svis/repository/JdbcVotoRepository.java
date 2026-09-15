package co.edu.svis.repository;

import co.edu.svis.config.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Implementación JDBC para {@link VotoRepository}.
 */
public class JdbcVotoRepository implements VotoRepository {

    @Override
    public int insertVoto(int encuestaId, int opcionId, Connection conn) {
        String sql = "INSERT INTO votos (encuesta_id, opcion_id, fecha) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, encuestaId);
            ps.setInt(2, opcionId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("No se pudo obtener el ID del voto generado");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error insertando voto anónimo: " + e.getMessage(), e);
        }
    }

    @Override
    public int countByEncuestaId(int encuestaId) {
        try (Connection conn = ConexionBD.getConnection()) {
            return countByEncuestaId(encuestaId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error contando votos por encuesta: " + e.getMessage(), e);
        }
    }

    @Override
    public int countByEncuestaId(int encuestaId, Connection conn) {
        String sql = "SELECT COUNT(*) AS total FROM votos WHERE encuesta_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error contando votos por encuesta en conexión: " + e.getMessage(), e);
        }
        return 0;
    }
}
