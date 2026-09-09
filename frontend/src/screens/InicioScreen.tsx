import React from 'react';
import { useInscripciones, useContratos, useSeguimientosDe } from '../api/queries';
import { Card, EmptyState, SkeletonList } from '../components/primitives';
import { ErrorScreen } from '../components/ErrorSurface';
import { ScreenHeader, type Route } from '../shell/AppShell';
import type { InscripcionResponse } from '../api/types';

/**
 * Tablero de entrada. Todo se calcula en cliente sobre DOS requests
 * (GET /api/inscripciones + GET /api/contratos): no hay endpoint de métricas
 * y no se hace fan-out por inscripción.
 *
 * "Requiere atención" anticipa los rechazos de la API: cada fila es una request
 * que hoy fallaría (409 por pool agotado, alta de contrato faltante).
 */
export function InicioScreen({ onNavigate }: { onNavigate: (r: Route) => void }) {
  const ins = useInscripciones();
  const con = useContratos();

  if (ins.isPending || con.isPending) return <Card><SkeletonList rows={4} /></Card>;
  if (ins.error) return <ErrorScreen error={ins.error} onRetry={ins.refetch} entidad="listado" />;
  if (con.error) return <ErrorScreen error={con.error} onRetry={con.refetch} entidad="listado" />;

  const inscripciones = ins.data ?? [];
  const contratos = con.data ?? [];

  const activas = inscripciones.filter((i) => i.estado === 'ACTIVA');
  const pausadas = inscripciones.filter((i) => i.estado === 'PAUSADA');
  const escolares = activas.filter((i) => i.categoria === 'ESCOLAR').length;
  const particulares = activas.length - escolares;

  const vigentes = contratos.filter((c) => c.estado === 'ACTIVO');
  const deEmpresa = vigentes.filter((c) => c.empresaId != null).length;

  // Solo PAQUETE tiene tope real en el backend (ContratoService.consumirClase):
  // con POR_CLASE / MENSUAL el total es referencial y no entra en el KPI de pool.
  const paquetes = vigentes.filter((c) => c.tipoFacturacion === 'PAQUETE');
  const consumidas = paquetes.reduce((a, c) => a + c.clasesConsumidas, 0);
  const contratadas = paquetes.reduce((a, c) => a + c.clasesContratadas, 0);

  const agotados = paquetes.filter((c) => c.clasesConsumidas >= c.clasesContratadas);
  const sinContrato = activas.filter((i) => i.categoria === 'PARTICULAR' && i.contratoId === null);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <ScreenHeader
        title="Inicio"
        subtitle="Estado de las dos ramas y lo que la API va a rechazar si no se resuelve."
      />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(210px,1fr))', gap: 16 }}>
        <Kpi label="Inscripciones activas" value={activas.length}
             detail={escolares + ' escolares · ' + particulares + ' particulares'}
             onClick={() => onNavigate({ name: 'inscripciones', rama: 'ESCOLAR' })} />
        <Kpi label="Pausadas" value={pausadas.length}
             detail={pausadas.length === 0 ? 'Ninguna en pausa' : 'Se reanudan desde el detalle'}
             onClick={() => onNavigate({ name: 'inscripciones', rama: 'PARTICULAR' })} />
        <Kpi label="Contratos activos" value={vigentes.length}
             detail={deEmpresa + ' de empresa · ' + (vigentes.length - deEmpresa) + ' individuales'}
             onClick={() => onNavigate({ name: 'contratos' })} />
        <Kpi label="Clases del pool" value={consumidas + ' / ' + contratadas}
             detail={Math.max(0, contratadas - consumidas) + ' disponibles en total'}
             onClick={() => onNavigate({ name: 'contratos' })} />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(330px,1fr))', gap: 18, alignItems: 'start' }}>
        <Card title="Requiere atención" meta={(agotados.length + sinContrato.length) + ' pendientes'}>
          {agotados.length + sinContrato.length === 0
            ? <EmptyState
                title="Nada pendiente"
                text="Todos los contratos tienen cupo y todas las inscripciones particulares están cubiertas." />
            : <div style={{ display: 'flex', flexDirection: 'column' }}>
                {agotados.map((c) => (
                  <Aviso
                    key={'c' + c.id}
                    tone="danger"
                    title={'Pool agotado — ' + (c.empresaNombre ?? 'Contrato individual #' + c.id)}
                    text={'Van ' + c.clasesConsumidas + ' de ' + c.clasesContratadas + ' clases. Consumir otra devuelve 409: hay que ampliar el cupo.'}
                    onClick={() => onNavigate({ name: 'contratos', contratoId: c.id })}
                  />
                ))}
                {sinContrato.map((i) => (
                  <Aviso
                    key={'i' + i.id}
                    tone="warn"
                    title={i.personaNombre + ' — inscripción particular sin contrato'}
                    text="No hay pool asociado: no se pueden consumir clases hasta crear el contrato."
                    onClick={() => onNavigate({ name: 'inscripcion', id: i.id })}
                  />
                ))}
              </div>}
          {/* Honestidad de datos: el 422 al finalizar depende de las instancias de cada
              inscripción y detectarlo acá sería una request por inscripción escolar. */}
          <div style={{ padding: '12px 22px', borderTop: '1px solid var(--ga-line-soft)', fontSize: 12.5, lineHeight: 1.5, color: 'var(--ga-soft)' }}>
            Las escolares con módulos sin aprobar (422 al finalizar) se avisan en el detalle:
            detectarlas acá pediría las instancias de cada inscripción.
          </div>
        </Card>

        <Observaciones activas={activas} onNavigate={onNavigate} />
      </div>
    </div>
  );
}

function Kpi({ label, value, detail, onClick }: {
  label: string; value: React.ReactNode; detail: string; onClick?: () => void;
}) {
  return (
    <div
      onClick={onClick}
      onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--ga-primary-100)')}
      onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--ga-line)')}
      style={{
        background: 'var(--ga-surface)', border: '1px solid var(--ga-line)',
        borderRadius: 'var(--ga-radius-lg)', padding: '18px 20px',
        display: 'flex', flexDirection: 'column', gap: 7,
        cursor: onClick ? 'pointer' : 'default', minWidth: 0,
      }}
    >
      <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 10.5, letterSpacing: '.12em', textTransform: 'uppercase', color: 'var(--ga-soft)' }}>{label}</span>
      <span style={{ fontSize: 34, fontWeight: 600, letterSpacing: '-.03em', fontVariantNumeric: 'tabular-nums', lineHeight: 1.05 }}>{value}</span>
      <span style={{ fontSize: 13, color: 'var(--ga-muted)' }}>{detail}</span>
    </div>
  );
}

function Aviso({ tone, title, text, onClick }: {
  tone: 'danger' | 'warn'; title: string; text: string; onClick?: () => void;
}) {
  const danger = tone === 'danger';
  return (
    <div
      onClick={onClick}
      onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--ga-row-alt)')}
      onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
      style={{
        display: 'flex', gap: 11, alignItems: 'flex-start', padding: '14px 22px',
        borderBottom: '1px solid var(--ga-line-soft)', cursor: onClick ? 'pointer' : 'default',
      }}
    >
      <span style={{
        width: 20, height: 20, borderRadius: 999, flex: 'none', marginTop: 1,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 12, fontWeight: 700,
        background: danger ? 'var(--ga-danger-bg)' : 'var(--ga-warn-bg)',
        color: danger ? 'var(--ga-danger-fg)' : 'var(--ga-warn-fg)',
      }}>!</span>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 0 }}>
        <span style={{ fontSize: 14, fontWeight: 500 }}>{title}</span>
        <span style={{ fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>{text}</span>
      </div>
    </div>
  );
}

/**
 * Últimas observaciones. Ver useSeguimientosDe: sin listado global de seguimientos,
 * se consultan solo las 6 inscripciones activas más recientes.
 */
function Observaciones({ activas, onNavigate }: {
  activas: InscripcionResponse[]; onNavigate: (r: Route) => void;
}) {
  const recientes = [...activas]
    .sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio))
    .slice(0, 6);
  const queries = useSeguimientosDe(recientes.map((i) => i.id));

  const filas = queries
    .flatMap((res, idx) => (res.data ?? []).map((s) => ({ ...s, inscripcion: recientes[idx] })))
    .sort((a, b) => b.fecha.localeCompare(a.fecha))
    .slice(0, 4);

  return (
    <Card title="Últimas observaciones" meta="Rama particular">
      {queries.some((r) => r.isPending)
        ? <SkeletonList rows={4} />
        : filas.length === 0
          ? <EmptyState
              title="Sin observaciones todavía"
              text="El seguimiento se carga desde el detalle de cada inscripción particular." />
          : <div style={{ display: 'flex', flexDirection: 'column' }}>
              {filas.map((s) => (
                <div
                  key={s.id}
                  onClick={() => onNavigate({ name: 'inscripcion', id: s.inscripcionId })}
                  onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--ga-row-alt)')}
                  onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                  style={{
                    display: 'flex', flexDirection: 'column', gap: 5, padding: '14px 22px',
                    borderBottom: '1px solid var(--ga-line-soft)', cursor: 'pointer',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
                    <span style={{ fontSize: 14, fontWeight: 500 }}>{s.inscripcion.personaNombre}</span>
                    <span style={{ marginLeft: 'auto', fontFamily: 'var(--ga-font-mono)', fontSize: 12.5, color: 'var(--ga-soft)' }}>{s.fecha}</span>
                  </div>
                  <span style={{ fontSize: 13.5, color: 'var(--ga-muted)', lineHeight: 1.5 }}>{s.observacion}</span>
                </div>
              ))}
            </div>}
    </Card>
  );
}
