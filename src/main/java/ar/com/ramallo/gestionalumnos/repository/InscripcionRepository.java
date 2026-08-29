package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.enums.CategoriaPrograma;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    List<Inscripcion> findByPersonaId(Long personaId);

    boolean existsByPersonaIdAndPrograma_CategoriaAndEstadoIn(
            Long personaId, CategoriaPrograma categoria, List<EstadoInscripcion> estados);

    List<Inscripcion> findByContratoId(Long contratoId);
}