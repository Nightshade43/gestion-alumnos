package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstanciaEvaluativaRepository extends JpaRepository<InstanciaEvaluativa, Long> {

    List<InstanciaEvaluativa> findByInscripcionId(Long inscripcionId);

    List<InstanciaEvaluativa> findByInscripcionIdAndModuloId(Long inscripcionId, Long moduloId);

    List<InstanciaEvaluativa> findByInscripcionIdAndModuloIdAndTipo(
            Long inscripcionId, Long moduloId, TipoInstanciaEvaluativa tipo);

    boolean existsByRecuperaAId(Long instanciaEvaluativaId);
}