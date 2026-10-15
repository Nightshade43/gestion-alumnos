package ar.com.ramallo.gestionalumnos.service.evaluacion;

import ar.com.ramallo.gestionalumnos.domain.Inscripcion;
import ar.com.ramallo.gestionalumnos.domain.InstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.domain.Modulo;
import ar.com.ramallo.gestionalumnos.domain.enums.TipoInstanciaEvaluativa;
import ar.com.ramallo.gestionalumnos.repository.InstanciaEvaluativaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
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
        return moduloAprobado(instanciasDe(inscripcion, modulo));
    }

    @Override
    public boolean moduloAprobado(List<InstanciaEvaluativa> instancias) {
        List<InstanciaEvaluativa> vigentes = notasVigentesParaPromedio(instancias);
        if (vigentes.size() < MINIMO_NOTAS_POR_MODULO) {
            return false;
        }
        boolean promedioAprobado = promedio(vigentes)
                .map(nota -> nota.compareTo(NOTA_APROBACION) >= 0)
                .orElse(false);
        return promedioAprobado && integradorAprobado(instancias);
    }

    /**
     * El integrador interdisciplinario lleva nota propia y se aprueba con 6 o más. Si se rinde
     * de nuevo vale el más reciente. No entra al promedio: es un requisito aparte de las 3 notas.
     */
    public boolean integradorAprobado(Inscripcion inscripcion, Modulo modulo) {
        return integradorAprobado(instanciasDe(inscripcion, modulo));
    }

    private boolean integradorAprobado(List<InstanciaEvaluativa> instancias) {
        return instancias.stream()
                .filter(instancia -> instancia.getTipo() == TipoInstanciaEvaluativa.INTEGRADOR)
                .max(Comparator.comparing(InstanciaEvaluativa::getFecha))
                .map(InstanciaEvaluativa::getNota)
                .map(nota -> nota.compareTo(NOTA_APROBACION) >= 0)
                .orElse(false);
    }

    @Override
    public Optional<BigDecimal> notaModulo(Inscripcion inscripcion, Modulo modulo) {
        return promedio(notasVigentesParaPromedio(instanciasDe(inscripcion, modulo)));
    }

    public DetalleNotasModulo detalleNotas(Inscripcion inscripcion, Modulo modulo) {
        List<InstanciaEvaluativa> todasLasNotas = instanciasDe(inscripcion, modulo);
        return new DetalleNotasModulo(todasLasNotas, promedio(notasVigentesParaPromedio(todasLasNotas)));
    }

    private List<InstanciaEvaluativa> instanciasDe(Inscripcion inscripcion, Modulo modulo) {
        return instanciaEvaluativaRepository.findByInscripcionIdAndModuloId(inscripcion.getId(), modulo.getId());
    }

    private List<InstanciaEvaluativa> notasVigentesParaPromedio(List<InstanciaEvaluativa> todas) {
        Set<Long> reemplazadasPorRecuperatorio = todas.stream()
                .map(InstanciaEvaluativa::getRecuperaA)
                .filter(Objects::nonNull)
                .map(InstanciaEvaluativa::getId)
                .collect(Collectors.toSet());

        return todas.stream()
                // Por tipo y no solo por el flag: cuentaParaPromedio es true por defecto en la API y un
                // INTEGRADOR cargado sin indicarlo no debe contaminar el promedio de las notas.
                .filter(instancia -> instancia.getTipo() == TipoInstanciaEvaluativa.NOTA)
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