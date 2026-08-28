package ar.com.ramallo.gestionalumnos.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PersonaRequest(
        @NotBlank String nombre,
        @Email String email,
        String telefono,
        String documento) {
}