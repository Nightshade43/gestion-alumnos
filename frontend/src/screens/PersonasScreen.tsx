import React from 'react';
import { usePersonas, useCrearPersona, useTodasLasInscripciones } from '../api/queries';
import { Button, Card, DataTable, EmptyState, Field, Input, Modal, SkeletonList } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler } from '../components/ErrorSurface';
import { ScreenHeader, type Route } from '../shell/AppShell';
import { useToasts } from '../components/Toasts';

/**
 * Pantalla de referencia: muestra cómo se combinan tokens + primitivas +
 * TanStack Query + manejo de error para el resto de las pantallas.
 * Las demás están especificadas en SCREENS.md y en los mockups HTML.
 */
export function PersonasScreen({ onNavigate }: { onNavigate: (r: Route) => void }) {
  const { data, isPending, error, refetch } = usePersonas();
  const todas = useTodasLasInscripciones();
  const [query, setQuery] = React.useState('');
  const [nueva, setNueva] = React.useState(false);

  if (isPending) return <Card title="Personas"><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={refetch} entidad="listado" />;

  const q = query.toLowerCase();
  const rows = (data ?? []).filter((p) =>
    !q || p.nombre.toLowerCase().includes(q) || (p.documento ?? '').includes(q));

  const resumen = (personaId: number) => {
    const mias = todas.data.filter((i) => i.personaId === personaId);
    const esc = mias.filter((i) => i.programaNombre.includes('CENMA')).length; // ver nota en SCREENS.md
    const par = mias.length - esc;
    const partes = [];
    if (esc) partes.push(`${esc} escolar${esc > 1 ? 'es' : ''}`);
    if (par) partes.push(`${par} particular${par > 1 ? 'es' : ''}`);
    return partes.join(' · ') || 'Sin inscripciones';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <ScreenHeader
        title="Personas"
        subtitle="Transversal a las dos ramas — una persona puede tener inscripciones escolares y particulares."
        actions={<>
          <Input placeholder="Buscar por nombre o documento…" value={query}
                 onChange={(e) => setQuery(e.target.value)} style={{ width: 250 }} />
          <Button onClick={() => setNueva(true)}>Nueva persona</Button>
        </>}
      />

      <Card>
        <DataTable
          rows={rows}
          onRowClick={(p) => onNavigate({ name: 'persona', id: p.id })}
          empty={<EmptyState
            title="Todavía no hay personas cargadas"
            text="Creá la primera y después vinculala a un programa escolar o particular desde su ficha."
            action={<Button onClick={() => setNueva(true)}>Nueva persona</Button>} />}
          columns={[
            { key: 'nombre', header: 'Nombre', render: (p) => <strong style={{ fontWeight: 500 }}>{p.nombre}</strong> },
            { key: 'doc', header: 'Documento', mono: true, render: (p) => p.documento ?? '—' },
            { key: 'tel', header: 'Teléfono', mono: true, render: (p) => p.telefono ?? '—' },
            { key: 'ins', header: 'Inscripciones', render: (p) => resumen(p.id) },
          ]}
        />
      </Card>

      {nueva && <NuevaPersonaModal onClose={() => setNueva(false)} />}
    </div>
  );
}

function NuevaPersonaModal({ onClose }: { onClose: () => void }) {
  const crear = useCrearPersona();
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [form, setForm] = React.useState({ nombre: '', documento: '', telefono: '', email: '' });
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });

  const submit = () => crear.mutate(
    { nombre: form.nombre, documento: form.documento || null, telefono: form.telefono || null, email: form.email || null },
    {
      onSuccess: () => { push({ tone: 'ok', title: 'Persona creada', text: '201 · Ya está disponible para inscribir.' }); onClose(); },
      onError: (e) => handle(e),
    });

  return (
    <Modal
      title="Nueva persona"
      onClose={onClose}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button onClick={submit} disabled={crear.isPending}>Crear persona</Button>
      </>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Nombre" required error={fieldErrors.nombre}>
            <Input value={form.nombre} onChange={set('nombre')} invalid={!!fieldErrors.nombre} placeholder="Nombre y apellido" />
          </Field>
        </div>
        <Field label="Documento"><Input mono value={form.documento} onChange={set('documento')} placeholder="00.000.000" /></Field>
        <Field label="Teléfono"><Input mono value={form.telefono} onChange={set('telefono')} placeholder="351 000 0000" /></Field>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Email" hint="Opcional. Si viene, la API valida el formato (400 inline)." error={fieldErrors.email}>
            <Input value={form.email} onChange={set('email')} invalid={!!fieldErrors.email} placeholder="nombre@correo.com" />
          </Field>
        </div>
      </div>
    </Modal>
  );
}
