package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.Modulo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EstrategiaEvaluacionService {

    boolean moduloAprobado(Inscripcion inscripcion, Modulo modulo);

    /**
     * La misma regla de {@link #moduloAprobado(Inscripcion, Modulo)} sobre instancias ya cargadas: recibe todas las
     * instancias de UNA inscripción en UN módulo y no consulta la base. Existe para evaluar listados completos
     * con dos consultas en total en lugar de varias por inscripción y por módulo.
     */
    boolean moduloAprobado(List<InstanciaEvaluativa> instanciasDelModulo);

    Optional<BigDecimal> notaModulo(Inscripcion inscripcion, Modulo modulo);
}