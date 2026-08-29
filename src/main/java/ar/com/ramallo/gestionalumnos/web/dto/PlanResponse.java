package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Plan;

public record PlanResponse(Long id, Long programaId, String codigo, Integer moduloInicio) {
    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.getId(), p.getPrograma().getId(), p.getCodigo(), p.getModuloInicio());
    }
}