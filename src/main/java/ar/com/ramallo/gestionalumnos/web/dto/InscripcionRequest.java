package ar.com.ramallo.gestionalumnos.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record InscripcionRequest(
        @NotNull Long personaId,
        @NotNull Long programaId,
        Long planId,
        Long grupoId,
        @NotNull LocalDate fechaInicio) {
}