package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Contrato;
import ar.com.ramallo.gestionalumnos.domain.enums.EstadoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    List<Contrato> findByEmpresaId(Long empresaId);

    List<Contrato> findByEstado(EstadoContrato estado);

    List<Contrato> findByEmpresaIdAndEstado(Long empresaId, EstadoContrato estado);
}
