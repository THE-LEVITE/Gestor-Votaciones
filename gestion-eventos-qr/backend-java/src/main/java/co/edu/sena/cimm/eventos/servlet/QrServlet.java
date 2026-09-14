package co.edu.sena.cimm.eventos.servlet;

import co.edu.sena.cimm.eventos.config.AppContext;
import co.edu.sena.cimm.eventos.dto.NegocioException;
import co.edu.sena.cimm.eventos.service.InscripcionService;
import co.edu.sena.cimm.eventos.service.QrService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * GET /api/qr/{token} -> imagen PNG del codigo QR que codifica el token.
 */
@WebServlet("/api/qr/*")
public class QrServlet extends HttpServlet {

    private final InscripcionService inscripcionService = AppContext.get().getInscripcionService();
    private final QrService qrService = AppContext.get().getQrService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.length() < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Debe indicar el token");
            return;
        }
        String token = path.substring(1);

        // El token debe corresponder a una inscripcion real.
        try {
            inscripcionService.buscarPorToken(token);
        } catch (NegocioException ex) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Inscripcion no encontrada");
            return;
        }

        byte[] png = qrService.generarQrPng(token, 300);
        resp.setContentType("image/png");
        resp.setContentLength(png.length);
        try (OutputStream out = resp.getOutputStream()) {
            out.write(png);
        }
    }
}
