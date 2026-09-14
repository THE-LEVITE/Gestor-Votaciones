package co.edu.sena.cimm.eventos.repository;

import co.edu.sena.cimm.eventos.model.EstadoInscripcion;
import co.edu.sena.cimm.eventos.model.Inscripcion;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryInscripcionRepository implements InscripcionRepository {

    private final Map<Long, Inscripcion> data = new ConcurrentHashMap<>();
    private final Map<String, Long> indicePorToken = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(0);

    @Override
    public Inscripcion save(Inscripcion inscripcion) {
        if (inscripcion.getId() == null) {
            inscripcion.setId(secuencia.incrementAndGet());
        }
        data.put(inscripcion.getId(), inscripcion);
        if (inscripcion.getToken() != null) {
            indicePorToken.put(inscripcion.getToken(), inscripcion.getId());
        }
        return inscripcion;
    }

    @Override
    public Optional<Inscripcion> findByToken(String token) {
        Long id = indicePorToken.get(token);
        return id == null ? Optional.empty() : Optional.ofNullable(data.get(id));
    }

    @Override
    public long countByEventoId(Long eventoId) {
        return data.values().stream()
                .filter(i -> i.getEstado() != EstadoInscripcion.CANCELADA)
                .filter(i -> i.getEvento() != null && eventoId.equals(i.getEvento().getId()))
                .count();
    }

    @Override
    public List<Inscripcion> findByEventoId(Long eventoId) {
        return data.values().stream()
                .filter(i -> i.getEvento() != null && eventoId.equals(i.getEvento().getId()))
                .collect(Collectors.toList());
    }
}
