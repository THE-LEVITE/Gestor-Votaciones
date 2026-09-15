package co.edu.svis.service;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.dto.NegocioException;
import co.edu.svis.dto.RecursoNoEncontradoException;
import co.edu.svis.dto.TokenGenerarRequest;
import co.edu.svis.dto.TokenGenerarResponse;
import co.edu.svis.dto.TokenUsuarioResponse;
import co.edu.svis.model.TokenOtp;
import co.edu.svis.model.Usuario;
import co.edu.svis.repository.TokenRepository;
import co.edu.svis.repository.UsuarioRepository;
import co.edu.svis.util.TokenGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de lógica de negocio para la emisión masiva, consulta y control de tokens OTP.
 */
public class TokenService {

    private final TokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;

    public TokenService(TokenRepository tokenRepository, UsuarioRepository usuarioRepository) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public TokenGenerarResponse generarTokens(TokenGenerarRequest req) {
        if (req == null || req.encuestaId == null) {
            throw new NegocioException("Parámetro 'encuestaId' obligatorio");
        }

        int ttlMinutos = (req.ttlMinutos != null && req.ttlMinutos > 0) ? req.ttlMinutos : 1440;
        if (ttlMinutos <= 0) {
            throw new NegocioException("El TTL en minutos debe ser mayor a 0");
        }

        int encuestaId = req.encuestaId;
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime expiracion = ahora.plusMinutes(ttlMinutos);
        Timestamp timestampExpiracion = Timestamp.valueOf(expiracion);

        Connection conn = null;
        int tokensGenerados = 0;

        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            List<Integer> usuariosElegibles = usuarioRepository.findIdsVotantesActivosSinToken(encuestaId, conn);

            if (!usuariosElegibles.isEmpty()) {
                List<TokenOtp> nuevosTokens = new ArrayList<>();
                for (int usuarioId : usuariosElegibles) {
                    String tokenOtp = TokenGenerator.generarToken();
                    TokenOtp t = new TokenOtp();
                    t.setEncuestaId(encuestaId);
                    t.setUsuarioId(usuarioId);
                    t.setToken(tokenOtp);
                    t.setFechaExpiracion(timestampExpiracion);
                    nuevosTokens.add(t);
                    tokensGenerados++;
                }
                tokenRepository.insertBatch(nuevosTokens, conn);
            }

            conn.commit();

            return new TokenGenerarResponse(
                    true,
                    "Padrón electoral y tokens generados con éxito",
                    encuestaId,
                    tokensGenerados,
                    ttlMinutos,
                    expiracion.toString()
            );

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            throw new RuntimeException("Error al generar tokens OTP: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignore) {}
            }
        }
    }

    public TokenUsuarioResponse consultarTokenUsuario(int usuarioId, int encuestaId) {
        TokenOtp token = tokenRepository.findByUsuarioAndEncuesta(usuarioId, encuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un token asignado para este usuario en esta encuesta"));

        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("id", token.getId());
        tokenInfo.put("token", token.getToken());
        tokenInfo.put("estado", token.getEstado());
        tokenInfo.put("fechaExpiracion", token.getFechaExpiracion() != null ? token.getFechaExpiracion().toString() : null);
        tokenInfo.put("fechaUso", token.getFechaUso() != null ? token.getFechaUso().toString() : null);

        return new TokenUsuarioResponse(true, tokenInfo);
    }

    public List<Map<String, Object>> consultarPadron(int encuestaId) {
        List<TokenOtp> padron = tokenRepository.findPadronByEncuesta(encuestaId);
        List<Map<String, Object>> lista = new ArrayList<>();

        for (TokenOtp item : padron) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("token", item.getToken());
            m.put("estado", item.getEstado());
            m.put("fechaExpiracion", item.getFechaExpiracion() != null ? item.getFechaExpiracion().toString() : null);
            m.put("fechaUso", item.getFechaUso() != null ? item.getFechaUso().toString() : null);

            Usuario u = item.getUsuario();
            if (u != null) {
                Map<String, Object> uMap = new HashMap<>();
                uMap.put("id", u.getId());
                uMap.put("documento", u.getDocumento());
                uMap.put("nombreCompleto", u.getNombreCompleto());
                uMap.put("email", u.getEmail());
                m.put("usuario", uMap);
            }
            lista.add(m);
        }
        return lista;
    }
}
