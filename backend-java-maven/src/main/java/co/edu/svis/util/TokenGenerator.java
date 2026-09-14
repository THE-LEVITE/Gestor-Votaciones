package co.edu.svis.util;

import java.security.SecureRandom;

/**
 * Generador criptográficamente seguro de Tokens OTP (One-Time Password).
 * Utiliza exclusivamente java.security.SecureRandom sin dependencias externas.
 */
public final class TokenGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Sin caracteres ambiguos (O, 0, 1, I)
    private static final int DEFAULT_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    /**
     * Genera un token aleatorio con prefijo institucional y alta entropía.
     * Ejemplo: SVIS-7K8M-9P2X-W4YT
     *
     * @return Cadena con el token OTP seguro.
     */
    public static String generarToken() {
        return generarToken("SVIS", DEFAULT_LENGTH);
    }

    /**
     * Genera un token aleatorio con prefijo personalizado y longitud especificada.
     *
     * @param prefijo  Prefijo identificador (ej. "SVIS")
     * @param longitud Longitud de caracteres aleatorios
     * @return Token OTP formateado
     */
    public static String generarToken(String prefijo, int longitud) {
        StringBuilder sb = new StringBuilder(prefijo != null && !prefijo.isEmpty() ? prefijo + "-" : "");
        for (int i = 0; i < longitud; i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            int randomIndex = SECURE_RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}
