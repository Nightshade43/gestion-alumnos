package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Plan;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.PlanRepository;
import ar.com.ramallo.gestionalumnos.repository.ProgramaRepository;
import ar.com.ramallo.gestionalumnos.web.dto.PlanRequest;
import ar.com.ramallo.gestionalumnos.web.dto.PlanResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planes")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final ProgramaRepository programaRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse crear(@Valid @RequestBody PlanRequest request) {
        Programa programa = programaRepository.findById(request.programaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Programa no encontrado: " + request.programaId()));
        Plan plan = Plan.builder()
                .programa(programa).codigo(request.codigo()).moduloInicio(request.moduloInicio()).build();
        return PlanResponse.from(planRepository.save(plan));
    }

    @GetMapping("/{id}")
    public PlanResponse obtener(@PathVariable Long id) {
        return PlanResponse.from(planRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado: " + id)));
    }

    @GetMapping
    public List<PlanResponse> listarPorPrograma(@RequestParam Long programaId) {
        return planRepository.findByProgramaId(programaId).stream()
                .map(PlanResponse::from).toList();
    }
}
