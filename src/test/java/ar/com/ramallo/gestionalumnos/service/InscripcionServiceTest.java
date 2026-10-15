package ar.com.ramallo.gestionalumnos.service;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.exception.EstadoInvalidoException;
import ar.com.ramallo.gestionalumnos.exception.RecursoNoEncontradoException;
import ar.com.ramallo.gestionalumnos.exception.RegistroDuplicadoException;
import ar.com.ramallo.gestionalumnos.exception.RequisitosAcademicosIncompletosException;
import ar.com.ramallo.gestionalumnos.repository.*;
import ar.com.ramallo.gestionalumnos.service.evaluacion.EstrategiaEvaluacionService;
import ar.com.ramallo.gestionalumnos.service.evaluacion.EvaluacionServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
    // Mocks nuevos, junto a los que ya tenías
    @Mock private PersonaRepository personaRepository;
    @Mock private ProgramaRepository programaRepository;
    @Mock private PlanRepository planRepository;
    @Mock private GrupoRepository grupoRepository;
    @Mock private InstanciaEvaluativaRepository instanciaEvaluativaRepository;

    private InscripcionService inscripcionService;

    private Persona persona;
    private Programa programaEscolar;
    private Programa programaParticular;

    @BeforeEach
    void setUp() {
// setUp() actualizado
        inscripcionService = new InscripcionService(
                inscripcionRepository, historialGrupoRepository, moduloRepository, evaluacionServiceFactory,
                personaRepository, programaRepository, planRepository, grupoRepository,
                instanciaEvaluativaRepository);

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

    // El test existente cambia de excepción esperada
    @Test
    void lanzaExcepcionSiLaInscripcionNoExiste() {
        when(inscripcionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscripcionService.pausar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);
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

    @Test
    void evaluarEstadoAcademicoEsSiemprePuedeFinalizarParaParticulares() {
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programaParticular)
                .fechaInicio(LocalDate.now().minusMonths(1)).estado(EstadoInscripcion.ACTIVA).build();

        InscripcionService.EstadoAcademico resultado = inscripcionService.evaluarEstadoAcademico(inscripcion);

        assertThat(resultado.puedeFinalizar()).isTrue();
        assertThat(resultado.modulosPendientes()).isZero();
        verify(moduloRepository, never()).findByProgramaIdOrderByOrden(any());
        verify(evaluacionServiceFactory, never()).resolver(any());
    }

    @Test
    void evaluarEstadoAcademicoCuentaModulosPendientesCorrectamente() {
        Modulo modulo1 = Modulo.builder().id(40L).orden(1).esSecuencial(false).programa(programaEscolar).build();
        Modulo modulo2 = Modulo.builder().id(41L).orden(2).esSecuencial(false).programa(programaEscolar).build();
        Modulo modulo3 = Modulo.builder().id(42L).orden(3).esSecuencial(false).programa(programaEscolar).build();
        Inscripcion inscripcion = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        when(moduloRepository.findByProgramaIdOrderByOrden(any())).thenReturn(List.of(modulo1, modulo2, modulo3));
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);
        when(estrategiaEvaluacionService.moduloAprobado(inscripcion, modulo1)).thenReturn(true);
        when(estrategiaEvaluacionService.moduloAprobado(inscripcion, modulo2)).thenReturn(false);
        when(estrategiaEvaluacionService.moduloAprobado(inscripcion, modulo3)).thenReturn(false);

        InscripcionService.EstadoAcademico resultado = inscripcionService.evaluarEstadoAcademico(inscripcion);

        assertThat(resultado.puedeFinalizar()).isFalse();
        assertThat(resultado.modulosPendientes()).isEqualTo(2);
    }

    @Test
    void evaluarEstadoAcademicoDevuelvePuedeFinalizarTrueSinPendientesCuandoTodoAprobado() {
        Modulo modulo1 = Modulo.builder().id(50L).orden(1).esSecuencial(false).programa(programaEscolar).build();
        Modulo modulo2 = Modulo.builder().id(51L).orden(2).esSecuencial(false).programa(programaEscolar).build();
        Inscripcion inscripcion = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        when(moduloRepository.findByProgramaIdOrderByOrden(any())).thenReturn(List.of(modulo1, modulo2));
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);
        when(estrategiaEvaluacionService.moduloAprobado(eq(inscripcion), any(Modulo.class))).thenReturn(true);

        InscripcionService.EstadoAcademico resultado = inscripcionService.evaluarEstadoAcademico(inscripcion);

        assertThat(resultado.puedeFinalizar()).isTrue();
        assertThat(resultado.modulosPendientes()).isZero();
    }

    @Test
    void evaluarEstadoAcademicoDevuelveFalseYCeroPendientesSiNoHayModulosConfigurados() {
        Inscripcion inscripcion = inscripcionConEstado(EstadoInscripcion.ACTIVA);
        when(moduloRepository.findByProgramaIdOrderByOrden(any())).thenReturn(List.of());
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);

        InscripcionService.EstadoAcademico resultado = inscripcionService.evaluarEstadoAcademico(inscripcion);

        assertThat(resultado.puedeFinalizar()).isFalse();
        assertThat(resultado.modulosPendientes()).isZero();
    }

    // --- Evaluación por lotes (listado de inscripciones) ---

    private Inscripcion inscripcionEscolar(Long id, Plan plan) {
        return Inscripcion.builder().id(id).persona(persona).programa(programaEscolar).plan(plan)
                .fechaInicio(LocalDate.now().minusMonths(1)).estado(EstadoInscripcion.ACTIVA).build();
    }

    private InstanciaEvaluativa instanciaDe(Inscripcion inscripcion, Modulo modulo) {
        return InstanciaEvaluativa.builder().inscripcion(inscripcion).modulo(modulo)
                .tipo(TipoInstanciaEvaluativa.NOTA).nota(new java.math.BigDecimal("8.00")).fecha(LocalDate.now()).build();
    }

    @Test
    void evaluarEstadosAcademicosEvaluaTodoElListadoConUnaConsultaDeModulosYUnaDeInstancias() {
        Modulo modulo1 = Modulo.builder().id(60L).orden(1).esSecuencial(false).programa(programaEscolar).build();
        Modulo modulo2 = Modulo.builder().id(61L).orden(2).esSecuencial(false).programa(programaEscolar).build();
        Plan desdeModulo2 = Plan.builder().id(5L).codigo("B").moduloInicio(2).programa(programaEscolar).build();
        Inscripcion ana = inscripcionEscolar(100L, null);
        Inscripcion bruno = inscripcionEscolar(101L, desdeModulo2);
        Inscripcion particular = Inscripcion.builder().id(102L).persona(persona).programa(programaParticular)
                .fechaInicio(LocalDate.now()).estado(EstadoInscripcion.ACTIVA).build();
        InstanciaEvaluativa deAnaEnModulo1 = instanciaDe(ana, modulo1);
        InstanciaEvaluativa deBrunoEnModulo2 = instanciaDe(bruno, modulo2);

        when(moduloRepository.findByProgramaIdInOrderByOrden(List.of(1L))).thenReturn(List.of(modulo1, modulo2));
        when(instanciaEvaluativaRepository.findByInscripcionIdIn(List.of(100L, 101L)))
                .thenReturn(List.of(deAnaEnModulo1, deBrunoEnModulo2));
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);
        // Un módulo se da por aprobado si llegó con instancias; sin instancias (lista vacía) queda pendiente.
        // any() y no anyList(): esta clase define su propio anyList() de EstadoInscripcion (ver el final del archivo).
        when(estrategiaEvaluacionService.moduloAprobado(ArgumentMatchers.<List<InstanciaEvaluativa>>any()))
                .thenAnswer(invocacion -> !invocacion.<List<?>>getArgument(0).isEmpty());

        List<InscripcionService.EstadoAcademico> resultado =
                inscripcionService.evaluarEstadosAcademicos(List.of(ana, particular, bruno));

        assertThat(resultado).containsExactly(
                new InscripcionService.EstadoAcademico(false, 1),   // ana: módulo 1 aprobado, falta el 2
                new InscripcionService.EstadoAcademico(true, 0),    // particular: no se evalúa
                new InscripcionService.EstadoAcademico(true, 0));   // bruno: el plan B exige solo el módulo 2
        verify(moduloRepository, times(1)).findByProgramaIdInOrderByOrden(any());
        verify(instanciaEvaluativaRepository, times(1)).findByInscripcionIdIn(any());
        verify(moduloRepository, never()).findByProgramaIdOrderByOrden(any());
        verify(instanciaEvaluativaRepository, never()).findByInscripcionId(any());
        verify(instanciaEvaluativaRepository, never()).findByInscripcionIdAndModuloId(any(), any());
    }

    @Test
    void evaluarEstadosAcademicosNoConsultaNadaSiNoHayInscripcionesEscolares() {
        Inscripcion particular = Inscripcion.builder().id(102L).persona(persona).programa(programaParticular)
                .fechaInicio(LocalDate.now()).estado(EstadoInscripcion.ACTIVA).build();

        assertThat(inscripcionService.evaluarEstadosAcademicos(List.of(particular)))
                .containsExactly(new InscripcionService.EstadoAcademico(true, 0));
        assertThat(inscripcionService.evaluarEstadosAcademicos(List.of())).isEmpty();
        verifyNoInteractions(moduloRepository, instanciaEvaluativaRepository, evaluacionServiceFactory);
    }

    @Test
    void evaluarEstadosAcademicosConProgramaSinModulosNoPuedeFinalizarNiTienePendientes() {
        Inscripcion ana = inscripcionEscolar(100L, null);
        when(moduloRepository.findByProgramaIdInOrderByOrden(any())).thenReturn(List.of());
        when(instanciaEvaluativaRepository.findByInscripcionIdIn(any())).thenReturn(List.of());
        when(evaluacionServiceFactory.resolver(programaEscolar)).thenReturn(estrategiaEvaluacionService);

        assertThat(inscripcionService.evaluarEstadosAcademicos(List.of(ana)))
                .containsExactly(new InscripcionService.EstadoAcademico(false, 0));
    }

    private Inscripcion inscripcionConEstado(EstadoInscripcion estado) {
        return Inscripcion.builder()
                .persona(persona).programa(programaEscolar).fechaInicio(LocalDate.now().minusMonths(1))
                .estado(estado).build();
    }

    // Tests nuevos para la sobrecarga que resuelve ids
    @Test
    void crearInscripcionPorIdsResuelveLasCuatroRelaciones() {
        Programa programa = Programa.builder().id(1L).nombre("Ingles Sede").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build();
        Plan plan = Plan.builder().id(1L).codigo("A").moduloInicio(1).programa(programa).build();
        Grupo grupo = Grupo.builder().id(1L).dia("Miercoles").horario("18:00-19:30").programa(programa).build();

        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programa));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(grupoRepository.findById(1L)).thenReturn(Optional.of(grupo));
        when(inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(any(), any(), anyList()))
                .thenReturn(false);

        Inscripcion resultado = inscripcionService.crearInscripcion(1L, 1L, 1L, 1L, LocalDate.now());

        assertThat(resultado.getPlan()).isEqualTo(plan);
        assertThat(resultado.getGrupo()).isEqualTo(grupo);
    }

    @Test
    void crearInscripcionPorIdsFallaSiLaPersonaNoExiste() {
        when(personaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscripcionService.crearInscripcion(99L, 1L, null, null, LocalDate.now()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearInscripcionPorIdsFallaSiElProgramaNoExiste() {
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(programaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscripcionService.crearInscripcion(1L, 99L, null, null, LocalDate.now()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearInscripcionPorIdsNoResuelvePlanNiGrupoSiVienenNulos() {
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(programaRepository.findById(1L)).thenReturn(Optional.of(programaParticular));

        inscripcionService.crearInscripcion(1L, 1L, null, null, LocalDate.now());

        verify(planRepository, never()).findById(any());
        verify(grupoRepository, never()).findById(any());
    }

    private static List<EstadoInscripcion> anyList() {
        return anyList0();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> anyList0() {
        return any(List.class);
    }
}