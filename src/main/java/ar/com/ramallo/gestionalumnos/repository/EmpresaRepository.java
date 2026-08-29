package ar.com.ramallo.gestionalumnos.repository;

import ar.com.ramallo.gestionalumnos.domain.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {}