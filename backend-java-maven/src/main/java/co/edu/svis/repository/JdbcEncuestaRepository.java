package co.edu.svis.repository;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.model.Encuesta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC para {@link EncuestaRepository}.
 */
public class JdbcEncuestaRepository implements EncuestaRepository {

    @Override
    public List<Encuesta> findAll() {
        String sql = "SELECT e.id, e.titulo, e.descripcion, e.estado, e.fecha_creacion, e.fecha_cierre, " +
                     "(SELECT COUNT(*) FROM votos v WHERE v.encuesta_id = e.id) AS total_votos, " +
                     "(SELECT COUNT(*) FROM tokens_otp t WHERE t.encuesta_id = e.id) AS total_tokens " +
                     "FROM encuestas e ORDER BY e.id DESC";

        List<Encuesta> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Encuesta e = new Encuesta();
                e.setId(rs.getInt("id"));
                e.setTitulo(rs.getString("titulo"));
                e.setDescripcion(rs.getString("descripcion"));
                e.setEstado(rs.getString("estado"));
                e.setFechaCreacion(rs.getString("fecha_creacion"));
                e.setFechaCierre(rs.getString("fecha_cierre"));
                e.setTotalVotos(rs.getInt("total_votos"));
                e.setTotalTokens(rs.getInt("total_tokens"));
                lista.add(e);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error consultando todas las encuestas: " + ex.getMessage(), ex);
        }
        return lista;
    }

    @Override
    public List<Encuesta> findActivas() {
        String sql = "SELECT id, titulo, descripcion, estado, fecha_creacion FROM encuestas WHERE estado = 'ACTIVA' ORDER BY id DESC";
        List<Encuesta> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Encuesta e = new Encuesta();
                e.setId(rs.getInt("id"));
                e.setTitulo(rs.getString("titulo"));
                e.setDescripcion(rs.getString("descripcion"));
                e.setEstado(rs.getString("estado"));
                e.setFechaCreacion(rs.getString("fecha_creacion"));
                lista.add(e);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error consultando encuestas activas: " + ex.getMessage(), ex);
        }
        return lista;
    }

    @Override
    public Optional<Encuesta> findById(int id) {
        try (Connection conn = ConexionBD.getConnection()) {
            return findById(id, conn);
        } catch (SQLException ex) {
            throw new RuntimeException("Error consultando encuesta por ID: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<Encuesta> findById(int id, Connection conn) {
        String sql = "SELECT id, titulo, descripcion, estado, fecha_creacion, fecha_cierre FROM encuestas WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Encuesta e = new Encuesta();
                    e.setId(rs.getInt("id"));
                    e.setTitulo(rs.getString("titulo"));
                    e.setDescripcion(rs.getString("descripcion"));
                    e.setEstado(rs.getString("estado"));
                    e.setFechaCreacion(rs.getString("fecha_creacion"));
                    e.setFechaCierre(rs.getString("fecha_cierre"));
                    return Optional.of(e);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error consultando encuesta por ID en conexión: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    @Override
    public int insert(Encuesta encuesta, Connection conn) {
        String sql = "INSERT INTO encuestas (titulo, descripcion, estado, fecha_creacion) VALUES (?, ?, 'ACTIVA', CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, encuesta.getTitulo());
            ps.setString(2, encuesta.getDescripcion());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    encuesta.setId(id);
                    return id;
                } else {
                    throw new SQLException("No se pudo obtener el ID de la encuesta generada");
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error insertando encuesta: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean updateEstado(int id, String nuevoEstado) {
        String sql = "UPDATE encuestas SET estado = ?, fecha_cierre = " + 
                     (nuevoEstado.equals("CERRADA") ? "CURRENT_TIMESTAMP" : "NULL") + 
                     " WHERE id = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RuntimeException("Error actualizando estado de la encuesta: " + ex.getMessage(), ex);
        }
    }
}
