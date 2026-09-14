package co.edu.sena.cimm.eventos.repository;

import co.edu.sena.cimm.eventos.model.Inscripcion;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository {
    Inscripcion save(Inscripcion inscripcion);
    Optional<Inscripcion> findByToken(String token);
    long countByEventoId(Long eventoId);
    List<Inscripcion> findByEventoId(Long eventoId);
}
