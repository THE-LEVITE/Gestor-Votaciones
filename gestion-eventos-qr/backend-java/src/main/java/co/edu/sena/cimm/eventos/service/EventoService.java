package co.edu.sena.cimm.eventos.service;

import co.edu.sena.cimm.eventos.dto.EventoRequest;
import co.edu.sena.cimm.eventos.dto.EventoView;
import co.edu.sena.cimm.eventos.dto.NegocioException;
import co.edu.sena.cimm.eventos.model.Evento;
import co.edu.sena.cimm.eventos.repository.EventoRepository;
import co.edu.sena.cimm.eventos.repository.InscripcionRepository;

import java.util.List;
import java.util.stream.Collectors;

public class EventoService {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;

    public EventoService(EventoRepository eventoRepository,
                         InscripcionRepository inscripcionRepository) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    public List<EventoView> listar() {
        return eventoRepository.findAll().stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    public EventoView obtener(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new NegocioException("Evento no encontrado"));
        return toView(evento);
    }

    public EventoView crear(EventoRequest req) {
        if (req == null || req.nombre == null || req.nombre.trim().isEmpty()) {
            throw new NegocioException("El nombre del evento es obligatorio");
        }
        if (req.cupoMaximo <= 0) {
            throw new NegocioException("El cupo maximo debe ser mayor a cero");
        }
        Evento evento = new Evento(null, req.nombre.trim(), req.descripcion,
                req.lugar, req.fecha, req.cupoMaximo);
        return toView(eventoRepository.save(evento));
    }

    private EventoView toView(Evento e) {
        long inscritos = inscripcionRepository.countByEventoId(e.getId());
        EventoView v = new EventoView();
        v.id = e.getId();
        v.nombre = e.getNombre();
        v.descripcion = e.getDescripcion();
        v.lugar = e.getLugar();
        v.fecha = e.getFecha();
        v.cupoMaximo = e.getCupoMaximo();
        v.inscritos = inscritos;
        v.cupoDisponible = Math.max(0, e.getCupoMaximo() - inscritos);
        return v;
    }
}
