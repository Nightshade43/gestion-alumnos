package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.GrupoRepository;
import ar.com.ramallo.gestionalumnos.repository.ProgramaRepository;
import ar.com.ramallo.gestionalumnos.web.dto.GrupoRequest;
import ar.com.ramallo.gestionalumnos.web.dto.GrupoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoRepository grupoRepository;
    private final ProgramaRepository programaRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GrupoResponse crear(@Valid @RequestBody GrupoRequest request) {
        Programa programa = programaRepository.findById(request.programaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + request.programaId()));
        Grupo grupo = Grupo.builder()
                .programa(programa).dia(request.dia()).horario(request.horario()).build();
        return GrupoResponse.from(grupoRepository.save(grupo));
    }

    @GetMapping("/{id}")
    public GrupoResponse obtener(@PathVariable Long id) {
        return GrupoResponse.from(grupoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Grupo no encontrado: " + id)));
    }

    @GetMapping
    public List<GrupoResponse> listarPorPrograma(@RequestParam Long programaId) {
        return grupoRepository.findByProgramaId(programaId).stream()
                .map(GrupoResponse::from).toList();
    }
}
