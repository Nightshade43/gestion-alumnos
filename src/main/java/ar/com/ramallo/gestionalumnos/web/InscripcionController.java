package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.GrupoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.service.InscripcionService;
import ar.com.ramallo.gestionalumnos.web.dto.InscripcionRequest;
import ar.com.ramallo.gestionalumnos.web.dto.InscripcionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/inscripciones")
@RequiredArgsConstructor
public class InscripcionController {

    private final InscripcionService inscripcionService;
    private final InscripcionRepository inscripcionRepository;
    private final GrupoRepository grupoRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionResponse crear(@Valid @RequestBody InscripcionRequest request) {
        Inscripcion inscripcion = inscripcionService.crearInscripcion(
                request.personaId(), request.programaId(), request.planId(),
                request.grupoId(), request.fechaInicio());
        return armarResponse(inscripcion);
    }

    @GetMapping("/{id}")
    public InscripcionResponse obtener(@PathVariable Long id) {
        return armarResponse(buscarOFallar(id));
    }

    /**
     * Todos los filtros son opcionales y combinables. {@code estado} admite varios valores
     * ({@code ?estado=ACTIVA&estado=PAUSADA}): la pantalla de Inicio solo necesita las vigentes, y pedirlas
     * acota el costo a los alumnos en curso en lugar de arrastrar todo el historial de finalizadas y canceladas.
     */
    @GetMapping
    public List<InscripcionResponse> listar(
            @RequestParam(required = false) Long personaId,
            @RequestParam(required = false) CategoriaPrograma categoria,
            @RequestParam(required = false) List<EstadoInscripcion> estado) {
        boolean filtraEstado = estado != null && !estado.isEmpty();

        // personaId manda en la consulta; el estado solo va a la base cuando es el único criterio de fondo.
        List<Inscripcion> inscripciones;
        if (personaId != null) {
            inscripciones = inscripcionRepository.findByPersonaId(personaId);
        } else if (filtraEstado) {
            inscripciones = inscripcionRepository.findByEstadoIn(estado);
        } else {
            inscripciones = inscripcionRepository.findAll();
        }

        // Los filtros restantes se aplican antes de armar la respuesta: evaluarEstadoAcademico es lo caro.
        if (personaId != null && filtraEstado) {
            inscripciones = inscripciones.stream()
                    .filter(i -> estado.contains(i.getEstado()))
                    .toList();
        }
        if (categoria != null) {
            inscripciones = inscripciones.stream()
                    .filter(i -> i.getPrograma().getCategoria() == categoria)
                    .toList();
        }

        // Una sola evaluación por lotes para todo el listado (ver InscripcionService.evaluarEstadosAcademicos).
        // `inscripciones` se reasigna al filtrar, así que no se puede usar dentro de la lambda: se fija en una copia.
        List<Inscripcion> listado = inscripciones;
        List<InscripcionService.EstadoAcademico> estados = inscripcionService.evaluarEstadosAcademicos(listado);
        return IntStream.range(0, listado.size())
                .mapToObj(i -> InscripcionResponse.from(
                        listado.get(i), estados.get(i).puedeFinalizar(), estados.get(i).modulosPendientes()))
                .toList();
    }

    @PostMapping("/{id}/pausar")
    public InscripcionResponse pausar(@PathVariable Long id) {
        return armarResponse(inscripcionService.pausar(id));
    }

    @PostMapping("/{id}/reanudar")
    public InscripcionResponse reanudar(@PathVariable Long id) {
        return armarResponse(inscripcionService.reanudar(id));
    }

    @PostMapping("/{id}/finalizar")
    public InscripcionResponse finalizar(@PathVariable Long id) {
        return armarResponse(inscripcionService.finalizar(id));
    }

    @PostMapping("/{id}/cancelar")
    public InscripcionResponse cancelar(@PathVariable Long id) {
        return armarResponse(inscripcionService.cancelar(id));
    }

    @PatchMapping("/{id}/grupo")
    public InscripcionResponse cambiarGrupo(@PathVariable Long id, @RequestParam Long grupoId) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado: " + grupoId));
        return armarResponse(inscripcionService.cambiarGrupo(id, grupo));
    }

    private Inscripcion buscarOFallar(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + id));
    }

    private InscripcionResponse armarResponse(Inscripcion inscripcion) {
        InscripcionService.EstadoAcademico estadoAcademico = inscripcionService.evaluarEstadoAcademico(inscripcion);
        return InscripcionResponse.from(inscripcion, estadoAcademico.puedeFinalizar(), estadoAcademico.modulosPendientes());
    }
}