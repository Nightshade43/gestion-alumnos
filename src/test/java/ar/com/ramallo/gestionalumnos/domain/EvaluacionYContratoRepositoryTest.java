package ar.com.ramallo.gestionalumnos.domain;

import ar.com.ramallo.gestionalumnos.domain.enums.*;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EvaluacionYContratoRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteInstanciaEvaluativaConRecuperatorioYConservaHistorial() {
        Programa programa = Programa.builder()
                .nombre("Ingles - CENMA Base")
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE)
                .build();
        entityManager.persist(programa);

        Modulo modulo = Modulo.builder().orden(1).esSecuencial(false).programa(programa).build();
        entityManager.persist(modulo);

        Persona persona = Persona.builder().nombre("Luis Perez").build();
        entityManager.persist(persona);

        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programa).fechaInicio(LocalDate.now()).build();
        entityManager.persist(inscripcion);

        InstanciaEvaluativa original = InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo)
                .tipo(TipoInstanciaEvaluativa.NOTA)
                .nota(new BigDecimal("4.00"))
                .fecha(LocalDate.now())
                .build();
        entityManager.persist(original);

        InstanciaEvaluativa recuperatorio = InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo)
                .tipo(TipoInstanciaEvaluativa.NOTA)
                .nota(new BigDecimal("7.00"))
                .fecha(LocalDate.now())
                .recuperaA(original)
                .build();
        entityManager.persist(recuperatorio);
        entityManager.flush();

        assertThat(recuperatorio.getId()).isNotNull();
        assertThat(recuperatorio.getRecuperaA()).isEqualTo(original);
        assertThat(original.getNota()).isEqualByComparingTo("4.00");
    }

    @Test
    void cuentaParaPromedioUsaDefaultTrueSiNoSeEspecifica() {
        Programa programa = Programa.builder()
                .nombre("Ingles - CENMA Base 2")
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE)
                .build();
        entityManager.persist(programa);
        Modulo modulo = Modulo.builder().orden(1).esSecuencial(false).programa(programa).build();
        entityManager.persist(modulo);
        Persona persona = Persona.builder().nombre("Sofia Lopez").build();
        entityManager.persist(persona);
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programa).fechaInicio(LocalDate.now()).build();
        entityManager.persist(inscripcion);

        InstanciaEvaluativa instancia = InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo)
                .tipo(TipoInstanciaEvaluativa.NOTA)
                .nota(new BigDecimal("8.00"))
                .fecha(LocalDate.now())
                .build();
        entityManager.persist(instancia);
        entityManager.flush();

        assertThat(instancia.isCuentaParaPromedio()).isTrue();
    }

    @Test
    void contratoUsaDefaultsDeEstadoActivoYCeroClasesConsumidas() {
        Programa programaParticular = Programa.builder()
                .nombre("Ingles turismo")
                .categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE)
                .build();
        entityManager.persist(programaParticular);
        Persona persona = Persona.builder().nombre("Roberto Fernandez").build();
        entityManager.persist(persona);
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programaParticular).fechaInicio(LocalDate.now()).build();
        entityManager.persist(inscripcion);

        Contrato contrato = Contrato.builder()
                .tipoFacturacion(TipoFacturacion.PAQUETE)
                .clasesContratadas(10)
                .build();
        entityManager.persist(contrato);

        inscripcion.setContrato(contrato);
        entityManager.persist(inscripcion);
        entityManager.flush();

        assertThat(contrato.getEstado()).isEqualTo(EstadoContrato.ACTIVO);
        assertThat(contrato.getClasesConsumidas()).isZero();
    }

    @Test
    void persisteSeguimientoConObservacionLibre() {
        Programa programaParticular = Programa.builder()
                .nombre("Consultoria IA educativa")
                .categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE)
                .build();
        entityManager.persist(programaParticular);
        Persona persona = Persona.builder().nombre("Diego Alvarez").build();
        entityManager.persist(persona);
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programaParticular).fechaInicio(LocalDate.now()).build();
        entityManager.persist(inscripcion);

        Seguimiento seguimiento = Seguimiento.builder()
                .inscripcion(inscripcion)
                .fecha(LocalDate.now())
                .observacion("Buen manejo de vocabulario tecnico, falta fluidez oral")
                .build();
        entityManager.persist(seguimiento);
        entityManager.flush();

        assertThat(seguimiento.getId()).isNotNull();
    }
}