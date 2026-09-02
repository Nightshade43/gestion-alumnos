package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
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
        return InscripcionResponse.from(inscripcion);
    }

    @GetMapping("/{id}")
    public InscripcionResponse obtener(@PathVariable Long id) {
        return InscripcionResponse.from(buscarOFallar(id));
    }

    @GetMapping
    public List<InscripcionResponse> listarPorPersona(@RequestParam Long personaId) {
        return inscripcionRepository.findByPersonaId(personaId).stream()
                .map(InscripcionResponse::from).toList();
    }

    @GetMapping
    public List<InscripcionResponse> listar(
            @RequestParam(required = false) Long personaId,
            @RequestParam(required = false) CategoriaPrograma categoria) {
        List<Inscripcion> inscripciones = personaId != null
                ? inscripcionRepository.findByPersonaId(personaId)
                : inscripcionRepository.findAll();

        if (categoria != null) {
            inscripciones = inscripciones.stream()
                    .filter(i -> i.getPrograma().getCategoria() == categoria)
                    .toList();
        }

        return inscripciones.stream().map(InscripcionResponse::from).toList();
    }

    @PostMapping("/{id}/pausar")
    public InscripcionResponse pausar(@PathVariable Long id) {
        return InscripcionResponse.from(inscripcionService.pausar(id));
    }

    @PostMapping("/{id}/reanudar")
    public InscripcionResponse reanudar(@PathVariable Long id) {
        return InscripcionResponse.from(inscripcionService.reanudar(id));
    }

    @PostMapping("/{id}/finalizar")
    public InscripcionResponse finalizar(@PathVariable Long id) {
        return InscripcionResponse.from(inscripcionService.finalizar(id));
    }

    @PostMapping("/{id}/cancelar")
    public InscripcionResponse cancelar(@PathVariable Long id) {
        return InscripcionResponse.from(inscripcionService.cancelar(id));
    }

    @PatchMapping("/{id}/grupo")
    public InscripcionResponse cambiarGrupo(@PathVariable Long id, @RequestParam Long grupoId) {
        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado: " + grupoId));
        return InscripcionResponse.from(inscripcionService.cambiarGrupo(id, grupo));
    }

    private Inscripcion buscarOFallar(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + id));
    }
}