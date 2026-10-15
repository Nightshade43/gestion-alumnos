package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.domain.Persona;
import ar.com.ramallo.gestionalumnos.domain.Plan;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultas del listado de inscripciones: el filtro por estado y, sobre todo, que el listado traiga persona,
 * programa, plan, grupo y contrato en un solo SELECT (sin una consulta por inscripción y por relación).
 *
 * Corren contra la base local compartida (con rollback), que puede tener datos reales: cada caso afirma solo
 * sobre lo que él mismo creó.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InscripcionListadoRepositoryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private InscripcionRepository inscripcionRepository;
    @Autowired private ModuloRepository moduloRepository;
    @Autowired private InstanciaEvaluativaRepository instanciaEvaluativaRepository;

    private static String unico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private Programa programa() {
        Programa programa = Programa.builder()
                .nombre(unico("Programa test"))
                .categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE)
                .build();
        entityManager.persist(programa);
        return programa;
    }

    private Persona persona() {
        Persona persona = Persona.builder().nombre(unico("Persona test")).build();
        entityManager.persist(persona);
        return persona;
    }

    private Inscripcion inscripcion(Persona persona, Programa programa, EstadoInscripcion estado) {
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programa).fechaInicio(LocalDate.of(2026, 3, 1)).estado(estado).build();
        entityManager.persist(inscripcion);
        return inscripcion;
    }

    @Test
    void findByEstadoInTraeSoloLosEstadosPedidos() {
        Programa programa = programa();
        Inscripcion activa = inscripcion(persona(), programa, EstadoInscripcion.ACTIVA);
        Inscripcion pausada = inscripcion(persona(), programa, EstadoInscripcion.PAUSADA);
        Inscripcion finalizada = inscripcion(persona(), programa, EstadoInscripcion.FINALIZADA);
        Inscripcion cancelada = inscripcion(persona(), programa, EstadoInscripcion.CANCELADA);
        entityManager.flush();
        entityManager.clear();

        List<Long> vigentes = inscripcionRepository
                .findByEstadoIn(List.of(EstadoInscripcion.ACTIVA, EstadoInscripcion.PAUSADA))
                .stream().map(Inscripcion::getId).toList();

        assertThat(vigentes).contains(activa.getId(), pausada.getId());
        assertThat(vigentes).doesNotContain(finalizada.getId(), cancelada.getId());
    }

    @Test
    void elListadoDeUnaPersonaTraeTodasSusRelacionesEnUnaSolaConsulta() {
        Persona persona = persona();
        for (int i = 0; i < 3; i++) {
            Programa programa = programa();
            Plan plan = Plan.builder().codigo("A").moduloInicio(1).programa(programa).build();
            Grupo grupo = Grupo.builder().dia("Lunes").horario("18:00").programa(programa).build();
            Contrato contrato = Contrato.builder().tipoFacturacion(TipoFacturacion.PAQUETE).clasesContratadas(10).build();
            entityManager.persist(plan);
            entityManager.persist(grupo);
            entityManager.persist(contrato);
            Inscripcion inscripcion = inscripcion(persona, programa, EstadoInscripcion.ACTIVA);
            inscripcion.setPlan(plan);
            inscripcion.setGrupo(grupo);
            inscripcion.setContrato(contrato);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics estadisticas = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        estadisticas.setStatisticsEnabled(true);
        long antes = estadisticas.getPrepareStatementCount();

        List<Inscripcion> inscripciones = inscripcionRepository.findByPersonaId(persona.getId());
        // Se recorren las mismas relaciones que lee InscripcionResponse: con carga diferida cada una sería una consulta.
        inscripciones.forEach(i -> {
            assertThat(i.getPersona().getNombre()).isNotNull();
            assertThat(i.getPrograma().getNombre()).isNotNull();
            assertThat(i.getPlan().getCodigo()).isEqualTo("A");
            assertThat(i.getGrupo().getDia()).isEqualTo("Lunes");
            assertThat(i.getContrato().getClasesContratadas()).isEqualTo(10);
        });

        assertThat(inscripciones).hasSize(3);
        assertThat(estadisticas.getPrepareStatementCount() - antes).isEqualTo(1);
    }

    @Test
    void findByProgramaIdInTraeLosModulosDeVariosProgramasEnOrdenSinMezclarOtros() {
        Programa uno = programa();
        Programa dos = programa();
        Programa ajeno = programa();
        entityManager.persist(Modulo.builder().orden(2).esSecuencial(false).programa(uno).build());
        entityManager.persist(Modulo.builder().orden(1).esSecuencial(false).programa(dos).build());
        entityManager.persist(Modulo.builder().orden(1).esSecuencial(false).programa(uno).build());
        entityManager.persist(Modulo.builder().orden(1).esSecuencial(false).programa(ajeno).build());
        entityManager.flush();
        entityManager.clear();

        List<Modulo> modulos = moduloRepository.findByProgramaIdInOrderByOrden(List.of(uno.getId(), dos.getId()));

        assertThat(modulos).extracting(m -> m.getPrograma().getId()).doesNotContain(ajeno.getId());
        assertThat(modulos).filteredOn(m -> m.getPrograma().getId().equals(uno.getId()))
                .extracting(Modulo::getOrden).containsExactly(1, 2);
        assertThat(modulos).filteredOn(m -> m.getPrograma().getId().equals(dos.getId()))
                .extracting(Modulo::getOrden).containsExactly(1);
    }

    @Test
    void lasInstanciasDeVariasInscripcionesSeTraenEnUnaSolaConsultaConSuModuloYRecuperatorio() {
        Programa programa = programa();
        Modulo modulo1 = Modulo.builder().orden(1).esSecuencial(false).programa(programa).build();
        Modulo modulo2 = Modulo.builder().orden(2).esSecuencial(false).programa(programa).build();
        entityManager.persist(modulo1);
        entityManager.persist(modulo2);
        Inscripcion ana = inscripcion(persona(), programa, EstadoInscripcion.ACTIVA);
        Inscripcion bruno = inscripcion(persona(), programa, EstadoInscripcion.ACTIVA);
        InstanciaEvaluativa original = instancia(ana, modulo1, "4.00");
        InstanciaEvaluativa recuperatorio = instancia(ana, modulo1, "8.00");
        recuperatorio.setRecuperaA(original);
        instancia(ana, modulo2, "7.00");
        instancia(bruno, modulo1, "6.00");
        instancia(bruno, modulo2, "9.00");
        entityManager.flush();
        entityManager.clear();

        // Como en el listado real, las inscripciones ya están cargadas cuando se piden sus instancias.
        inscripcionRepository.findByPersonaId(ana.getPersona().getId());
        inscripcionRepository.findByPersonaId(bruno.getPersona().getId());
        Statistics estadisticas = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        estadisticas.setStatisticsEnabled(true);
        long antes = estadisticas.getPrepareStatementCount();

        List<InstanciaEvaluativa> instancias =
                instanciaEvaluativaRepository.findByInscripcionIdIn(List.of(ana.getId(), bruno.getId()));
        instancias.forEach(i -> {
            assertThat(i.getInscripcion().getId()).isNotNull();
            assertThat(i.getModulo().getOrden()).isNotNull();
        });

        assertThat(instancias).hasSize(5);
        assertThat(instancias).filteredOn(i -> i.getRecuperaA() != null).hasSize(1);
        assertThat(estadisticas.getPrepareStatementCount() - antes).isEqualTo(1);
    }

    private InstanciaEvaluativa instancia(Inscripcion inscripcion, Modulo modulo, String nota) {
        InstanciaEvaluativa instancia = InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo).tipo(TipoInstanciaEvaluativa.NOTA)
                .nota(new BigDecimal(nota)).fecha(LocalDate.of(2026, 4, 1)).build();
        entityManager.persist(instancia);
        return instancia;
    }
}
