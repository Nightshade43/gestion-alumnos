package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Grupo;

public record GrupoResponse(Long id, Long programaId, String dia, String horario) {
    public static GrupoResponse from(Grupo g) {
        return new GrupoResponse(g.getId(), g.getPrograma().getId(), g.getDia(), g.getHorario());
    }
}