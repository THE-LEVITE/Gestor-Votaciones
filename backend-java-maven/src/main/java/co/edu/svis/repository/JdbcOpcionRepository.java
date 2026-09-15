package co.edu.svis.repository;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.model.Opcion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC para {@link OpcionRepository}.
 */
public class JdbcOpcionRepository implements OpcionRepository {

    @Override
    public List<Opcion> findByEncuestaId(int encuestaId) {
        try (Connection conn = ConexionBD.getConnection()) {
            return findByEncuestaId(encuestaId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando opciones por encuesta: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Opcion> findByEncuestaId(int encuestaId, Connection conn) {
        String sql = "SELECT id, encuesta_id, nombre, descripcion, votos_conteo " +
                     "FROM opciones WHERE encuesta_id = ? ORDER BY votos_conteo DESC, id ASC";

        List<Opcion> opciones = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Opcion o = new Opcion();
                    o.setId(rs.getInt("id"));
                    o.setEncuestaId(rs.getInt("encuesta_id"));
                    o.setNombre(rs.getString("nombre"));
                    o.setDescripcion(rs.getString("descripcion"));
                    o.setVotosConteo(rs.getInt("votos_conteo"));
                    opciones.add(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando opciones de encuesta: " + e.getMessage(), e);
        }
        return opciones;
    }

    @Override
    public void insertBatch(int encuestaId, List<Opcion> opciones, Connection conn) {
        String sql = "INSERT INTO opciones (encuesta_id, nombre, descripcion, votos_conteo) VALUES (?, ?, ?, 0)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Opcion o : opciones) {
                if (o.getNombre() != null && !o.getNombre().trim().isEmpty()) {
                    ps.setInt(1, encuestaId);
                    ps.setString(2, o.getNombre().trim());
                    ps.setString(3, o.getDescripcion() != null ? o.getDescripcion().trim() : "");
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar opciones en lote: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByIdAndEncuestaId(int opcionId, int encuestaId, Connection conn) {
        String sql = "SELECT id FROM opciones WHERE id = ? AND encuesta_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, opcionId);
            ps.setInt(2, encuestaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error validando existencia de opción en encuesta: " + e.getMessage(), e);
        }
    }

    @Override
    public void incrementarVoto(int opcionId, Connection conn) {
        String sql = "UPDATE opciones SET votos_conteo = votos_conteo + 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, opcionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error incrementando conteo de opción: " + e.getMessage(), e);
        }
    }
}
