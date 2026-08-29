package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Institucion;

public record InstitucionResponse(Long id, String nombre) {
    public static InstitucionResponse from(Institucion i) { return new InstitucionResponse(i.getId(), i.getNombre()); }
}