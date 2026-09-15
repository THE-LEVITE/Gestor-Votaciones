package co.edu.svis.service;

import co.edu.svis.dto.LoginRequest;
import co.edu.svis.dto.LoginResponse;
import co.edu.svis.dto.NegocioException;
import co.edu.svis.dto.NoAutorizadoException;
import co.edu.svis.dto.ProhibidoException;
import co.edu.svis.model.Usuario;
import co.edu.svis.repository.UsuarioRepository;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de lógica de negocio para autenticación de usuarios institucionales.
 */
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponse login(LoginRequest req) {
        if (req == null) {
            throw new NegocioException("Cuerpo de solicitud requerido");
        }

        String identificador = req.getIdentificadorEfectivo();
        String password = req.password != null ? req.password.trim() : "";

        if (identificador.isEmpty() || password.isEmpty()) {
            throw new NegocioException("Identificador (correo o documento) y contraseña requeridos");
        }

        Usuario usuario = usuarioRepository.findByIdentificador(identificador)
                .orElseThrow(() -> new NoAutorizadoException("Credenciales inválidas"));

        if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado())) {
            throw new ProhibidoException("Usuario inactivo en el sistema");
        }

        if (!usuario.getPassword().equals(password)) {
            throw new NoAutorizadoException("Credenciales inválidas");
        }

        Map<String, Object> usuarioMap = new HashMap<>();
        usuarioMap.put("id", usuario.getId());
        usuarioMap.put("documento", usuario.getDocumento());
        usuarioMap.put("nombreCompleto", usuario.getNombreCompleto());
        usuarioMap.put("email", usuario.getEmail());
        usuarioMap.put("rol", usuario.getRol());

        return new LoginResponse(true, "Autenticación exitosa", usuarioMap);
    }
}
