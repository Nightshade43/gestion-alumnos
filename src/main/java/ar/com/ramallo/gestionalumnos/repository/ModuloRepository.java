package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    List<Modulo> findByProgramaIdOrderByOrden(Long programaId);

    Optional<Modulo> findByProgramaIdAndOrden(Long programaId, Integer orden);
}