package ar.com.ramallo.gestionalumnos.web.dto;

import ar.com.ramallo.gestionalumnos.domain.Institucion;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;

import java.util.List;

public record ProgramaResponse(
        Long id,
        String nombre,
        CategoriaPrograma categoria,
        EstrategiaEvaluacion estrategiaEvaluacion,
        Long institucionId,
        String institucionNombre,
        int cantidadModulos,
        int cantidadInscripcionesActivas,
        List<String> diasDeGrupo) {

    public static ProgramaResponse from(
            Programa programa, int cantidadModulos, int cantidadInscripcionesActivas, List<String> diasDeGrupo) {
        Institucion institucion = programa.getInstitucion();
        return new ProgramaResponse(
                programa.getId(), programa.getNombre(), programa.getCategoria(),
                programa.getEstrategiaEvaluacion(),
                institucion != null ? institucion.getId() : null,
                institucion != null ? institucion.getNombre() : null,
                cantidadModulos, cantidadInscripcionesActivas, diasDeGrupo);
    }
}
