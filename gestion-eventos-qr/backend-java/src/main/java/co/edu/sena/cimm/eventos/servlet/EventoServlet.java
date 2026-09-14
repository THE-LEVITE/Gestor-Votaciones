package co.edu.sena.cimm.eventos.servlet;

import co.edu.sena.cimm.eventos.config.AppContext;
import co.edu.sena.cimm.eventos.dto.ApiError;
import co.edu.sena.cimm.eventos.dto.EventoRequest;
import co.edu.sena.cimm.eventos.dto.NegocioException;
import co.edu.sena.cimm.eventos.service.EventoService;
import co.edu.sena.cimm.eventos.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET  /api/eventos       -> lista de eventos (con cupo disponible)
 * GET  /api/eventos/{id}  -> un evento
 * POST /api/eventos       -> crea un evento (administracion)
 */
@WebServlet("/api/eventos/*")
public class EventoServlet extends BaseApiServlet {

    private final EventoService service = AppContext.get().getEventoService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                writeJson(resp, 200, service.listar());
            } else {
                Long id = Long.parseLong(path.substring(1));
                writeJson(resp, 200, service.obtener(id));
            }
        } catch (NumberFormatException ex) {
            writeJson(resp, 400, new ApiError("BAD_REQUEST", "El id del evento no es valido"));
        } catch (NegocioException ex) {
            writeJson(resp, 404, new ApiError("NOT_FOUND", ex.getMessage()));
        } catch (Exception ex) {
            writeJson(resp, 500, new ApiError("ERROR", ex.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            EventoRequest r = JsonUtil.fromJson(readBody(req), EventoRequest.class);
            writeJson(resp, 201, service.crear(r));
        } catch (NegocioException ex) {
            writeJson(resp, 400, new ApiError("BAD_REQUEST", ex.getMessage()));
        } catch (Exception ex) {
            writeJson(resp, 500, new ApiError("ERROR", ex.getMessage()));
        }
    }
}
