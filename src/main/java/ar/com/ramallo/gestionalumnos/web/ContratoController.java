package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.service.ContratoService;
import ar.com.ramallo.gestionalumnos.web.dto.ContratoRequest;
import ar.com.ramallo.gestionalumnos.web.dto.ContratoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;
    private final ContratoRepository contratoRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContratoResponse crear(@Valid @RequestBody ContratoRequest request) {
        return ContratoResponse.from(contratoService.crearContrato(
                request.inscripcionId(), request.tipoFacturacion(), request.clasesContratadas()));
    }

    @GetMapping("/{id}")
    public ContratoResponse obtener(@PathVariable Long id) {
        return ContratoResponse.from(buscarOFallar(id));
    }

    @PostMapping("/{id}/consumir-clase")
    public ContratoResponse consumirClase(@PathVariable Long id) {
        return ContratoResponse.from(contratoService.consumirClase(id));
    }

    @PostMapping("/{id}/finalizar")
    public ContratoResponse finalizar(@PathVariable Long id) {
        return ContratoResponse.from(contratoService.finalizar(id));
    }

    private Contrato buscarOFallar(Long id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Contrato no encontrado: " + id));
    }
}