import React from 'react';
import {
  useInstituciones, useProgramas, useInscripciones,
  useModulos, useGrupos, useCrearInstitucion,
} from '../api/queries';
import { Button, Card, EmptyState, Field, Input, Modal, MonoChip, SkeletonList } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler } from '../components/ErrorSurface';
import { ScreenHeader, type Route } from '../shell/AppShell';
import { useToasts } from '../components/Toasts';
import type { ProgramaResponse } from '../api/types';

/**
 * Instituciones. Son pocas y de uso poco frecuente, así que no tienen pantalla
 * de detalle: cada fila se despliega en su lugar.
 *
 * Los programas se agrupan en cliente por institucionId (ProgramaResponse ya lo
 * trae). Módulos y grupos, en cambio, se piden por programa: se consultan SOLO
 * cuando la fila está abierta, para no hacer fan-out en el listado.
 */
export function InstitucionesScreen({ onNavigate }: { onNavigate: (r: Route) => void }) {
  const { data, isPending, error, refetch } = useInstituciones();
  const programas = useProgramas();
  const inscripciones = useInscripciones({ categoria: 'ESCOLAR' });
  const [abierta, setAbierta] = React.useState<number | null>(null);
  const [nueva, setNueva] = React.useState(false);

  if (isPending) return <Card title="Instituciones"><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={refetch} entidad="listado" />;

  const instituciones = data ?? [];
  const programasDe = (id: number) => (programas.data ?? []).filter((p) => p.institucionId === id);
  const activasDe = (programaId: number) =>
    (inscripciones.data ?? []).filter((i) => i.programaId === programaId && i.estado === 'ACTIVA').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <ScreenHeader
        title="Instituciones"
        subtitle="Dan marco a los programas escolares. Una institución sin programas no se usa en ninguna inscripción."
        actions={<Button onClick={() => setNueva(true)}>Nueva institución</Button>}
      />

      <Card>
        {instituciones.length === 0
          ? <EmptyState
              title="Todavía no hay instituciones"
              text="Los programas escolares pueden existir sin institución, pero el CENMA se registra acá."
              action={<Button onClick={() => setNueva(true)}>Nueva institución</Button>} />
          : <div style={{ display: 'flex', flexDirection: 'column' }}>
              {instituciones.map((inst) => {
                const propios = programasDe(inst.id);
                const activas = propios.reduce((a, p) => a + activasDe(p.id), 0);
                const abiertaEsta = abierta === inst.id;
                return (
                  <div key={inst.id} style={{ borderBottom: '1px solid var(--ga-line-soft)' }}>
                    <div
                      onClick={() => setAbierta(abiertaEsta ? null : inst.id)}
                      style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '15px 22px', cursor: 'pointer' }}
                    >
                      <div style={{
                        width: 34, height: 34, borderRadius: 11, flex: 'none',
                        background: 'var(--ga-escolar-bg)', color: 'var(--ga-escolar-fg)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 13, fontWeight: 600,
                      }}>{inst.nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase()}</div>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 0 }}>
                        <span style={{ fontSize: 15, fontWeight: 500 }}>{inst.nombre}</span>
                        <span style={{ fontSize: 13, color: 'var(--ga-muted)' }}>
                          {propios.length ? propios.map((p) => p.nombre).join(' · ') : 'Sin programas asociados'}
                        </span>
                      </div>
                      <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--ga-soft)', whiteSpace: 'nowrap' }}>
                        {propios.length} programas · {activas} inscripciones activas
                      </span>
                      <span style={{ fontSize: 12, color: 'var(--ga-soft)', transform: abiertaEsta ? 'rotate(90deg)' : 'none' }}>›</span>
                    </div>

                    {abiertaEsta && (
                      <div style={{ background: 'var(--ga-row-alt)', borderTop: '1px solid var(--ga-line-soft)', padding: '16px 22px', display: 'flex', flexDirection: 'column', gap: 10 }}>
                        {propios.length === 0
                          ? <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>
                              Todavía no hay programas de esta institución. Se crean desde Programas, eligiéndola en el alta.
                            </span>
                          : propios.map((p) => (
                              <FilaPrograma key={p.id} programa={p} activas={activasDe(p.id)} onClick={() => onNavigate({ name: 'programa', id: p.id })} />
                            ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>}
      </Card>

      {nueva && <NuevaInstitucionModal onClose={() => setNueva(false)} />}
    </div>
  );
}

/** Solo se monta con la fila abierta: los hooks de módulos y grupos corren on demand. */
function FilaPrograma({ programa, activas, onClick }: {
  programa: ProgramaResponse; activas: number; onClick: () => void;
}) {
  const modulos = useModulos(programa.id);
  const grupos = useGrupos(programa.id);

  return (
    <div
      onClick={onClick}
      onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--ga-primary-100)')}
      onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--ga-line)')}
      style={{
        display: 'grid', gridTemplateColumns: 'minmax(0,1.5fr) repeat(3,minmax(0,1fr))', gap: 14,
        alignItems: 'center', background: 'var(--ga-surface)', border: '1px solid var(--ga-line)',
        borderRadius: 'var(--ga-radius-md)', padding: '13px 16px', cursor: 'pointer',
      }}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 5, minWidth: 0 }}>
        <span style={{ fontSize: 14.5, fontWeight: 500 }}>{programa.nombre}</span>
        <MonoChip>{programa.estrategiaEvaluacion}</MonoChip>
      </div>
      <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>
        {modulos.isPending ? '…' : (modulos.data ?? []).length + ' módulos'}
      </span>
      <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>
        {grupos.isPending
          ? '…'
          : (grupos.data ?? []).length
            ? (grupos.data ?? []).map((g) => g.dia).join(', ')
            : 'Sin grupos'}
      </span>
      <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 13, color: 'var(--ga-muted)' }}>{activas} activas</span>
    </div>
  );
}

/** InstitucionController solo expone list/get/create: no hay edición ni baja. */
function NuevaInstitucionModal({ onClose }: { onClose: () => void }) {
  const crear = useCrearInstitucion();
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [nombre, setNombre] = React.useState('');

  return (
    <Modal
      title="Nueva institución"
      onClose={onClose}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button
          disabled={crear.isPending}
          onClick={() => crear.mutate({ nombre }, {
            onSuccess: () => { push({ tone: 'ok', title: 'Institución creada', text: '201 · Ya se puede elegir en el alta de programas.' }); onClose(); },
            onError: (e) => handle(e),
          })}
        >Crear institución</Button>
      </>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <span style={{ fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
          Solo tiene nombre. Se asocia a los programas desde el alta de cada programa.
        </span>
        <Field label="Nombre" required error={fieldErrors.nombre}>
          <Input value={nombre} onChange={(e) => setNombre(e.target.value)} invalid={!!fieldErrors.nombre} placeholder="CENMA Ramallo" />
        </Field>
      </div>
    </Modal>
  );
}
