import { api, tokenStore } from './client';
import type * as T from './types';

/** Superficie completa de la API. Cada función corresponde a un endpoint real del backend. */
export const auth = {
  login: async (body: T.LoginRequest) => {
    const res = await api.post<T.LoginResponse>('/api/auth/login', body);
    tokenStore.set(res.token);
    return res;
  },
  logout: () => tokenStore.clear(),
};

export const personas = {
  list:   () => api.get<T.PersonaResponse[]>('/api/personas'),
  get:    (id: number) => api.get<T.PersonaResponse>(`/api/personas/${id}`),
  create: (b: T.PersonaRequest) => api.post<T.PersonaResponse>('/api/personas', b),
  update: (id: number, b: T.PersonaRequest) => api.put<T.PersonaResponse>(`/api/personas/${id}`, b),
  remove: (id: number) => api.del(`/api/personas/${id}`),
};

export const instituciones = {
  list:   () => api.get<T.InstitucionResponse[]>('/api/instituciones'),
  get:    (id: number) => api.get<T.InstitucionResponse>(`/api/instituciones/${id}`),
  create: (b: T.InstitucionRequest) => api.post<T.InstitucionResponse>('/api/instituciones', b),
};

export const programas = {
  list:   () => api.get<T.ProgramaResponse[]>('/api/programas'),
  get:    (id: number) => api.get<T.ProgramaResponse>(`/api/programas/${id}`),
  create: (b: T.ProgramaRequest) => api.post<T.ProgramaResponse>('/api/programas', b),
};

export const modulos = {
  byPrograma: (programaId: number) => api.get<T.ModuloResponse[]>(`/api/modulos?programaId=${programaId}`),
  create:     (b: T.ModuloRequest) => api.post<T.ModuloResponse>('/api/modulos', b),
};

export const planes = {
  byPrograma: (programaId: number) => api.get<T.PlanResponse[]>(`/api/planes?programaId=${programaId}`),
  create:     (b: T.PlanRequest) => api.post<T.PlanResponse>('/api/planes', b),
};

export const grupos = {
  byPrograma: (programaId: number) => api.get<T.GrupoResponse[]>(`/api/grupos?programaId=${programaId}`),
  create:     (b: T.GrupoRequest) => api.post<T.GrupoResponse>('/api/grupos', b),
};

export const inscripciones = {
  /** OJO: personaId es obligatorio en el backend. No existe "listar todas". Ver GAPS en el README. */
  byPersona:  (personaId: number) => api.get<T.InscripcionResponse[]>(`/api/inscripciones?personaId=${personaId}`),
  get:        (id: number) => api.get<T.InscripcionResponse>(`/api/inscripciones/${id}`),
  create:     (b: T.InscripcionRequest) => api.post<T.InscripcionResponse>('/api/inscripciones', b),
  pausar:     (id: number) => api.post<T.InscripcionResponse>(`/api/inscripciones/${id}/pausar`),
  reanudar:   (id: number) => api.post<T.InscripcionResponse>(`/api/inscripciones/${id}/reanudar`),
  finalizar:  (id: number) => api.post<T.InscripcionResponse>(`/api/inscripciones/${id}/finalizar`),
  cancelar:   (id: number) => api.post<T.InscripcionResponse>(`/api/inscripciones/${id}/cancelar`),
  cambiarGrupo: (id: number, grupoId: number) => api.patch<T.InscripcionResponse>(`/api/inscripciones/${id}/grupo?grupoId=${grupoId}`),
};

export const empresas = {
  list:   () => api.get<T.EmpresaResponse[]>('/api/empresas'),
  get:    (id: number) => api.get<T.EmpresaResponse>(`/api/empresas/${id}`),
  create: (b: T.EmpresaRequest) => api.post<T.EmpresaResponse>('/api/empresas', b),
};

export const contratos = {
  /** Solo por id: no hay GET de lista. Ver GAPS en el README. */
  get:           (id: number) => api.get<T.ContratoResponse>(`/api/contratos/${id}`),
  createIndividual: (b: T.ContratoRequest) => api.post<T.ContratoResponse>('/api/contratos', b),
  createEmpresa:    (b: T.ContratoEmpresaRequest) => api.post<T.ContratoResponse>('/api/contratos/empresa', b),
  consumirClase: (id: number) => api.post<T.ContratoResponse>(`/api/contratos/${id}/consumir-clase`),
  ampliarCupo:   (id: number, clasesAdicionales: number) => api.post<T.ContratoResponse>(`/api/contratos/${id}/ampliar-cupo?clasesAdicionales=${clasesAdicionales}`),
  finalizar:     (id: number) => api.post<T.ContratoResponse>(`/api/contratos/${id}/finalizar`),
};

export const instanciasEvaluativas = {
  byInscripcion: (inscripcionId: number, moduloId?: number) =>
    api.get<T.InstanciaEvaluativaResponse[]>(
      `/api/instancias-evaluativas?inscripcionId=${inscripcionId}` + (moduloId ? `&moduloId=${moduloId}` : '')),
  create: (b: T.InstanciaEvaluativaRequest) => api.post<T.InstanciaEvaluativaResponse>('/api/instancias-evaluativas', b),
};

export const seguimientos = {
  byInscripcion: (inscripcionId: number) => api.get<T.SeguimientoResponse[]>(`/api/seguimientos?inscripcionId=${inscripcionId}`),
  create: (b: T.SeguimientoRequest) => api.post<T.SeguimientoResponse>('/api/seguimientos', b),
};
