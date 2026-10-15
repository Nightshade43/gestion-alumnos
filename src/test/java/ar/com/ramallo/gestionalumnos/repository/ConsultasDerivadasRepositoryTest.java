package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.Empresa;
import ar.com.ramallo.gestionalumnos.domain.Grupo;
import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.domain.Persona;
import ar.com.ramallo.gestionalumnos.domain.Programa;
import ar.com.ramallo.gestionalumnos.domain.Seguimiento;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.EstrategiaEvaluacion;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoFacturacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultas derivadas que usan los controllers para los contadores de Programa, los filtros de Contrato
 * y el listado reciente de seguimientos. Hasta ahora solo se probaban mockeadas desde la capa web.
 *
 * Corren contra la misma base local que el resto de los tests de persistencia (con rollback), que puede
 * tener datos reales: por eso cada caso crea sus propios programas y empresas con nombre único y solo afirma
 * sobre lo que él mismo creó, nunca sobre totales globales.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConsultasDerivadasRepositoryTest {

    @Autowired private EntityManager entityManager;
    @Autowired private ModuloRepository moduloRepository;
    @Autowired private InscripcionRepository inscripcionRepository;
    @Autowired private GrupoRepository grupoRepository;
    @Autowired private ContratoRepository contratoRepository;
    @Autowired private SeguimientoRepository seguimientoRepository;

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

    private Inscripcion inscripcion(Programa programa, EstadoInscripcion estado) {
        Persona persona = Persona.builder().nombre(unico("Persona test")).build();
        entityManager.persist(persona);
        Inscripcion inscripcion = Inscripcion.builder()
                .persona(persona).programa(programa).fechaInicio(LocalDate.of(2026, 3, 1)).estado(estado).build();
        entityManager.persist(inscripcion);
        return inscripcion;
    }

    private Empresa empresa() {
        Empresa empresa = Empresa.builder().nombre(unico("Empresa test")).build();
        entityManager.persist(empresa);
        return empresa;
    }

    private Contrato contrato(Empresa empresa, EstadoContrato estado) {
        Contrato contrato = Contrato.builder()
                .empresa(empresa).tipoFacturacion(TipoFacturacion.PAQUETE)
                .clasesContratadas(10).estado(estado).build();
        entityManager.persist(contrato);
        return contrato;
    }

    @Test
    void countByProgramaIdCuentaSoloLosModulosDeEsePrograma() {
        Programa conDos = programa();
        Programa conUno = programa();
        Programa sinModulos = programa();
        entityManager.persist(Modulo.builder().orden(1).esSecuencial(false).programa(conDos).build());
        entityManager.persist(Modulo.builder().orden(2).esSecuencial(false).programa(conDos).build());
        entityManager.persist(Modulo.builder().orden(1).esSecuencial(false).programa(conUno).build());
        entityManager.flush();

        assertThat(moduloRepository.countByProgramaId(conDos.getId())).isEqualTo(2);
        assertThat(moduloRepository.countByProgramaId(conUno.getId())).isEqualTo(1);
        assertThat(moduloRepository.countByProgramaId(sinModulos.getId())).isZero();
    }

    @Test
    void countByProgramaIdAndEstadoSeparaPorProgramaYPorEstado() {
        Programa programa = programa();
        Programa otro = programa();
        inscripcion(programa, EstadoInscripcion.ACTIVA);
        inscripcion(programa, EstadoInscripcion.ACTIVA);
        inscripcion(programa, EstadoInscripcion.PAUSADA);
        inscripcion(programa, EstadoInscripcion.CANCELADA);
        inscripcion(otro, EstadoInscripcion.ACTIVA);
        entityManager.flush();

        assertThat(inscripcionRepository.countByProgramaIdAndEstado(programa.getId(), EstadoInscripcion.ACTIVA)).isEqualTo(2);
        assertThat(inscripcionRepository.countByProgramaIdAndEstado(programa.getId(), EstadoInscripcion.PAUSADA)).isEqualTo(1);
        assertThat(inscripcionRepository.countByProgramaIdAndEstado(programa.getId(), EstadoInscripcion.FINALIZADA)).isZero();
        assertThat(inscripcionRepository.countByProgramaIdAndEstado(otro.getId(), EstadoInscripcion.ACTIVA)).isEqualTo(1);
    }

    @Test
    void findByProgramaIdOrderByIdAscDevuelveLosGruposEnOrdenDeAltaSinMezclarProgramas() {
        Programa programa = programa();
        Programa otro = programa();
        // Alta en orden inverso al alfabético: el orden esperado es el de id, no el del día.
        Grupo sabado = Grupo.builder().dia("Sabado").horario("09:00").programa(programa).build();
        Grupo miercoles = Grupo.builder().dia("Miercoles").horario("20:00").programa(programa).build();
        Grupo ajeno = Grupo.builder().dia("Lunes").horario("18:00").programa(otro).build();
        entityManager.persist(sabado);
        entityManager.persist(ajeno);
        entityManager.persist(miercoles);
        entityManager.flush();

        List<Grupo> grupos = grupoRepository.findByProgramaIdOrderByIdAsc(programa.getId());

        assertThat(grupos).extracting(Grupo::getDia).containsExactly("Sabado", "Miercoles");
    }

    @Test
    void losFiltrosDeContratoCombinanEmpresaYEstado() {
        Empresa e1 = empresa();
        Empresa e2 = empresa();
        Contrato e1Activo = contrato(e1, EstadoContrato.ACTIVO);
        Contrato e1Finalizado = contrato(e1, EstadoContrato.FINALIZADO);
        Contrato e2Activo = contrato(e2, EstadoContrato.ACTIVO);
        Contrato individualActivo = contrato(null, EstadoContrato.ACTIVO);
        entityManager.flush();

        assertThat(contratoRepository.findByEmpresaId(e1.getId()))
                .extracting(Contrato::getId).containsExactlyInAnyOrder(e1Activo.getId(), e1Finalizado.getId());
        assertThat(contratoRepository.findByEmpresaIdAndEstado(e1.getId(), EstadoContrato.ACTIVO))
                .extracting(Contrato::getId).containsExactly(e1Activo.getId());
        assertThat(contratoRepository.findByEmpresaIdAndEstado(e2.getId(), EstadoContrato.FINALIZADO)).isEmpty();

        // Sin empresa filtra solo por estado y, al ser global, puede traer contratos ajenos al test.
        List<Long> activos = contratoRepository.findByEstado(EstadoContrato.ACTIVO).stream().map(Contrato::getId).toList();
        assertThat(activos).contains(e1Activo.getId(), e2Activo.getId(), individualActivo.getId());
        assertThat(activos).doesNotContain(e1Finalizado.getId());
    }

    @Test
    void losSeguimientosRecientesVanPorFechaDescYDesempatanPorIdDesc() {
        Inscripcion inscripcion = inscripcion(programa(), EstadoInscripcion.ACTIVA);
        Seguimiento viejo = Seguimiento.builder()
                .inscripcion(inscripcion).fecha(LocalDate.of(2026, 3, 1)).observacion("viejo").build();
        Seguimiento mismoDiaPrimero = Seguimiento.builder()
                .inscripcion(inscripcion).fecha(LocalDate.of(2026, 3, 5)).observacion("mismo dia, primero").build();
        Seguimiento mismoDiaSegundo = Seguimiento.builder()
                .inscripcion(inscripcion).fecha(LocalDate.of(2026, 3, 5)).observacion("mismo dia, segundo").build();
        entityManager.persist(viejo);
        entityManager.persist(mismoDiaPrimero);
        entityManager.persist(mismoDiaSegundo);
        entityManager.flush();

        Set<Long> propios = Set.of(viejo.getId(), mismoDiaPrimero.getId(), mismoDiaSegundo.getId());
        List<Long> orden = seguimientoRepository.findAllByOrderByFechaDescIdDesc().stream()
                .map(Seguimiento::getId).filter(propios::contains).toList();

        assertThat(orden).containsExactly(mismoDiaSegundo.getId(), mismoDiaPrimero.getId(), viejo.getId());
    }
}
