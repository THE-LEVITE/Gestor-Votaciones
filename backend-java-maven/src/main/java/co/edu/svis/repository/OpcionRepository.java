package co.edu.svis.repository;

import co.edu.svis.model.Opcion;
import java.sql.Connection;
import java.util.List;

/**
 * Abstracción de acceso a datos para opciones de encuestas (DIP).
 */
public interface OpcionRepository {
    List<Opcion> findByEncuestaId(int encuestaId);
    List<Opcion> findByEncuestaId(int encuestaId, Connection conn);
    void insertBatch(int encuestaId, List<Opcion> opciones, Connection conn);
    boolean existsByIdAndEncuestaId(int opcionId, int encuestaId, Connection conn);
    void incrementarVoto(int opcionId, Connection conn);
}
