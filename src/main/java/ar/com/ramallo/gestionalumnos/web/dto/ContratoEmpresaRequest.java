package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ContratoEmpresaRequest(
        @NotNull Long empresaId, @NotNull TipoFacturacion tipoFacturacion,
        @NotNull Integer clasesContratadas, @NotEmpty List<Long> inscripcionIds) {}