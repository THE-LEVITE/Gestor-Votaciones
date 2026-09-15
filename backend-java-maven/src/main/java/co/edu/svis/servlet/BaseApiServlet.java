package co.edu.svis.servlet;

import co.edu.svis.dto.ConflictoException;
import co.edu.svis.dto.ExpiradoException;
import co.edu.svis.dto.NegocioException;
import co.edu.svis.dto.NoAutorizadoException;
import co.edu.svis.dto.ProhibidoException;
import co.edu.svis.dto.RecursoNoEncontradoException;
import co.edu.svis.util.JsonUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servlet base con métodos de apoyo para peticiones y respuestas REST en JSON.
 */
public abstract class BaseApiServlet extends HttpServlet {

    protected void writeJson(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(JsonUtil.toJson(body));
    }

    protected void writeError(HttpServletResponse resp, int status, String mensaje) throws IOException {
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("error", mensaje);
        errorBody.put("status", status);
        writeJson(resp, status, errorBody);
    }

    protected void handleException(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof NegocioException) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } else if (e instanceof RecursoNoEncontradoException) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } else if (e instanceof ConflictoException) {
            writeError(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } else if (e instanceof ExpiradoException) {
            writeError(resp, HttpServletResponse.SC_GONE, e.getMessage());
        } else if (e instanceof ProhibidoException) {
            writeError(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } else if (e instanceof NoAutorizadoException) {
            writeError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } else {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error interno del servidor: " + e.getMessage());
        }
    }

    protected String readBody(HttpServletRequest req) throws IOException {
        req.setCharacterEncoding("UTF-8");
        try (BufferedReader reader = req.getReader()) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
