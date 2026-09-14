package co.edu.sena.cimm.eventos.config;

import co.edu.sena.cimm.eventos.model.Evento;
import co.edu.sena.cimm.eventos.repository.EventoRepository;
import co.edu.sena.cimm.eventos.repository.InMemoryEventoRepository;
import co.edu.sena.cimm.eventos.repository.InMemoryInscripcionRepository;
import co.edu.sena.cimm.eventos.repository.InscripcionRepository;
import co.edu.sena.cimm.eventos.repository.JdbcEventoRepository;
import co.edu.sena.cimm.eventos.repository.JdbcInscripcionRepository;
import co.edu.sena.cimm.eventos.service.EventoService;
import co.edu.sena.cimm.eventos.service.InscripcionService;
import co.edu.sena.cimm.eventos.service.QrService;
import co.edu.sena.cimm.eventos.service.ValidacionService;

import java.time.LocalDateTime;

/**
 * "Inyeccion de dependencias a mano": crea repositorios y servicios una sola vez.
 *
 * El modo de almacenamiento se elige con la propiedad de sistema
 * "eventos.storage" o la variable de entorno EVENTOS_STORAGE:
 *   - "memory" (por defecto): repositorios en memoria + datos demo.
 *   - "mysql": repositorios JDBC (los datos demo los carga schema.sql).
 *
 * Observa que al cambiar de modo NO se toca ni un servicio ni un servlet:
 * ese es el beneficio de programar contra las interfaces (DIP).
 */
public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private final EventoService eventoService;
    private final InscripcionService inscripcionService;
    private final ValidacionService validacionService;
    private final QrService qrService;
    private final String modo;

    private AppContext() {
        this.modo = resolverModo();

        EventoRepository eventoRepo;
        InscripcionRepository inscripcionRepo;

        if ("mysql".equalsIgnoreCase(modo)) {
            eventoRepo = new JdbcEventoRepository();
            inscripcionRepo = new JdbcInscripcionRepository();
        } else {
            eventoRepo = new InMemoryEventoRepository();
            inscripcionRepo = new InMemoryInscripcionRepository();
            cargarDatosDemo(eventoRepo);
        }

        this.qrService = new QrService();
        this.eventoService = new EventoService(eventoRepo, inscripcionRepo);
        this.inscripcionService = new InscripcionService(eventoRepo, inscripcionRepo);
        this.validacionService = new ValidacionService(inscripcionRepo);

        System.out.println("[eventos-qr] Modo de almacenamiento: " + modo);
    }

    public static AppContext get() {
        return INSTANCE;
    }

    public EventoService getEventoService() { return eventoService; }
    public InscripcionService getInscripcionService() { return inscripcionService; }
    public ValidacionService getValidacionService() { return validacionService; }
    public QrService getQrService() { return qrService; }
    public String getModo() { return modo; }

    private String resolverModo() {
        String m = System.getProperty("eventos.storage");
        if (m == null || m.trim().isEmpty()) {
            m = System.getenv("EVENTOS_STORAGE");
        }
        return (m == null || m.trim().isEmpty()) ? "mysql" : m.trim();
    }

    private void cargarDatosDemo(EventoRepository repo) {
        repo.save(new Evento(null,
                "Feria de Innovacion CIMM 2026",
                "Muestra de proyectos de aprendices de tecnologia y manufactura.",
                "Auditorio Principal - CIMM, Paipa",
                LocalDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0),
                120));
        repo.save(new Evento(null,
                "Charla: Ingenieria Agentica y Prompt Engineering",
                "Introduccion al estandar PIC 2026 y herramientas de IA generativa.",
                "Sala de Sistemas 2 - CIMM",
                LocalDateTime.now().plusDays(14).withHour(14).withMinute(0).withSecond(0).withNano(0),
                40));
        repo.save(new Evento(null,
                "Taller de Mantenimiento Industrial 4.0",
                "Sensorica, IoT y mantenimiento predictivo en planta.",
                "Taller de Mecatronica - CIMM",
                LocalDateTime.now().plusDays(21).withHour(8).withMinute(0).withSecond(0).withNano(0),
                30));
    }
}

