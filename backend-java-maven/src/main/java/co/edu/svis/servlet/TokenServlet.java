package co.edu.svis.servlet;

import co.edu.svis.config.AppContext;
import co.edu.svis.dto.TokenGenerarRequest;
import co.edu.svis.dto.TokenGenerarResponse;
import co.edu.svis.dto.TokenUsuarioResponse;
import co.edu.svis.service.TokenService;
import co.edu.svis.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la generación masiva y consulta de Tokens OTP.
 * Cumple el principio de Responsabilidad Única (SRP), delegando a {@link TokenService}.
 */
@WebServlet(name = "TokenServlet", urlPatterns = {"/api/tokens/*"})
public class TokenServlet extends BaseApiServlet {

    private final TokenService tokenService = AppContext.get().getTokenService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo != null && pathInfo.equals("/usuario")) {
                consultarTokenUsuario(req, resp);
            } else if (pathInfo != null && pathInfo.equals("/padron")) {
                consultarPadronElectoral(req, resp);
            } else {
                writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Ruta de tokens no encontrada");
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/generar")) {
            generarPadronTokens(req, resp);
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no válido para POST");
        }
    }

    private void generarPadronTokens(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String body = readBody(req);
            if (body == null || body.trim().isEmpty()) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON requerido");
                return;
            }

            TokenGenerarRequest requestData = JsonUtil.fromJson(body, TokenGenerarRequest.class);
            TokenGenerarResponse responseData = tokenService.generarTokens(requestData);
            writeJson(resp, HttpServletResponse.SC_OK, responseData);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private void consultarTokenUsuario(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String usuarioIdStr = req.getParameter("usuarioId");
        String encuestaIdStr = req.getParameter("encuestaId");

        if (usuarioIdStr == null || encuestaIdStr == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Parámetros 'usuarioId' y 'encuestaId' requeridos");
            return;
        }

        try {
            int usuarioId = Integer.parseInt(usuarioIdStr);
            int encuestaId = Integer.parseInt(encuestaIdStr);
            TokenUsuarioResponse responseData = tokenService.consultarTokenUsuario(usuarioId, encuestaId);
            writeJson(resp, HttpServletResponse.SC_OK, responseData);
        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Formato inválido en los IDs proporcionados");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private void consultarPadronElectoral(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String encuestaIdStr = req.getParameter("encuestaId");
        if (encuestaIdStr == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Parámetro 'encuestaId' requerido");
            return;
        }

        try {
            int encuestaId = Integer.parseInt(encuestaIdStr);
            List<Map<String, Object>> padron = tokenService.consultarPadron(encuestaId);
            writeJson(resp, HttpServletResponse.SC_OK, padron);
        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Formato inválido en 'encuestaId'");
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
