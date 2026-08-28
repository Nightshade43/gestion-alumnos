package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.Persona;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.exception.CategoriaInvalidaException;
import ar.com.ramallo.gestionalumnos.exception.EstadoInvalidoException;
import ar.com.ramallo.gestionalumnos.exception.LimiteClasesExcedidoException;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock private ContratoRepository contratoRepository;
    @Mock private InscripcionRepository inscripcionRepository;

    private ContratoService contratoService;

    private Inscripcion inscripcionParticular;
    private Inscripcion inscripcionEscolar;

    @BeforeEach
    void setUp() {
        contratoService = new ContratoService(contratoRepository, inscripcionRepository);

        Persona persona = Persona.builder().id(1L).nombre("Martin Sosa").build();

        Programa programaParticular = Programa.builder().id(1L).nombre("Ingles IT")
                .categoria(CategoriaPrograma.PARTICULAR).estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE)
                .build();
        inscripcionParticular = Inscripcion.builder().id(1L).persona(persona).programa(programaParticular)
                .fechaInicio(LocalDate.now()).estado(EstadoInscripcion.ACTIVA).build();

        Programa programaEscolar = Programa.builder().id(2L).nombre("Ingles Base")
                .categoria(CategoriaPrograma.ESCOLAR).estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build();
        inscripcionEscolar = Inscripcion.builder().id(2L).persona(persona).programa(programaEscolar)
                .fechaInicio(LocalDate.now()).estado(EstadoInscripcion.ACTIVA).build();

        lenient().when(contratoRepository.save(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaContratoParaInscripcionParticular() {
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcionParticular));

        Contrato resultado = contratoService.crearContrato(1L, TipoFacturacion.PAQUETE, 10);

        assertThat(resultado.getEstado()).isEqualTo(EstadoContrato.ACTIVO);
        assertThat(resultado.getClasesConsumidas()).isZero();
        assertThat(resultado.getClasesContratadas()).isEqualTo(10);
    }

    @Test
    void rechazaContratoParaInscripcionEscolar() {
        when(inscripcionRepository.findById(2L)).thenReturn(Optional.of(inscripcionEscolar));

        assertThatThrownBy(() -> contratoService.crearContrato(2L, TipoFacturacion.MENSUAL, null))
                .isInstanceOf(CategoriaInvalidaException.class);
    }

    @Test
    void fallaSiLaInscripcionNoExiste() {
        when(inscripcionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.crearContrato(99L, TipoFacturacion.POR_CLASE, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void consumirClaseIncrementaElContadorSinLimiteParaPorClase() {
        Contrato contrato = contratoConEstado(TipoFacturacion.POR_CLASE, null, 0, EstadoContrato.ACTIVO);
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        Contrato resultado = contratoService.consumirClase(1L);

        assertThat(resultado.getClasesConsumidas()).isEqualTo(1);
    }

    @Test
    void consumirClasePermiteConsumoDentroDelLimiteDelPaquete() {
        Contrato contrato = contratoConEstado(TipoFacturacion.PAQUETE, 10, 5, EstadoContrato.ACTIVO);
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        Contrato resultado = contratoService.consumirClase(1L);

        assertThat(resultado.getClasesConsumidas()).isEqualTo(6);
    }

    @Test
    void consumirClaseBloqueaAlAlcanzarElLimiteDelPaquete() {
        Contrato contrato = contratoConEstado(TipoFacturacion.PAQUETE, 10, 10, EstadoContrato.ACTIVO);
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.consumirClase(1L))
                .isInstanceOf(LimiteClasesExcedidoException.class);
    }

    @Test
    void consumirClaseFallaSiElContratoEstaFinalizado() {
        Contrato contrato = contratoConEstado(TipoFacturacion.MENSUAL, null, 3, EstadoContrato.FINALIZADO);
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.consumirClase(1L))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void finalizarCambiaElEstadoAFinalizado() {
        Contrato contrato = contratoConEstado(TipoFacturacion.MENSUAL, null, 2, EstadoContrato.ACTIVO);
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        Contrato resultado = contratoService.finalizar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoContrato.FINALIZADO);
    }

    private Contrato contratoConEstado(
            TipoFacturacion tipo, Integer contratadas, Integer consumidas, EstadoContrato estado) {
        return Contrato.builder()
                .id(1L).inscripcion(inscripcionParticular).tipoFacturacion(tipo)
                .clasesContratadas(contratadas).clasesConsumidas(consumidas).estado(estado)
                .build();
    }
}