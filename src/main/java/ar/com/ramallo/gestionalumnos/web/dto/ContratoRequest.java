package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import jakarta.validation.constraints.NotNull;

public record ContratoRequest(
        @NotNull Long inscripcionId,
        @NotNull TipoFacturacion tipoFacturacion,
        Integer clasesContratadas) {
}
