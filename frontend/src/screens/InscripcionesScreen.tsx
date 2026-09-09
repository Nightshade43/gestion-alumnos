import React from 'react';
import { useInscripciones, useContratos, usePersonas, useProgramas, usePlanes, useGrupos, useCrearInscripcion } from '../api/queries';
import { Button, Card, DataTable, EmptyState, Field, Input, inputStyle, Modal, Select, SkeletonList, StatusBadge } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler, BusinessRuleModal } from '../components/ErrorSurface';
import { useToasts } from '../components/Toasts';
import { ScreenHeader } from '../shell/AppShell';
import type { Route } from '../shell/AppShell';
import type { CategoriaPrograma, EstadoInscripcion, ContratoResponse } from '../api/types';

/**
 * Un solo componente para las dos ramas: son la MISMA plantilla con distintas
 * columnas, nunca una tabla universal con columnas condicionales sueltas.
 * El filtro de rama lo hace el backend (?categoria=), el de estado el cliente.
 */

const ESTADOS: (EstadoInscripcion | 'TODOS')[] = ['TODOS', 'ACTIVA', 'PAUSADA', 'FINALIZADA', 'CANCELADA'];
const LABEL: Record<string, string> = {
  TODOS: 'Todos los estados', ACTIVA: 'Activas', PAUSADA: 'Pausadas',
  FINALIZADA: 'Finalizadas', CANCELADA: 'Canceladas',
};

export function InscripcionesScreen({ rama, onNavigate }: { rama: CategoriaPrograma; onNavigate: (r: Route) => void }) {
  const esc = rama === 'ESCOLAR';
  const { data, isPending, error, refetch } = useInscripciones({ categoria: rama });
  // Solo la rama particular necesita los contratos para resolver empresa y clases.
  const contratos = useContratos();
  const [estado, setEstado] = React.useState<EstadoInscripcion | 'TODOS'>('TODOS');
  const [nueva, setNueva] = React.useState(false);

  const porId = React.useMemo(() => {
    const m = new Map<number, ContratoResponse>();
    (contratos.data ?? []).forEach((c) => m.set(c.id, c));
    return m;
  }, [contratos.data]);

  const header = (
    <ScreenHeader
      title={esc ? 'Inscripciones escolares' : 'Inscripciones particulares'}
      subtitle={esc
        ? 'CENMA Base y CENMA Sede — avance por módulos y notas.'
        : 'Clases individuales y de empresa — contratos y seguimiento.'}
      actions={<>
        <Select value={estado} onChange={(e) => setEstado(e.target.value as EstadoInscripcion | 'TODOS')}>
          {ESTADOS.map((v) => <option key={v} value={v}>{LABEL[v]}</option>)}
        </Select>
        <Button onClick={() => setNueva(true)}>Nueva inscripción</Button>
      </>}
    />
  );

  if (isPending) return <div style={wrap}>{header}<Card><SkeletonList /></Card></div>;
  if (error) return <div style={wrap}>{header}<Card><ErrorScreen error={error} onRetry={refetch} entidad="listado" /></Card></div>;

  const rows = (data ?? []).filter((i) => estado === 'TODOS' || i.estado === estado);

  return (
    <div style={wrap}>
      {header}
      <Card>
        <DataTable
          rows={rows}
          onRowClick={(i) => onNavigate({ name: 'inscripcion', id: i.id })}
          empty={estado === 'TODOS'
            ? <EmptyState
                title={esc ? 'Sin inscripciones escolares' : 'Sin inscripciones particulares'}
                text={esc
                  ? 'Creá la primera sobre un programa CENMA: el plan define desde qué módulo arranca el alumno.'
                  : 'Creá la primera y después asociala a un contrato individual o al pool de una empresa.'}
                action={<Button onClick={() => setNueva(true)}>Nueva inscripción</Button>} />
            : <EmptyState title="Ninguna inscripción con ese estado" text={`Probá con otro filtro: hoy no hay ${LABEL[estado].toLowerCase()}.`} />}
          columns={[
            { key: 'persona', header: esc ? 'Alumno' : 'Cliente', render: (i) => <strong style={{ fontWeight: 500 }}>{i.personaNombre}</strong> },
            { key: 'programa', header: 'Programa', render: (i) => i.programaNombre },
            esc
              // Base no usa planes: "Sin plan" es el dato, no un hueco.
              ? { key: 'plan', header: 'Plan', render: (i) => (i.planCodigo ? `Plan ${i.planCodigo}` : 'Sin plan') }
              : { key: 'contrato', header: 'Contrato', render: (i) => {
                  const c = i.contratoId ? porId.get(i.contratoId) : null;
                  if (!i.contratoId) return <span style={{ color: 'var(--ga-warn-fg)' }}>Sin contrato</span>;
                  return c ? (c.empresaNombre ?? 'Individual') : `#${i.contratoId}`;
                } },
            esc
              ? { key: 'grupo', header: 'Grupo', render: (i) => (i.grupoDia ? `${i.grupoDia} ${i.grupoHorario ?? ''}`.trim() : '—') }
              : { key: 'clases', header: 'Clases', mono: true, render: (i) => {
                  const c = i.contratoId ? porId.get(i.contratoId) : null;
                  return c ? `${c.clasesConsumidas} / ${c.clasesContratadas}` : '—';
                } },
            { key: 'inicio', header: 'Inicio', mono: true, render: (i) => i.fechaInicio },
            { key: 'estado', header: 'Estado', render: (i) => <StatusBadge estado={i.estado} /> },
          ]}
        />
      </Card>

      {nueva && <NuevaInscripcionModal rama={rama} onClose={() => setNueva(false)} />}
    </div>
  );
}

const wrap: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 22 };

/**
 * Alta. La API rechaza con 409 una segunda inscripción ESCOLAR activa/pausada
 * de la misma persona, y con 422 una categoría que no corresponde al programa:
 * los dos casos llegan como error de mutación, no se validan acá.
 */
export function NuevaInscripcionModal({ rama: ramaProp, personaFija, onClose }: {
  /** null = la elige el usuario (alta desde la ficha de una persona, que es transversal). */
  rama: CategoriaPrograma | null;
  personaFija?: { id: number; nombre: string };
  onClose: () => void;
}) {
  const personas = usePersonas();
  const programas = useProgramas();
  const crear = useCrearInscripcion();
  const { push } = useToasts();
  const { handle, fieldErrors, ruleError, clearRuleError } = useApiErrorHandler();

  const [rama, setRama] = React.useState<CategoriaPrograma | ''>(ramaProp ?? '');
  const [personaId, setPersonaId] = React.useState(personaFija ? String(personaFija.id) : '');
  const [programaId, setProgramaId] = React.useState('');
  const [planId, setPlanId] = React.useState('');
  const [grupoId, setGrupoId] = React.useState('');
  const [fechaInicio, setFechaInicio] = React.useState(new Date().toISOString().slice(0, 10));

  const disponibles = (programas.data ?? []).filter((p) => p.categoria === rama);
  const programa = disponibles.find((p) => String(p.id) === programaId);
  const esc = rama === 'ESCOLAR';
  // Los planes solo existen en CENMA_SEDE; los grupos son agenda, no afectan el avance.
  const tienePlanes = programa?.estrategiaEvaluacion === 'CENMA_SEDE';
  const esBase = programa?.estrategiaEvaluacion === 'CENMA_BASE';
  const planes = usePlanes(tienePlanes ? programa?.id : undefined);
  const grupos = useGrupos(esc ? programa?.id : undefined);

  const submit = () => {
    if (!personaId || !programaId) { push({ tone: 'warn', title: '400 · Faltan datos', text: 'personaId y programaId son obligatorios.' }); return; }
    crear.mutate(
      {
        personaId: Number(personaId), programaId: Number(programaId),
        planId: planId ? Number(planId) : null,
        grupoId: grupoId ? Number(grupoId) : null,
        fechaInicio,
      },
      {
        onSuccess: () => { push({ tone: 'ok', title: 'Inscripción creada', text: '201 · Estado inicial ACTIVA.' }); onClose(); },
        onError: (e) => handle(e),
      });
  };

  if (ruleError) return <BusinessRuleModal error={ruleError} onClose={() => { clearRuleError(); onClose(); }} />;

  return (
    <Modal
      title="Nueva inscripción"
      onClose={onClose}
      width={560}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button onClick={submit} disabled={crear.isPending}>Crear inscripción</Button>
      </>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div style={{ gridColumn: 'span 2' }}>
          {personaFija
            ? <Field label="Persona" hint="Viene de la ficha: el alta queda vinculada a esta persona.">
                <div style={{ ...inputStyle(), background: 'var(--ga-surface-2)', color: 'var(--ga-muted)' }}>{personaFija.nombre}</div>
              </Field>
            : <Field label="Persona" required error={fieldErrors.personaId}>
                <Select value={personaId} onChange={(e) => setPersonaId(e.target.value)}>
                  <option value="">Elegí una persona…</option>
                  {(personas.data ?? []).map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
                </Select>
              </Field>}
        </div>
        {/* Desde la ficha no hay rama de contexto: una persona puede tener las dos. */}
        {ramaProp === null && (
          <div style={{ gridColumn: 'span 2' }}>
            <Field label="Rama" required hint="Define qué programas se ofrecen y cómo se registra el avance.">
              <Select value={rama} onChange={(e) => { setRama(e.target.value as CategoriaPrograma); setProgramaId(''); setPlanId(''); setGrupoId(''); }}>
                <option value="">Elegí una rama…</option>
                <option value="ESCOLAR">Escolar — avance por módulos y notas</option>
                <option value="PARTICULAR">Particular — clases y seguimiento</option>
              </Select>
            </Field>
          </div>
        )}
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Programa" required error={fieldErrors.programaId}>
            <Select value={programaId} onChange={(e) => { setProgramaId(e.target.value); setPlanId(''); setGrupoId(''); }}>
              <option value="">Elegí un programa…</option>
              {disponibles.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
            </Select>
          </Field>
        </div>

        {tienePlanes && (
          <Field label="Plan" hint="Exime los módulos anteriores al de inicio.">
            <Select value={planId} onChange={(e) => setPlanId(e.target.value)}>
              <option value="">Sin plan</option>
              {(planes.data ?? []).map((pl) => <option key={pl.id} value={pl.id}>Plan {pl.codigo} — desde el módulo {pl.moduloInicio}</option>)}
            </Select>
          </Field>
        )}
        {/* Base manda planId vacío: no hay campo que ocultar, hay una regla que explicar. */}
        {esBase && (
          <div style={{
            gridColumn: 'span 2', display: 'flex', gap: 10, alignItems: 'flex-start',
            background: 'var(--ga-surface-2)', border: '1px solid var(--ga-line-soft)',
            borderRadius: 'var(--ga-radius-md)', padding: '11px 13px',
          }}>
            <span style={{ width: 18, height: 18, borderRadius: 999, flex: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, background: 'var(--ga-line-soft)', color: 'var(--ga-muted)', marginTop: 1 }}>i</span>
            <span style={{ fontSize: 13, lineHeight: 1.5, color: 'var(--ga-muted)' }}>
              Base no usa planes: se cursan los 2 módulos del ciclo lectivo completos y <code style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 12 }}>planId</code> se manda vacío.
            </span>
          </div>
        )}
        {esc && (
          <Field label="Grupo" hint={esBase
            ? 'Único grupo del ciclo lectivo. Informativo: no afecta el avance.'
            : 'Agenda de cursado. Informativo: no afecta el avance ni las notas.'}>
            <Select value={grupoId} onChange={(e) => setGrupoId(e.target.value)}>
              <option value="">Sin grupo</option>
              {(grupos.data ?? []).map((g) => <option key={g.id} value={g.id}>{g.dia} {g.horario}</option>)}
            </Select>
          </Field>
        )}

        <Field label="Fecha de inicio" required error={fieldErrors.fechaInicio}>
          <Input mono type="date" value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} />
        </Field>

        <div style={{ gridColumn: 'span 2', fontSize: 12.5, color: 'var(--ga-soft)', lineHeight: 1.5 }}>
          {tienePlanes
            ? 'El plan exime los módulos anteriores al de inicio. Si la persona ya tiene una inscripción escolar ACTIVA o PAUSADA, la API responde 409.'
            : esBase
              ? 'Base cursa los 2 módulos del ciclo lectivo completos. Si la persona ya tiene una inscripción escolar ACTIVA o PAUSADA, la API responde 409.'
              : esc
                ? 'Si la persona ya tiene una inscripción escolar ACTIVA o PAUSADA, la API responde 409 y el alta se rechaza.'
                : 'Las inscripciones particulares se crean sin plan, sin grupo y sin contrato: el contrato se asocia después desde Contratos, individualmente o a un pool de empresa.'}
        </div>
      </div>
    </Modal>
  );
}
