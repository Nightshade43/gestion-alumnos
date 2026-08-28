package ar.com.ramallo.gestionalumnos.web.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SeguimientoRequest(
        @NotNull Long inscripcionId,
        @NotNull LocalDate fecha,
        @NotBlank String observacion) {
}