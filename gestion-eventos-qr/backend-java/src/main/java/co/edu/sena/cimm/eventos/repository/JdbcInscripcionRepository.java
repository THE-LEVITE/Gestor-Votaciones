package co.edu.sena.cimm.eventos.repository;

import co.edu.sena.cimm.eventos.model.Asistente;
import co.edu.sena.cimm.eventos.model.EstadoInscripcion;
import co.edu.sena.cimm.eventos.model.Evento;
import co.edu.sena.cimm.eventos.model.Inscripcion;

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
 * Implementacion JDBC (MySQL) de {@link InscripcionRepository}.
 * Persiste el asistente embebido dentro de la misma operacion de guardado
 * (una transaccion) y reconstruye el grafo evento+asistente al leer.
 */
public class JdbcInscripcionRepository implements InscripcionRepository {

    private static final String SELECT_BASE =
            "SELECT i.id AS i_id, i.token AS i_token, i.estado AS i_estado, "
            + "i.fecha_inscripcion AS i_fi, i.fecha_validacion AS i_fv, "
            + "e.id AS e_id, e.nombre AS e_nombre, e.descripcion AS e_desc, "
            + "e.lugar AS e_lugar, e.fecha AS e_fecha, e.cupo_maximo AS e_cupo, "
            + "a.id AS a_id, a.nombre_completo AS a_nombre, a.documento AS a_doc, "
            + "a.email AS a_email, a.telefono AS a_tel "
            + "FROM inscripcion i "
            + "JOIN evento e ON e.id = i.evento_id "
            + "JOIN asistente a ON a.id = i.asistente_id ";

    @Override
    public Inscripcion save(Inscripcion inscripcion) {
        return inscripcion.getId() == null ? insertar(inscripcion) : actualizar(inscripcion);
    }

    private Inscripcion insertar(Inscripcion i) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                Asistente a = i.getAsistente();
                if (a.getId() == null) {
                    insertarAsistente(c, a);
                }
                String sql = "INSERT INTO inscripcion "
                        + "(token, evento_id, asistente_id, estado, fecha_inscripcion, fecha_validacion) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, i.getToken());
                    ps.setLong(2, i.getEvento().getId());
                    ps.setLong(3, a.getId());
                    ps.setString(4, i.getEstado().name());
                    ps.setObject(5, i.getFechaInscripcion());
                    ps.setObject(6, i.getFechaValidacion());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            i.setId(keys.getLong(1));
                        }
                    }
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error creando la inscripcion", ex);
        }
        return i;
    }

    private void insertarAsistente(Connection c, Asistente a) throws SQLException {
        String sql = "INSERT INTO asistente (nombre_completo, documento, email, telefono) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getNombreCompleto());
            ps.setString(2, a.getDocumento());
            ps.setString(3, a.getEmail());
            ps.setString(4, a.getTelefono());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getLong(1));
                }
            }
        }
    }

    private Inscripcion actualizar(Inscripcion i) {
        String sql = "UPDATE inscripcion SET estado = ?, fecha_validacion = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, i.getEstado().name());
            ps.setObject(2, i.getFechaValidacion());
            ps.setLong(3, i.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Error actualizando la inscripcion", ex);
        }
        return i;
    }

    @Override
    public Optional<Inscripcion> findByToken(String token) {
        String sql = SELECT_BASE + "WHERE i.token = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error consultando la inscripcion", ex);
        }
        return Optional.empty();
    }

    @Override
    public long countByEventoId(Long eventoId) {
        String sql = "SELECT COUNT(*) FROM inscripcion WHERE evento_id = ? AND estado <> 'CANCELADA'";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, eventoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error contando inscripciones", ex);
        }
        return 0;
    }

    @Override
    public List<Inscripcion> findByEventoId(Long eventoId) {
        String sql = SELECT_BASE + "WHERE i.evento_id = ?";
        List<Inscripcion> lista = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, eventoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error consultando inscripciones", ex);
        }
        return lista;
    }

    private Inscripcion map(ResultSet rs) throws SQLException {
        Evento evento = new Evento(
                rs.getLong("e_id"),
                rs.getString("e_nombre"),
                rs.getString("e_desc"),
                rs.getString("e_lugar"),
                rs.getObject("e_fecha", LocalDateTime.class),
                rs.getInt("e_cupo"));

        Asistente asistente = new Asistente(
                rs.getLong("a_id"),
                rs.getString("a_nombre"),
                rs.getString("a_doc"),
                rs.getString("a_email"),
                rs.getString("a_tel"));

        return new Inscripcion(
                rs.getLong("i_id"),
                rs.getString("i_token"),
                evento,
                asistente,
                EstadoInscripcion.valueOf(rs.getString("i_estado")),
                rs.getObject("i_fi", LocalDateTime.class),
                rs.getObject("i_fv", LocalDateTime.class));
    }
}
