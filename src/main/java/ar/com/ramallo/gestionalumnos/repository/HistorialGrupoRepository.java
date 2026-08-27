package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.HistorialGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistorialGrupoRepository extends JpaRepository<HistorialGrupo, Long> {

    List<HistorialGrupo> findByInscripcionIdOrderByFechaDesde(Long inscripcionId);

    Optional<HistorialGrupo> findByInscripcionIdAndFechaHastaIsNull(Long inscripcionId);
}