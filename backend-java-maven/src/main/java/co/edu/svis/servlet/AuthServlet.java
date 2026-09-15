package co.edu.svis.servlet;

import co.edu.svis.config.AppContext;
import co.edu.svis.dto.LoginRequest;
import co.edu.svis.dto.LoginResponse;
import co.edu.svis.service.AuthService;
import co.edu.svis.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controlador REST para autenticación institucional de usuarios.
 * Aplica el principio de Responsabilidad Única (SRP), delegando la lógica a {@link AuthService}.
 */
@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/login"})
public class AuthServlet extends BaseApiServlet {

    private final AuthService authService = AppContext.get().getAuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = readBody(req);
            if (body == null || body.trim().isEmpty()) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo de solicitud requerido");
                return;
            }

            LoginRequest loginReq = JsonUtil.fromJson(body, LoginRequest.class);
            LoginResponse responseData = authService.login(loginReq);
            writeJson(resp, HttpServletResponse.SC_OK, responseData);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
