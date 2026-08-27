package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.exception.EstadoInvalidoException;
import ar.com.ramallo.gestionalumnos.exception.RegistroDuplicadoException;
import ar.com.ramallo.gestionalumnos.repository.HistorialGrupoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final HistorialGrupoRepository historialGrupoRepository;

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
    public Inscripcion pausar(Long inscripcionId) {
        return cambiarEstado(inscripcionId, EstadoInscripcion.PAUSADA);
    }

    @Transactional
    public Inscripcion reanudar(Long inscripcionId) {
        return cambiarEstado(inscripcionId, EstadoInscripcion.ACTIVA);
    }

    @Transactional
    public Inscripcion finalizar(Long inscripcionId) {
        Inscripcion inscripcion = cambiarEstado(inscripcionId, EstadoInscripcion.FINALIZADA);
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

    private Inscripcion cambiarEstado(Long inscripcionId, EstadoInscripcion destino) {
        Inscripcion inscripcion = obtener(inscripcionId);
        validarTransicion(inscripcion.getEstado(), destino);
        inscripcion.setEstado(destino);
        return inscripcionRepository.save(inscripcion);
    }

    private Inscripcion obtener(Long inscripcionId) {
        return inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada: " + inscripcionId));
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