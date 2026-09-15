package co.edu.svis.repository;

import co.edu.svis.model.Usuario;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Abstracción de acceso a datos para usuarios (DIP).
 */
public interface UsuarioRepository {
    Optional<Usuario> findByIdentificador(String identificador);
    List<Integer> findIdsVotantesActivosSinToken(int encuestaId, Connection conn);
}
