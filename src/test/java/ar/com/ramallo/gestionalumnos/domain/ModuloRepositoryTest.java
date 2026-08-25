package ar.com.ramallo.gestionalumnos.domain;

import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ModuloRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteProgramaConModuloYPlan() {
        Programa programa = Programa.builder()
                .nombre("Ingles - CENMA Sede")
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE)
                .build();
        entityManager.persist(programa);

        Modulo modulo = Modulo.builder().orden(1).esSecuencial(true).programa(programa).build();
        entityManager.persist(modulo);

        Plan planA = Plan.builder().codigo("A").moduloInicio(1).programa(programa).build();
        entityManager.persist(planA);

        entityManager.flush();

        assertThat(modulo.getId()).isNotNull();
        assertThat(planA.getId()).isNotNull();
    }

    @Test
    void noPermiteDosModulosConMismoOrdenEnElMismoPrograma() {
        Programa programa = Programa.builder()
                .nombre("Ingles - CENMA Base")
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE)
                .build();
        entityManager.persist(programa);
        entityManager.persist(Modulo.builder().orden(1).esSecuencial(false).programa(programa).build());
        entityManager.flush();

        Modulo moduloDuplicado = Modulo.builder().orden(1).esSecuencial(false).programa(programa).build();

        assertThrows(ConstraintViolationException.class, () -> entityManager.persist(moduloDuplicado));
    }
}