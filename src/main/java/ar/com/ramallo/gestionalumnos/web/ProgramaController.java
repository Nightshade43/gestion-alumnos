package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Institucion;
import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.GrupoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.repository.InstitucionRepository;
import ar.com.ramallo.gestionalumnos.repository.ModuloRepository;
import ar.com.ramallo.gestionalumnos.repository.ProgramaRepository;
import ar.com.ramallo.gestionalumnos.web.dto.ProgramaRequest;
import ar.com.ramallo.gestionalumnos.web.dto.ProgramaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/programas")
@RequiredArgsConstructor
public class ProgramaController {

    private final ProgramaRepository programaRepository;
    private final InstitucionRepository institucionRepository;
    private final ModuloRepository moduloRepository;
    private final GrupoRepository grupoRepository;
    private final InscripcionRepository inscripcionRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramaResponse crear(@Valid @RequestBody ProgramaRequest request) {
        Institucion institucion = resolverInstitucion(request.institucionId());
        Programa programa = Programa.builder()
                .nombre(request.nombre()).categoria(request.categoria())
                .estrategiaEvaluacion(request.estrategiaEvaluacion()).institucion(institucion)
                .build();
        return armarResponse(programaRepository.save(programa));
    }

    @GetMapping("/{id}")
    public ProgramaResponse obtener(@PathVariable Long id) {
        return armarResponse(buscarOFallar(id));
    }

    @GetMapping
    public List<ProgramaResponse> listar() {
        return programaRepository.findAll().stream().map(this::armarResponse).toList();
    }

    /**
     * Los contadores se calculan acá y no en el DTO: son consultas por programa (3 por fila en el
     * listado), aceptables con la cantidad de programas de una sede, y le ahorran al cliente
     * pedir módulos y grupos de cada programa que despliega.
     */
    private ProgramaResponse armarResponse(Programa programa) {
        Long id = programa.getId();
        List<String> dias = grupoRepository.findByProgramaIdOrderByIdAsc(id).stream()
                .map(Grupo::getDia)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return ProgramaResponse.from(
                programa,
                Math.toIntExact(moduloRepository.countByProgramaId(id)),
                Math.toIntExact(inscripcionRepository.countByProgramaIdAndEstado(id, EstadoInscripcion.ACTIVA)),
                dias);
    }

    private Institucion resolverInstitucion(Long institucionId) {
        if (institucionId == null) {
            return null;
        }
        return institucionRepository.findById(institucionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Institucion no encontrada: " + institucionId));
    }

    private Programa buscarOFallar(Long id) {
        return programaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + id));
    }
}