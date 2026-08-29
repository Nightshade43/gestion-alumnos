package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Institucion;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.InstitucionRepository;
import ar.com.ramallo.gestionalumnos.web.dto.InstitucionRequest;
import ar.com.ramallo.gestionalumnos.web.dto.InstitucionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instituciones")
@RequiredArgsConstructor
public class InstitucionController {

    private final InstitucionRepository institucionRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitucionResponse crear(@Valid @RequestBody InstitucionRequest request) {
        return InstitucionResponse.from(institucionRepository.save(
                Institucion.builder().nombre(request.nombre()).build()));
    }

    @GetMapping("/{id}")
    public InstitucionResponse obtener(@PathVariable Long id) {
        return InstitucionResponse.from(institucionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Institucion no encontrada: " + id)));
    }

    @GetMapping
    public List<InstitucionResponse> listar() {
        return institucionRepository.findAll().stream().map(InstitucionResponse::from).toList();
    }
}