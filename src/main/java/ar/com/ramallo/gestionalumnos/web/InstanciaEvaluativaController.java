package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.exception.SolicitudInvalidaException;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.repository.InstanciaEvaluativaRepository;
import ar.com.ramallo.gestionalumnos.repository.ModuloRepository;
import ar.com.ramallo.gestionalumnos.web.dto.InstanciaEvaluativaRequest;
import ar.com.ramallo.gestionalumnos.web.dto.InstanciaEvaluativaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instancias-evaluativas")
@RequiredArgsConstructor
public class InstanciaEvaluativaController {

    private final InstanciaEvaluativaRepository instanciaEvaluativaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ModuloRepository moduloRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstanciaEvaluativaResponse crear(@Valid @RequestBody InstanciaEvaluativaRequest request) {
        // El gate de Sede se carga como aprobado / no aprobado, no como una nota ficticia.
        if (request.tipo() == TipoInstanciaEvaluativa.TP_INTEGRADOR && request.aprobado() == null) {
            throw new SolicitudInvalidaException("aprobado: es obligatorio para TP_INTEGRADOR");
        }
        Inscripcion inscripcion = inscripcionRepository.findById(request.inscripcionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + request.inscripcionId()));
        Modulo modulo = moduloRepository.findById(request.moduloId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Modulo no encontrado: " + request.moduloId()));
        InstanciaEvaluativa recuperaA = request.recuperaAId() != null
                ? instanciaEvaluativaRepository.findById(request.recuperaAId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Instancia a recuperar no encontrada: " + request.recuperaAId()))
                : null;

        InstanciaEvaluativa instancia = InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo).tipo(request.tipo())
                .nota(request.nota()).fecha(request.fecha())
                .cuentaParaPromedio(request.cuentaParaPromedio() != null ? request.cuentaParaPromedio() : true)
                .recuperaA(recuperaA)
                // Solo el gate de Sede tiene resultado propio; en los demás tipos el campo se ignora.
                .aprobado(request.tipo() == TipoInstanciaEvaluativa.TP_INTEGRADOR ? request.aprobado() : null)
                .build();
        return InstanciaEvaluativaResponse.from(instanciaEvaluativaRepository.save(instancia));
    }

    @GetMapping
    public List<InstanciaEvaluativaResponse> listarPorInscripcionYModulo(
            @RequestParam Long inscripcionId, @RequestParam(required = false) Long moduloId) {
        List<InstanciaEvaluativa> instancias = moduloId != null
                ? instanciaEvaluativaRepository.findByInscripcionIdAndModuloId(inscripcionId, moduloId)
                : instanciaEvaluativaRepository.findByInscripcionId(inscripcionId);
        return instancias.stream().map(InstanciaEvaluativaResponse::from).toList();
    }
}