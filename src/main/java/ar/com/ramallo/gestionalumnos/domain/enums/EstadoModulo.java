package ar.com.ramallo.gestionalumnos.domain.enums;

/**
 * Estado de un módulo para una inscripción escolar. No se persiste: se calcula en cada consulta a partir de las
 * instancias evaluativas y del plan. REPROBADO queda para cuando existan ventanas de fecha por cohorte en Base.
 */
public enum EstadoModulo {
    APROBADO,
    PENDIENTE,
    /** Anterior al módulo de inicio del plan (Sede): no se exige para finalizar. */
    EXIMIDO
}
