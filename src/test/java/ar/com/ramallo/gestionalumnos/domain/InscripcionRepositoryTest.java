package ar.com.ramallo.gestionalumnos.domain;

import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InscripcionRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteInscripcionEscolarConTodasSusRelaciones() {
        Programa programa = Programa.builder()
                .nombre("Ingles - CENMA Sede")
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE)
                .build();
        entityManager.persist(programa);

        Plan planA = Plan.builder().codigo("A").moduloInicio(1).programa(programa).build();
        entityManager.persist(planA);

        Grupo grupoMiercoles = Grupo.builder().dia("Miercoles").horario("18:00-19:30").programa(programa).build();
        entityManager.persist(grupoMiercoles);

        Persona persona = Persona.builder().nombre("Ana Gomez").email("ana@mail.com").build();
        entityManager.persist(persona);

        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona)
                .programa(programa)
                .plan(planA)
                .grupo(grupoMiercoles)
                .fechaInicio(LocalDate.now())
                .build();
        entityManager.persist(inscripcion);
        entityManager.flush();

        assertThat(inscripcion.getId()).isNotNull();
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void persisteInscripcionParticularSinPlanNiGrupo() {
        Programa programaParticular = Programa.builder()
                .nombre("Ingles IT - clases particulares")
                .categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE)
                .build();
        entityManager.persist(programaParticular);

        Persona persona = Persona.builder().nombre("Carlos Diaz").telefono("3511234567").build();
        entityManager.persist(persona);

        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona)
                .programa(programaParticular)
                .fechaInicio(LocalDate.now())
                .build();
        entityManager.persist(inscripcion);
        entityManager.flush();

        assertThat(inscripcion.getId()).isNotNull();
        assertThat(inscripcion.getPlan()).isNull();
        assertThat(inscripcion.getGrupo()).isNull();
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void persisteHistorialGrupoAsociadoAUnaInscripcion() {
        Programa programa = Programa.builder()
                .nombre("Ingles - CENMA Base")
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE)
                .build();
        entityManager.persist(programa);

        Grupo grupoLunes = Grupo.builder().dia("Lunes").horario("19:00-21:00").programa(programa).build();
        entityManager.persist(grupoLunes);

        Persona persona = Persona.builder().nombre("Marta Ruiz").build();
        entityManager.persist(persona);

        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona)
                .programa(programa)
                .grupo(grupoLunes)
                .fechaInicio(LocalDate.now())
                .build();
        entityManager.persist(inscripcion);

        HistorialGrupo historial = HistorialGrupo.builder()
                .inscripcion(inscripcion)
                .grupo(grupoLunes)
                .fechaDesde(LocalDate.now())
                .build();
        entityManager.persist(historial);
        entityManager.flush();

        assertThat(historial.getId()).isNotNull();
        assertThat(historial.getFechaHasta()).isNull();
    }
}