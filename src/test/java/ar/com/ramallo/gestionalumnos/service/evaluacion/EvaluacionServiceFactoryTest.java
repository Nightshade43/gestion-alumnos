package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EvaluacionServiceFactoryTest {

    @Mock private CenmaBaseEvaluacionService cenmaBaseEvaluacionService;
    @Mock private CenmaSedeEvaluacionService cenmaSedeEvaluacionService;

    private EvaluacionServiceFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EvaluacionServiceFactory(cenmaBaseEvaluacionService, cenmaSedeEvaluacionService);
    }

    @Test
    void resuelveCenmaBaseParaEsaEstrategia() {
        Programa programa = Programa.builder().categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build();

        assertThat(factory.resolver(programa)).isSameAs(cenmaBaseEvaluacionService);
    }

    @Test
    void resuelveCenmaSedeParaEsaEstrategia() {
        Programa programa = Programa.builder().categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build();

        assertThat(factory.resolver(programa)).isSameAs(cenmaSedeEvaluacionService);
    }

    @Test
    void lanzaExcepcionParaSeguimientoLibre() {
        Programa programa = Programa.builder().categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE).build();

        assertThatThrownBy(() -> factory.resolver(programa)).isInstanceOf(IllegalArgumentException.class);
    }
}