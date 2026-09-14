package co.edu.sena.cimm.eventos.service;

import co.edu.sena.cimm.eventos.dto.ValidacionResponse;
import co.edu.sena.cimm.eventos.model.EstadoInscripcion;
import co.edu.sena.cimm.eventos.model.Inscripcion;
import co.edu.sena.cimm.eventos.repository.InscripcionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Valida el ingreso a la puerta a partir del token del QR.
 * Aplica la transicion PENDIENTE -> VALIDADA una sola vez (control antifraude).
 */
public class ValidacionService {

    private final InscripcionRepository inscripcionRepository;

    public ValidacionService(InscripcionRepository inscripcionRepository) {
        this.inscripcionRepository = inscripcionRepository;
    }

    public ValidacionResponse validar(String token) {
        ValidacionResponse r = new ValidacionResponse();

        if (token == null || token.trim().isEmpty()) {
            r.valido = false;
            r.estado = "INVALIDO";
            r.mensaje = "Codigo vacio";
            return r;
        }

        Optional<Inscripcion> opt = inscripcionRepository.findByToken(token.trim());
        if (!opt.isPresent()) {
            r.valido = false;
            r.estado = "NO_ENCONTRADO";
            r.mensaje = "Codigo no valido o no registrado";
            return r;
        }

        Inscripcion i = opt.get();
        r.asistenteNombre = i.getAsistente().getNombreCompleto();
        r.documento = i.getAsistente().getDocumento();
        r.eventoNombre = i.getEvento().getNombre();
        r.eventoFecha = i.getEvento().getFecha();

        switch (i.getEstado()) {
            case VALIDADA:
                r.valido = false;
                r.estado = "VALIDADA";
                r.fechaValidacion = i.getFechaValidacion();
                r.mensaje = "Este codigo YA fue utilizado";
                return r;
            case CANCELADA:
                r.valido = false;
                r.estado = "CANCELADA";
                r.mensaje = "La inscripcion fue cancelada";
                return r;
            case PENDIENTE:
            default:
                i.setEstado(EstadoInscripcion.VALIDADA);
                i.setFechaValidacion(LocalDateTime.now());
                inscripcionRepository.save(i);
                r.valido = true;
                r.estado = "VALIDADA";
                r.fechaValidacion = i.getFechaValidacion();
                r.mensaje = "Ingreso autorizado. Bienvenido(a)!";
                return r;
        }
    }
}
