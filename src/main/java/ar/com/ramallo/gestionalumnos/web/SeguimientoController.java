package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.repository.SeguimientoRepository;
import ar.com.ramallo.gestionalumnos.service.SeguimientoService;
import ar.com.ramallo.gestionalumnos.web.dto.SeguimientoRequest;
import ar.com.ramallo.gestionalumnos.web.dto.SeguimientoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seguimientos")
@RequiredArgsConstructor
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
    public List<SeguimientoResponse> listarPorInscripcion(@RequestParam Long inscripcionId) {
        return seguimientoRepository.findByInscripcionIdOrderByFechaDesc(inscripcionId).stream()
                .map(SeguimientoResponse::from).toList();
    }
}
