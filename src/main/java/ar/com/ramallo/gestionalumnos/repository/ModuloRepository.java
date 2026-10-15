package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    List<Modulo> findByProgramaIdOrderByOrden(Long programaId);

    /** Módulos de varios programas en una sola consulta (evaluación por lotes del listado de inscripciones). */
    List<Modulo> findByProgramaIdInOrderByOrden(Collection<Long> programaIds);

    long countByProgramaId(Long programaId);

    Optional<Modulo> findByProgramaIdAndOrden(Long programaId, Integer orden);
}