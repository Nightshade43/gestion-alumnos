package ar.com.ramallo.gestionalumnos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record InstitucionRequest(@NotBlank String nombre) {}
