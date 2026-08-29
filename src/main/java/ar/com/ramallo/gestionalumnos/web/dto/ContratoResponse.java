package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Empresa;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;

import java.util.List;

public record ContratoResponse(
        Long id, Long empresaId, String empresaNombre, TipoFacturacion tipoFacturacion,
        Integer clasesContratadas, Integer clasesConsumidas, EstadoContrato estado,
        List<EmpleadoCubierto> inscripciones) {

    public record EmpleadoCubierto(Long inscripcionId, String personaNombre) {}

    public static ContratoResponse from(Contrato c, List<Inscripcion> inscripciones) {
        Empresa empresa = c.getEmpresa();
        return new ContratoResponse(
                c.getId(), empresa != null ? empresa.getId() : null, empresa != null ? empresa.getNombre() : null,
                c.getTipoFacturacion(), c.getClasesContratadas(), c.getClasesConsumidas(), c.getEstado(),
                inscripciones.stream()
                        .map(i -> new EmpleadoCubierto(i.getId(), i.getPersona().getNombre())).toList());
    }
}