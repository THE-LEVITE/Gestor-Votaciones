package co.edu.svis.servlet;

import co.edu.svis.config.AppContext;
import co.edu.svis.dto.VotoEmitirRequest;
import co.edu.svis.dto.VotoEmitirResponse;
import co.edu.svis.service.VotoService;
import co.edu.svis.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controlador REST para el procesamiento de votos institucionales.
 * Aplica el principio de Responsabilidad Única (SRP), delegando la orquestación
 * transaccional y reglas de negocio críticas a {@link VotoService}.
 */
@WebServlet(name = "VotoServlet", urlPatterns = {"/api/votos/*"})
public class VotoServlet extends BaseApiServlet {

    private final VotoService votoService = AppContext.get().getVotoService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/emitir") && !pathInfo.equals("/")) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no encontrado. Use POST /api/votos/emitir");
            return;
        }

        try {
            String body = readBody(req);
            if (body == null || body.trim().isEmpty()) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON de voto requerido");
                return;
            }

            VotoEmitirRequest votoReq = JsonUtil.fromJson(body, VotoEmitirRequest.class);
            VotoEmitirResponse responseData = votoService.emitirVoto(votoReq);
            writeJson(resp, HttpServletResponse.SC_OK, responseData);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
