package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.*;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProgramaModuloPlanRepositoryTest {

    @Autowired private ProgramaRepository programaRepository;
    @Autowired private ModuloRepository moduloRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private GrupoRepository grupoRepository;

    @Test
    void encuentraProgramasPorInstitucion() {
        Institucion institucion = Institucion.builder().nombre("CENMA Bo SMATA - test institucion").build();
        entityManager.persist(institucion);

        Programa programaBase = programaRepository.save(Programa.builder()
                .nombre("Ingles Base").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).institucion(institucion).build());
        Programa programaSede = programaRepository.save(Programa.builder()
                .nombre("Ingles Sede").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).institucion(institucion).build());
        programaRepository.save(Programa.builder()
                .nombre("Ingles IT").categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE).build());

        List<Programa> programasDeLaInstitucion = programaRepository.findByInstitucionId(institucion.getId());

        assertThat(programasDeLaInstitucion).containsExactlyInAnyOrder(programaBase, programaSede);
    }

    @Test
    void encuentraProgramasPorCategoria() {
        Programa programaParticular = programaRepository.save(Programa.builder()
                .nombre("Ingles turismo").categoria(CategoriaPrograma.PARTICULAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.SEGUIMIENTO_LIBRE).build());

        assertThat(programaRepository.findByCategoria(CategoriaPrograma.PARTICULAR)).contains(programaParticular);
    }

    @Test
    void ordenaModulosPorOrdenDentroDeUnPrograma() {
        Programa programa = programaRepository.save(Programa.builder()
                .nombre("Ingles Sede - test orden").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build());

        Modulo modulo3 = moduloRepository.save(Modulo.builder().orden(3).esSecuencial(true).programa(programa).build());
        Modulo modulo1 = moduloRepository.save(Modulo.builder().orden(1).esSecuencial(true).programa(programa).build());
        Modulo modulo2 = moduloRepository.save(Modulo.builder().orden(2).esSecuencial(true).programa(programa).build());

        assertThat(moduloRepository.findByProgramaIdOrderByOrden(programa.getId()))
                .containsExactly(modulo1, modulo2, modulo3);
    }

    @Test
    void encuentraModuloPorProgramaYOrdenCuandoExiste() {
        Programa programa = programaRepository.save(Programa.builder()
                .nombre("Ingles Base - test modulo").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build());
        Modulo modulo = moduloRepository.save(Modulo.builder().orden(1).esSecuencial(false).programa(programa).build());

        assertThat(moduloRepository.findByProgramaIdAndOrden(programa.getId(), 1)).contains(modulo);
    }

    @Test
    void noEncuentraModuloSiElOrdenNoExisteEnEsePrograma() {
        Programa programa = programaRepository.save(Programa.builder()
                .nombre("Ingles Base - test modulo vacio").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build());

        Optional<Modulo> encontrado = moduloRepository.findByProgramaIdAndOrden(programa.getId(), 99);

        assertThat(encontrado).isEmpty();
    }

    @Test
    void encuentraPlanPorProgramaYCodigo() {
        Programa programa = programaRepository.save(Programa.builder()
                .nombre("Ingles Sede - test plan").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_SEDE).build());
        Plan planB = planRepository.save(Plan.builder().codigo("B").moduloInicio(4).programa(programa).build());

        assertThat(planRepository.findByProgramaIdAndCodigo(programa.getId(), "B")).contains(planB);
        assertThat(planRepository.findByProgramaId(programa.getId())).containsExactly(planB);
    }

    @Test
    void encuentraGruposPorPrograma() {
        Programa programa = programaRepository.save(Programa.builder()
                .nombre("Ingles Base - test grupos").categoria(CategoriaPrograma.ESCOLAR)
                .estrategiaEvaluacion(EstrategiaEvaluacion.CENMA_BASE).build());
        Grupo grupo = grupoRepository.save(Grupo.builder().dia("Lunes").horario("19:00-21:00").programa(programa).build());

        assertThat(grupoRepository.findByProgramaId(programa.getId())).containsExactly(grupo);
    }
}