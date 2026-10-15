package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InstanciaEvaluativaRepository extends JpaRepository<InstanciaEvaluativa, Long> {

    List<InstanciaEvaluativa> findByInscripcionId(Long inscripcionId);

    /**
     * Instancias de varias inscripciones en una sola consulta. El grafo trae módulo y recuperatorio junto con la
     * fila: sin él, cada instancia dispararía cargas adicionales de esas relaciones EAGER.
     */
    @EntityGraph(attributePaths = {"modulo", "recuperaA"})
    List<InstanciaEvaluativa> findByInscripcionIdIn(Collection<Long> inscripcionIds);

    List<InstanciaEvaluativa> findByInscripcionIdAndModuloId(Long inscripcionId, Long moduloId);

    List<InstanciaEvaluativa> findByInscripcionIdAndModuloIdAndTipo(
            Long inscripcionId, Long moduloId, TipoInstanciaEvaluativa tipo);

    boolean existsByRecuperaAId(Long instanciaEvaluativaId);
}