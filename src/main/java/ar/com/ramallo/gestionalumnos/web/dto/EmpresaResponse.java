package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Empresa;

public record EmpresaResponse(Long id, String nombre, String contacto) {
    public static EmpresaResponse from(Empresa e) { return new EmpresaResponse(e.getId(), e.getNombre(), e.getContacto()); }
}