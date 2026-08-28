package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Persona;

public record PersonaResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String documento) {

    public static PersonaResponse from(Persona persona) {
        return new PersonaResponse(
                persona.getId(), persona.getNombre(), persona.getEmail(),
                persona.getTelefono(), persona.getDocumento());
    }
}