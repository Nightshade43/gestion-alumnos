package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.Persona;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.Seguimiento;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.exception.CategoriaInvalidaException;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.repository.SeguimientoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguimientoServiceTest {

    @Mock private SeguimientoRepository seguimientoRepository;
    @Mock private InscripcionRepository inscripcionRepository;

    private SeguimientoService seguimientoService;

    @BeforeEach
    void setUp() {
        seguimientoService = new SeguimientoService(seguimientoRepository, inscripcionRepository);
    }

    @Test
    void creaSeguimientoParaInscripcionParticular() {
        Persona persona = Persona.builder().id(1L).nombre("Martin Sosa").build();
        Programa programaParticular = Programa.builder().id(1L).nombre("Ingles IT")
                .categoria(CategoriaPrograma.PARTICULAR).estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE)
                .build();
        Inscripcion inscripcion = Inscripcion.builder().id(1L).persona(persona).programa(programaParticular)
                .fechaInicio(LocalDate.now()).estado(EstadoInscripcion.ACTIVA).build();
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(seguimientoRepository.save(any(Seguimiento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Seguimiento resultado = seguimientoService.crearSeguimiento(1L, LocalDate.now(), "Buen progreso");

        assertThat(resultado.getObservacion()).isEqualTo("Buen progreso");
        assertThat(resultado.getInscripcion()).isEqualTo(inscripcion);
    }

    @Test
    void rechazaSeguimientoParaInscripcionEscolar() {
        Persona persona = Persona.builder().id(1L).nombre("Ana Gomez").build();
        Programa programaEscolar = Programa.builder().id(2L).nombre("Ingles Base")
                .categoria(CategoriaPrograma.ESCOLAR).estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build();
        Inscripcion inscripcion = Inscripcion.builder().id(2L).persona(persona).programa(programaEscolar)
                .fechaInicio(LocalDate.now()).estado(EstadoInscripcion.ACTIVA).build();
        when(inscripcionRepository.findById(2L)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> seguimientoService.crearSeguimiento(2L, LocalDate.now(), "Observacion"))
                .isInstanceOf(CategoriaInvalidaException.class);
    }

    @Test
    void fallaSiLaInscripcionNoExiste() {
        when(inscripcionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seguimientoService.crearSeguimiento(99L, LocalDate.now(), "Observacion"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}