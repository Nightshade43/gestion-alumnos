package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.enums.EstadoModulo;
import ar.com.ramallo.gestionalumnos.service.InscripcionService;
import ar.com.ramallo.gestionalumnos.service.evaluacion.EvaluacionModulo;

import java.math.BigDecimal;

/** {@code notasVigentes} y {@code notasMinimas} solo vienen en CENMA_BASE; en CENMA_SEDE son null. */
public record EstadoModuloResponse(
        Long moduloId, Integer orden, EstadoModulo estado, BigDecimal nota,
        boolean integradorAprobado, Integer notasVigentes, Integer notasMinimas) {

    public static EstadoModuloResponse from(InscripcionService.EstadoDeModulo estadoDeModulo) {
        EvaluacionModulo evaluacion = estadoDeModulo.evaluacion();
        return new EstadoModuloResponse(
                estadoDeModulo.modulo().getId(), estadoDeModulo.modulo().getOrden(), estadoDeModulo.estado(),
                evaluacion.nota(), evaluacion.integradorAprobado(),
                evaluacion.notasVigentes(), evaluacion.notasMinimas());
    }
}
