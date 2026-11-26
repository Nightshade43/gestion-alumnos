package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import ar.com.ramallo.gestionalumnos.repository.InstanciaEvaluativaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CenmaSedeEvaluacionServiceTest {

    @Mock private InstanciaEvaluativaRepository instanciaEvaluativaRepository;

    private CenmaSedeEvaluacionService service;
    private Inscripcion inscripcion;
    private Modulo modulo;

    @BeforeEach
    void setUp() {
        service = new CenmaSedeEvaluacionService(instanciaEvaluativaRepository);

        Programa programa = Programa.builder().id(1L).nombre("Ingles Sede")
                .categoria(CategoriaPrograma.ESCOLAR).estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build();
        modulo = Modulo.builder().id(1L).orden(1).esSecuencial(true).programa(programa).build();
        Persona persona = Persona.builder().id(1L).nombre("Test").build();
        inscripcion = Inscripcion.builder().id(1L).persona(persona).programa(programa)
                .fechaInicio(LocalDate.now()).build();
    }

    @Test
    void moduloNoAprobadoSiElTpNoEstaAprobado() {
        mockearTp(instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "4.00", LocalDate.now()));

        assertThat(service.moduloAprobado(inscripcion, modulo)).isFalse();
    }

    @Test
    void moduloNoAprobadoSiElTpEstaAprobadoPeroNoHayEvaluacionFinal() {
        mockearTp(instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now()));
        mockearFinal();

        assertThat(service.moduloAprobado(inscripcion, modulo)).isFalse();
    }

    @Test
    void moduloAprobadoSiTpYEvaluacionFinalAprobados() {
        mockearTp(instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now()));
        mockearFinal(instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "6.00", LocalDate.now()));

        assertThat(service.moduloAprobado(inscripcion, modulo)).isTrue();
    }

    @Test
    void moduloNoAprobadoSiLaEvaluacionFinalEstaDesaprobada() {
        mockearTp(instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now()));
        mockearFinal(instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "5.00", LocalDate.now()));

        assertThat(service.moduloAprobado(inscripcion, modulo)).isFalse();
    }

    @Test
    void notaModuloDevuelveLaNotaDeLaEvaluacionFinal() {
        mockearFinal(instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "8.00", LocalDate.now()));

        assertThat(service.notaModulo(inscripcion, modulo)).contains(new BigDecimal("8.00"));
    }

    @Test
    void notaModuloVacioSinEvaluacionFinalCargada() {
        mockearFinal();

        assertThat(service.notaModulo(inscripcion, modulo)).isEmpty();
    }

    @Test
    void tpAprobadoTomaLaInstanciaMasReciente() {
        mockearTp(
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "3.00", LocalDate.now().minusDays(5)),
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now())
        );

        assertThat(service.tpAprobado(inscripcion, modulo)).isTrue();
    }

    @Test
    void tpAprobadoUsaElCampoAprobadoAunSinNota() {
        InstanciaEvaluativa tp = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "0.00", LocalDate.now());
        tp.setNota(null);
        tp.setAprobado(true);
        mockearTp(tp);

        assertThat(service.tpAprobado(inscripcion, modulo)).isTrue();
    }

    @Test
    void elCampoAprobadoManda_porSobreLaNota() {
        InstanciaEvaluativa tp = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "10.00", LocalDate.now());
        tp.setAprobado(false);
        mockearTp(tp);

        assertThat(service.tpAprobado(inscripcion, modulo)).isFalse();
    }

    @Test
    void tpSinNotaNiAprobadoNoCuentaComoAprobado() {
        InstanciaEvaluativa tp = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "0.00", LocalDate.now());
        tp.setNota(null);
        mockearTp(tp);

        assertThat(service.tpAprobado(inscripcion, modulo)).isFalse();
    }

    // --- Versión sobre instancias ya cargadas (evaluación por lotes): misma regla, sin tocar el repositorio ---

    @Test
    void porListaAprobadoConTpYEvaluacionFinalAprobados() {
        assertThat(service.moduloAprobado(List.of(
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now()),
                instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "6.00", LocalDate.now())))).isTrue();
    }

    @Test
    void porListaNoAprobadoSiFaltaElTpLaFinalOSiEstaDesaprobada() {
        InstanciaEvaluativa tp = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now());
        InstanciaEvaluativa finalAprobada = instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "8.00", LocalDate.now());

        assertThat(service.moduloAprobado(List.of(finalAprobada))).isFalse();
        assertThat(service.moduloAprobado(List.of(tp))).isFalse();
        assertThat(service.moduloAprobado(List.of(
                tp, instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "5.00", LocalDate.now())))).isFalse();
        assertThat(service.moduloAprobado(List.of())).isFalse();
    }

    @Test
    void porListaVale_laInstanciaMasRecienteDeCadaTipo() {
        assertThat(service.moduloAprobado(List.of(
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "3.00", LocalDate.now().minusDays(9)),
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now()),
                instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "4.00", LocalDate.now().minusDays(3)),
                instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "6.00", LocalDate.now())))).isTrue();
        assertThat(service.moduloAprobado(List.of(
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now().minusDays(9)),
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "3.00", LocalDate.now()),
                instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "9.00", LocalDate.now())))).isFalse();
    }

    @Test
    void porListaElCampoAprobadoMandaSobreLaNotaDelTp() {
        InstanciaEvaluativa tp = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "10.00", LocalDate.now());
        tp.setAprobado(false);

        assertThat(service.moduloAprobado(List.of(
                tp, instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "9.00", LocalDate.now())))).isFalse();
    }

    @Test
    void porListaIgnoraInstanciasDeOtrosTipos() {
        assertThat(service.moduloAprobado(List.of(
                instancia(TipoInstanciaEvaluativa.NOTA, "10.00", LocalDate.now()),
                instancia(TipoInstanciaEvaluativa.INTEGRADOR, "10.00", LocalDate.now())))).isFalse();
    }

    @Test
    void laVersionPorListaCoincideConLaQueConsultaLaBase() {
        InstanciaEvaluativa tp = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now());
        InstanciaEvaluativa evaluacionFinal = instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "6.00", LocalDate.now());
        mockearTp(tp);
        mockearFinal(evaluacionFinal);

        assertThat(service.moduloAprobado(inscripcion, modulo)).isEqualTo(service.moduloAprobado(List.of(tp, evaluacionFinal)));
    }

    // --- evaluarModulo: lo que recibe la pantalla de la inscripción ---

    @Test
    void evaluarModuloTomaLaFinalMasRecientePorFechaNoLaUltimaDeLaLista() {
        // El repositorio no ordena: la final más reciente (8) puede llegar antes que una anterior (4).
        // El frontend tomaba la última de la lista y daba el módulo por desaprobado.
        EvaluacionModulo resultado = service.evaluarModulo(List.of(
                instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "10.00", LocalDate.now().minusDays(40)),
                instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "8.00", LocalDate.now()),
                instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "4.00", LocalDate.now().minusDays(20))));

        assertThat(resultado.nota()).isEqualByComparingTo("8.00");
        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.integradorAprobado()).isTrue();
    }

    @Test
    void evaluarModuloConFinalAprobadaPeroTpNoAprobadoNoApruebaYLoInforma() {
        InstanciaEvaluativa tp = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "10.00", LocalDate.now());
        tp.setAprobado(false);

        EvaluacionModulo resultado = service.evaluarModulo(List.of(
                tp, instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "9.00", LocalDate.now())));

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.integradorAprobado()).isFalse();
        assertThat(resultado.nota()).isEqualByComparingTo("9.00");
    }

    @Test
    void evaluarModuloEnSedeNoInformaConteoDeNotas() {
        EvaluacionModulo resultado = service.evaluarModulo(List.of());

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.nota()).isNull();
        assertThat(resultado.integradorAprobado()).isFalse();
        assertThat(resultado.notasVigentes()).isNull();
        assertThat(resultado.notasMinimas()).isNull();
    }

    @Test
    void evaluarModuloCoincideConModuloAprobadoPorLista() {
        InstanciaEvaluativa tpRechazado = instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "10.00", LocalDate.now());
        tpRechazado.setAprobado(false);
        List<List<InstanciaEvaluativa>> casos = List.of(
                List.of(instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now()),
                        instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "6.00", LocalDate.now())),
                List.of(instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "9.00", LocalDate.now())),
                List.of(tpRechazado, instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "9.00", LocalDate.now())),
                List.of(instancia(TipoInstanciaEvaluativa.TP_INTEGRADOR, "7.00", LocalDate.now()),
                        instancia(TipoInstanciaEvaluativa.EVALUACION_FINAL, "5.00", LocalDate.now())),
                List.of());

        for (List<InstanciaEvaluativa> caso : casos) {
            assertThat(service.evaluarModulo(caso).aprobado()).isEqualTo(service.moduloAprobado(caso));
        }
    }

    private InstanciaEvaluativa instancia(TipoInstanciaEvaluativa tipo, String nota, LocalDate fecha) {
        return InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo).tipo(tipo)
                .nota(new BigDecimal(nota)).fecha(fecha).build();
    }

    private void mockearTp(InstanciaEvaluativa... instancias) {
        when(instanciaEvaluativaRepository.findByInscripcionIdAndModuloIdAndTipo(
                1L, 1L, TipoInstanciaEvaluativa.TP_INTEGRADOR)).thenReturn(List.of(instancias));
    }

    private void mockearFinal(InstanciaEvaluativa... instancias) {
        when(instanciaEvaluativaRepository.findByInscripcionIdAndModuloIdAndTipo(
                1L, 1L, TipoInstanciaEvaluativa.EVALUACION_FINAL)).thenReturn(List.of(instancias));
    }
}