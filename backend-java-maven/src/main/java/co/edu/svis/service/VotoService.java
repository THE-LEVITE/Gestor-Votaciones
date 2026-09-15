package co.edu.svis.service;

import co.edu.svis.config.ConexionBD;
import co.edu.svis.dto.ConflictoException;
import co.edu.svis.dto.ExpiradoException;
import co.edu.svis.dto.NegocioException;
import co.edu.svis.dto.RecursoNoEncontradoException;
import co.edu.svis.dto.VotoEmitirRequest;
import co.edu.svis.dto.VotoEmitirResponse;
import co.edu.svis.model.Encuesta;
import co.edu.svis.model.TokenOtp;
import co.edu.svis.repository.EncuestaRepository;
import co.edu.svis.repository.OpcionRepository;
import co.edu.svis.repository.TokenRepository;
import co.edu.svis.repository.VotoRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Servicio central para el procesamiento transaccional de votos institucionales.
 * Implementa las 4 Reglas Críticas de Integridad del Sistema SVIS:
 *
 * - REGLA 1: Validación de correspondencia del token OTP con la encuesta.
 * - REGLA 2: Quema Atómica del OTP (DISPONIBLE -> USADO) en la misma transacción JDBC.
 * - REGLA 3: Aislamiento contra Race Conditions mediante SELECT ... FOR UPDATE (bloqueo pesimista).
 * - REGLA 4: Secreto Absoluto del Voto (desacoplamiento total de la identidad del votante).
 */
public class VotoService {

    private final TokenRepository tokenRepository;
    private final EncuestaRepository encuestaRepository;
    private final OpcionRepository opcionRepository;
    private final VotoRepository votoRepository;

    public VotoService(TokenRepository tokenRepository,
                       EncuestaRepository encuestaRepository,
                       OpcionRepository opcionRepository,
                       VotoRepository votoRepository) {
        this.tokenRepository = tokenRepository;
        this.encuestaRepository = encuestaRepository;
        this.opcionRepository = opcionRepository;
        this.votoRepository = votoRepository;
    }

    public VotoEmitirResponse emitirVoto(VotoEmitirRequest req) {
        if (req == null) {
            throw new NegocioException("Cuerpo JSON de voto requerido");
        }
        if (req.encuestaId == null || req.opcionId == null || req.token == null) {
            throw new NegocioException("Campos 'encuestaId', 'opcionId' y 'token' son obligatorios");
        }

        String tokenOtp = req.token.trim();
        if (tokenOtp.isEmpty()) {
            throw new NegocioException("El token OTP no puede estar vacío");
        }

        int encuestaId = req.encuestaId;
        int opcionId = req.opcionId;

        Connection conn = null;
        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            // PASO 1: REGLA 3 - Bloqueo Pesimista de Fila (SELECT ... FOR UPDATE)
            Optional<TokenOtp> tokenOpt = tokenRepository.findByTokenForUpdate(tokenOtp, conn);
            if (!tokenOpt.isPresent()) {
                conn.rollback();
                throw new RecursoNoEncontradoException("El código de votación no existe o no es válido");
            }

            TokenOtp token = tokenOpt.get();

            // PASO 2: Evaluación del Estado bajo Bloqueo Exclusivo (Anti-Replay / Race Condition)
            if (!"DISPONIBLE".equalsIgnoreCase(token.getEstado())) {
                conn.rollback();
                throw new ConflictoException("Ya has participado en esta votación. Cada persona solo puede votar una vez.");
            }

            // PASO 3: Validación de Expiración (TTL)
            Timestamp fechaExpiracion = token.getFechaExpiracion();
            if (fechaExpiracion != null && fechaExpiracion.before(new java.util.Date())) {
                tokenRepository.marcarExpirado(token.getId(), conn);
                conn.commit();
                throw new ExpiradoException("El tiempo para votar con este código ha vencido.");
            }

            // PASO 4: Validación de correspondencia de encuesta
            if (token.getEncuestaId() != encuestaId) {
                conn.rollback();
                throw new NegocioException("Este código de votación no corresponde a esta encuesta.");
            }

            // PASO 5: Validación de Estado ACTIVA de la encuesta
            Optional<Encuesta> encOpt = encuestaRepository.findById(encuestaId, conn);
            if (!encOpt.isPresent() || !"ACTIVA".equalsIgnoreCase(encOpt.get().getEstado())) {
                conn.rollback();
                throw new NegocioException("Esta votación ha finalizado o no está activa en este momento.");
            }

            // PASO 6: Validación de Existencia de la Opción en la Encuesta
            if (!opcionRepository.existsByIdAndEncuestaId(opcionId, encuestaId, conn)) {
                conn.rollback();
                throw new NegocioException("La opción seleccionada no pertenece a esta encuesta.");
            }

            // PASO 7: REGLA 2 - Quema Atómica del Token OTP (DISPONIBLE -> USADO)
            int filasAfectadas = tokenRepository.quemarToken(token.getId(), conn);
            if (filasAfectadas == 0) {
                conn.rollback();
                throw new ConflictoException("No fue posible quemar el token OTP");
            }

            // PASO 8: REGLA 4 - Registro del Voto Anónimo (sin usuario ni token)
            int votoIdGenerado = votoRepository.insertVoto(encuestaId, opcionId, conn);

            // PASO 9: REGLA 4 - Incremento Numérico Atómico de la Opción (+1)
            opcionRepository.incrementarVoto(opcionId, conn);

            // PASO 10: Confirmación Transaccional ACID (libera cerraduras pesimistas)
            conn.commit();

            // PASO 11: Generación de Comprobante Digital Anónimo (Hash SHA-256)
            String timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String rawReceipt = "SVIS-CERT-" + votoIdGenerado + "-" + encuestaId + "-" + System.nanoTime();
            String reciboHash = calcularSha256(rawReceipt);

            return new VotoEmitirResponse(
                    true,
                    "¡Tu voto ha sido registrado y contabilizado con éxito!",
                    reciboHash,
                    timestampStr,
                    encuestaId
            );

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            if (e.getErrorCode() == 1205 || e.getErrorCode() == 1213) {
                throw new ConflictoException("Conflicto por colisión de transacciones en bloqueo pesimista");
            }
            throw new RuntimeException("Error transaccional al emitir voto: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignore) {}
            }
        }
    }

    private String calcularSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "RECIBO-" + Long.toHexString(System.currentTimeMillis());
        }
    }
}
