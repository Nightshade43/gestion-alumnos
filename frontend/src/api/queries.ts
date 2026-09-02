import { useMutation, useQuery, useQueryClient, useQueries } from '@tanstack/react-query';
import * as ep from './endpoints';
import type * as T from './types';

export const qk = {
  personas: ['personas'] as const,
  persona: (id: number) => ['personas', id] as const,
  programas: ['programas'] as const,
  instituciones: ['instituciones'] as const,
  empresas: ['empresas'] as const,
  modulos: (pid: number) => ['modulos', pid] as const,
  planes: (pid: number) => ['planes', pid] as const,
  grupos: (pid: number) => ['grupos', pid] as const,
  inscripcionesPorPersona: (pid: number) => ['inscripciones', 'persona', pid] as const,
  inscripcion: (id: number) => ['inscripciones', id] as const,
  contrato: (id: number) => ['contratos', id] as const,
  instancias: (iid: number) => ['instancias', iid] as const,
  seguimientos: (iid: number) => ['seguimientos', iid] as const,
};

export const usePersonas = () => useQuery({ queryKey: qk.personas, queryFn: ep.personas.list });
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
 * WORKAROUND al gap del backend: no existe GET /api/inscripciones sin personaId.
 * Se hace fan-out: personas -> N requests por persona -> se aplana en memoria.
 * Aceptable con el volumen actual (un docente). Si el backend agrega el listado
 * global, reemplazar por una sola query y borrar este hook.
 */
export function useTodasLasInscripciones() {
  const personas = usePersonas();
  const ids = (personas.data ?? []).map((p) => p.id);
  const results = useQueries({
    queries: ids.map((id) => ({
      queryKey: qk.inscripcionesPorPersona(id),
      queryFn: () => ep.inscripciones.byPersona(id),
    })),
  });
  return {
    isPending: personas.isPending || results.some((r) => r.isPending),
    error: personas.error ?? results.find((r) => r.error)?.error ?? null,
    data: results.flatMap((r) => r.data ?? []) as T.InscripcionResponse[],
  };
}

/** Invalidación: toda transición de estado toca la inscripción y el listado de su persona. */
function useInscripcionMutation(fn: (id: number) => Promise<T.InscripcionResponse>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: (ins) => {
      qc.setQueryData(qk.inscripcion(ins.id), ins);
      qc.invalidateQueries({ queryKey: qk.inscripcionesPorPersona(ins.personaId) });
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
    onSuccess: (c) => qc.setQueryData(qk.contrato(c.id), c),
  });
}

export function useAmpliarCupo(contratoId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (clasesAdicionales: number) => ep.contratos.ampliarCupo(contratoId, clasesAdicionales),
    onSuccess: (c) => qc.setQueryData(qk.contrato(c.id), c),
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
