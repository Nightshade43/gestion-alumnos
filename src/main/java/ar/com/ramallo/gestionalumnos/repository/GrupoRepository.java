package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByProgramaId(Long programaId);

    /** Orden de alta: el listado de días de ProgramaResponse no depende del orden en que la base devuelva las filas. */
    List<Grupo> findByProgramaIdOrderByIdAsc(Long programaId);
}