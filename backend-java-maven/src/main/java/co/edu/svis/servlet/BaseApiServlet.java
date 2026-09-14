package co.edu.svis.servlet;

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

    protected String readBody(HttpServletRequest req) throws IOException {
        req.setCharacterEncoding("UTF-8");
        try (BufferedReader reader = req.getReader()) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
