package co.edu.svis.repository;

import co.edu.svis.model.TokenOtp;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Abstracción de acceso a datos para tokens OTP (DIP).
 */
public interface TokenRepository {
    void insertBatch(List<TokenOtp> tokens, Connection conn);
    Optional<TokenOtp> findByUsuarioAndEncuesta(int usuarioId, int encuestaId);
    List<TokenOtp> findPadronByEncuesta(int encuestaId);
    Optional<TokenOtp> findByTokenForUpdate(String token, Connection conn);
    int quemarToken(int tokenId, Connection conn);
    void marcarExpirado(int tokenId, Connection conn);
}
