package ar.com.ramallo.gestionalumnos.service.evaluacion;

import java.math.BigDecimal;

/**
 * Resultado de evaluar UN módulo de UNA inscripción con la regla de su estrategia.
 *
 * @param aprobado           la misma respuesta que {@link EstrategiaEvaluacionService#moduloAprobado(java.util.List)}
 * @param nota               la de {@code notaModulo}: promedio vigente en Base, evaluación final más reciente en Sede;
 *                           null si todavía no hay con qué calcularla
 * @param integradorAprobado el último TP integrador (Sede) o el último integrador interdisciplinario (Base), por fecha
 * @param notasVigentes      solo Base: notas que entran al promedio (sin integrador ni originales recuperadas); null en Sede
 * @param notasMinimas       solo Base: mínimo de notas exigido por módulo; null en Sede
 */
public record EvaluacionModulo(
        boolean aprobado,
        BigDecimal nota,
        boolean integradorAprobado,
        Integer notasVigentes,
        Integer notasMinimas) {
}
