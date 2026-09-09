import React from 'react';
import { useEmpresas, useContratos, useInscripciones, useCrearEmpresa } from '../api/queries';
import { Button, Card, EmptyState, Field, Input, Modal, MonoChip, PoolBar, SkeletonList, StatusBadge } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler } from '../components/ErrorSurface';
import { ScreenHeader, type Route } from '../shell/AppShell';
import { useToasts } from '../components/Toasts';
import type { ContratoResponse } from '../api/types';

/**
 * Empresas. EmpresaResponse solo trae { id, nombre, contacto }: los contratos y
 * los empleados cubiertos salen de GET /api/contratos filtrando por empresaId en
 * cliente — el listado es chico y ya está cargado para Contratos e Inicio.
 *
 * Igual que Instituciones, es una lista desplegable: no hay pantalla de detalle.
 */
export function EmpresasScreen({ onNavigate }: { onNavigate: (r: Route) => void }) {
  const { data, isPending, error, refetch } = useEmpresas();
  const contratos = useContratos();
  const inscripciones = useInscripciones({ categoria: 'PARTICULAR' });
  const [abierta, setAbierta] = React.useState<number | null>(null);
  const [nueva, setNueva] = React.useState(false);

  if (isPending) return <Card title="Empresas"><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={refetch} entidad="listado" />;

  const empresas = data ?? [];
  const contratosDe = (empresaId: number) => (contratos.data ?? []).filter((c) => c.empresaId === empresaId);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <ScreenHeader
        title="Empresas"
        subtitle="Contratan pools de clases para sus empleados. Todos consumen del mismo total."
        actions={<Button onClick={() => setNueva(true)}>Nueva empresa</Button>}
      />

      <Card>
        {empresas.length === 0
          ? <EmptyState
              title="Todavía no hay empresas"
              text="La empresa es el paso previo al contrato de empresa: sin ella no se puede armar un pool compartido."
              action={<Button onClick={() => setNueva(true)}>Nueva empresa</Button>} />
          : <div style={{ display: 'flex', flexDirection: 'column' }}>
              {empresas.map((emp) => {
                const propios = contratosDe(emp.id);
                const activos = propios.filter((c) => c.estado === 'ACTIVO').length;
                const cubiertos = propios.reduce((a, c) => a + c.inscripciones.length, 0);
                const abiertaEsta = abierta === emp.id;
                return (
                  <div key={emp.id} style={{ borderBottom: '1px solid var(--ga-line-soft)' }}>
                    <div
                      onClick={() => setAbierta(abiertaEsta ? null : emp.id)}
                      style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '15px 22px', cursor: 'pointer' }}
                    >
                      <div style={{
                        width: 34, height: 34, borderRadius: 11, flex: 'none',
                        background: 'var(--ga-particular-bg)', color: 'var(--ga-particular-fg)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 13, fontWeight: 600,
                      }}>{emp.nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase()}</div>
                      <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 0 }}>
                        <span style={{ fontSize: 15, fontWeight: 500 }}>{emp.nombre}</span>
                        <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 12.5, color: 'var(--ga-muted)' }}>
                          {emp.contacto ?? 'Sin contacto'}
                        </span>
                      </div>
                      <span style={{ marginLeft: 'auto', fontSize: 13, color: 'var(--ga-soft)', whiteSpace: 'nowrap' }}>
                        {activos} contratos activos · {cubiertos} empleados cubiertos
                      </span>
                      <span style={{ fontSize: 12, color: 'var(--ga-soft)', transform: abiertaEsta ? 'rotate(90deg)' : 'none' }}>›</span>
                    </div>

                    {abiertaEsta && (
                      <div style={{ background: 'var(--ga-row-alt)', borderTop: '1px solid var(--ga-line-soft)', padding: '16px 22px', display: 'flex', flexDirection: 'column', gap: 12 }}>
                        {propios.length === 0
                          ? <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>
                              Sin contratos todavía. Se arman desde Contratos → "Contrato de empresa".
                            </span>
                          : propios.map((c) => (
                              <TarjetaContratoEmpresa
                                key={c.id}
                                contrato={c}
                                inscripciones={inscripciones.data ?? []}
                                onAbrirContrato={() => onNavigate({ name: 'contratos', contratoId: c.id })}
                                onAbrirInscripcion={(id) => onNavigate({ name: 'inscripcion', id })}
                              />
                            ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>}
      </Card>

      {nueva && <NuevaEmpresaModal onClose={() => setNueva(false)} />}
    </div>
  );
}

function TarjetaContratoEmpresa({ contrato: c, inscripciones, onAbrirContrato, onAbrirInscripcion }: {
  contrato: ContratoResponse;
  inscripciones: { id: number; programaNombre: string; estado: 'ACTIVA' | 'PAUSADA' | 'FINALIZADA' | 'CANCELADA' }[];
  onAbrirContrato: () => void;
  onAbrirInscripcion: (id: number) => void;
}) {
  return (
    <div style={{ background: 'var(--ga-surface)', border: '1px solid var(--ga-line)', borderRadius: 'var(--ga-radius-md)', overflow: 'hidden' }}>
      <div
        onClick={onAbrirContrato}
        style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', borderBottom: '1px solid var(--ga-line-soft)', cursor: 'pointer' }}
      >
        <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 13, color: 'var(--ga-muted)' }}>#{c.id}</span>
        <MonoChip>{c.tipoFacturacion}</MonoChip>
        <span style={{ marginLeft: 'auto' }}><StatusBadge estado={c.estado} /></span>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '220px minmax(0,1fr)', gap: 18, padding: '14px 16px', alignItems: 'start' }}>
        <PoolBar
          consumidas={c.clasesConsumidas}
          contratadas={c.clasesContratadas}
          tipoFacturacion={c.tipoFacturacion}
          finalizado={c.estado === 'FINALIZADO'}
        />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 7, minWidth: 0 }}>
          {c.inscripciones.length === 0
            ? <span style={{ fontSize: 13, color: 'var(--ga-muted)' }}>Sin empleados cubiertos.</span>
            : c.inscripciones.map((e) => {
                const ins = inscripciones.find((i) => i.id === e.inscripcionId);
                return (
                  <div
                    key={e.inscripcionId}
                    onClick={() => onAbrirInscripcion(e.inscripcionId)}
                    style={{ display: 'flex', alignItems: 'center', gap: 9, cursor: 'pointer', minWidth: 0 }}
                  >
                    <span style={{ width: 8, height: 8, borderRadius: 3, flex: 'none', background: 'var(--ga-particular)' }} />
                    <span style={{ fontSize: 13.5, fontWeight: 500 }}>{e.personaNombre}</span>
                    <span style={{ fontSize: 12.5, color: 'var(--ga-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{ins?.programaNombre ?? ''}</span>
                    {ins && <span style={{ marginLeft: 'auto' }}><StatusBadge estado={ins.estado} /></span>}
                  </div>
                );
              })}
        </div>
      </div>
    </div>
  );
}

/** EmpresaController solo expone list/get/create: no hay edición ni baja. */
function NuevaEmpresaModal({ onClose }: { onClose: () => void }) {
  const crear = useCrearEmpresa();
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [nombre, setNombre] = React.useState('');
  const [contacto, setContacto] = React.useState('');

  return (
    <Modal
      title="Nueva empresa"
      onClose={onClose}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button
          disabled={crear.isPending}
          onClick={() => crear.mutate({ nombre, contacto: contacto || null }, {
            onSuccess: () => { push({ tone: 'ok', title: 'Empresa creada', text: '201 · Ya se le puede armar un contrato de pool.' }); onClose(); },
            onError: (e) => handle(e),
          })}
        >Crear empresa</Button>
      </>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div style={{ gridColumn: 'span 2', fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
          La empresa no tiene inscripciones propias: cubre las de sus empleados a través de un contrato.
        </div>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Nombre" required error={fieldErrors.nombre}>
            <Input value={nombre} onChange={(e) => setNombre(e.target.value)} invalid={!!fieldErrors.nombre} placeholder="Alcor S.A." />
          </Field>
        </div>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Contacto" hint="Opcional. Teléfono o email de quien administra el contrato.">
            <Input mono value={contacto} onChange={(e) => setContacto(e.target.value)} placeholder="351 000 0000" />
          </Field>
        </div>
      </div>
    </Modal>
  );
}
