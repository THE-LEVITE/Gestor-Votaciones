package co.edu.sena.cimm.eventos.servlet;

import co.edu.sena.cimm.eventos.config.AppContext;
import co.edu.sena.cimm.eventos.dto.ApiError;
import co.edu.sena.cimm.eventos.dto.InscripcionRequest;
import co.edu.sena.cimm.eventos.dto.NegocioException;
import co.edu.sena.cimm.eventos.service.InscripcionService;
import co.edu.sena.cimm.eventos.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * POST /api/inscripciones          -> crea inscripcion y devuelve el token del QR
 * GET  /api/inscripciones/{token}  -> consulta el estado de una inscripcion
 */
@WebServlet("/api/inscripciones/*")
public class InscripcionServlet extends BaseApiServlet {

    private final InscripcionService service = AppContext.get().getInscripcionService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            InscripcionRequest r = JsonUtil.fromJson(readBody(req), InscripcionRequest.class);
            writeJson(resp, 201, service.inscribir(r));
        } catch (NegocioException ex) {
            writeJson(resp, 400, new ApiError("BAD_REQUEST", ex.getMessage()));
        } catch (Exception ex) {
            writeJson(resp, 500, new ApiError("ERROR", ex.getMessage()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null || path.length() < 2) {
                writeJson(resp, 400, new ApiError("BAD_REQUEST", "Debe indicar el token"));
                return;
            }
            String token = path.substring(1);
            writeJson(resp, 200, service.buscarPorToken(token));
        } catch (NegocioException ex) {
            writeJson(resp, 404, new ApiError("NOT_FOUND", ex.getMessage()));
        } catch (Exception ex) {
            writeJson(resp, 500, new ApiError("ERROR", ex.getMessage()));
        }
    }
}
