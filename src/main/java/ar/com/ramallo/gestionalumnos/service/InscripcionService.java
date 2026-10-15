package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.exception.EstadoInvalidoException;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.exception.RegistroDuplicadoException;
import ar.com.ramallo.gestionalumnos.exception.RequisitosAcademicosIncompletosException;
import ar.com.ramallo.gestionalumnos.repository.*;
import ar.com.ramallo.gestionalumnos.service.evaluacion.EstrategiaEvaluacionService;
import ar.com.ramallo.gestionalumnos.service.evaluacion.EvaluacionServiceFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final HistorialGrupoRepository historialGrupoRepository;
    private final ModuloRepository moduloRepository;
    private final EvaluacionServiceFactory evaluacionServiceFactory;
    private final PersonaRepository personaRepository;
    private final ProgramaRepository programaRepository;
    private final PlanRepository planRepository;
    private final GrupoRepository grupoRepository;
    private final InstanciaEvaluativaRepository instanciaEvaluativaRepository;

    public record EstadoAcademico(boolean puedeFinalizar, int modulosPendientes) {}

    @Transactional
    public Inscripcion crearInscripcion(Persona persona, Programa programa, Plan plan, Grupo grupo, LocalDate fechaInicio) {
        if (programa.getCategoria() == CategoriaPrograma.ESCOLAR) {
            boolean yaTieneEscolarActiva = inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
                    persona.getId(), CategoriaPrograma.ESCOLAR,
                    List.of(EstadoInscripcion.ACTIVA, EstadoInscripcion.PAUSADA));
            if (yaTieneEscolarActiva) {
                throw new RegistroDuplicadoException(
                        "La persona ya tiene una inscripcion escolar activa o pausada");
            }
        }

        Inscripcion inscripcion = inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programa).plan(plan).grupo(grupo)
                .fechaInicio(fechaInicio)
                .build());

        if (grupo != null) {
            historialGrupoRepository.save(HistorialGrupo.builder()
                    .inscripcion(inscripcion).grupo(grupo).fechaDesde(fechaInicio).build());
        }

        return inscripcion;
    }

    @Transactional
    public Inscripcion crearInscripcion(Long personaId, Long programaId, Long planId, Long grupoId, LocalDate fechaInicio) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Persona no encontrada: " + personaId));
        Programa programa = programaRepository.findById(programaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + programaId));
        Plan plan = planId != null
                ? planRepository.findById(planId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado: " + planId))
                : null;
        Grupo grupo = grupoId != null
                ? grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado: " + grupoId))
                : null;

        return crearInscripcion(persona, programa, plan, grupo, fechaInicio);
    }

    @Transactional
    public Inscripcion pausar(Long inscripcionId) {
        return cambiarEstado(inscripcionId, EstadoInscripcion.PAUSADA);
    }

    @Transactional
    public Inscripcion reanudar(Long inscripcionId) {
        return cambiarEstado(inscripcionId, EstadoInscripcion.ACTIVA);
    }

    @Transactional
    public Inscripcion finalizar(Long inscripcionId) {
        Inscripcion inscripcion = obtener(inscripcionId);
        validarTransicion(inscripcion.getEstado(), EstadoInscripcion.FINALIZADA);

        if (!evaluarEstadoAcademico(inscripcion).puedeFinalizar()) {
            throw new RequisitosAcademicosIncompletosException(
                    "No se puede finalizar: no todos los modulos requeridos estan aprobados");
        }

        inscripcion.setEstado(EstadoInscripcion.FINALIZADA);
        inscripcion.setFechaFin(LocalDate.now());
        return inscripcionRepository.save(inscripcion);
    }

    @Transactional
    public Inscripcion cancelar(Long inscripcionId) {
        Inscripcion inscripcion = cambiarEstado(inscripcionId, EstadoInscripcion.CANCELADA);
        inscripcion.setFechaFin(LocalDate.now());
        return inscripcionRepository.save(inscripcion);
    }

    @Transactional
    public Inscripcion cambiarGrupo(Long inscripcionId, Grupo nuevoGrupo) {
        Inscripcion inscripcion = obtener(inscripcionId);
        LocalDate hoy = LocalDate.now();

        historialGrupoRepository.findByInscripcionIdAndFechaHastaIsNull(inscripcionId)
                .ifPresent(registroAbierto -> {
                    registroAbierto.setFechaHasta(hoy);
                    historialGrupoRepository.save(registroAbierto);
                });

        inscripcion.setGrupo(nuevoGrupo);
        inscripcionRepository.save(inscripcion);

        historialGrupoRepository.save(HistorialGrupo.builder()
                .inscripcion(inscripcion).grupo(nuevoGrupo).fechaDesde(hoy).build());

        return inscripcion;
    }

    public EstadoAcademico evaluarEstadoAcademico(Inscripcion inscripcion) {
        if (inscripcion.getPrograma().getCategoria() != CategoriaPrograma.ESCOLAR) {
            return new EstadoAcademico(true, 0);
        }

        EstrategiaEvaluacionService estrategia = evaluacionServiceFactory.resolver(inscripcion.getPrograma());
        List<Modulo> modulos = moduloRepository.findByProgramaIdOrderByOrden(inscripcion.getPrograma().getId());

        return calcularEstadoAcademico(inscripcion, modulos, modulo -> estrategia.moduloAprobado(inscripcion, modulo));
    }

    /**
     * Evalúa varias inscripciones juntas y devuelve los resultados en el mismo orden que la lista recibida.
     * Aplica exactamente la regla de {@link #evaluarEstadoAcademico(Inscripcion)}, pero en lugar de consultar
     * módulos e instancias por cada inscripción (y por cada módulo) las carga una sola vez para todas:
     * el costo pasa de crecer con alumnos x módulos a ser constante (dos consultas).
     */
    public List<EstadoAcademico> evaluarEstadosAcademicos(List<Inscripcion> inscripciones) {
        List<Inscripcion> escolares = inscripciones.stream()
                .filter(i -> i.getPrograma().getCategoria() == CategoriaPrograma.ESCOLAR)
                .toList();
        if (escolares.isEmpty()) {
            return inscripciones.stream().map(i -> new EstadoAcademico(true, 0)).toList();
        }

        List<Long> programaIds = escolares.stream().map(i -> i.getPrograma().getId()).distinct().toList();
        Map<Long, List<Modulo>> modulosPorPrograma = new HashMap<>();
        for (Modulo modulo : moduloRepository.findByProgramaIdInOrderByOrden(programaIds)) {
            modulosPorPrograma.computeIfAbsent(modulo.getPrograma().getId(), id -> new ArrayList<>()).add(modulo);
        }

        List<Long> inscripcionIds = escolares.stream().map(Inscripcion::getId).toList();
        // inscripcionId -> moduloId -> instancias de esa inscripción en ese módulo
        Map<Long, Map<Long, List<InstanciaEvaluativa>>> instanciasPorInscripcion = new HashMap<>();
        for (InstanciaEvaluativa instancia : instanciaEvaluativaRepository.findByInscripcionIdIn(inscripcionIds)) {
            instanciasPorInscripcion
                    .computeIfAbsent(instancia.getInscripcion().getId(), id -> new HashMap<>())
                    .computeIfAbsent(instancia.getModulo().getId(), id -> new ArrayList<>())
                    .add(instancia);
        }

        List<EstadoAcademico> resultado = new ArrayList<>(inscripciones.size());
        for (Inscripcion inscripcion : inscripciones) {
            if (inscripcion.getPrograma().getCategoria() != CategoriaPrograma.ESCOLAR) {
                resultado.add(new EstadoAcademico(true, 0));
                continue;
            }
            EstrategiaEvaluacionService estrategia = evaluacionServiceFactory.resolver(inscripcion.getPrograma());
            List<Modulo> modulos = modulosPorPrograma.getOrDefault(inscripcion.getPrograma().getId(), List.of());
            Map<Long, List<InstanciaEvaluativa>> delAlumno =
                    instanciasPorInscripcion.getOrDefault(inscripcion.getId(), Map.of());
            resultado.add(calcularEstadoAcademico(inscripcion, modulos,
                    modulo -> estrategia.moduloAprobado(delAlumno.getOrDefault(modulo.getId(), List.of()))));
        }
        return resultado;
    }

    /** La regla común a ambos caminos: qué módulos se exigen según el plan y cuántos faltan aprobar. */
    private EstadoAcademico calcularEstadoAcademico(
            Inscripcion inscripcion, List<Modulo> modulos, Predicate<Modulo> moduloAprobado) {
        Integer moduloInicio = inscripcion.getPlan() != null ? inscripcion.getPlan().getModuloInicio() : null;

        List<Modulo> modulosRequeridos = modulos.stream()
                .filter(modulo -> moduloInicio == null || modulo.getOrden() >= moduloInicio)
                .toList();

        long pendientes = modulosRequeridos.stream()
                .filter(modulo -> !moduloAprobado.test(modulo))
                .count();

        boolean puedeFinalizar = !modulosRequeridos.isEmpty() && pendientes == 0;
        return new EstadoAcademico(puedeFinalizar, (int) pendientes);
    }

    private Inscripcion cambiarEstado(Long inscripcionId, EstadoInscripcion destino) {
        Inscripcion inscripcion = obtener(inscripcionId);
        validarTransicion(inscripcion.getEstado(), destino);
        inscripcion.setEstado(destino);
        return inscripcionRepository.save(inscripcion);
    }

    private Inscripcion obtener(Long inscripcionId) {
        return inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + inscripcionId));
    }

    private void validarTransicion(EstadoInscripcion actual, EstadoInscripcion destino) {
        boolean esValida = switch (actual) {
            case ACTIVA -> destino == EstadoInscripcion.PAUSADA
                    || destino == EstadoInscripcion.FINALIZADA
                    || destino == EstadoInscripcion.CANCELADA;
            case PAUSADA -> destino == EstadoInscripcion.ACTIVA
                    || destino == EstadoInscripcion.CANCELADA;
            case FINALIZADA, CANCELADA -> false;
        };
        if (!esValida) {
            throw new EstadoInvalidoException("Transicion invalida: " + actual + " -> " + destino);
        }
    }
}