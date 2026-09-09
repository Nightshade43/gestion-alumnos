package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Seguimiento;
import ar.com.ramallo.gestionalumnos.repository.SeguimientoRepository;
import ar.com.ramallo.gestionalumnos.service.SeguimientoService;
import ar.com.ramallo.gestionalumnos.web.dto.SeguimientoRequest;
import ar.com.ramallo.gestionalumnos.web.dto.SeguimientoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seguimientos")
@RequiredArgsConstructor
@Validated
public class SeguimientoController {

    private final SeguimientoService seguimientoService;
    private final SeguimientoRepository seguimientoRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SeguimientoResponse crear(@Valid @RequestBody SeguimientoRequest request) {
        return SeguimientoResponse.from(seguimientoService.crearSeguimiento(
                request.inscripcionId(), request.fecha(), request.observacion()));
    }

    @GetMapping
    public List<SeguimientoResponse> listar(
            @RequestParam(required = false) Long inscripcionId,
            @RequestParam(required = false) @Max(100) Integer limit) {
        List<Seguimiento> seguimientos = inscripcionId != null
                ? seguimientoRepository.findByInscripcionIdOrderByFechaDesc(inscripcionId)
                : seguimientoRepository.findAllByOrderByFechaDescIdDesc();

        if (limit != null) {
            seguimientos = seguimientos.stream().limit(limit).toList();
        }

        return seguimientos.stream().map(SeguimientoResponse::from).toList();
    }
}