package ar.com.ramallo.gestionalumnos.web.dto;

import jakarta.validation.constraints.NotNull;

public record ModuloRequest(@NotNull Long programaId, @NotNull Integer orden, @NotNull Boolean esSecuencial) {}
