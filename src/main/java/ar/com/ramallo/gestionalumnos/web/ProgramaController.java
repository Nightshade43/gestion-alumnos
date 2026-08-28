package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Institucion;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.InstitucionRepository;
import ar.com.ramallo.gestionalumnos.repository.ProgramaRepository;
import ar.com.ramallo.gestionalumnos.web.dto.ProgramaRequest;
import ar.com.ramallo.gestionalumnos.web.dto.ProgramaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programas")
@RequiredArgsConstructor
public class ProgramaController {

    private final ProgramaRepository programaRepository;
    private final InstitucionRepository institucionRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramaResponse crear(@Valid @RequestBody ProgramaRequest request) {
        Institucion institucion = resolverInstitucion(request.institucionId());
        Programa programa = Programa.builder()
                .nombre(request.nombre()).categoria(request.categoria())
                .estrategiaEvaluacion(request.estrategiaEvaluacion()).institucion(institucion)
                .build();
        return ProgramaResponse.from(programaRepository.save(programa));
    }

    @GetMapping("/{id}")
    public ProgramaResponse obtener(@PathVariable Long id) {
        return ProgramaResponse.from(buscarOFallar(id));
    }

    @GetMapping
    public List<ProgramaResponse> listar() {
        return programaRepository.findAll().stream().map(ProgramaResponse::from).toList();
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