package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.repository.InstanciaEvaluativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenmaBaseEvaluacionService implements EstrategiaEvaluacionService {

    private static final int MINIMO_NOTAS_POR_MODULO = 3;
    private static final BigDecimal NOTA_APROBACION = BigDecimal.valueOf(6);

    private final InstanciaEvaluativaRepository instanciaEvaluativaRepository;

    public record DetalleNotasModulo(List<InstanciaEvaluativa> notas, Optional<BigDecimal> promedio) {}

    @Override
    public boolean moduloAprobado(Inscripcion inscripcion, Modulo modulo) {
        List<InstanciaEvaluativa> vigentes = notasVigentesParaPromedio(inscripcion, modulo);
        if (vigentes.size() < MINIMO_NOTAS_POR_MODULO) {
            return false;
        }
        return promedio(vigentes)
                .map(nota -> nota.compareTo(NOTA_APROBACION) >= 0)
                .orElse(false);
    }

    @Override
    public Optional<BigDecimal> notaModulo(Inscripcion inscripcion, Modulo modulo) {
        return promedio(notasVigentesParaPromedio(inscripcion, modulo));
    }

    public DetalleNotasModulo detalleNotas(Inscripcion inscripcion, Modulo modulo) {
        List<InstanciaEvaluativa> todasLasNotas = instanciaEvaluativaRepository
                .findByInscripcionIdAndModuloId(inscripcion.getId(), modulo.getId());
        return new DetalleNotasModulo(todasLasNotas, notaModulo(inscripcion, modulo));
    }

    private List<InstanciaEvaluativa> notasVigentesParaPromedio(Inscripcion inscripcion, Modulo modulo) {
        List<InstanciaEvaluativa> todas = instanciaEvaluativaRepository
                .findByInscripcionIdAndModuloId(inscripcion.getId(), modulo.getId());

        Set<Long> reemplazadasPorRecuperatorio = todas.stream()
                .map(InstanciaEvaluativa::getRecuperaA)
                .filter(Objects::nonNull)
                .map(InstanciaEvaluativa::getId)
                .collect(Collectors.toSet());

        return todas.stream()
                .filter(InstanciaEvaluativa::isCuentaParaPromedio)
                .filter(instancia -> !reemplazadasPorRecuperatorio.contains(instancia.getId()))
                .toList();
    }

    private Optional<BigDecimal> promedio(List<InstanciaEvaluativa> notas) {
        if (notas.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal suma = notas.stream()
                .map(InstanciaEvaluativa::getNota)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(suma.divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP));
    }
}