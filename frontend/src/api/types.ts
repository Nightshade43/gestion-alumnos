// Tipos derivados 1:1 de los records de src/main/java/.../web/dto del backend
// (Nightshade43/gestion-alumnos@master). No agregar campos que la API no devuelve.

export type CategoriaPrograma = 'ESCOLAR' | 'PARTICULAR';
export type EstrategiaEvaluacion = 'CENMA_BASE' | 'CENMA_SEDE' | 'SEGUIMIENTO_LIBRE';
export type EstadoInscripcion = 'ACTIVA' | 'PAUSADA' | 'FINALIZADA' | 'CANCELADA';
export type TipoFacturacion = 'POR_CLASE' | 'PAQUETE' | 'MENSUAL';
export type EstadoContrato = 'ACTIVO' | 'FINALIZADO';
export type TipoInstanciaEvaluativa = 'NOTA' | 'INTEGRADOR' | 'TP_INTEGRADOR' | 'EVALUACION_FINAL';

/** ISO date, "2026-03-01" (LocalDate en el backend — nunca datetime). */
export type IsoDate = string;
/** BigDecimal serializado por Jackson como number. Formatear siempre con 2 decimales. */
export type Decimal = number;

export interface LoginRequest { username: string; password: string }
export interface LoginResponse { token: string }

/** Shape único de error de toda la API (web/ErrorResponse.java). */
export interface ErrorResponse {
  timestamp: string;   // Instant
  status: number;
  error: string;       // reason phrase
  message: string;
}

export interface PersonaRequest { nombre: string; email?: string | null; telefono?: string | null; documento?: string | null }
export interface PersonaResponse { id: number; nombre: string; email: string | null; telefono: string | null; documento: string | null }

export interface InstitucionRequest { nombre: string }
export interface InstitucionResponse { id: number; nombre: string }

export interface ProgramaRequest {
  nombre: string; categoria: CategoriaPrograma;
  estrategiaEvaluacion: EstrategiaEvaluacion; institucionId?: number | null;
}
export interface ProgramaResponse {
  id: number; nombre: string; categoria: CategoriaPrograma;
  estrategiaEvaluacion: EstrategiaEvaluacion;
  institucionId: number | null; institucionNombre: string | null;
}

export interface ModuloRequest { programaId: number; orden: number; esSecuencial: boolean }
export interface ModuloResponse { id: number; programaId: number; orden: number; esSecuencial: boolean }

export interface PlanRequest { programaId: number; codigo: string; moduloInicio: number }
export interface PlanResponse { id: number; programaId: number; codigo: string; moduloInicio: number }

export interface GrupoRequest { programaId: number; dia: string; horario: string }
export interface GrupoResponse { id: number; programaId: number; dia: string; horario: string }

export interface InscripcionRequest {
  personaId: number; programaId: number;
  planId?: number | null; grupoId?: number | null; fechaInicio: IsoDate;
}
export interface InscripcionResponse {
  id: number; personaId: number; personaNombre: string;
  programaId: number; programaNombre: string;
  planCodigo: string | null; grupoDia: string | null; grupoHorario: string | null;
  fechaInicio: IsoDate; fechaFin: IsoDate | null; estado: EstadoInscripcion;
}

export interface EmpresaRequest { nombre: string; contacto?: string | null }
export interface EmpresaResponse { id: number; nombre: string; contacto: string | null }

export interface ContratoRequest { inscripcionId: number; tipoFacturacion: TipoFacturacion; clasesContratadas?: number | null }
export interface ContratoEmpresaRequest {
  empresaId: number; tipoFacturacion: TipoFacturacion;
  clasesContratadas: number; inscripcionIds: number[];
}
export interface EmpleadoCubierto { inscripcionId: number; personaNombre: string }
export interface ContratoResponse {
  id: number; empresaId: number | null; empresaNombre: string | null;
  tipoFacturacion: TipoFacturacion; clasesContratadas: number; clasesConsumidas: number;
  estado: EstadoContrato; inscripciones: EmpleadoCubierto[];
}

export interface InstanciaEvaluativaRequest {
  inscripcionId: number; moduloId: number; tipo: TipoInstanciaEvaluativa;
  nota?: Decimal | null; fecha: IsoDate; cuentaParaPromedio?: boolean | null; recuperaAId?: number | null;
}
export interface InstanciaEvaluativaResponse {
  id: number; inscripcionId: number; moduloId: number; tipo: TipoInstanciaEvaluativa;
  nota: Decimal | null; fecha: IsoDate; cuentaParaPromedio: boolean; recuperaAId: number | null;
}

export interface SeguimientoRequest { inscripcionId: number; fecha: IsoDate; observacion: string }
export interface SeguimientoResponse { id: number; inscripcionId: number; fecha: IsoDate; observacion: string }
