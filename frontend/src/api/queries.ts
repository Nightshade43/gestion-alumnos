import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query';
import * as ep from './endpoints';
import type * as T from './types';

export const qk = {
  personas: ['personas'] as const,
  persona: (id: number) => ['personas', id] as const,
  programas: ['programas'] as const,
  programa: (id: number) => ['programas', id] as const,
  instituciones: ['instituciones'] as const,
  empresas: ['empresas'] as const,
  modulos: (pid: number) => ['modulos', pid] as const,
  planes: (pid: number) => ['planes', pid] as const,
  grupos: (pid: number) => ['grupos', pid] as const,
  inscripciones: (f: { personaId?: number; categoria?: T.CategoriaPrograma } = {}) =>
    ['inscripciones', 'list', f.personaId ?? null, f.categoria ?? null] as const,
  inscripcionesPorPersona: (pid: number) => ['inscripciones', 'persona', pid] as const,
  contratos: ['contratos'] as const,
  inscripcion: (id: number) => ['inscripciones', id] as const,
  contrato: (id: number) => ['contratos', id] as const,
  instancias: (iid: number) => ['instancias', iid] as const,
  seguimientos: (iid: number) => ['seguimientos', iid] as const,
};

export const usePersonas = () => useQuery({ queryKey: qk.personas, queryFn: ep.personas.list });
export const usePersona = (id?: number) => useQuery({
  queryKey: qk.persona(id!), queryFn: () => ep.personas.get(id!), enabled: !!id,
});
export const useProgramas = () => useQuery({ queryKey: qk.programas, queryFn: ep.programas.list });
export const useInstituciones = () => useQuery({ queryKey: qk.instituciones, queryFn: ep.instituciones.list });
export const useEmpresas = () => useQuery({ queryKey: qk.empresas, queryFn: ep.empresas.list });

export const useModulos = (programaId?: number) => useQuery({
  queryKey: qk.modulos(programaId!), queryFn: () => ep.modulos.byPrograma(programaId!), enabled: !!programaId,
});
export const usePlanes = (programaId?: number) => useQuery({
  queryKey: qk.planes(programaId!), queryFn: () => ep.planes.byPrograma(programaId!), enabled: !!programaId,
});
export const useGrupos = (programaId?: number) => useQuery({
  queryKey: qk.grupos(programaId!), queryFn: () => ep.grupos.byPrograma(programaId!), enabled: !!programaId,
});

export const useInscripcion = (id?: number) => useQuery({
  queryKey: qk.inscripcion(id!), queryFn: () => ep.inscripciones.get(id!), enabled: !!id,
});
export const useSeguimientos = (iid?: number) => useQuery({
  queryKey: qk.seguimientos(iid!), queryFn: () => ep.seguimientos.byInscripcion(iid!), enabled: !!iid,
});
export const useInstancias = (iid?: number) => useQuery({
  queryKey: qk.instancias(iid!), queryFn: () => ep.instanciasEvaluativas.byInscripcion(iid!), enabled: !!iid,
});
export const useContrato = (id?: number | null) => useQuery({
  queryKey: qk.contrato(id!), queryFn: () => ep.contratos.get(id!), enabled: !!id,
});

/**
 * Listado global. `categoria` filtra en el backend, así que las pantallas
 * "Inscripciones escolares" / "Inscripciones particulares" piden solo su rama.
 */
export const useInscripciones = (f: { personaId?: number; categoria?: T.CategoriaPrograma } = {}) =>
  useQuery({ queryKey: qk.inscripciones(f), queryFn: () => ep.inscripciones.list(f) });

export const useContratos = () => useQuery({ queryKey: qk.contratos, queryFn: ep.contratos.list });

/**
 * La estrategia de evaluación del programa decide TODA la UI académica
 * (tipos de instancia, promedio vs nota final, planes en el alta), así que
 * el detalle de una inscripción escolar necesita el programa, no solo su nombre.
 */
export const usePrograma = (id?: number) => useQuery({
  queryKey: qk.programa(id!), queryFn: () => ep.programas.get(id!), enabled: !!id,
});

export function useCrearInstancia(inscripcionId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.instanciasEvaluativas.create,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.instancias(inscripcionId) });
      qc.invalidateQueries({ queryKey: qk.inscripcion(inscripcionId) });
    },
  });
}

export function useLogin() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ep.auth.login, onSuccess: () => qc.clear() });
}

export function useCrearInscripcion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.inscripciones.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['inscripciones'] }),
  });
}

export function useFinalizarContrato(contratoId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => ep.contratos.finalizar(contratoId),
    onSuccess: (c) => {
      qc.setQueryData(qk.contrato(c.id), c);
      qc.invalidateQueries({ queryKey: qk.contratos });
    },
  });
}

/** Invalidación: toda transición de estado toca la inscripción y todos los listados. */
function useInscripcionMutation(fn: (id: number) => Promise<T.InscripcionResponse>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: (ins) => {
      qc.setQueryData(qk.inscripcion(ins.id), ins);
      qc.invalidateQueries({ queryKey: ['inscripciones'] });
    },
  });
}

export const usePausar    = () => useInscripcionMutation(ep.inscripciones.pausar);
export const useReanudar  = () => useInscripcionMutation(ep.inscripciones.reanudar);
export const useFinalizar = () => useInscripcionMutation(ep.inscripciones.finalizar);
export const useCancelar  = () => useInscripcionMutation(ep.inscripciones.cancelar);

export function useCrearPersona() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.personas.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.personas }),
  });
}

export function useConsumirClase(contratoId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => ep.contratos.consumirClase(contratoId),
    onSuccess: (c) => {
      qc.setQueryData(qk.contrato(c.id), c);
      qc.invalidateQueries({ queryKey: qk.contratos });
    },
  });
}

export function useAmpliarCupo(contratoId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (clasesAdicionales: number) => ep.contratos.ampliarCupo(contratoId, clasesAdicionales),
    onSuccess: (c) => {
      qc.setQueryData(qk.contrato(c.id), c);
      qc.invalidateQueries({ queryKey: qk.contratos });
    },
  });
}

export function useCrearSeguimiento(inscripcionId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: Omit<T.SeguimientoRequest, 'inscripcionId'>) =>
      ep.seguimientos.create({ ...b, inscripcionId }),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.seguimientos(inscripcionId) }),
  });
}

/**
 * La API no expone un listado global de seguimientos (solo ?inscripcionId=), así que
 * Inicio pide los de un conjunto ACOTADO de inscripciones (las 6 activas más recientes).
 * Si el backend agrega GET /api/seguimientos sin filtro, esto pasa a ser una sola request.
 */
export const useSeguimientosDe = (inscripcionIds: number[]) => useQueries({
  queries: inscripcionIds.map((id) => ({
    queryKey: qk.seguimientos(id),
    queryFn: () => ep.seguimientos.byInscripcion(id),
  })),
});

/** PUT reemplaza el registro completo. El nombre viaja denormalizado en las inscripciones. */
export function useActualizarPersona(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (b: T.PersonaRequest) => ep.personas.update(id, b),
    onSuccess: (p) => {
      qc.setQueryData(qk.persona(p.id), p);
      qc.invalidateQueries({ queryKey: qk.personas });
      qc.invalidateQueries({ queryKey: ['inscripciones'] });
    },
  });
}

export function useCrearPrograma() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.programas.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.programas }),
  });
}

export function useCrearModulo(programaId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.modulos.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.modulos(programaId) }),
  });
}

export function useCrearPlan(programaId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.planes.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.planes(programaId) }),
  });
}

export function useCrearGrupo(programaId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.grupos.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.grupos(programaId) }),
  });
}

export function useCrearInstitucion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.instituciones.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.instituciones }),
  });
}

/** POST /api/contratos — individual: una sola inscripción particular sin contrato previo. */
export function useCrearContratoIndividual() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.contratos.createIndividual,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.contratos });
      // contratoId de la inscripción cambia: los listados de inscripciones quedan viejos.
      qc.invalidateQueries({ queryKey: ['inscripciones'] });
    },
  });
}

/** POST /api/contratos/empresa — varias inscripciones sobre un mismo pool. */
export function useCrearContratoEmpresa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.contratos.createEmpresa,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.contratos });
      qc.invalidateQueries({ queryKey: ['inscripciones'] });
    },
  });
}

export function useCrearEmpresa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ep.empresas.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.empresas }),
  });
}
