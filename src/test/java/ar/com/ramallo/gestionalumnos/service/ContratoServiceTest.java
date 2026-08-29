package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.exception.CategoriaInvalidaException;
import ar.com.ramallo.gestionalumnos.exception.LimiteClasesExcedidoException;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.exception.RegistroDuplicadoException;
import ar.com.ramallo.gestionalumnos.repository.ContratoRepository;
import ar.com.ramallo.gestionalumnos.repository.EmpresaRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
    @Mock private EmpresaRepository empresaRepository;

    private ContratoService contratoService;

    private Persona persona1;
    private Persona persona2;
    private Programa programaParticular;
    private Programa programaEscolar;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        contratoService = new ContratoService(contratoRepository, inscripcionRepository, empresaRepository);

        persona1 = Persona.builder().id(1L).nombre("Martin Sosa").build();
        persona2 = Persona.builder().id(2L).nombre("Julia Torres").build();
        programaParticular = Programa.builder().id(1L).nombre("Ingles IT")
                .categoria(CategoriaPrograma.PARTICULAR).estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE).build();
        programaEscolar = Programa.builder().id(2L).nombre("Ingles Base")
                .categoria(CategoriaPrograma.ESCOLAR).estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build();
        empresa = Empresa.builder().id(1L).nombre("Acme SA").build();

        lenient().when(contratoRepository.save(any(Contrato.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(inscripcionRepository.save(any(Inscripcion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(inscripcionRepository.saveAll(any()))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaContratoIndividualYLoAsociaALaInscripcion() {
        Inscripcion inscripcion = inscripcionSinContrato(1L, persona1, programaParticular);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));

        Contrato resultado = contratoService.crearContratoIndividual(1L, TipoFacturacion.PAQUETE, 10);

        assertThat(inscripcion.getContrato()).isEqualTo(resultado);
        assertThat(resultado.getEmpresa()).isNull();
    }

    @Test
    void rechazaContratoIndividualParaInscripcionEscolar() {
        Inscripcion inscripcion = inscripcionSinContrato(2L, persona1, programaEscolar);
        when(inscripcionRepository.findById(2L)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> contratoService.crearContratoIndividual(2L, TipoFacturacion.MENSUAL, null))
                .isInstanceOf(CategoriaInvalidaException.class);
    }

    @Test
    void rechazaContratoIndividualSiLaInscripcionYaTieneUno() {
        Inscripcion inscripcion = inscripcionSinContrato(3L, persona1, programaParticular);
        inscripcion.setContrato(Contrato.builder().id(99L).build());
        when(inscripcionRepository.findById(3L)).thenReturn(Optional.of(inscripcion));

        assertThatThrownBy(() -> contratoService.crearContratoIndividual(3L, TipoFacturacion.PAQUETE, 5))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void creaContratoDeEmpresaCubriendoVariasInscripciones() {
        Inscripcion inscripcion1 = inscripcionSinContrato(1L, persona1, programaParticular);
        Inscripcion inscripcion2 = inscripcionSinContrato(2L, persona2, programaParticular);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion1));
        when(inscripcionRepository.findById(2L)).thenReturn(Optional.of(inscripcion2));

        Contrato resultado = contratoService.crearContratoEmpresa(
                1L, List.of(1L, 2L), TipoFacturacion.PAQUETE, 50);

        assertThat(resultado.getEmpresa()).isEqualTo(empresa);
        assertThat(inscripcion1.getContrato()).isEqualTo(resultado);
        assertThat(inscripcion2.getContrato()).isEqualTo(resultado);
    }

    @Test
    void rechazaContratoDeEmpresaSiUnaInscripcionYaTieneContrato() {
        Inscripcion inscripcion1 = inscripcionSinContrato(1L, persona1, programaParticular);
        Inscripcion inscripcion2 = inscripcionSinContrato(2L, persona2, programaParticular);
        inscripcion2.setContrato(Contrato.builder().id(50L).build());
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion1));
        when(inscripcionRepository.findById(2L)).thenReturn(Optional.of(inscripcion2));

        assertThatThrownBy(() -> contratoService.crearContratoEmpresa(1L, List.of(1L, 2L), TipoFacturacion.PAQUETE, 50))
                .isInstanceOf(RegistroDuplicadoException.class);
    }

    @Test
    void fallaSiLaEmpresaNoExiste() {
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contratoService.crearContratoEmpresa(99L, List.of(1L), TipoFacturacion.PAQUETE, 50))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void ampliarCupoSumaALasClasesContratadas() {
        Contrato contrato = Contrato.builder().id(1L).tipoFacturacion(TipoFacturacion.PAQUETE)
                .clasesContratadas(50).clasesConsumidas(48).estado(EstadoContrato.ACTIVO).build();
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        Contrato resultado = contratoService.ampliarCupo(1L, 20);

        assertThat(resultado.getClasesContratadas()).isEqualTo(70);
    }

    @Test
    void consumirClaseSigueRespetandoElLimiteConVariosEmpleados() {
        Contrato contrato = Contrato.builder().id(1L).tipoFacturacion(TipoFacturacion.PAQUETE)
                .clasesContratadas(50).clasesConsumidas(50).estado(EstadoContrato.ACTIVO).build();
        when(contratoRepository.findById(1L)).thenReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoService.consumirClase(1L))
                .isInstanceOf(LimiteClasesExcedidoException.class);
    }

    private Inscripcion inscripcionSinContrato(Long id, Persona persona, Programa programa) {
        return Inscripcion.builder().id(id).persona(persona).programa(programa)
                .fechaInicio(LocalDate.now()).estado(EstadoInscripcion.ACTIVA).build();
    }
}