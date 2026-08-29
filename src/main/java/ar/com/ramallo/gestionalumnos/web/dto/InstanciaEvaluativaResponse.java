package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstanciaEvaluativaResponse(
        Long id, Long inscripcionId, Long moduloId, TipoInstanciaEvaluativa tipo,
        BigDecimal nota, LocalDate fecha, boolean cuentaParaPromedio, Long recuperaAId) {
    public static InstanciaEvaluativaResponse from(InstanciaEvaluativa i) {
        return new InstanciaEvaluativaResponse(
                i.getId(), i.getInscripcion().getId(), i.getModulo().getId(), i.getTipo(),
                i.getNota(), i.getFecha(), i.isCuentaParaPromedio(),
                i.getRecuperaA() != null ? i.getRecuperaA().getId() : null);
    }
}