package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.Modulo;

import java.math.BigDecimal;
import java.util.Optional;

public interface EstrategiaEvaluacionService {

    boolean moduloAprobado(Inscripcion inscripcion, Modulo modulo);

    Optional<BigDecimal> notaModulo(Inscripcion inscripcion, Modulo modulo);
}