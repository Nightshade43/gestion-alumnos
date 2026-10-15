package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    // Los listados que arma InscripcionResponse leen persona, programa, plan, grupo y contrato de cada fila.
    // Las relaciones @ManyToOne son EAGER, y sin este grafo Hibernate las trae de a una (N+1): con 300 inscripciones
    // eran ~2500 consultas por request. Con el grafo todo viaja en un solo SELECT con joins.
    @Override
    @EntityGraph(attributePaths = {"persona", "programa", "programa.institucion", "plan", "grupo", "contrato", "contrato.empresa"})
    List<Inscripcion> findAll();

    @EntityGraph(attributePaths = {"persona", "programa", "programa.institucion", "plan", "grupo", "contrato", "contrato.empresa"})
    List<Inscripcion> findByPersonaId(Long personaId);

    @EntityGraph(attributePaths = {"persona", "programa", "programa.institucion", "plan", "grupo", "contrato", "contrato.empresa"})
    List<Inscripcion> findByEstadoIn(Collection<EstadoInscripcion> estados);

    boolean existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
            Long personaId, CategoriaPrograma categoria, List<EstadoInscripcion> estados);

    List<Inscripcion> findByContratoId(Long contratoId);

    long countByProgramaIdAndEstado(Long programaId, EstadoInscripcion estado);
}
