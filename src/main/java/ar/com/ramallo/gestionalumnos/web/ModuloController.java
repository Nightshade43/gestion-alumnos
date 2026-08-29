package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.ModuloRepository;
import ar.com.ramallo.gestionalumnos.repository.ProgramaRepository;
import ar.com.ramallo.gestionalumnos.web.dto.ModuloRequest;
import ar.com.ramallo.gestionalumnos.web.dto.ModuloResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modulos")
@RequiredArgsConstructor
public class ModuloController {

    private final ModuloRepository moduloRepository;
    private final ProgramaRepository programaRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModuloResponse crear(@Valid @RequestBody ModuloRequest request) {
        Programa programa = programaRepository.findById(request.programaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + request.programaId()));
        Modulo modulo = Modulo.builder()
                .programa(programa).orden(request.orden()).esSecuencial(request.esSecuencial()).build();
        return ModuloResponse.from(moduloRepository.save(modulo));
    }

    @GetMapping("/{id}")
    public ModuloResponse obtener(@PathVariable Long id) {
        return ModuloResponse.from(moduloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Modulo no encontrado: " + id)));
    }

    @GetMapping
    public List<ModuloResponse> listarPorPrograma(@RequestParam Long programaId) {
        return moduloRepository.findByProgramaIdOrderByOrden(programaId).stream()
                .map(ModuloResponse::from).toList();
    }
}