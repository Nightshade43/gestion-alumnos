package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;

public record ContratoResponse(
        Long id, Long inscripcionId, TipoFacturacion tipoFacturacion,
        Integer clasesContratadas, Integer clasesConsumidas, EstadoContrato estado) {

    public static ContratoResponse from(Contrato contrato) {
        return new ContratoResponse(
                contrato.getId(), contrato.getInscripcion().getId(), contrato.getTipoFacturacion(),
                contrato.getClasesContratadas(), contrato.getClasesConsumidas(), contrato.getEstado());
    }
}