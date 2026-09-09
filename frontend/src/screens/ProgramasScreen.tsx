import React from 'react';
import { useProgramas, useInscripciones, useInstituciones, useCrearPrograma } from '../api/queries';
import { Button, Card, DataTable, EmptyState, Field, Input, Modal, MonoChip, RamaTag, Select, SkeletonList } from '../components/primitives';
import { ErrorScreen, useApiErrorHandler } from '../components/ErrorSurface';
import { ScreenHeader, type Route } from '../shell/AppShell';
import { useToasts } from '../components/Toasts';
import type { CategoriaPrograma, EstrategiaEvaluacion } from '../api/types';

/**
 * Índice de programas. El programa es el nodo que define la estrategia de
 * evaluación, y de ahí cuelgan módulos, planes y grupos: por eso esos tres
 * NO están en el sidebar, son pestañas del detalle.
 */
export function ProgramasScreen({ onNavigate }: { onNavigate: (r: Route) => void }) {
  const { data, isPending, error, refetch } = useProgramas();
  const inscripciones = useInscripciones();
  const [filtro, setFiltro] = React.useState<'TODOS' | CategoriaPrograma>('TODOS');
  const [nuevo, setNuevo] = React.useState(false);

  if (isPending) return <Card title="Programas"><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={refetch} entidad="listado" />;

  const rows = (data ?? []).filter((p) => filtro === 'TODOS' || p.categoria === filtro);
  const conteo = (programaId: number) => (inscripciones.data ?? []).filter((i) => i.programaId === programaId).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
      <ScreenHeader
        title="Programas"
        subtitle="Módulos, planes y grupos viven adentro de cada programa, no en el sidebar."
        actions={<>
          <Segmented
            value={filtro}
            onChange={setFiltro}
            options={[
              { value: 'TODOS' as const, label: 'Todos' },
              { value: 'ESCOLAR' as const, label: 'Escolar' },
              { value: 'PARTICULAR' as const, label: 'Particular' },
            ]}
          />
          <Button onClick={() => setNuevo(true)}>Nuevo programa</Button>
        </>}
      />

      <Card>
        <DataTable
          rows={rows}
          onRowClick={(p) => onNavigate({ name: 'programa', id: p.id })}
          empty={filtro !== 'TODOS'
            ? <EmptyState title="Ningún programa en esta rama" text="Cambiá el filtro o creá uno nuevo." />
            : <EmptyState
                title="Todavía no hay programas"
                text="El programa define la estrategia de evaluación: es lo primero que hay que crear, antes de inscribir."
                action={<Button onClick={() => setNuevo(true)}>Nuevo programa</Button>} />}
          columns={[
            { key: 'nombre', header: 'Programa', render: (p) => <strong style={{ fontWeight: 500 }}>{p.nombre}</strong> },
            { key: 'cat', header: 'Categoría', render: (p) => <RamaTag categoria={p.categoria} /> },
            { key: 'estrategia', header: 'Estrategia', render: (p) => <MonoChip>{p.estrategiaEvaluacion}</MonoChip> },
            { key: 'inst', header: 'Institución', render: (p) => p.institucionNombre ?? '—' },
            { key: 'ins', header: 'Inscripciones', mono: true, render: (p) => String(conteo(p.id)) },
          ]}
        />
      </Card>

      {nuevo && <NuevoProgramaModal onClose={() => setNuevo(false)} />}
    </div>
  );
}

export function Segmented<V extends string>({ value, options, onChange }: {
  value: V; options: { value: V; label: string }[]; onChange: (v: V) => void;
}) {
  return (
    <div style={{ display: 'flex', gap: 3, background: 'var(--ga-line-soft)', borderRadius: 9, padding: 3 }}>
      {options.map((o) => {
        const on = o.value === value;
        return (
          <button
            key={o.value}
            onClick={() => onChange(o.value)}
            style={{
              border: 'none', cursor: 'pointer', fontFamily: 'inherit', fontSize: 13.5,
              padding: '6px 13px', borderRadius: 7,
              background: on ? 'var(--ga-surface)' : 'transparent',
              color: on ? 'var(--ga-ink)' : 'var(--ga-muted)',
              fontWeight: on ? 600 : 500,
              boxShadow: on ? 'var(--ga-shadow-sm)' : 'none',
            }}
          >{o.label}</button>
        );
      })}
    </div>
  );
}

/**
 * La estrategia es el campo con consecuencias: decide si el programa tiene planes,
 * qué instancias se pueden cargar y cómo se cierra el módulo. Por eso cada opción
 * se explica en el select y no solo se nombra.
 */
const ESTRATEGIAS: { value: EstrategiaEvaluacion; label: string; cat: CategoriaPrograma }[] = [
  { value: 'CENMA_SEDE', label: 'CENMA Sede — TP integrador habilita la evaluación final', cat: 'ESCOLAR' },
  { value: 'CENMA_BASE', label: 'CENMA Base — 3 notas que promedian + integrador', cat: 'ESCOLAR' },
  { value: 'SEGUIMIENTO_LIBRE', label: 'Seguimiento libre — sin notas, solo observaciones', cat: 'PARTICULAR' },
];

function NuevoProgramaModal({ onClose }: { onClose: () => void }) {
  const crear = useCrearPrograma();
  const instituciones = useInstituciones();
  const { push } = useToasts();
  const { handle, fieldErrors } = useApiErrorHandler();
  const [categoria, setCategoria] = React.useState<CategoriaPrograma>('ESCOLAR');
  const [nombre, setNombre] = React.useState('');
  const [estrategia, setEstrategia] = React.useState<EstrategiaEvaluacion>('CENMA_SEDE');
  const [institucionId, setInstitucionId] = React.useState('');

  const opciones = ESTRATEGIAS.filter((e) => e.cat === categoria);

  const cambiarCategoria = (c: CategoriaPrograma) => {
    setCategoria(c);
    setEstrategia(c === 'ESCOLAR' ? 'CENMA_SEDE' : 'SEGUIMIENTO_LIBRE');
  };

  const submit = () => crear.mutate(
    { nombre, categoria, estrategiaEvaluacion: estrategia, institucionId: institucionId ? Number(institucionId) : null },
    {
      onSuccess: () => { push({ tone: 'ok', title: 'Programa creado', text: '201 · Ya se le pueden cargar módulos y grupos.' }); onClose(); },
      onError: (e) => handle(e),
    });

  return (
    <Modal
      title="Nuevo programa"
      onClose={onClose}
      width={540}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button onClick={submit} disabled={crear.isPending}>Crear programa</Button>
      </>}
    >
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div style={{ gridColumn: 'span 2', fontSize: 13, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
          La estrategia de evaluación no se puede deducir del nombre: es lo que define toda la
          UI académica de las inscripciones de este programa.
        </div>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Nombre" required error={fieldErrors.nombre}>
            <Input value={nombre} onChange={(e) => setNombre(e.target.value)} invalid={!!fieldErrors.nombre} placeholder="CENMA Sede — Ciclo 2026" />
          </Field>
        </div>
        <Field label="Categoría" required>
          <Select value={categoria} onChange={(e) => cambiarCategoria(e.target.value as CategoriaPrograma)}>
            <option value="ESCOLAR">Escolar</option>
            <option value="PARTICULAR">Particular</option>
          </Select>
        </Field>
        <Field label="Institución" hint="Opcional en programas particulares.">
          <Select value={institucionId} onChange={(e) => setInstitucionId(e.target.value)}>
            <option value="">Sin institución</option>
            {(instituciones.data ?? []).map((i) => <option key={i.id} value={i.id}>{i.nombre}</option>)}
          </Select>
        </Field>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Estrategia de evaluación" required error={fieldErrors.estrategiaEvaluacion}>
            <Select value={estrategia} onChange={(e) => setEstrategia(e.target.value as EstrategiaEvaluacion)}>
              {opciones.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </Select>
          </Field>
        </div>
      </div>
    </Modal>
  );
}
