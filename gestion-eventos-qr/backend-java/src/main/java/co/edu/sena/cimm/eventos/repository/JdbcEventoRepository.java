package co.edu.sena.cimm.eventos.repository;

import co.edu.sena.cimm.eventos.model.Evento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion JDBC (MySQL) de {@link EventoRepository}.
 * Mismos metodos que la version en memoria: los servicios no cambian.
 */
public class JdbcEventoRepository implements EventoRepository {

    private static final String COLUMNAS =
            "id, nombre, descripcion, lugar, fecha, cupo_maximo";

    @Override
    public List<Evento> findAll() {
        String sql = "SELECT " + COLUMNAS + " FROM evento ORDER BY fecha";
        List<Evento> lista = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando eventos", e);
        }
        return lista;
    }

    @Override
    public Optional<Evento> findById(Long id) {
        String sql = "SELECT " + COLUMNAS + " FROM evento WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error consultando el evento", e);
        }
        return Optional.empty();
    }

    @Override
    public Evento save(Evento evento) {
        return evento.getId() == null ? insertar(evento) : actualizar(evento);
    }

    private Evento insertar(Evento e) {
        String sql = "INSERT INTO evento (nombre, descripcion, lugar, fecha, cupo_maximo) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getDescripcion());
            ps.setString(3, e.getLugar());
            ps.setObject(4, e.getFecha());
            ps.setInt(5, e.getCupoMaximo());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    e.setId(keys.getLong(1));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error creando el evento", ex);
        }
        return e;
    }

    private Evento actualizar(Evento e) {
        String sql = "UPDATE evento SET nombre = ?, descripcion = ?, lugar = ?, "
                + "fecha = ?, cupo_maximo = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getNombre());
            ps.setString(2, e.getDescripcion());
            ps.setString(3, e.getLugar());
            ps.setObject(4, e.getFecha());
            ps.setInt(5, e.getCupoMaximo());
            ps.setLong(6, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Error actualizando el evento", ex);
        }
        return e;
    }

    private Evento map(ResultSet rs) throws SQLException {
        return new Evento(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("lugar"),
                rs.getObject("fecha", LocalDateTime.class),
                rs.getInt("cupo_maximo"));
    }
}
