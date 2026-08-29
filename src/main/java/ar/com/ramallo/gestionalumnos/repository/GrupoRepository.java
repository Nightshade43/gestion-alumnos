package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {
    List<Grupo> findByProgramaId(Long programaId);
}