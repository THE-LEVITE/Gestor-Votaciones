package co.edu.svis.config;

import co.edu.svis.repository.EncuestaRepository;
import co.edu.svis.repository.JdbcEncuestaRepository;
import co.edu.svis.repository.JdbcOpcionRepository;
import co.edu.svis.repository.JdbcTokenRepository;
import co.edu.svis.repository.JdbcUsuarioRepository;
import co.edu.svis.repository.JdbcVotoRepository;
import co.edu.svis.repository.OpcionRepository;
import co.edu.svis.repository.TokenRepository;
import co.edu.svis.repository.UsuarioRepository;
import co.edu.svis.repository.VotoRepository;
import co.edu.svis.service.AuthService;
import co.edu.svis.service.EncuestaService;
import co.edu.svis.service.TokenService;
import co.edu.svis.service.VotoService;

/**
 * Contenedor de Inversión de Control (IoC) e Inyección de Dependencias manual (DIP).
 * Instancia repositorios y servicios de forma centralizada una sola vez.
 */
public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private final AuthService authService;
    private final EncuestaService encuestaService;
    private final TokenService tokenService;
    private final VotoService votoService;

    private AppContext() {
        // Inicialización de componentes de persistencia (Repositories)
        UsuarioRepository usuarioRepo = new JdbcUsuarioRepository();
        EncuestaRepository encuestaRepo = new JdbcEncuestaRepository();
        OpcionRepository opcionRepo = new JdbcOpcionRepository();
        TokenRepository tokenRepo = new JdbcTokenRepository();
        VotoRepository votoRepo = new JdbcVotoRepository();

        // Inyección de dependencias en la capa de servicios (Services)
        this.authService = new AuthService(usuarioRepo);
        this.encuestaService = new EncuestaService(encuestaRepo, opcionRepo, votoRepo);
        this.tokenService = new TokenService(tokenRepo, usuarioRepo);
        this.votoService = new VotoService(tokenRepo, encuestaRepo, opcionRepo, votoRepo);

        System.out.println("[SVIS] AppContext inicializado con arquitectura por capas desacoplada.");
    }

    public static AppContext get() {
        return INSTANCE;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public EncuestaService getEncuestaService() {
        return encuestaService;
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public VotoService getVotoService() {
        return votoService;
    }
}
