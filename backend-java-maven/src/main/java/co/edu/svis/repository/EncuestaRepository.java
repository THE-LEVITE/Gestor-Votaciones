package co.edu.svis.repository;

import co.edu.svis.model.Encuesta;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Abstracción de acceso a datos para encuestas (DIP).
 */
public interface EncuestaRepository {
    List<Encuesta> findAll();
    List<Encuesta> findActivas();
    Optional<Encuesta> findById(int id);
    Optional<Encuesta> findById(int id, Connection conn);
    int insert(Encuesta encuesta, Connection conn);
    boolean updateEstado(int id, String nuevoEstado);
}
