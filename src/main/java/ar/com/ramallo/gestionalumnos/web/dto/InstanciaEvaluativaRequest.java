package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstanciaEvaluativaRequest(
        @NotNull Long inscripcionId, @NotNull Long moduloId, @NotNull TipoInstanciaEvaluativa tipo,
        BigDecimal nota, @NotNull LocalDate fecha, Boolean cuentaParaPromedio, Long recuperaAId) {}
