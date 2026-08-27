package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InscripcionHistorialInstanciaRepositoryTest {

    @Autowired private InscripcionRepository inscripcionRepository;
    @Autowired private HistorialGrupoRepository historialGrupoRepository;
    @Autowired private InstanciaEvaluativaRepository instanciaEvaluativaRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void detectaInscripcionEscolarActivaExistente() {
        Programa programa = persistirPrograma("Ingles Base - unicidad", CategoriaPrograma.ESCOLAR, EstrategiaEvaluacion.CENMA_BASE);
        Persona persona = persistirPersona("Ana Unicidad");
        inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programa).fechaInicio(LocalDate.now()).build());

        boolean existe = inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
                persona.getId(), CategoriaPrograma.ESCOLAR, List.of(EstadoInscripcion.ACTIVA, EstadoInscripcion.PAUSADA));

        assertThat(existe).isTrue();
    }

    @Test
    void noDetectaConflictoSiLaInscripcionEscolarEstaCancelada() {
        Programa programa = persistirPrograma("Ingles Base - cancelada", CategoriaPrograma.ESCOLAR, EstrategiaEvaluacion.CENMA_BASE);
        Persona persona = persistirPersona("Bruno Cancelado");
        inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programa).fechaInicio(LocalDate.now())
                .estado(EstadoInscripcion.CANCELADA).build());

        boolean existe = inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
                persona.getId(), CategoriaPrograma.ESCOLAR, List.of(EstadoInscripcion.ACTIVA, EstadoInscripcion.PAUSADA));

        assertThat(existe).isFalse();
    }

    @Test
    void noDetectaConflictoEntreCategoriasDistintas() {
        Programa programaParticular = persistirPrograma("Ingles IT - unicidad", CategoriaPrograma.PARTICULAR, EstrategiaEvaluacion.SEGUIMIENTO_LIBRE);
        Persona persona = persistirPersona("Carla Particular");
        inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programaParticular).fechaInicio(LocalDate.now()).build());

        boolean existe = inscripcionRepository.existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
                persona.getId(), CategoriaPrograma.ESCOLAR, List.of(EstadoInscripcion.ACTIVA, EstadoInscripcion.PAUSADA));

        assertThat(existe).isFalse();
    }

    @Test
    void encuentraTodasLasInscripcionesDeUnaPersona() {
        Persona persona = persistirPersona("Diego Multi");
        Programa programaEscolar = persistirPrograma("Ingles Base - multi", CategoriaPrograma.ESCOLAR, EstrategiaEvaluacion.CENMA_BASE);
        Programa programaParticular = persistirPrograma("Ingles IT - multi", CategoriaPrograma.PARTICULAR, EstrategiaEvaluacion.SEGUIMIENTO_LIBRE);

        Inscripcion inscripcionEscolar = inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programaEscolar).fechaInicio(LocalDate.now()).build());
        Inscripcion inscripcionParticular = inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programaParticular).fechaInicio(LocalDate.now()).build());

        assertThat(inscripcionRepository.findByPersonaId(persona.getId()))
                .containsExactlyInAnyOrder(inscripcionEscolar, inscripcionParticular);
    }

    @Test
    void encuentraElRegistroDeHistorialGrupoAbierto() {
        Programa programa = persistirPrograma("Ingles Base - historial", CategoriaPrograma.ESCOLAR, EstrategiaEvaluacion.CENMA_BASE);
        Grupo grupoLunes = persistirGrupo("Lunes", "19:00-21:00", programa);
        Grupo grupoMartes = persistirGrupo("Martes", "19:00-21:00", programa);
        Persona persona = persistirPersona("Elena Historial");
        Inscripcion inscripcion = inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programa).grupo(grupoMartes).fechaInicio(LocalDate.now()).build());

        historialGrupoRepository.save(HistorialGrupo.builder()
                .inscripcion(inscripcion).grupo(grupoLunes)
                .fechaDesde(LocalDate.now().minusMonths(2)).fechaHasta(LocalDate.now().minusDays(1)).build());
        HistorialGrupo registroAbierto = historialGrupoRepository.save(HistorialGrupo.builder()
                .inscripcion(inscripcion).grupo(grupoMartes).fechaDesde(LocalDate.now()).build());

        Optional<HistorialGrupo> encontrado = historialGrupoRepository.findByInscripcionIdAndFechaHastaIsNull(inscripcion.getId());

        assertThat(encontrado).contains(registroAbierto);
        assertThat(historialGrupoRepository.findByInscripcionIdOrderByFechaDesde(inscripcion.getId())).hasSize(2);
    }

    @Test
    void encuentraInstanciasEvaluativasPorInscripcionModuloYTipo() {
        Programa programa = persistirPrograma("Ingles Sede - instancias", CategoriaPrograma.ESCOLAR, EstrategiaEvaluacion.CENMA_SEDE);
        Modulo modulo = persistirModulo(1, true, programa);
        Persona persona = persistirPersona("Franco Instancias");
        Inscripcion inscripcion = inscripcionRepository.save(Inscripcion.builder()
                .persona(persona).programa(programa).fechaInicio(LocalDate.now()).build());

        InstanciaEvaluativa tp = instanciaEvaluativaRepository.save(InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo).tipo(TipoInstanciaEvaluativa.TP_INTEGRADOR)
                .fecha(LocalDate.now()).cuentaParaPromedio(false).build());
        InstanciaEvaluativa original = instanciaEvaluativaRepository.save(InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo).tipo(TipoInstanciaEvaluativa.EVALUACION_FINAL)
                .nota(new BigDecimal("5.00")).fecha(LocalDate.now()).build());
        InstanciaEvaluativa recuperatorio = instanciaEvaluativaRepository.save(InstanciaEvaluativa.builder()
                .inscripcion(inscripcion).modulo(modulo).tipo(TipoInstanciaEvaluativa.EVALUACION_FINAL)
                .nota(new BigDecimal("7.00")).fecha(LocalDate.now()).recuperaA(original).build());

        assertThat(instanciaEvaluativaRepository.findByInscripcionId(inscripcion.getId())).hasSize(3);
        assertThat(instanciaEvaluativaRepository.findByInscripcionIdAndModuloId(inscripcion.getId(), modulo.getId())).hasSize(3);
        assertThat(instanciaEvaluativaRepository.findByInscripcionIdAndModuloIdAndTipo(
                inscripcion.getId(), modulo.getId(), TipoInstanciaEvaluativa.TP_INTEGRADOR)).containsExactly(tp);
        assertThat(instanciaEvaluativaRepository.existsByRecuperaAId(original.getId())).isTrue();
        assertThat(instanciaEvaluativaRepository.existsByRecuperaAId(recuperatorio.getId())).isFalse();
    }

    private Programa persistirPrograma(String nombre, CategoriaPrograma categoria, EstrategiaEvaluacion estrategia) {
        Programa programa = Programa.builder().nombre(nombre).categoria(categoria).estrategiaEvaluacion(estrategia).build();
        entityManager.persist(programa);
        return programa;
    }

    private Persona persistirPersona(String nombre) {
        Persona persona = Persona.builder().nombre(nombre).build();
        entityManager.persist(persona);
        return persona;
    }

    private Grupo persistirGrupo(String dia, String horario, Programa programa) {
        Grupo grupo = Grupo.builder().dia(dia).horario(horario).programa(programa).build();
        entityManager.persist(grupo);
        return grupo;
    }

    private Modulo persistirModulo(int orden, boolean esSecuencial, Programa programa) {
        Modulo modulo = Modulo.builder().orden(orden).esSecuencial(esSecuencial).programa(programa).build();
        entityManager.persist(modulo);
        return modulo;
    }
}