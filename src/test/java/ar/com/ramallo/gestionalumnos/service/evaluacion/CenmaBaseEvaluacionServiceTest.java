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
class CenmaBaseEvaluacionServiceTest {

    @Mock private InstanciaEvaluativaRepository instanciaEvaluativaRepository;

    private CenmaBaseEvaluacionService service;
    private Inscripcion inscripcion;
    private Modulo modulo;

    @BeforeEach
    void setUp() {
        service = new CenmaBaseEvaluacionService(instanciaEvaluativaRepository);

        Programa programa = Programa.builder().id(1L).nombre("Ingles Base")
                .categoria(CategoriaPrograma.ESCOLAR).estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build();
        modulo = Modulo.builder().id(1L).orden(1).esSecuencial(false).programa(programa).build();
        Persona persona = Persona.builder().id(1L).nombre("Test").build();
        inscripcion = Inscripcion.builder().id(1L).persona(persona).programa(programa)
                .fechaInicio(LocalDate.now()).build();
    }

    @Test
    void moduloNoAprobadoConMenosDeTresNotas() {
        mockearInstancias(nota("6.00"), nota("7.00"));

        assertThat(service.moduloAprobado(inscripcion, modulo)).isFalse();
    }

    @Test
    void moduloAprobadoConPromedioMayorOIgualASeisYAlMenosTresNotas() {
        mockearInstancias(nota("6.00"), nota("7.00"), nota("8.00"));

        assertThat(service.moduloAprobado(inscripcion, modulo)).isTrue();
    }

    @Test
    void moduloNoAprobadoConPromedioMenorASeis() {
        mockearInstancias(nota("4.00"), nota("5.00"), nota("5.00"));

        assertThat(service.moduloAprobado(inscripcion, modulo)).isFalse();
    }

    @Test
    void elIntegradorNoCuentaParaElPromedio() {
        InstanciaEvaluativa integrador = nota("2.00");
        integrador.setTipo(TipoInstanciaEvaluativa.INTEGRADOR);
        integrador.setCuentaParaPromedio(false);
        mockearInstancias(nota("7.00"), nota("7.00"), nota("7.00"), integrador);

        assertThat(service.notaModulo(inscripcion, modulo)).contains(new BigDecimal("7.00"));
    }

    @Test
    void laNotaOriginalRecuperadaNoCuentaParaElPromedio() {
        InstanciaEvaluativa original = nota("4.00");
        InstanciaEvaluativa recuperatorio = nota("8.00");
        recuperatorio.setRecuperaA(original);
        mockearInstancias(original, recuperatorio, nota("7.00"), nota("6.00"));

        // promedio esperado: (8.00 + 7.00 + 6.00) / 3 = 7.00, la original de 4.00 queda afuera
        assertThat(service.notaModulo(inscripcion, modulo)).contains(new BigDecimal("7.00"));
    }

    @Test
    void notaModuloDevuelvePromedioParcialAunSinLlegarAlMinimo() {
        mockearInstancias(nota("8.00"));

        assertThat(service.notaModulo(inscripcion, modulo)).contains(new BigDecimal("8.00"));
    }

    @Test
    void notaModuloVacioSinNingunaInstancia() {
        mockearInstancias();

        assertThat(service.notaModulo(inscripcion, modulo)).isEmpty();
    }

    @Test
    void detalleNotasIncluyeTodasLasInstanciasYElPromedioVigente() {
        InstanciaEvaluativa original = nota("4.00");
        InstanciaEvaluativa recuperatorio = nota("9.00");
        recuperatorio.setRecuperaA(original);
        mockearInstancias(original, recuperatorio, nota("7.00"), nota("8.00"));

        CenmaBaseEvaluacionService.DetalleNotasModulo detalle = service.detalleNotas(inscripcion, modulo);

        assertThat(detalle.notas()).hasSize(4);
        // promedio: (9.00 + 7.00 + 8.00) / 3 = 8.00, la original de 4.00 sigue afuera del promedio
        assertThat(detalle.promedio()).contains(new BigDecimal("8.00"));
    }

    private InstanciaEvaluativa nota(String valor) {
        return InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo)
                .tipo(TipoInstanciaEvaluativa.NOTA)
                .nota(new BigDecimal(valor))
                .fecha(LocalDate.now())
                .cuentaParaPromedio(true)
                .build();
    }

    private void mockearInstancias(InstanciaEvaluativa... instancias) {
        when(instanciaEvaluativaRepository.findByInscripcionIdAndModuloId(1L, 1L))
                .thenReturn(List.of(instancias));
    }
}