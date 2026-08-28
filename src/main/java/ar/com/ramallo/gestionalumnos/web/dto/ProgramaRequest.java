package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import jakarta.validation.constraints.NotNull;

public record ProgramaRequest(
        @NotNull String nombre,
        @NotNull CategoriaPrograma categoria,
        @NotNull EstrategiaEvaluacion estrategiaEvaluacion,
        Long institucionId) {
}