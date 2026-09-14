package co.edu.sena.cimm.eventos.service;

import co.edu.sena.cimm.eventos.dto.InscripcionRequest;
import co.edu.sena.cimm.eventos.dto.InscripcionResponse;
import co.edu.sena.cimm.eventos.dto.NegocioException;
import co.edu.sena.cimm.eventos.model.Asistente;
import co.edu.sena.cimm.eventos.model.EstadoInscripcion;
import co.edu.sena.cimm.eventos.model.Evento;
import co.edu.sena.cimm.eventos.model.Inscripcion;
import co.edu.sena.cimm.eventos.repository.EventoRepository;
import co.edu.sena.cimm.eventos.repository.InscripcionRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public class InscripcionService {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;

    public InscripcionService(EventoRepository eventoRepository,
                              InscripcionRepository inscripcionRepository) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    public InscripcionResponse inscribir(InscripcionRequest req) {
        validar(req);

        Evento evento = eventoRepository.findById(req.eventoId)
                .orElseThrow(() -> new NegocioException("El evento indicado no existe"));

        long inscritos = inscripcionRepository.countByEventoId(evento.getId());
        if (inscritos >= evento.getCupoMaximo()) {
            throw new NegocioException("El evento no tiene cupos disponibles");
        }

        boolean documentoDuplicado = inscripcionRepository.findByEventoId(evento.getId()).stream()
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA)
                .anyMatch(i -> i.getAsistente() != null
                        && req.documento.equalsIgnoreCase(i.getAsistente().getDocumento()));
        if (documentoDuplicado) {
            throw new NegocioException("Este documento ya esta inscrito en el evento");
        }

        Asistente asistente = new Asistente(null, req.nombreCompleto.trim(),
                req.documento.trim(), req.email, req.telefono);

        String token = UUID.randomUUID().toString();
        Inscripcion inscripcion = new Inscripcion(null, token, evento, asistente,
                EstadoInscripcion.PENDIENTE, LocalDateTime.now(), null);
        inscripcionRepository.save(inscripcion);

        InscripcionResponse resp = new InscripcionResponse();
        resp.token = token;
        resp.estado = inscripcion.getEstado().name();
        resp.mensaje = "Inscripcion exitosa. Presenta este codigo QR en la puerta del evento.";
        resp.eventoNombre = evento.getNombre();
        resp.eventoFecha = evento.getFecha();
        resp.eventoLugar = evento.getLugar();
        resp.asistenteNombre = asistente.getNombreCompleto();
        resp.qrUrl = "/api/qr/" + token;
        return resp;
    }

    public Inscripcion buscarPorToken(String token) {
        return inscripcionRepository.findByToken(token)
                .orElseThrow(() -> new NegocioException("Inscripcion no encontrada"));
    }

    private void validar(InscripcionRequest req) {
        if (req == null) {
            throw new NegocioException("Datos de inscripcion vacios");
        }
        if (req.eventoId == null) {
            throw new NegocioException("Debe indicar el evento");
        }
        if (req.nombreCompleto == null || req.nombreCompleto.trim().isEmpty()) {
            throw new NegocioException("El nombre completo es obligatorio");
        }
        if (req.documento == null || req.documento.trim().isEmpty()) {
            throw new NegocioException("El numero de documento es obligatorio");
        }
    }
}
