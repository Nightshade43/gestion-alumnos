package ar.com.ramallo.gestionalumnos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GrupoRequest(@NotNull Long programaId, @NotBlank String dia, @NotBlank String horario) {}
