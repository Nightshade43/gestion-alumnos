import React from 'react';
import {
  usePrograma, useModulos, usePlanes, useGrupos,
  useCrearModulo, useCrearPlan, useCrearGrupo,
} from '../api/queries';
import { Button, Card, EmptyState, Field, Input, Modal, MonoChip, Select, SkeletonList, thStyle, tdStyle } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler } from '../components/ErrorSurface';
import { useToasts } from '../components/Toasts';
import type { Route } from '../shell/AppShell';
import type { ProgramaResponse } from '../api/types';

type Pestania = 'modulos' | 'planes' | 'grupos';

/**
 * Detalle de programa. Las tres pestañas son los tres controllers hijos
 * (ModuloController, PlanController, GrupoController), que solo se consultan
 * por programaId — no tienen sentido como pantallas globales.
 *
 * "Planes" se renderiza SOLO en CENMA_SEDE: es la única línea que exime módulos
 * anteriores. En Base y en los particulares la pestaña no existe (no se muestra vacía).
 */
export function ProgramaScreen({ id, onNavigate }: { id: number; onNavigate: (r: Route) => void }) {
  const { data: programa, isPending, error } = usePrograma(id);
  const [tab, setTab] = React.useState<Pestania>('modulos');

  if (isPending) return <Card><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={() => onNavigate({ name: 'programas' })} entidad="programa" />;
  if (!programa) return null;

  const sede = programa.estrategiaEvaluacion === 'CENMA_SEDE';
  const escolar = programa.categoria === 'ESCOLAR';
  const tabs: { key: Pestania; label: string }[] = [
    { key: 'modulos', label: 'Módulos' },
    ...(sede ? [{ key: 'planes' as const, label: 'Planes' }] : []),
    { key: 'grupos', label: 'Grupos' },
  ];
  const activa = tabs.some((t) => t.key === tab) ? tab : 'modulos';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <span
        onClick={() => onNavigate({ name: 'programas' })}
        style={{ fontSize: 13, color: 'var(--ga-muted)', cursor: 'pointer', width: 'fit-content' }}
      >← Programas</span>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <h1 style={{ margin: 0, fontSize: 30, fontWeight: 600, letterSpacing: '-.025em' }}>{programa.nombre}</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: 11, flexWrap: 'wrap', fontSize: 14.5, color: 'var(--ga-muted)' }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 7, color: escolar ? 'var(--ga-escolar-fg)' : 'var(--ga-particular-fg)' }}>
            <span style={{ width: 8, height: 8, borderRadius: 3, background: escolar ? 'var(--ga-escolar)' : 'var(--ga-particular)' }} />
            {escolar ? 'Escolar' : 'Particular'}
          </span>
          <MonoChip>{programa.estrategiaEvaluacion}</MonoChip>
          <span>{programa.institucionNombre ?? 'Sin institución'}</span>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 2, borderBottom: '1px solid var(--ga-line)', marginBottom: -1 }}>
        {tabs.map((t) => {
          const on = t.key === activa;
          return (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              style={{
                border: 'none', background: 'transparent', cursor: 'pointer', fontFamily: 'inherit',
                fontSize: 14, padding: '10px 16px', marginBottom: -1,
                color: on ? 'var(--ga-primary-700)' : 'var(--ga-muted)',
                fontWeight: on ? 600 : 500,
                borderBottom: '2px solid ' + (on ? 'var(--ga-primary-600)' : 'transparent'),
              }}
            >{t.label}</button>
          );
        })}
      </div>

      {activa === 'modulos' && <Modulos programa={programa} />}
      {activa === 'planes' && <Planes programa={programa} />}
      {activa === 'grupos' && <Grupos programa={programa} />}
    </div>
  );
}

function Tabla({ headers, children }: { headers: string[]; children: React.ReactNode }) {
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', minWidth: 480, borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: 'var(--ga-surface-2)' }}>
            {headers.map((h) => <th key={h} style={thStyle}>{h}</th>)}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  );
}

const fila: React.CSSProperties = { borderTop: '1px solid var(--ga-line-soft)' };
const mono: React.CSSProperties = { ...tdStyle, fontFamily: 'var(--ga-font-mono)', fontSize: 13.5, color: 'var(--ga-muted)' };

function Modulos({ programa }: { programa: ProgramaResponse }) {
  const { data, isPending, error, refetch } = useModulos(programa.id);
  const [nuevo, setNuevo] = React.useState(false);
  const escolar = programa.categoria === 'ESCOLAR';
  const modulos = [...(data ?? [])].sort((a, b) => a.orden - b.orden);

  return (
    <Card
      title="Módulos"
      meta={escolar ? 'Definen el avance académico' : undefined}
      actions={escolar ? <Button variant="secondary" onClick={() => setNuevo(true)}>Nuevo módulo</Button> : undefined}
    >
      {isPending
        ? <SkeletonList rows={3} />
        : error
          ? <ErrorScreen error={error} onRetry={refetch} entidad="listado" />
          : !escolar
            ? <EmptyState
                title="Este programa no usa módulos"
                text="Los programas particulares no usan módulos: el progreso se registra en Seguimiento, dentro de cada inscripción." />
            : modulos.length === 0
              ? <EmptyState
                  title="Todavía no hay módulos"
                  text="Sin módulos no se pueden cargar notas: las instancias evaluativas se asocian siempre a un módulo."
                  action={<Button onClick={() => setNuevo(true)}>Nuevo módulo</Button>} />
              : <Tabla headers={['Orden', 'Secuencia', 'Regla']}>
                  {modulos.map((m) => (
                    <tr key={m.id} style={fila}>
                      <td style={tdStyle}>Módulo {m.orden}</td>
                      <td style={mono}>{m.esSecuencial ? 'Secuencial' : 'Independiente'}</td>
                      <td style={{ ...tdStyle, color: 'var(--ga-muted)', fontSize: 13.5 }}>
                        {m.esSecuencial
                          ? 'Requiere aprobar el anterior'
                          : 'Se puede cursar en cualquier momento'}
                      </td>
                    </tr>
                  ))}
                </Tabla>}
      {nuevo && <NuevoModuloModal programaId={programa.id} siguienteOrden={(modulos[modulos.length - 1]?.orden ?? 0) + 1} onClose={() => setNuevo(false)} />}
    </Card>
  );
}

function Planes({ programa }: { programa: ProgramaResponse }) {
  const { data, isPending, error, refetch } = usePlanes(programa.id);
  const modulos = useModulos(programa.id);
  const [nuevo, setNuevo] = React.useState(false);

  return (
    <Card
      title="Planes"
      meta="Solo CENMA Sede"
      actions={<Button variant="secondary" onClick={() => setNuevo(true)}>Nuevo plan</Button>}
    >
      {isPending
        ? <SkeletonList rows={3} />
        : error
          ? <ErrorScreen error={error} onRetry={refetch} entidad="listado" />
          : (data ?? []).length === 0
            ? <EmptyState
                title="Todavía no hay planes"
                text="Un plan arranca en un módulo y exime los anteriores: es lo que permite inscribir a alguien que ya cursó parte del ciclo."
                action={<Button onClick={() => setNuevo(true)}>Nuevo plan</Button>} />
            : <Tabla headers={['Código', 'Módulo de inicio', 'Efecto']}>
                {(data ?? []).map((p) => (
                  <tr key={p.id} style={fila}>
                    <td style={tdStyle}>Plan {p.codigo}</td>
                    <td style={mono}>Módulo {p.moduloInicio}</td>
                    <td style={{ ...tdStyle, color: 'var(--ga-muted)', fontSize: 13.5 }}>
                      {p.moduloInicio > 1
                        ? 'Exime los módulos anteriores al ' + p.moduloInicio
                        : 'Cursa el ciclo completo'}
                    </td>
                  </tr>
                ))}
              </Tabla>}
      {nuevo && <NuevoPlanModal programaId={programa.id} modulos={(modulos.data ?? []).map((m) => m.orden).sort((a, b) => a - b)} onClose={() => setNuevo(false)} />}
    </Card>
  );
}

function Grupos({ programa }: { programa: ProgramaResponse }) {
  const { data, isPending, error, refetch } = useGrupos(programa.id);
  const [nuevo, setNuevo] = React.useState(false);
  const sede = programa.estrategiaEvaluacion === 'CENMA_SEDE';

  return (
    <Card
      title="Grupos"
      meta="Agenda de cursado"
      actions={<Button variant="secondary" onClick={() => setNuevo(true)}>Nuevo grupo</Button>}
    >
      {isPending
        ? <SkeletonList rows={3} />
        : error
          ? <ErrorScreen error={error} onRetry={refetch} entidad="listado" />
          : (data ?? []).length === 0
            ? <EmptyState
                title="Todavía no hay grupos"
                text="El grupo es informativo: ordena el cursado pero no afecta el avance ni las notas."
                action={<Button onClick={() => setNuevo(true)}>Nuevo grupo</Button>} />
            : <Tabla headers={['Día', 'Horario', 'Observación']}>
                {(data ?? []).map((g) => (
                  <tr key={g.id} style={fila}>
                    <td style={tdStyle}>{g.dia}</td>
                    <td style={mono}>{g.horario}</td>
                    <td style={{ ...tdStyle, color: 'var(--ga-muted)', fontSize: 13.5 }}>
                      {sede ? 'Informativo — no afecta el avance' : 'Único grupo del ciclo lectivo'}
                    </td>
                  </tr>
                ))}
              </Tabla>}
      {nuevo && <NuevoGrupoModal programaId={programa.id} onClose={() => setNuevo(false)} />}
    </Card>
  );
}

/* ─────────────── Altas de los tres hijos ─────────────── */

function NuevoModuloModal({ programaId, siguienteOrden, onClose }: {
  programaId: number; siguienteOrden: number; onClose: () => void;
}) {
  const crear = useCrearModulo(programaId);
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [orden, setOrden] = React.useState(String(siguienteOrden));
  const [secuencial, setSecuencial] = React.useState('true');

  return (
    <AltaModal
      title="Nuevo módulo"
      bajada="El orden define la secuencia del ciclo. Las instancias evaluativas se cargan siempre contra un módulo."
      onClose={onClose}
      pendiente={crear.isPending}
      onSubmit={() => crear.mutate(
        { programaId, orden: Number(orden), esSecuencial: secuencial === 'true' },
        {
          onSuccess: () => { push({ tone: 'ok', title: 'Módulo creado', text: '201 · Módulo ' + orden + '.' }); onClose(); },
          onError: (e) => handle(e),
        })}
    >
      <Field label="Orden" required error={fieldErrors.orden}>
        <Input mono type="number" min={1} value={orden} onChange={(e) => setOrden(e.target.value)} invalid={!!fieldErrors.orden} />
      </Field>
      <Field label="Secuencia" required hint="Secuencial exige aprobar el módulo anterior.">
        <Select value={secuencial} onChange={(e) => setSecuencial(e.target.value)}>
          <option value="true">Secuencial</option>
          <option value="false">Independiente</option>
        </Select>
      </Field>
    </AltaModal>
  );
}

function NuevoPlanModal({ programaId, modulos, onClose }: {
  programaId: number; modulos: number[]; onClose: () => void;
}) {
  const crear = useCrearPlan(programaId);
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [codigo, setCodigo] = React.useState('');
  const [moduloInicio, setModuloInicio] = React.useState(String(modulos[0] ?? 1));

  return (
    <AltaModal
      title="Nuevo plan"
      bajada="El plan exime los módulos anteriores al de inicio. Solo existe en CENMA Sede."
      onClose={onClose}
      pendiente={crear.isPending}
      onSubmit={() => crear.mutate(
        { programaId, codigo, moduloInicio: Number(moduloInicio) },
        {
          onSuccess: () => { push({ tone: 'ok', title: 'Plan creado', text: '201 · Plan ' + codigo + '.' }); onClose(); },
          onError: (e) => handle(e),
        })}
    >
      <Field label="Código" required error={fieldErrors.codigo}>
        <Input value={codigo} onChange={(e) => setCodigo(e.target.value)} invalid={!!fieldErrors.codigo} placeholder="B" />
      </Field>
      <Field label="Módulo de inicio" required error={fieldErrors.moduloInicio}>
        <Select value={moduloInicio} onChange={(e) => setModuloInicio(e.target.value)}>
          {(modulos.length ? modulos : [1]).map((o) => <option key={o} value={o}>Módulo {o}</option>)}
        </Select>
      </Field>
    </AltaModal>
  );
}

function NuevoGrupoModal({ programaId, onClose }: { programaId: number; onClose: () => void }) {
  const crear = useCrearGrupo(programaId);
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [dia, setDia] = React.useState('Lunes');
  const [horario, setHorario] = React.useState('');

  return (
    <AltaModal
      title="Nuevo grupo"
      bajada="Informativo: ordena el cursado pero no afecta el avance ni las notas."
      onClose={onClose}
      pendiente={crear.isPending}
      onSubmit={() => crear.mutate(
        { programaId, dia, horario },
        {
          onSuccess: () => { push({ tone: 'ok', title: 'Grupo creado', text: '201 · ' + dia + ' ' + horario }); onClose(); },
          onError: (e) => handle(e),
        })}
    >
      <Field label="Día" required error={fieldErrors.dia}>
        <Select value={dia} onChange={(e) => setDia(e.target.value)}>
          {['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'].map((d) => <option key={d} value={d}>{d}</option>)}
        </Select>
      </Field>
      <Field label="Horario" required error={fieldErrors.horario}>
        <Input mono value={horario} onChange={(e) => setHorario(e.target.value)} invalid={!!fieldErrors.horario} placeholder="19–21" />
      </Field>
    </AltaModal>
  );
}

/** Shell único de alta (SCREENS.md §14): header con bajada + grid de 2 columnas. */
function AltaModal({ title, bajada, children, onClose, onSubmit, pendiente }: {
  title: string; bajada: string; children: React.ReactNode;
  onClose: () => void; onSubmit: () => void; pendiente?: boolean;
}) {
  return (
    <Modal
      title={title}
      onClose={onClose}
      width={540}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button onClick={onSubmit} disabled={pendiente}>Crear</Button>
      </>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div style={{ gridColumn: 'span 2', fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>{bajada}</div>
        {children}
      </div>
    </Modal>
  );
}
