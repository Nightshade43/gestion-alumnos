package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Seguimiento;

import java.time.LocalDate;

public record SeguimientoResponse(
        Long id, Long inscripcionId, String personaNombre, String programaNombre,
        LocalDate fecha, String observacion) {

    public static SeguimientoResponse from(Seguimiento seguimiento) {
        return new SeguimientoResponse(
                seguimiento.getId(), seguimiento.getInscripcion().getId(),
                seguimiento.getInscripcion().getPersona().getNombre(),
                seguimiento.getInscripcion().getPrograma().getNombre(),
                seguimiento.getFecha(), seguimiento.getObservacion());
    }
}