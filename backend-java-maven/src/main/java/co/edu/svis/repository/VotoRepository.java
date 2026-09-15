package co.edu.svis.repository;

import java.sql.Connection;

/**
 * Abstracción de acceso a datos para votos institucionales anónimos (DIP).
 * Cumple estrictamente la REGLA 4: Ningún método almacena ni relaciona usuario o token.
 */
public interface VotoRepository {
    int insertVoto(int encuestaId, int opcionId, Connection conn);
    int countByEncuestaId(int encuestaId);
    int countByEncuestaId(int encuestaId, Connection conn);
}
