package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.repository.InstanciaEvaluativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CenmaSedeEvaluacionService implements EstrategiaEvaluacionService {

    private static final BigDecimal NOTA_APROBACION = BigDecimal.valueOf(6);

    private final InstanciaEvaluativaRepository instanciaEvaluativaRepository;

    @Override
    public boolean moduloAprobado(Inscripcion inscripcion, Modulo modulo) {
        return tpAprobado(inscripcion, modulo) && evaluacionFinalAprobada(inscripcion, modulo);
    }

    @Override
    public Optional<BigDecimal> notaModulo(Inscripcion inscripcion, Modulo modulo) {
        return ultimaInstancia(inscripcion, modulo, TipoInstanciaEvaluativa.EVALUACION_FINAL)
                .map(InstanciaEvaluativa::getNota);
    }

    public boolean tpAprobado(Inscripcion inscripcion, Modulo modulo) {
        return ultimaInstancia(inscripcion, modulo, TipoInstanciaEvaluativa.TP_INTEGRADOR)
                .map(InstanciaEvaluativa::getNota)
                .map(nota -> nota.compareTo(NOTA_APROBACION) >= 0)
                .orElse(false);
    }

    private boolean evaluacionFinalAprobada(Inscripcion inscripcion, Modulo modulo) {
        return notaModulo(inscripcion, modulo)
                .map(nota -> nota.compareTo(NOTA_APROBACION) >= 0)
                .orElse(false);
    }

    private Optional<InstanciaEvaluativa> ultimaInstancia(
            Inscripcion inscripcion, Modulo modulo, TipoInstanciaEvaluativa tipo) {
        List<InstanciaEvaluativa> instancias = instanciaEvaluativaRepository
                .findByInscripcionIdAndModuloIdAndTipo(inscripcion.getId(), modulo.getId(), tipo);
        return instancias.stream().max(Comparator.comparing(InstanciaEvaluativa::getFecha));
    }
}