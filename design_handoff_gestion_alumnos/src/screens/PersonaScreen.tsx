import React from 'react';
import { usePersona, useInscripciones, useActualizarPersona } from '../api/queries';
import { Button, Card, EmptyState, Field, Input, Modal, RamaTag, SkeletonList, StatusBadge } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler } from '../components/ErrorSurface';
import { NuevaInscripcionModal } from './InscripcionesScreen';
import { useToasts } from '../components/Toasts';
import type { Route } from '../shell/AppShell';
import type { InscripcionResponse, PersonaResponse } from '../api/types';

/**
 * Ficha de persona: la ÚNICA pantalla donde conviven las dos ramas, porque la
 * persona es transversal (PersonaController no conoce categorías).
 * Datos: GET /api/personas/{id} + GET /api/inscripciones?personaId={id}.
 */
export function PersonaScreen({ id, onNavigate }: { id: number; onNavigate: (r: Route) => void }) {
  const { data: persona, isPending, error, refetch } = usePersona(id);
  const inscripciones = useInscripciones({ personaId: id });
  const [editar, setEditar] = React.useState(false);
  const [nueva, setNueva] = React.useState(false);

  if (isPending) return <Card><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={() => onNavigate({ name: 'personas' })} entidad="persona" />;
  if (!persona) return null;

  const iniciales = persona.nombre.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  const contacto = [persona.documento, persona.telefono, persona.email].filter(Boolean);
  const filas = [...(inscripciones.data ?? [])].sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio));

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <span
        onClick={() => onNavigate({ name: 'personas' })}
        style={{ fontSize: 13, color: 'var(--ga-muted)', cursor: 'pointer', width: 'fit-content' }}
      >← Personas</span>

      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 16, flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 15, minWidth: 0 }}>
          <div style={{
            width: 54, height: 54, borderRadius: 16, flex: 'none',
            background: 'var(--ga-primary-100)', color: 'var(--ga-primary-700)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 19, fontWeight: 600, letterSpacing: '.02em',
          }}>{iniciales}</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, minWidth: 0 }}>
            <h1 style={{ margin: 0, fontSize: 30, fontWeight: 600, letterSpacing: '-.025em' }}>{persona.nombre}</h1>
            <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 13.5, color: 'var(--ga-muted)' }}>
              {contacto.length ? contacto.join('  ·  ') : 'Sin datos de contacto'}
            </span>
          </div>
        </div>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 10 }}>
          <Button variant="secondary" onClick={() => setEditar(true)}>Editar datos</Button>
          <Button onClick={() => setNueva(true)}>Nueva inscripción</Button>
        </div>
      </div>

      <Card title="Inscripciones" meta={filas.length ? filas.length + ' en total' : undefined}>
        {inscripciones.isPending
          ? <SkeletonList rows={3} />
          : inscripciones.error
            ? <ErrorScreen error={inscripciones.error} onRetry={inscripciones.refetch} entidad="listado" />
            : filas.length === 0
              ? <EmptyState
                  title="Todavía no tiene inscripciones"
                  text="Puede tener una escolar y varias particulares al mismo tiempo: son ramas independientes."
                  action={<Button onClick={() => setNueva(true)}>Nueva inscripción</Button>} />
              : <div style={{ display: 'flex', flexDirection: 'column' }}>
                  {filas.map((i) => <FilaInscripcion key={i.id} ins={i} onClick={() => onNavigate({ name: 'inscripcion', id: i.id })} />)}
                </div>}
      </Card>

      {editar && <EditarPersonaModal persona={persona} onClose={() => setEditar(false)} onSaved={refetch} />}
      {nueva && <NuevaInscripcionModal rama={null} personaFija={persona} onClose={() => setNueva(false)} />}
    </div>
  );
}

function FilaInscripcion({ ins, onClick }: { ins: InscripcionResponse; onClick: () => void }) {
  const esc = ins.categoria === 'ESCOLAR';
  const detalle = esc
    ? [ins.planCodigo ? 'Plan ' + ins.planCodigo : null, ins.grupoDia ? ins.grupoDia + ' ' + (ins.grupoHorario ?? '') : null]
        .filter(Boolean).join(' · ') || 'Sin plan ni grupo'
    : ins.contratoId ? 'Contrato #' + ins.contratoId : 'Sin contrato';

  return (
    <div
      onClick={onClick}
      onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--ga-row-alt)')}
      onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
      style={{
        display: 'grid', gridTemplateColumns: '14px minmax(0,1.6fr) minmax(0,1.1fr) 130px 110px',
        gap: 14, alignItems: 'center', padding: '14px 22px',
        borderBottom: '1px solid var(--ga-line-soft)', cursor: 'pointer',
      }}
    >
      <span style={{ width: 9, height: 9, borderRadius: 3, background: esc ? 'var(--ga-escolar)' : 'var(--ga-particular)' }} />
      <div style={{ display: 'flex', flexDirection: 'column', gap: 3, minWidth: 0 }}>
        <span style={{ fontSize: 14.5, fontWeight: 500 }}>{ins.programaNombre}</span>
        <RamaTag categoria={ins.categoria} dot={false} />
      </div>
      <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>{detalle}</span>
      <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 13, color: 'var(--ga-muted)' }}>{ins.fechaInicio}</span>
      <StatusBadge estado={ins.estado} />
    </div>
  );
}

/** PUT /api/personas/{id}: reemplaza el registro, no toca inscripciones ni seguimiento. */
function EditarPersonaModal({ persona, onClose, onSaved }: {
  persona: PersonaResponse; onClose: () => void; onSaved: () => void;
}) {
  const guardar = useActualizarPersona(persona.id);
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [form, setForm] = React.useState({
    nombre: persona.nombre,
    documento: persona.documento ?? '',
    telefono: persona.telefono ?? '',
    email: persona.email ?? '',
  });
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });

  const submit = () => guardar.mutate(
    {
      nombre: form.nombre,
      documento: form.documento || null,
      telefono: form.telefono || null,
      email: form.email || null,
    },
    {
      onSuccess: () => {
        push({ tone: 'ok', title: 'Datos actualizados', text: '200 · El nombre se propaga a las inscripciones.' });
        onSaved();
        onClose();
      },
      onError: (e) => handle(e),
    });

  return (
    <Modal
      title="Editar datos"
      onClose={onClose}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button onClick={submit} disabled={guardar.isPending}>Guardar cambios</Button>
      </>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div style={{ gridColumn: 'span 2', fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
          Reemplaza el registro completo de la persona. No toca sus inscripciones ni su historial.
        </div>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Nombre" required error={fieldErrors.nombre}>
            <Input value={form.nombre} onChange={set('nombre')} invalid={!!fieldErrors.nombre} />
          </Field>
        </div>
        <Field label="Documento"><Input mono value={form.documento} onChange={set('documento')} /></Field>
        <Field label="Teléfono"><Input mono value={form.telefono} onChange={set('telefono')} /></Field>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Email" hint="Opcional. Si viene, la API valida el formato (400 inline)." error={fieldErrors.email}>
            <Input value={form.email} onChange={set('email')} invalid={!!fieldErrors.email} />
          </Field>
        </div>
      </div>
    </Modal>
  );
}
