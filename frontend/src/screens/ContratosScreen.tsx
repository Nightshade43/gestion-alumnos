import React from 'react';
import {
  useContratos, useContrato, useInscripciones, useEmpresas,
  useCrearContratoIndividual, useCrearContratoEmpresa,
  useConsumirClase, useAmpliarCupo, useFinalizarContrato,
} from '../api/queries';
import { Button, Card, EmptyState, Field, Input, Modal, MonoChip, PoolBar, Select, SkeletonList, StatusBadge } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler } from '../components/ErrorSurface';
import { ScreenHeader, type Route } from '../shell/AppShell';
import { useToasts } from '../components/Toasts';
import type { ContratoResponse, InscripcionResponse, TipoFacturacion } from '../api/types';

/**
 * Contratos. El contrato es el pool de clases de la rama particular: existe
 * individual (una inscripción) o de empresa (varias sobre el mismo total).
 *
 * Recordatorio del backend: solo PAQUETE topea el pool (ContratoService.consumirClase).
 * Con POR_CLASE / MENSUAL el total es referencial y consumir de más NO da 409.
 */
export function ContratosScreen({ abrirContratoId, onNavigate }: {
  abrirContratoId?: number; onNavigate: (r: Route) => void;
}) {
  const { data, isPending, error, refetch } = useContratos();
  const inscripciones = useInscripciones({ categoria: 'PARTICULAR' });
  const [alta, setAlta] = React.useState<'individual' | 'empresa' | null>(null);
  const [detalle, setDetalle] = React.useState<number | null>(abrirContratoId ?? null);

  if (isPending) return <Card title="Contratos"><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={refetch} entidad="listado" />;

  const contratos = data ?? [];
  const elegibles = (inscripciones.data ?? []).filter((i) => i.contratoId === null && i.estado !== 'CANCELADA');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <ScreenHeader
        title="Contratos"
        subtitle="El pool de clases de la rama particular. Consumir con el pool agotado devuelve 409."
        actions={<>
          <Button variant="secondary" onClick={() => setAlta('individual')}>Contrato individual</Button>
          <Button onClick={() => setAlta('empresa')}>Contrato de empresa</Button>
        </>}
      />

      {contratos.length === 0
        ? <Card><EmptyState
            title="Todavía no hay contratos"
            text="Una inscripción particular sin contrato no puede consumir clases: el contrato es lo que define el pool."
            action={<Button onClick={() => setAlta('individual')}>Contrato individual</Button>} /></Card>
        : <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(330px,1fr))', gap: 18 }}>
            {contratos.map((c) => <TarjetaContrato key={c.id} contrato={c} onClick={() => setDetalle(c.id)} />)}
          </div>}

      {alta === 'individual' && <AltaIndividualModal elegibles={elegibles} onClose={() => setAlta(null)} />}
      {alta === 'empresa' && <AltaEmpresaModal elegibles={elegibles} onClose={() => setAlta(null)} />}
      {detalle != null && <ContratoDetalleModal id={detalle} onClose={() => setDetalle(null)} onNavigate={onNavigate} />}
    </div>
  );
}

function TarjetaContrato({ contrato: c, onClick }: { contrato: ContratoResponse; onClick: () => void }) {
  const empresa = c.empresaId != null;
  return (
    <div
      onClick={onClick}
      onMouseEnter={(e) => { e.currentTarget.style.borderColor = 'var(--ga-primary-100)'; e.currentTarget.style.boxShadow = 'var(--ga-shadow-md)'; }}
      onMouseLeave={(e) => { e.currentTarget.style.borderColor = 'var(--ga-line)'; e.currentTarget.style.boxShadow = 'none'; }}
      style={{
        background: 'var(--ga-surface)', border: '1px solid var(--ga-line)',
        borderRadius: 'var(--ga-radius-lg)', padding: '18px 20px',
        display: 'flex', flexDirection: 'column', gap: 14, cursor: 'pointer', minWidth: 0,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <span style={{ fontSize: 16, fontWeight: 600 }}>
          {empresa ? c.empresaNombre : 'Contrato individual #' + c.id}
        </span>
        <MonoChip>{c.tipoFacturacion}</MonoChip>
        <span style={{ marginLeft: 'auto' }}><StatusBadge estado={c.estado} /></span>
      </div>
      <PoolBar
        consumidas={c.clasesConsumidas}
        contratadas={c.clasesContratadas}
        tipoFacturacion={c.tipoFacturacion}
        finalizado={c.estado === 'FINALIZADO'}
      />
      <span style={{ fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
        {c.inscripciones.length === 0
          ? 'Sin inscripciones cubiertas'
          : 'Cubre a ' + c.inscripciones.map((i) => i.personaNombre).join(', ')}
      </span>
    </div>
  );
}

/* ─────────────── Detalle ─────────────── */

function ContratoDetalleModal({ id, onClose, onNavigate }: {
  id: number; onClose: () => void; onNavigate: (r: Route) => void;
}) {
  const { data: c, isPending, error } = useContrato(id);
  // EmpleadoCubierto solo trae inscripcionId + personaNombre: el programa y el estado
  // se cruzan contra el listado global de inscripciones particulares.
  const inscripciones = useInscripciones({ categoria: 'PARTICULAR' });
  const consumir = useConsumirClase(id);
  const ampliar = useAmpliarCupo(id);
  const finalizar = useFinalizarContrato(id);
  const { push } = useToasts();
  const { handle } = useApiErrorHandler();
  const [confirmar, setConfirmar] = React.useState(false);

  if (isPending || error || !c) {
    return (
      <Modal title="Contrato" onClose={onClose} width={580}>
        {error ? <ErrorScreen error={error} entidad="contrato" /> : <SkeletonList rows={3} />}
      </Modal>
    );
  }

  const empresa = c.empresaId != null;
  const finalizado = c.estado === 'FINALIZADO';
  const agotado = c.tipoFacturacion === 'PAQUETE' && c.clasesConsumidas >= c.clasesContratadas;
  const detalleDe = (inscripcionId: number): InscripcionResponse | undefined =>
    (inscripciones.data ?? []).find((i) => i.id === inscripcionId);

  return (
    <Modal
      title={empresa ? (c.empresaNombre ?? 'Contrato de empresa') : 'Contrato individual'}
      kicker={'#' + c.id + ' · ' + c.estado}
      onClose={onClose}
      width={580}
      footer={<>
        <Button
          variant="secondary"
          disabled={finalizado || agotado || consumir.isPending}
          onClick={() => consumir.mutate(undefined, {
            onSuccess: (r) => push({ tone: 'ok', title: 'Clase consumida', text: r.clasesConsumidas + ' de ' + r.clasesContratadas + ' del pool.' }),
            onError: (e) => handle(e),
          })}
        >Consumir clase</Button>
        <Button
          variant="secondary"
          disabled={finalizado || ampliar.isPending}
          onClick={() => ampliar.mutate(4, {
            onSuccess: (r) => push({ tone: 'ok', title: 'Cupo ampliado', text: 'Nuevo total: ' + r.clasesContratadas + ' clases.' }),
            onError: (e) => handle(e),
          })}
        >Ampliar cupo +4</Button>
        <Button variant="danger" disabled={finalizado || finalizar.isPending} onClick={() => setConfirmar(true)}>Finalizar contrato</Button>
      </>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <MonoChip>{c.tipoFacturacion}</MonoChip>
          <span style={{ fontSize: 13.5, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
            {empresa
              ? 'Pool compartido: cualquier empleado cubierto consume del mismo total.'
              : 'Cubre una única inscripción particular.'}
          </span>
        </div>

        <div style={{ background: 'var(--ga-surface-2)', border: '1px solid var(--ga-line-soft)', borderRadius: 'var(--ga-radius-md)', padding: '14px 16px' }}>
          <PoolBar
            consumidas={c.clasesConsumidas}
            contratadas={c.clasesContratadas}
            tipoFacturacion={c.tipoFacturacion}
            finalizado={finalizado}
          />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 10.5, letterSpacing: '.12em', textTransform: 'uppercase', color: 'var(--ga-soft)' }}>
            {empresa ? 'Empleados cubiertos' : 'Inscripción cubierta'}
          </span>
          {c.inscripciones.length === 0
            ? <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>Ninguna inscripción asociada.</span>
            : c.inscripciones.map((e) => {
                const ins = detalleDe(e.inscripcionId);
                return (
                  <div
                    key={e.inscripcionId}
                    onClick={() => { onClose(); onNavigate({ name: 'inscripcion', id: e.inscripcionId }); }}
                    onMouseEnter={(ev) => (ev.currentTarget.style.background = 'var(--ga-row-alt)')}
                    onMouseLeave={(ev) => (ev.currentTarget.style.background = 'transparent')}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px',
                      border: '1px solid var(--ga-line-soft)', borderRadius: 'var(--ga-radius-sm)', cursor: 'pointer',
                    }}
                  >
                    <span style={{ width: 8, height: 8, borderRadius: 3, flex: 'none', background: 'var(--ga-particular)' }} />
                    <span style={{ fontSize: 14, fontWeight: 500 }}>{e.personaNombre}</span>
                    <span style={{ fontSize: 13, color: 'var(--ga-muted)', minWidth: 0 }}>{ins?.programaNombre ?? ''}</span>
                    {ins && <span style={{ marginLeft: 'auto' }}><StatusBadge estado={ins.estado} /></span>}
                  </div>
                );
              })}
        </div>
      </div>

      {confirmar && (
        <Modal
          kicker="Acción irreversible"
          title="Finalizar el contrato"
          onClose={() => setConfirmar(false)}
          width={470}
          footer={<>
            <Button variant="secondary" onClick={() => setConfirmar(false)}>Volver</Button>
            <Button
              variant="danger"
              disabled={finalizar.isPending}
              onClick={() => finalizar.mutate(undefined, {
                onSuccess: () => { push({ tone: 'ok', title: 'Contrato finalizado', text: 'Consumir clases devuelve 409 desde ahora.' }); setConfirmar(false); },
                onError: (e) => handle(e),
              })}
            >Finalizar contrato</Button>
          </>}
        >
          <p style={{ margin: 0, fontSize: 14.5, lineHeight: 1.55, color: 'var(--ga-muted)' }}>
            FINALIZADO es terminal: no se puede reactivar ni consumir más clases, y las
            inscripciones cubiertas quedan sin pool. Las clases ya consumidas se conservan.
          </p>
        </Modal>
      )}
    </Modal>
  );
}

/* ─────────────── Altas ─────────────── */

const FACTURACION: { value: TipoFacturacion; label: string }[] = [
  { value: 'PAQUETE', label: 'PAQUETE — pool cerrado, el backend topea el consumo' },
  { value: 'POR_CLASE', label: 'POR_CLASE — total referencial, sin tope' },
  { value: 'MENSUAL', label: 'MENSUAL — total referencial, sin tope' },
];

function AltaIndividualModal({ elegibles, onClose }: {
  elegibles: InscripcionResponse[]; onClose: () => void;
}) {
  const crear = useCrearContratoIndividual();
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [inscripcionId, setInscripcionId] = React.useState('');
  const [tipoFacturacion, setTipo] = React.useState<TipoFacturacion>('PAQUETE');
  const [clases, setClases] = React.useState('8');
  const paquete = tipoFacturacion === 'PAQUETE';

  return (
    <Modal
      title="Contrato individual"
      onClose={onClose}
      width={560}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button
          disabled={crear.isPending}
          onClick={() => crear.mutate(
            {
              inscripcionId: Number(inscripcionId),
              tipoFacturacion,
              clasesContratadas: clases ? Number(clases) : null,
            },
            {
              onSuccess: () => { push({ tone: 'ok', title: 'Contrato creado', text: '201 · La inscripción ya puede consumir clases.' }); onClose(); },
              onError: (e) => handle(e),
            })}
        >Crear contrato</Button>
      </>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <span style={{ fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
          Solo aparecen las inscripciones particulares sin contrato: una inscripción ya
          cubierta no puede sumarse a otro pool.
        </span>

        {elegibles.length === 0
          ? <EmptyState
              title="No hay inscripciones sin contrato"
              text="Creá primero una inscripción particular, o sumá la persona al pool de una empresa." />
          : <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 210, overflowY: 'auto' }}>
              {elegibles.map((i) => {
                const on = String(i.id) === inscripcionId;
                return (
                  <label
                    key={i.id}
                    style={{
                      display: 'flex', alignItems: 'center', gap: 11, padding: '11px 13px', cursor: 'pointer',
                      border: '1px solid ' + (on ? 'var(--ga-primary-600)' : 'var(--ga-line)'),
                      background: on ? 'var(--ga-primary-50)' : 'var(--ga-surface)',
                      borderRadius: 'var(--ga-radius-sm)',
                    }}
                  >
                    <input type="radio" name="inscripcion" checked={on} onChange={() => setInscripcionId(String(i.id))} />
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0 }}>
                      <span style={{ fontSize: 14, fontWeight: 500 }}>{i.personaNombre}</span>
                      <span style={{ fontSize: 12.5, color: 'var(--ga-muted)' }}>{i.programaNombre}</span>
                    </div>
                    <span style={{ marginLeft: 'auto', fontFamily: 'var(--ga-font-mono)', fontSize: 12.5, color: 'var(--ga-soft)' }}>{i.fechaInicio}</span>
                  </label>
                );
              })}
            </div>}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
          <div style={{ gridColumn: 'span 2' }}>
            <Field label="Facturación" required error={fieldErrors.tipoFacturacion}>
              <Select value={tipoFacturacion} onChange={(e) => setTipo(e.target.value as TipoFacturacion)}>
                {FACTURACION.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
              </Select>
            </Field>
          </div>
          <Field
            label="Clases del pool"
            required={paquete}
            hint={paquete
              ? 'El consumo se topea acá: la clase 9 devuelve 409.'
              : 'Opcional y referencial: el backend no topea este tipo.'}
            error={fieldErrors.clasesContratadas}
          >
            <Input mono type="number" min={1} value={clases} onChange={(e) => setClases(e.target.value)} invalid={!!fieldErrors.clasesContratadas} />
          </Field>
        </div>
      </div>
    </Modal>
  );
}

function AltaEmpresaModal({ elegibles, onClose }: {
  elegibles: InscripcionResponse[]; onClose: () => void;
}) {
  const empresas = useEmpresas();
  const crear = useCrearContratoEmpresa();
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [empresaId, setEmpresaId] = React.useState('');
  const [tipoFacturacion, setTipo] = React.useState<TipoFacturacion>('PAQUETE');
  const [clases, setClases] = React.useState('24');
  const [elegidas, setElegidas] = React.useState<number[]>([]);

  const toggle = (id: number) => setElegidas((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);

  return (
    <Modal
      title="Contrato de empresa"
      onClose={onClose}
      width={600}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button
          disabled={crear.isPending}
          onClick={() => crear.mutate(
            {
              empresaId: Number(empresaId),
              tipoFacturacion,
              clasesContratadas: Number(clases),
              inscripcionIds: elegidas,
            },
            {
              onSuccess: () => { push({ tone: 'ok', title: 'Contrato de empresa creado', text: '201 · ' + elegidas.length + ' inscripciones sobre el mismo pool.' }); onClose(); },
              onError: (e) => handle(e),
            })}
        >Crear contrato</Button>
      </>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
          <div style={{ gridColumn: 'span 2' }}>
            <Field label="Empresa" required error={fieldErrors.empresaId}>
              <Select value={empresaId} onChange={(e) => setEmpresaId(e.target.value)}>
                <option value="">Elegí una empresa…</option>
                {(empresas.data ?? []).map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
              </Select>
            </Field>
          </div>
          <Field label="Facturación" required error={fieldErrors.tipoFacturacion}>
            <Select value={tipoFacturacion} onChange={(e) => setTipo(e.target.value as TipoFacturacion)}>
              {FACTURACION.map((f) => <option key={f.value} value={f.value}>{f.value}</option>)}
            </Select>
          </Field>
          <Field label="Clases del pool" required hint="Total compartido entre todas las inscripciones elegidas." error={fieldErrors.clasesContratadas}>
            <Input mono type="number" min={1} value={clases} onChange={(e) => setClases(e.target.value)} invalid={!!fieldErrors.clasesContratadas} />
          </Field>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--ga-ink-soft)' }}>Inscripciones cubiertas *</span>
          {fieldErrors.inscripcionIds && <span style={{ fontSize: 12, color: 'var(--ga-danger-fg)' }}>{fieldErrors.inscripcionIds}</span>}
          {elegibles.length === 0
            ? <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>
                No hay inscripciones particulares sin contrato. Una inscripción ya cubierta no puede sumarse a otro pool.
              </span>
            : <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 200, overflowY: 'auto' }}>
                {elegibles.map((i) => {
                  const on = elegidas.includes(i.id);
                  return (
                    <label
                      key={i.id}
                      style={{
                        display: 'flex', alignItems: 'center', gap: 11, padding: '11px 13px', cursor: 'pointer',
                        border: '1px solid ' + (on ? 'var(--ga-primary-600)' : 'var(--ga-line)'),
                        background: on ? 'var(--ga-primary-50)' : 'var(--ga-surface)',
                        borderRadius: 'var(--ga-radius-sm)',
                      }}
                    >
                      <input type="checkbox" checked={on} onChange={() => toggle(i.id)} />
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 2, minWidth: 0 }}>
                        <span style={{ fontSize: 14, fontWeight: 500 }}>{i.personaNombre}</span>
                        <span style={{ fontSize: 12.5, color: 'var(--ga-muted)' }}>{i.programaNombre}</span>
                      </div>
                      <span style={{ marginLeft: 'auto', fontFamily: 'var(--ga-font-mono)', fontSize: 12.5, color: 'var(--ga-soft)' }}>{i.fechaInicio}</span>
                    </label>
                  );
                })}
              </div>}
          <span style={{ fontSize: 12.5, color: 'var(--ga-soft)' }}>
            {elegidas.length} inscripciones sobre el mismo pool de {clases || '—'} clases.
            Sin selección la API responde 400 en inscripcionIds.
          </span>
        </div>
      </div>
    </Modal>
  );
}
