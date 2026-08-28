package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.exception.EstadoInvalidoException;
import ar.com.ramallo.gestionalumnos.exception.RegistroDuplicadoException;
import ar.com.ramallo.gestionalumnos.exception.RequisitosAcademicosIncompletosException;
import ar.com.ramallo.gestionalumnos.repository.HistorialGrupoRepository;
import ar.com.ramallo.gestionalumnos.repository.InscripcionRepository;
import ar.com.ramallo.gestionalumnos.repository.ModuloRepository;
import ar.com.ramallo.gestionalumnos.service.evaluacion.EstrategiaEvaluacionService;
import ar.com.ramallo.gestionalumnos.service.evaluacion.EvaluacionServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscripcionServiceTest {

    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private HistorialGrupoRepository historialGrupoRepository;
    @Mock private ModuloRepository moduloRepository;
    @Mock private EvaluacionServiceFactory evaluacionServiceFactory;
    @Mock private EstrategiaEvaluacionService estrategiaEvaluacionService;

    private InscripcionService inscripcionService;

    private Persona persona;
    private Programa programaEscolar;
    private Programa programaParticular;

    @BeforeEach
    void setUp() {
        inscripcionService = new InscripcionService(
                inscripcionRepository, historialGrupoRepository, moduloRepository, evaluacionServiceFactory);

        persona = Persona.builder().nombre("Test Persona").build();
        programaEscolar = Programa.builder().id(1L).nombre("Ingles Base").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build();
        programaParticular = Programa.builder().id(2L).nombre("Ingles IT").categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE).build();

        lenient().when(inscripcionRepository.save(any(Inscripcion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void creaInscripcionEscolarCuandoNoHayConflicto() {
        when(inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
                any(), eq(CategoriaPrograma.ESCOLAR), anyList())).thenReturn(false);

        Inscripcion resultado = inscripcionService.crearInscripcion(
                persona, programaEscolar, null, null, LocalDate.now());

        assertThat(resultado.getEstado()).isEqualTo(EstadoInscripcion.ACTIVA);
        assertThat(resultado.getPersona()).isEqualTo(persona);
        verify(historialGrupoRepository, never()).save(any());
    }

    @Test
    void rechazaSegundaInscripcionEscolarActivaParaLaMismaPersona() {
        when(inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
                any(), eq(CategoriaPrograma.ESCOLAR), anyList())).thenReturn(true);

        assertThatThrownBy(() -> inscripcionService.crearInscripcion(
                persona, programaEscolar, null, null, LocalDate.now()))
                .isInstanceOf(RegistroDuplicadoException.class);

        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void noValidaUnicidadParaCategoriaParticular() {
        Inscripcion resultado = inscripcionService.crearInscripcion(
                persona, programaParticular, null, null, LocalDate.now());

        assertThat(resultado.getPrograma()).isEqualTo(programaParticular);
        verify(inscripcionRepository, never())
                .existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(any(), any(), anyList());
    }

    @Test
    void abreHistorialGrupoAlCrearInscripcionConGrupoAsignado() {
        Grupo grupo = Grupo.builder().dia("Lunes").horario("19:00-21:00").programa(programaEscolar).build();
        when(inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
                any(), eq(CategoriaPrograma.ESCOLAR), anyList())).thenReturn(false);

        inscripcionService.crearInscripcion(persona, programaEscolar, null, grupo, LocalDate.now());

        ArgumentCaptor<HistorialGrupo> captor = ArgumentCaptor.forClass(HistorialGrupo.class);
        verify(historialGrupoRepository).save(captor.capture());
        assertThat(captor.getValue().getGrupo()).isEqualTo(grupo);
        assertThat(captor.getValue().getFechaHasta()).isNull();
    }

    @Test
    void pausaUnaInscripcionActiva() {
        Inscripcion inscripcionActiva = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcionActiva));

        Inscripcion resultado = inscripcionService.pausar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoInscripcion.PAUSADA);
    }

    @Test
    void reanudaUnaInscripcionPausada() {
        Inscripcion inscripcionPausada = inscripcionConEstado(EstadoInscripcion.PAUSADA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcionPausada));

        Inscripcion resultado = inscripcionService.reanudar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void cancelarDesdePausadaSeteaFechaFin() {
        Inscripcion inscripcionPausada = inscripcionConEstado(EstadoInscripcion.PAUSADA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcionPausada));

        Inscripcion resultado = inscripcionService.cancelar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoInscripcion.CANCELADA);
        assertThat(resultado.getFechaFin()).isNotNull();
    }

    @Test
    void noPermitePausadaAFinalizada() {
        Inscripcion inscripcionPausada = inscripcionConEstado(EstadoInscripcion.PAUSADA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcionPausada));

        assertThatThrownBy(() -> inscripcionService.finalizar(1L))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void noPermiteNingunaTransicionDesdeEstadosTerminales() {
        Inscripcion inscripcionFinalizada = inscripcionConEstado(EstadoInscripcion.FINALIZADA);
        Inscripcion inscripcionCancelada = inscripcionConEstado(EstadoInscripcion.CANCELADA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcionFinalizada));
        when(inscripcionRepository.findById(2L)).thenReturn(Optional.of(inscripcionCancelada));

        assertThatThrownBy(() -> inscripcionService.pausar(1L)).isInstanceOf(EstadoInvalidoException.class);
        assertThatThrownBy(() -> inscripcionService.reanudar(2L)).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void cambiarGrupoCierraElRegistroAbiertoYAbreUnoNuevo() {
        Grupo grupoViejo = Grupo.builder().dia("Lunes").horario("19:00-21:00").programa(programaEscolar).build();
        Grupo grupoNuevo = Grupo.builder().dia("Martes").horario("19:00-21:00").programa(programaEscolar).build();
        Inscripcion inscripcion = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setGrupo(grupoViejo);
        HistorialGrupo registroAbierto = HistorialGrupo.builder()
                .inscripcion(inscripcion).grupo(grupoViejo).fechaDesde(LocalDate.now().minusMonths(1)).build();

        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(historialGrupoRepository.findByInscripcionIdAndFechaHastaIsNull(1L))
                .thenReturn(Optional.of(registroAbierto));

        inscripcionService.cambiarGrupo(1L, grupoNuevo);

        assertThat(registroAbierto.getFechaHasta()).isEqualTo(LocalDate.now());
        ArgumentCaptor<HistorialGrupo> captor = ArgumentCaptor.forClass(HistorialGrupo.class);
        verify(historialGrupoRepository, times(2)).save(captor.capture());
        HistorialGrupo nuevoRegistro = captor.getAllValues().get(1);
        assertThat(nuevoRegistro.getGrupo()).isEqualTo(grupoNuevo);
        assertThat(nuevoRegistro.getFechaHasta()).isNull();
    }

    @Test
    void lanzaExcepcionSiLaInscripcionNoExiste() {
        when(inscripcionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscripcionService.pausar(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Tests nuevos: validacion academica al finalizar ---

    @Test
    void bloqueaFinalizacionSiNoTodosLosModulosEscolaresEstanAprobados() {
        Modulo modulo1 = Modulo.builder().id(10L).orden(1).esSecuencial(false).programa(programaEscolar).build();
        Modulo modulo2 = Modulo.builder().id(11L).orden(2).esSecuencial(false).programa(programaEscolar).build();
        Inscripcion inscripcion = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(moduloRepository.findByProgramaIdOrderByOrden(any())).thenReturn(List.of(modulo1, modulo2));
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);
        when(estrategiaEvaluacionService.moduloAprobado(inscripcion, modulo1)).thenReturn(true);
        when(estrategiaEvaluacionService.moduloAprobado(inscripcion, modulo2)).thenReturn(false);

        assertThatThrownBy(() -> inscripcionService.finalizar(1L))
                .isInstanceOf(RequisitosAcademicosIncompletosException.class);

        verify(inscripcionRepository, never()).save(argThat(i -> i.getEstado() == EstadoInscripcion.FINALIZADA));
    }

    @Test
    void permiteFinalizarCuandoTodosLosModulosEscolaresEstanAprobados() {
        Modulo modulo1 = Modulo.builder().id(20L).orden(1).esSecuencial(false).programa(programaEscolar).build();
        Modulo modulo2 = Modulo.builder().id(21L).orden(2).esSecuencial(false).programa(programaEscolar).build();
        Inscripcion inscripcion = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(moduloRepository.findByProgramaIdOrderByOrden(any())).thenReturn(List.of(modulo1, modulo2));
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);
        when(estrategiaEvaluacionService.moduloAprobado(eq(inscripcion), any(Modulo.class))).thenReturn(true);

        Inscripcion resultado = inscripcionService.finalizar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoInscripcion.FINALIZADA);
        assertThat(resultado.getFechaFin()).isEqualTo(LocalDate.now());
    }

    @Test
    void finalizarParticularNoValidaModulos() {
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programaParticular)
                .fechaInicio(LocalDate.now().minusMonths(1)).estado(EstadoInscripcion.ACTIVA).build();
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));

        Inscripcion resultado = inscripcionService.finalizar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoInscripcion.FINALIZADA);
        verify(moduloRepository, never()).findByProgramaIdOrderByOrden(any());
        verify(evaluacionServiceFactory, never()).resolver(any());
    }

    @Test
    void filtraModulosEximidosSegunPlanAlValidarFinalizacion() {
        Plan planB = Plan.builder().codigo("B").moduloInicio(4).programa(programaEscolar).build();
        List<Modulo> todosLosModulos = List.of(
                Modulo.builder().id(30L).orden(1).esSecuencial(true).programa(programaEscolar).build(),
                Modulo.builder().id(31L).orden(2).esSecuencial(true).programa(programaEscolar).build(),
                Modulo.builder().id(32L).orden(3).esSecuencial(true).programa(programaEscolar).build(),
                Modulo.builder().id(33L).orden(4).esSecuencial(true).programa(programaEscolar).build(),
                Modulo.builder().id(34L).orden(5).esSecuencial(true).programa(programaEscolar).build(),
                Modulo.builder().id(35L).orden(6).esSecuencial(true).programa(programaEscolar).build(),
                Modulo.builder().id(36L).orden(7).esSecuencial(true).programa(programaEscolar).build());
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programaEscolar).plan(planB)
                .fechaInicio(LocalDate.now().minusMonths(1)).estado(EstadoInscripcion.ACTIVA).build();

        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(moduloRepository.findByProgramaIdOrderByOrden(any())).thenReturn(todosLosModulos);
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);
        when(estrategiaEvaluacionService.moduloAprobado(eq(inscripcion), any(Modulo.class))).thenReturn(true);

        inscripcionService.finalizar(1L);

        // Plan B exime modulos 1-3: solo deben consultarse los modulos 4 a 7 (4 en total)
        verify(estrategiaEvaluacionService, times(4)).moduloAprobado(eq(inscripcion), any(Modulo.class));
    }

    @Test
    void bloqueaFinalizacionSiElProgramaNoTieneModulosConfigurados() {
        Inscripcion inscripcion = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(1L)).thenReturn(Optional.of(inscripcion));
        when(moduloRepository.findByProgramaIdOrderByOrden(any())).thenReturn(List.of());
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);

        assertThatThrownBy(() -> inscripcionService.finalizar(1L))
                .isInstanceOf(RequisitosAcademicosIncompletosException.class);
    }

    private Inscripcion inscripcionConEstado(EstadoInscripcion estado) {
        return Inscripcion.builder()
                .persona(persona).programa(programaEscolar).fechaInicio(LocalDate.now().minusMonths(1))
                .estado(estado).build();
    }

    private static List<EstadoInscripcion> anyList() {
        return anyList0();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> anyList0() {
        return any(List.class);
    }
}