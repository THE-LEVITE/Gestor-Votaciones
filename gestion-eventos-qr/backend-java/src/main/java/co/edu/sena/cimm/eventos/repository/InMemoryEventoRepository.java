package co.edu.sena.cimm.eventos.repository;

import co.edu.sena.cimm.eventos.model.Evento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryEventoRepository implements EventoRepository {

    private final Map<Long, Evento> data = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(0);

    @Override
    public List<Evento> findAll() {
        return new ArrayList<>(data.values()).stream()
                .sorted(Comparator.comparing(Evento::getFecha,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Evento> findById(Long id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public Evento save(Evento evento) {
        if (evento.getId() == null) {
            evento.setId(secuencia.incrementAndGet());
        }
        data.put(evento.getId(), evento);
        return evento;
    }
}
