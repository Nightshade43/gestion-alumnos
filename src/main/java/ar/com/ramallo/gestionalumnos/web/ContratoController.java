package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.service.ContratoService;
import ar.com.ramallo.gestionalumnos.web.dto.ContratoEmpresaRequest;
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
    private final InscripcionRepository inscripcionRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContratoResponse crear(@Valid @RequestBody ContratoRequest request) {
        Contrato contrato = contratoService.crearContratoIndividual(
                request.inscripcionId(), request.tipoFacturacion(), request.clasesContratadas());
        return armarResponse(contrato);
    }

    @PostMapping("/empresa")
    @ResponseStatus(HttpStatus.CREATED)
    public ContratoResponse crearParaEmpresa(@Valid @RequestBody ContratoEmpresaRequest request) {
        Contrato contrato = contratoService.crearContratoEmpresa(
                request.empresaId(), request.inscripcionIds(), request.tipoFacturacion(), request.clasesContratadas());
        return armarResponse(contrato);
    }

    @PostMapping("/{id}/ampliar-cupo")
    public ContratoResponse ampliarCupo(@PathVariable Long id, @RequestParam Integer clasesAdicionales) {
        return armarResponse(contratoService.ampliarCupo(id, clasesAdicionales));
    }

// obtener/consumirClase/finalizar también pasan por armarResponse en vez de ContratoResponse.from directo

    private ContratoResponse armarResponse(Contrato contrato) {
        return ContratoResponse.from(contrato, inscripcionRepository.findByContratoId(contrato.getId()));
    }
}