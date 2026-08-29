package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Modulo;

public record ModuloResponse(Long id, Long programaId, Integer orden, boolean esSecuencial) {
    public static ModuloResponse from(Modulo m) {
        return new ModuloResponse(m.getId(), m.getPrograma().getId(), m.getOrden(), m.isEsSecuencial());
    }
}
