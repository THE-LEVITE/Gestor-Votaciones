package co.edu.svis.servlet;

import co.edu.svis.config.AppContext;
import co.edu.svis.dto.EncuestaRequest;
import co.edu.svis.dto.EncuestaResultadosResponse;
import co.edu.svis.model.Encuesta;
import co.edu.svis.service.EncuestaService;
import co.edu.svis.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la administración y consulta de encuestas institucionales y sus resultados.
 * Cumple el principio de Responsabilidad Única (SRP), delegando a {@link EncuestaService}.
 */
@WebServlet(name = "EncuestaServlet", urlPatterns = {"/api/encuestas/*"})
public class EncuestaServlet extends BaseApiServlet {

    private final EncuestaService encuestaService = AppContext.get().getEncuestaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        try {
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                List<Encuesta> lista = encuestaService.listarTodas();
                writeJson(resp, HttpServletResponse.SC_OK, lista);
            } else if (pathInfo.equals("/activas")) {
                List<Encuesta> activas = encuestaService.listarActivasConOpciones();
                writeJson(resp, HttpServletResponse.SC_OK, activas);
            } else if (pathInfo.matches("/\\d+/resultados")) {
                String[] parts = pathInfo.split("/");
                int encuestaId = Integer.parseInt(parts[1]);
                EncuestaResultadosResponse resultados = encuestaService.obtenerResultados(encuestaId);
                writeJson(resp, HttpServletResponse.SC_OK, resultados);
            } else if (pathInfo.matches("/\\d+")) {
                int encuestaId = Integer.parseInt(pathInfo.substring(1));
                Encuesta detalle = encuestaService.obtenerPorId(encuestaId);
                writeJson(resp, HttpServletResponse.SC_OK, detalle);
            } else {
                writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Recurso de encuesta no encontrado");
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && !pathInfo.equals("/") && !pathInfo.isEmpty()) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no válido para POST");
            return;
        }

        try {
            String body = readBody(req);
            if (body == null || body.trim().isEmpty()) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON requerido");
                return;
            }

            EncuestaRequest requestData = JsonUtil.fromJson(body, EncuestaRequest.class);
            int encuestaId = encuestaService.crearEncuesta(requestData);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("mensaje", "Encuesta y opciones creadas exitosamente");
            responseData.put("encuestaId", encuestaId);

            writeJson(resp, HttpServletResponse.SC_CREATED, responseData);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/\\d+/estado")) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Endpoint no válido para PUT. Use /api/encuestas/{id}/estado");
            return;
        }

        try {
            int encuestaId = Integer.parseInt(pathInfo.split("/")[1]);
            String body = readBody(req);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String nuevoEstado = json.has("estado") ? json.get("estado").getAsString().toUpperCase().trim() : "";

            encuestaService.cambiarEstado(encuestaId, nuevoEstado);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("mensaje", "Estado de encuesta actualizado a " + nuevoEstado);
            writeJson(resp, HttpServletResponse.SC_OK, responseData);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
