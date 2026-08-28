package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.Seguimiento;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.exception.CategoriaInvalidaException;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.repository.SeguimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SeguimientoService {

    private final SeguimientoRepository seguimientoRepository;
    private final InscripcionRepository inscripcionRepository;

    @Transactional
    public Seguimiento crearSeguimiento(Long inscripcionId, LocalDate fecha, String observacion) {
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripcion no encontrada: " + inscripcionId));

        if (inscripcion.getPrograma().getCategoria() != CategoriaPrograma.PARTICULAR) {
            throw new CategoriaInvalidaException("Seguimiento solo aplica a inscripciones de categoria PARTICULAR");
        }

        return seguimientoRepository.save(Seguimiento.builder()
                .inscripcion(inscripcion).fecha(fecha).observacion(observacion).build());
    }
}