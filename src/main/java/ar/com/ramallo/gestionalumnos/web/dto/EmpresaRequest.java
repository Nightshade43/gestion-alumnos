package ar.com.ramallo.gestionalumnos.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EmpresaRequest(@NotBlank String nombre, String contacto) {}
