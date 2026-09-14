package co.edu.sena.cimm.eventos.servlet;

import co.edu.sena.cimm.eventos.config.AppContext;
import co.edu.sena.cimm.eventos.dto.ApiError;
import co.edu.sena.cimm.eventos.dto.ValidacionRequest;
import co.edu.sena.cimm.eventos.service.ValidacionService;
import co.edu.sena.cimm.eventos.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * POST /api/validaciones  body: {"token":"..."}  -> valida el ingreso en puerta.
 * Devuelve 200 siempre (valido true/false) para que la app de puerta muestre
 * verde o rojo sin tratarlo como error HTTP.
 */
@WebServlet("/api/validaciones")
public class ValidacionServlet extends BaseApiServlet {

    private final ValidacionService service = AppContext.get().getValidacionService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ValidacionRequest r = JsonUtil.fromJson(readBody(req), ValidacionRequest.class);
            String token = (r == null) ? null : r.token;
            writeJson(resp, 200, service.validar(token));
        } catch (Exception ex) {
            writeJson(resp, 500, new ApiError("ERROR", ex.getMessage()));
        }
    }
}
