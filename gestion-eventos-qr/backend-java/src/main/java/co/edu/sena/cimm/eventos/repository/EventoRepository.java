package co.edu.sena.cimm.eventos.repository;

import co.edu.sena.cimm.eventos.model.Evento;

import java.util.List;
import java.util.Optional;

/**
 * Abstraccion de persistencia de eventos (DIP): el servicio depende de esta
 * interfaz, no de una implementacion concreta. Hoy es en memoria; manana
 * podria ser JDBC/JPA sin tocar la logica de negocio.
 */
public interface EventoRepository {
    List<Evento> findAll();
    Optional<Evento> findById(Long id);
    Evento save(Evento evento);
}
