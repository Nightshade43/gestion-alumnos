package ar.com.ramallo.gestionalumnos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanRequest(@NotNull Long programaId, @NotBlank String codigo, @NotNull Integer moduloInicio) {}
