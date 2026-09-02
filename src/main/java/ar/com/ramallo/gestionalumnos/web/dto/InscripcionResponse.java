package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.Plan;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;

import java.time.LocalDate;

public record InscripcionResponse(
        Long id, Long personaId, String personaNombre, Long programaId, String programaNombre,
        CategoriaPrograma categoria, String planCodigo, String grupoDia, String grupoHorario,
        LocalDate fechaInicio, LocalDate fechaFin, EstadoInscripcion estado, Long contratoId) {

    public static InscripcionResponse from(Inscripcion inscripcion) {
        Plan plan = inscripcion.getPlan();
        Grupo grupo = inscripcion.getGrupo();
        Contrato contrato = inscripcion.getContrato();
        return new InscripcionResponse(
                inscripcion.getId(),
                inscripcion.getPersona().getId(), inscripcion.getPersona().getNombre(),
                inscripcion.getPrograma().getId(), inscripcion.getPrograma().getNombre(),
                inscripcion.getPrograma().getCategoria(),
                plan != null ? plan.getCodigo() : null,
                grupo != null ? grupo.getDia() : null,
                grupo != null ? grupo.getHorario() : null,
                inscripcion.getFechaInicio(), inscripcion.getFechaFin(), inscripcion.getEstado(),
                contrato != null ? contrato.getId() : null);
    }
}