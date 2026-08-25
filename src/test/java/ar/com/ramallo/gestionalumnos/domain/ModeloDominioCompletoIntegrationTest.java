package ar.com.ramallo.gestionalumnos.domain;

import ar.com.ramallo.gestionalumnos.domain.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ModeloDominioCompletoIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void armaElGrafoCompletoDeLaRamaEscolar() {
        Institucion institucion = Institucion.builder().nombre("CENMA Bo SMATA").build();
        entityManager.persist(institucion);

        Programa programa = Programa.builder()
                .nombre("Ingles - CENMA Sede")
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE)
                .institucion(institucion)
                .build();
        entityManager.persist(programa);

        Modulo modulo1 = Modulo.builder().orden(1).esSecuencial(true).programa(programa).build();
        entityManager.persist(modulo1);

        Plan planA = Plan.builder().codigo("A").moduloInicio(1).programa(programa).build();
        entityManager.persist(planA);

        Grupo grupoMiercoles = Grupo.builder().dia("Miercoles").horario("18:00-19:30").programa(programa).build();
        entityManager.persist(grupoMiercoles);

        Persona persona = Persona.builder().nombre("Valentina Ríos").email("valen@mail.com").build();
        entityManager.persist(persona);

        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programa).plan(planA).grupo(grupoMiercoles)
                .fechaInicio(LocalDate.now())
                .build();
        entityManager.persist(inscripcion);

        HistorialGrupo historial = HistorialGrupo.builder()
                .inscripcion(inscripcion).grupo(grupoMiercoles).fechaDesde(LocalDate.now())
                .build();
        entityManager.persist(historial);

        InstanciaEvaluativa tp = InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo1)
                .tipo(TipoInstanciaEvaluativa.TP_INTEGRADOR)
                .fecha(LocalDate.now())
                .cuentaParaPromedio(false)
                .build();
        entityManager.persist(tp);

        InstanciaEvaluativa evaluacionFinal = InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo1)
                .tipo(TipoInstanciaEvaluativa.EVALUACION_FINAL)
                .nota(new BigDecimal("7.50"))
                .fecha(LocalDate.now())
                .build();
        entityManager.persist(evaluacionFinal);

        entityManager.flush();
        entityManager.clear();

        Inscripcion recargada = entityManager.find(Inscripcion.class, inscripcion.getId());
        assertThat(recargada.getPersona().getNombre()).isEqualTo("Valentina Ríos");
        assertThat(recargada.getPrograma().getInstitucion().getNombre()).isEqualTo("CENMA Bo SMATA");
        assertThat(recargada.getPlan().getCodigo()).isEqualTo("A");
        assertThat(recargada.getGrupo().getDia()).isEqualTo("Miercoles");
        assertThat(recargada.getEstado()).isEqualTo(EstadoInscripcion.ACTIVA);
    }

    @Test
    void armaElGrafoCompletoDeLaRamaParticular() {
        Programa programaParticular = Programa.builder()
                .nombre("Ingles IT personalizado")
                .categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE)
                .build();
        entityManager.persist(programaParticular);

        Persona persona = Persona.builder().nombre("Martin Sosa").email("martin@mail.com").build();
        entityManager.persist(persona);

        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programaParticular).fechaInicio(LocalDate.now())
                .build();
        entityManager.persist(inscripcion);

        Contrato contrato = Contrato.builder()
                .inscripcion(inscripcion)
                .tipoFacturacion(TipoFacturacion.PAQUETE)
                .clasesContratadas(10)
                .build();
        entityManager.persist(contrato);

        Seguimiento seguimiento = Seguimiento.builder()
                .inscripcion(inscripcion)
                .fecha(LocalDate.now())
                .observacion("Nivel B1, buen progreso en vocabulario de cloud computing")
                .build();
        entityManager.persist(seguimiento);

        entityManager.flush();
        entityManager.clear();

        Inscripcion recargada = entityManager.find(Inscripcion.class, inscripcion.getId());
        assertThat(recargada.getPrograma().getCategoria()).isEqualTo(CategoriaPrograma.PARTICULAR);
        assertThat(recargada.getPlan()).isNull();
        assertThat(recargada.getGrupo()).isNull();
    }
}