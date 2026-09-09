import React from 'react';
import {
  useInscripcion, useModulos, useInstancias, useSeguimientos, useContrato, usePrograma,
  usePausar, useReanudar, useFinalizar, useCancelar,
  useConsumirClase, useAmpliarCupo, useCrearSeguimiento, useCrearInstancia,
} from '../api/queries';
import {
  Button, Card, EmptyState, Field, Input, Modal, MonoChip, PoolBar, RamaTag, Select,
  SkeletonList, StatusBadge, thStyle, tdStyle,
} from '../components/primitives';
import { ErrorScreen, useApiErrorHandler, BusinessRuleModal } from '../components/ErrorSurface';
import { useToasts } from '../components/Toasts';
import type { Route } from '../shell/AppShell';
import type {
  EstrategiaEvaluacion, InstanciaEvaluativaResponse, ModuloResponse, TipoInstanciaEvaluativa,
} from '../api/types';

/**
 * Detalle de inscripción. El cuerpo cambia por rama:
 *   ESCOLAR    → evaluaciones agrupadas por módulo
 *   PARTICULAR → contrato (pool de clases) + seguimiento en texto libre
 * La máquina de estados de InscripcionService es la misma para las dos.
 *
 * Dentro de ESCOLAR hay dos reglas académicas distintas, no una con variantes:
 *   CENMA_SEDE → TP_INTEGRADOR (gate) habilita EVALUACION_FINAL, y la final ES
 *                la nota del módulo (no se promedia con el TP).
 *   CENMA_BASE → mínimo 3 NOTA por módulo (promedian) + INTEGRADOR
 *                interdisciplinario (cuentaParaPromedio = false). Promedio a 2
 *                decimales; un recuperatorio reemplaza a la original vía recuperaAId.
 */

const wrap: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 22 };

export function InscripcionScreen({ id, from, onNavigate }: {
  id: number; from?: Route; onNavigate: (r: Route) => void;
}) {
  const { data: ins, isPending, error, refetch } = useInscripcion(id);
  const { data: programa } = usePrograma(ins?.programaId);
  const estrategia = programa?.estrategiaEvaluacion;
  const sede = estrategia === 'CENMA_SEDE';
  const { handle, ruleError, clearRuleError } = useApiErrorHandler();
  const { push } = useToasts();

  const pausar = usePausar();
  const reanudar = useReanudar();
  const finalizar = useFinalizar();
  const cancelar = useCancelar();
  const [confirmCancelar, setConfirmCancelar] = React.useState(false);

  if (isPending) return <Card><SkeletonList /></Card>;
  if (error) return <ErrorScreen error={error} onRetry={refetch} entidad="inscripción" />;
  if (!ins) return null;

  const esc = ins.categoria === 'ESCOLAR';
  const activa = ins.estado === 'ACTIVA';
  const pausada = ins.estado === 'PAUSADA';
  const terminal = ins.estado === 'FINALIZADA' || ins.estado === 'CANCELADA';

  const volver = from?.name === 'persona'
    ? { label: `← Ficha de ${ins.personaNombre.split(' ')[0]}`, route: from }
    : { label: esc ? '← Inscripciones escolares' : '← Inscripciones particulares',
        route: { name: 'inscripciones', rama: ins.categoria } as Route };

  const correr = (m: { mutate: (id: number, o?: any) => void }, ok: string) =>
    m.mutate(id, {
      onSuccess: () => push({ tone: 'ok', title: ok, text: `${ins.personaNombre} — ${ins.programaNombre}` }),
      onError: (e: unknown) => handle(e),
    });

  return (
    <div style={wrap}>
      <span
        onClick={() => onNavigate(volver.route)}
        style={{ fontSize: 13, color: 'var(--ga-muted)', cursor: 'pointer', width: 'fit-content' }}
      >{volver.label}</span>

      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 16 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
            <h1 style={{ margin: 0, fontSize: 30, fontWeight: 600, letterSpacing: '-.025em' }}>{ins.personaNombre}</h1>
            <StatusBadge estado={ins.estado} />
          </div>
          <span style={{ fontSize: 15, color: 'var(--ga-muted)' }}>{ins.programaNombre}</span>
        </div>
        {/* Botones inválidos deshabilitados, no ocultos: la transición imposible se ve. */}
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 9 }}>
          <Button
            variant="secondary"
            disabled={terminal || pausar.isPending || reanudar.isPending}
            onClick={() => (pausada ? correr(reanudar, 'Inscripción reanudada') : correr(pausar, 'Inscripción pausada'))}
          >{pausada ? 'Reanudar' : 'Pausar'}</Button>
          <Button
            variant="secondary"
            disabled={!activa || finalizar.isPending}
            onClick={() => correr(finalizar, 'Inscripción finalizada')}
          >Finalizar</Button>
          <Button
            variant="danger"
            disabled={terminal || cancelar.isPending}
            onClick={() => setConfirmCancelar(true)}
          >Cancelar</Button>
        </div>
      </div>

      <Card>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5,1fr)', gap: 1, background: 'var(--ga-line-soft)' }}>
          <Meta label="Rama" value={<RamaTag categoria={ins.categoria} />} />
          {/* Base no tiene planes: en su lugar mostramos la estrategia, que es lo que rige. */}
          <Meta label={esc ? (sede ? 'Plan' : 'Estrategia') : 'Contrato'} value={esc
            ? (sede ? (ins.planCodigo ? `Plan ${ins.planCodigo}` : 'Sin plan') : (estrategia ?? '—'))
            : (ins.contratoId ? `#${ins.contratoId}` : 'Sin contrato')} />
          <Meta label={esc ? 'Grupo' : 'Inscripción'} value={esc
            ? (ins.grupoDia ? `${ins.grupoDia} ${ins.grupoHorario ?? ''}`.trim() : '—')
            : `#${ins.id}`} />
          <Meta label="Inicio" value={ins.fechaInicio} mono />
          <Meta label="Fin" value={ins.fechaFin ?? '—'} mono />
        </div>
      </Card>

      {esc
        ? <Evaluaciones inscripcionId={ins.id} programaId={ins.programaId} estrategia={estrategia} />
        : <Particular inscripcionId={ins.id} contratoId={ins.contratoId} onNavigate={onNavigate} />}

      {ruleError && <BusinessRuleModal error={ruleError} onClose={clearRuleError} />}

      {/* CANCELADA es terminal: la única transición que pide confirmación explícita. */}
      {confirmCancelar && (
        <Modal
          kicker="Acción irreversible"
          title="Cancelar la inscripción"
          onClose={() => setConfirmCancelar(false)}
          width={470}
          footer={<>
            <Button variant="secondary" onClick={() => setConfirmCancelar(false)}>Volver</Button>
            <Button
              variant="danger"
              disabled={cancelar.isPending}
              onClick={() => { setConfirmCancelar(false); correr(cancelar, 'Inscripción cancelada'); }}
            >Cancelar inscripción</Button>
          </>}
        >
          <p style={{ margin: 0, fontSize: 14.5, lineHeight: 1.55, color: 'var(--ga-muted)' }}>
            CANCELADA es un estado terminal: después no se puede reanudar ni finalizar. Las
            evaluaciones y el seguimiento quedan como registro histórico.
          </p>
          <code style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 12, color: 'var(--ga-muted)' }}>
            POST /api/inscripciones/{ins.id}/cancelar
          </code>
        </Modal>
      )}
    </div>
  );
}

function Meta({ label, value, mono }: { label: string; value: React.ReactNode; mono?: boolean }) {
  return (
    <div style={{ background: 'var(--ga-surface)', padding: '15px 20px', display: 'flex', flexDirection: 'column', gap: 6 }}>
      <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 10.5, letterSpacing: '.12em', textTransform: 'uppercase', color: 'var(--ga-soft)' }}>{label}</span>
      <span style={{ fontSize: 14.5, fontFamily: mono ? 'var(--ga-font-mono)' : 'inherit' }}>{value}</span>
    </div>
  );
}

/* ─────────────── Rama escolar: evaluaciones por módulo ─────────────── */

const NOTA_APROBACION = 6;
const MIN_NOTAS_BASE = 3;   // CENMA Base: mínimo 3 notas por módulo, sin contar el integrador

const esGate = (t: TipoInstanciaEvaluativa) => t === 'TP_INTEGRADOR' || t === 'INTEGRADOR';

/**
 * "Módulo aprobado" no lo expone la API: se calcula en cliente y se marca como
 * heurística. Las dos líneas cierran el módulo de forma distinta.
 */
function evaluarModulo(instancias: InstanciaEvaluativaResponse[], estrategia?: EstrategiaEvaluacion) {
  const gates = instancias.filter((n) => esGate(n.tipo));
  const gatesOk = gates.length > 0 && gates.every((g) => (g.nota ?? 0) >= NOTA_APROBACION);
  const finales = instancias.filter((n) => n.tipo === 'EVALUACION_FINAL');
  const notas = instancias.filter((n) => n.tipo === 'NOTA');

  if (estrategia === 'CENMA_SEDE') {
    // La final es directamente la nota del módulo: no se promedia con el TP.
    const ultima = finales.length ? finales[finales.length - 1].nota ?? null : null;
    return {
      labelNota: 'Nota del módulo',
      valor: ultima,
      aprobado: ultima != null && ultima >= NOTA_APROBACION && gatesOk,
      fueraDeSecuencia: finales.length > 0 && gates.length > 0 && !gatesOk,
      conteoBase: null as string | null,
      conteoCumplido: false,
    };
  }

  // Base: promedio de las notas que promedian. Una nota recuperada por otra queda fuera
  // (el backend la marca con cuentaParaPromedio = false; acá también la descontamos por recuperaAId).
  const recuperadas = new Set(instancias.map((n) => n.recuperaAId).filter(Boolean) as number[]);
  const promedian = notas.filter((n) => n.cuentaParaPromedio && n.nota != null && !recuperadas.has(n.id));
  const promedio = promedian.length
    ? promedian.reduce((a, n) => a + (n.nota ?? 0), 0) / promedian.length
    : null;
  const cumplido = notas.length >= MIN_NOTAS_BASE;
  return {
    labelNota: 'Promedio',
    valor: promedio,
    aprobado: promedio != null && promedio >= NOTA_APROBACION && cumplido && gatesOk,
    fueraDeSecuencia: false,
    conteoBase: cumplido
      ? `${notas.length} notas · mínimo cumplido`
      : `${notas.length} de ${MIN_NOTAS_BASE} notas mínimas`,
    conteoCumplido: cumplido,
  };
}

const BAJADA: Record<string, string> = {
  CENMA_SEDE: 'Sede · el TP integrador habilita la final, y la final es la nota del módulo',
  CENMA_BASE: 'Base · mínimo 3 notas por módulo + integrador interdisciplinario, promedio con 2 decimales',
};

function Evaluaciones({ inscripcionId, programaId, estrategia }: {
  inscripcionId: number; programaId: number; estrategia?: EstrategiaEvaluacion;
}) {
  const modulos = useModulos(programaId);
  const instancias = useInstancias(inscripcionId);
  const [cargando, setCargando] = React.useState<ModuloResponse | null>(null);

  if (modulos.isPending || instancias.isPending) return <Card title="Evaluaciones"><SkeletonList rows={3} /></Card>;
  if (modulos.error) return <ErrorScreen error={modulos.error} onRetry={modulos.refetch} entidad="programa" />;
  if (instancias.error) return <ErrorScreen error={instancias.error} onRetry={instancias.refetch} entidad="evaluaciones" />;

  const todas = instancias.data ?? [];
  const ordenados = (modulos.data ?? []).slice().sort((a, b) => a.orden - b.orden);
  const conNotas = ordenados
    .map((m) => ({ modulo: m, instancias: todas.filter((n) => n.moduloId === m.id) }))
    .filter((x) => x.instancias.length > 0);

  const abrirCarga = () => setCargando(ordenados[0] ?? null);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
        <h2 style={{ margin: 0, fontSize: 17, fontWeight: 600 }}>Evaluaciones</h2>
        <span style={{ fontSize: 13.5, color: 'var(--ga-muted)' }}>
          {estrategia ? BAJADA[estrategia] ?? '' : ''}
        </span>
        <Button variant="secondary" onClick={abrirCarga} disabled={ordenados.length === 0} style={{ marginLeft: 'auto' }}>
          Cargar nota
        </Button>
      </div>

      {conNotas.length === 0
        ? <Card>
            <EmptyState
              title="Todavía no hay notas cargadas"
              text={estrategia === 'CENMA_SEDE'
                ? 'En Sede cada módulo se cierra con el TP integrador y la evaluación final. El plan del alumno exime los módulos anteriores al de inicio.'
                : 'En Base cada módulo lleva un mínimo de 3 notas más el integrador interdisciplinario.'}
              action={<Button onClick={abrirCarga} disabled={ordenados.length === 0}>Cargar nota</Button>}
            />
          </Card>
        : conNotas.map(({ modulo, instancias: ns }) => (
            <Modulo key={modulo.id} modulo={modulo} instancias={ns} estrategia={estrategia} />
          ))}

      {cargando && (
        <NotaModal
          inscripcionId={inscripcionId}
          modulos={ordenados}
          instancias={todas}
          estrategia={estrategia}
          onClose={() => setCargando(null)}
        />
      )}
    </div>
  );
}

function Modulo({ modulo, instancias, estrategia }: {
  modulo: ModuloResponse; instancias: InstanciaEvaluativaResponse[]; estrategia?: EstrategiaEvaluacion;
}) {
  const { labelNota, valor, aprobado, fueraDeSecuencia, conteoBase, conteoCumplido } = evaluarModulo(instancias, estrategia);
  const recuperadas = new Set(instancias.map((n) => n.recuperaAId).filter(Boolean) as number[]);
  return (
    <section style={{ background: 'var(--ga-surface)', border: '1px solid var(--ga-line)', borderRadius: 'var(--ga-radius-lg)', overflow: 'hidden' }}>
      <header style={{ padding: '14px 20px', borderBottom: '1px solid var(--ga-line-soft)', display: 'flex', alignItems: 'center', gap: 11, flexWrap: 'wrap' }}>
        <span style={{ fontSize: 15.5, fontWeight: 600 }}>Módulo {modulo.orden}</span>
        <span style={{
          fontFamily: 'var(--ga-font-mono)', fontSize: 11, fontWeight: 600, letterSpacing: '.04em',
          padding: '3px 8px', borderRadius: 'var(--ga-radius-xs)',
          background: aprobado ? 'var(--ga-ok-bg)' : 'var(--ga-warn-bg)',
          color: aprobado ? 'var(--ga-ok-fg)' : 'var(--ga-warn-fg)',
        }}>{aprobado ? 'APROBADO' : 'EN CURSO'}</span>
        {!modulo.esSecuencial && <MonoChip>INDEPENDIENTE</MonoChip>}
        {/* Solo Base tiene mínimo de notas: en Sede el conteo no significa nada. */}
        {conteoBase && (
          <span style={{
            fontFamily: 'var(--ga-font-mono)', fontSize: 11, fontWeight: 600, letterSpacing: '.04em',
            padding: '3px 8px', borderRadius: 'var(--ga-radius-xs)',
            background: conteoCumplido ? 'var(--ga-surface-2)' : 'var(--ga-warn-bg)',
            color: conteoCumplido ? 'var(--ga-muted)' : 'var(--ga-warn-fg)',
          }}>{conteoBase}</span>
        )}
        <span style={{ marginLeft: 'auto', fontFamily: 'var(--ga-font-mono)', fontSize: 13, color: 'var(--ga-muted)' }}>
          {labelNota} <strong style={{ color: 'var(--ga-ink)', fontSize: 15 }}>{valor != null ? valor.toFixed(2) : '—'}</strong>
        </span>
      </header>

      {/* Advertencia ámbar NO bloqueante: la API permite cargar fuera de secuencia. */}
      {fueraDeSecuencia && (
        <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start', padding: '12px 20px', background: 'var(--ga-warn-panel)', borderBottom: '1px solid var(--ga-warn-panel-line)' }}>
          <span style={{ width: 18, height: 18, borderRadius: 999, flex: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, background: 'var(--ga-warn-bg)', color: 'var(--ga-warn-fg)', marginTop: 1 }}>!</span>
          <span style={{ fontSize: 13, color: 'var(--ga-warn-panel-fg)', lineHeight: 1.5 }}>
            Se cargó una EVALUACION_FINAL sin el TP integrador aprobado en este módulo. La API no lo bloquea; revisá si es correcto.
          </span>
        </div>
      )}

      <table style={{ width: '100%', minWidth: 620, borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: 'var(--ga-surface-2)' }}>
            <th style={thStyle}>Instancia</th>
            <th style={thStyle}>Fecha</th>
            <th style={thStyle}>Nota</th>
            <th style={thStyle}>Promedia</th>
            <th style={thStyle}>Observación</th>
          </tr>
        </thead>
        <tbody>
          {instancias.slice().sort((a, b) => (a.fecha < b.fecha ? -1 : 1)).map((n) => {
            const gate = esGate(n.tipo);
            const ok = (n.nota ?? 0) >= NOTA_APROBACION;
            const fueRecuperada = recuperadas.has(n.id);
            const promedia = !gate && n.cuentaParaPromedio && !fueRecuperada;
            return (
              <tr key={n.id} style={{ borderTop: '1px solid var(--ga-line-soft)' }}>
                <td style={{ ...tdStyle, fontFamily: 'var(--ga-font-mono)', fontSize: 13 }}>{n.tipo}</td>
                <td style={{ ...tdStyle, fontFamily: 'var(--ga-font-mono)', fontSize: 13.5, color: 'var(--ga-muted)' }}>{n.fecha}</td>
                <td style={{ ...tdStyle, fontWeight: 500, color: ok ? 'var(--ga-ink)' : 'var(--ga-danger-fg)' }}>
                  {n.nota == null ? '—' : gate ? (ok ? 'Aprobado' : 'No aprobado') : n.nota.toFixed(2)}
                </td>
                <td style={{ ...tdStyle, fontSize: 13.5, color: 'var(--ga-muted)' }}>{promedia ? 'Sí' : 'No'}</td>
                <td style={{ ...tdStyle, fontSize: 13.5, color: 'var(--ga-muted)' }}>
                  {n.recuperaAId
                    ? `Recupera a la instancia #${n.recuperaAId}`
                    : fueRecuperada
                      ? 'Recuperada por otra nota'
                      : gate
                        ? (n.tipo === 'TP_INTEGRADOR' ? 'Gate de habilitación' : 'Interdisciplinario')
                        : ''}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </section>
  );
}

/** Los tipos ofrecidos dependen de la línea: no hay un select universal de 4 tipos. */
const TIPOS: Record<string, { v: TipoInstanciaEvaluativa; label: string }[]> = {
  CENMA_SEDE: [
    { v: 'TP_INTEGRADOR', label: 'TP_INTEGRADOR — gate, aprobado / no aprobado' },
    { v: 'EVALUACION_FINAL', label: 'EVALUACION_FINAL — es la nota del módulo' },
  ],
  CENMA_BASE: [
    { v: 'NOTA', label: 'NOTA — promedia (mínimo 3 por módulo)' },
    { v: 'INTEGRADOR', label: 'INTEGRADOR — interdisciplinario, no promedia' },
  ],
};

function NotaModal({ inscripcionId, modulos, instancias, estrategia, onClose }: {
  inscripcionId: number;
  modulos: ModuloResponse[];
  instancias: InstanciaEvaluativaResponse[];
  estrategia?: EstrategiaEvaluacion;
  onClose: () => void;
}) {
  const crear = useCrearInstancia(inscripcionId);
  const { handle, fieldErrors } = useApiErrorHandler();
  const { push } = useToasts();

  const sede = estrategia === 'CENMA_SEDE';
  const tipos = TIPOS[estrategia ?? 'CENMA_BASE'] ?? TIPOS.CENMA_BASE;

  const [moduloId, setModuloId] = React.useState(modulos[0]?.id ?? 0);
  const [tipo, setTipo] = React.useState<TipoInstanciaEvaluativa>(tipos[0].v);
  const [fecha, setFecha] = React.useState(new Date().toISOString().slice(0, 10));
  const [nota, setNota] = React.useState('');
  const [aprobado, setAprobado] = React.useState('Aprobado');
  const [recuperaAId, setRecuperaAId] = React.useState('');

  const gate = esGate(tipo);
  const delModulo = instancias.filter((n) => n.moduloId === moduloId);
  const notasCargadas = delModulo.filter((n) => n.tipo === 'NOTA');
  const recuperadas = new Set(delModulo.map((n) => n.recuperaAId).filter(Boolean) as number[]);
  const recuperables = notasCargadas.filter((n) => n.cuentaParaPromedio && !recuperadas.has(n.id));
  const puedeRecuperar = !sede && tipo === 'NOTA' && recuperables.length > 0;

  const aviso = sede
    ? (gate
        ? 'El TP integrador no lleva nota propia ni promedia: es el gate que habilita la EVALUACION_FINAL y, en módulos secuenciales, el módulo siguiente.'
        : 'En Sede la final ES la nota del módulo: no se promedia con el TP. La API no valida la secuencia, así que se puede cargar sin el TP aprobado.')
    : (gate
        ? 'El integrador interdisciplinario va con cuentaParaPromedio = false: no entra al promedio del módulo.'
        : 'La nota entra al promedio del módulo (2 decimales). Con recuperatorio, la nueva reemplaza a la original en el promedio y la original se conserva.');

  const submit = () => {
    if (!moduloId) { push({ tone: 'warn', title: '400 · Falta el módulo', text: 'moduloId es obligatorio.' }); return; }
    const valor = parseFloat(nota.replace(',', '.'));
    if (!gate && (isNaN(valor) || valor < 0 || valor > 10)) {
      push({ tone: 'warn', title: '400 · Nota inválida', text: 'La nota va de 0.00 a 10.00.' });
      return;
    }
    crear.mutate({
      inscripcionId,
      moduloId,
      tipo,
      // La API no tiene un booleano para el gate: se registra como nota y la UI lo lee como
      // aprobado / no aprobado. Si el backend agrega un campo propio, este mapeo se borra.
      nota: gate ? (aprobado === 'Aprobado' ? 10 : 0) : valor,
      fecha,
      cuentaParaPromedio: !gate,
      recuperaAId: puedeRecuperar && recuperaAId ? Number(recuperaAId) : undefined,
    }, {
      onSuccess: () => {
        onClose();
        push({
          tone: 'ok',
          title: 'Nota cargada',
          text: sede
            ? 'En Sede la final define sola la nota del módulo.'
            : 'Promedio recalculado sobre las notas que promedian.',
        });
      },
      onError: (e) => handle(e),
    });
  };

  return (
    <Modal
      title="Cargar nota"
      onClose={onClose}
      width={520}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cancelar</Button>
        <Button onClick={submit} disabled={crear.isPending}>Cargar nota</Button>
      </>}
    >
      <p style={{ margin: 0, fontSize: 13.5, lineHeight: 1.5, color: 'var(--ga-muted)' }}>
        {sede
          ? 'Queda asociada a la inscripción y al módulo. La EVALUACION_FINAL define sola la nota del módulo; el TP integrador no promedia.'
          : 'Queda asociada a la inscripción y al módulo. El promedio del módulo se recalcula sobre las notas que promedian.'}
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div style={{ gridColumn: 'span 2' }}>
          <Field label="Módulo" required error={fieldErrors.moduloId}>
            <Select value={moduloId} onChange={(e) => { setModuloId(Number(e.target.value)); setRecuperaAId(''); }}>
              {modulos.map((m) => <option key={m.id} value={m.id}>Módulo {m.orden}</option>)}
            </Select>
          </Field>
        </div>

        <div style={{ gridColumn: 'span 2' }}>
          <Field
            label="Tipo"
            required
            error={fieldErrors.tipo}
            hint={sede
              ? 'Sede: el TP habilita la final, y la final no se promedia con el TP.'
              : (notasCargadas.length >= MIN_NOTAS_BASE
                  ? `Este módulo ya tiene ${notasCargadas.length} notas: el mínimo está cumplido.`
                  : `Este módulo tiene ${notasCargadas.length} de las ${MIN_NOTAS_BASE} notas mínimas (el integrador no cuenta).`)}
          >
            <Select value={tipo} onChange={(e) => { setTipo(e.target.value as TipoInstanciaEvaluativa); setRecuperaAId(''); }}>
              {tipos.map((t) => <option key={t.v} value={t.v}>{t.label}</option>)}
            </Select>
          </Field>
        </div>

        <Field label="Fecha" required error={fieldErrors.fecha}>
          <Input value={fecha} onChange={(e) => setFecha(e.target.value)} mono invalid={!!fieldErrors.fecha} />
        </Field>

        <Field label={gate ? 'Resultado' : 'Nota'} required error={fieldErrors.nota}>
          {gate
            ? <Select value={aprobado} onChange={(e) => setAprobado(e.target.value)}>
                <option value="Aprobado">Aprobado</option>
                <option value="No aprobado">No aprobado</option>
              </Select>
            : <Input value={nota} onChange={(e) => setNota(e.target.value)} mono placeholder="0.00 – 10.00" invalid={!!fieldErrors.nota} />}
        </Field>

        {/* Recuperatorio: solo Base, solo NOTA, solo si hay notas vigentes que reemplazar. */}
        {puedeRecuperar && (
          <div style={{ gridColumn: 'span 2' }}>
            <Field
              label="Recupera a"
              hint="La nota recuperada deja de promediar, pero queda visible en la tabla vía recuperaAId."
            >
              <Select value={recuperaAId} onChange={(e) => setRecuperaAId(e.target.value)}>
                <option value="">— no es recuperatorio</option>
                {recuperables.map((n) => (
                  <option key={n.id} value={n.id}>Nota {(n.nota ?? 0).toFixed(2)} del {n.fecha}</option>
                ))}
              </Select>
            </Field>
          </div>
        )}
      </div>

      <div style={{ background: 'var(--ga-primary-50)', border: '1px solid var(--ga-primary-100)', borderRadius: 'var(--ga-radius-md)', padding: '11px 13px', fontSize: 13, lineHeight: 1.5, color: 'var(--ga-primary-700)' }}>
        {aviso}
      </div>
    </Modal>
  );
}

/* ─────────────── Rama particular: contrato + seguimiento ─────────────── */

function Particular({ inscripcionId, contratoId, onNavigate }: {
  inscripcionId: number; contratoId: number | null; onNavigate: (r: Route) => void;
}) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '360px minmax(0,1fr)', gap: 20, alignItems: 'start' }}>
      <Contrato contratoId={contratoId} onNavigate={onNavigate} />
      <Seguimiento inscripcionId={inscripcionId} />
    </div>
  );
}

function Contrato({ contratoId, onNavigate }: { contratoId: number | null; onNavigate: (r: Route) => void }) {
  const { data: c, isPending, error } = useContrato(contratoId);
  const consumir = useConsumirClase(contratoId ?? 0);
  const ampliar = useAmpliarCupo(contratoId ?? 0);
  const { handle } = useApiErrorHandler();
  const { push } = useToasts();

  if (!contratoId) {
    return (
      <Card title="Contrato">
        <EmptyState
          title="Sin contrato"
          text="Esta inscripción todavía no está cubierta: no se pueden registrar clases consumidas."
          action={<Button onClick={() => onNavigate({ name: 'contratos' })}>Ir a Contratos</Button>}
        />
      </Card>
    );
  }
  if (isPending) return <Card title="Contrato"><SkeletonList rows={3} /></Card>;
  if (error) return <Card title="Contrato"><ErrorScreen error={error} entidad="contrato" /></Card>;
  if (!c) return null;

  const finalizado = c.estado === 'FINALIZADO';
  const agotado = c.tipoFacturacion === 'PAQUETE' && c.clasesConsumidas >= c.clasesContratadas;

  return (
    <Card title={c.empresaNombre ?? 'Contrato individual'} actions={<StatusBadge estado={c.estado} />}>
      <div style={{ padding: '18px 20px', display: 'flex', flexDirection: 'column', gap: 14 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
          <MonoChip>{c.tipoFacturacion}</MonoChip>
          <span style={{ fontSize: 13, color: 'var(--ga-muted)' }}>
            {c.empresaNombre ? 'Pool compartido' : 'Individual'} · #{c.id}
          </span>
        </div>

        <PoolBar
          consumidas={c.clasesConsumidas}
          contratadas={c.clasesContratadas}
          tipoFacturacion={c.tipoFacturacion}
          finalizado={finalizado}
        />

        {c.inscripciones.length > 1 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, borderTop: '1px solid var(--ga-line-soft)', paddingTop: 14 }}>
            <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 10.5, letterSpacing: '.12em', textTransform: 'uppercase', color: 'var(--ga-soft)' }}>Cubre a</span>
            {c.inscripciones.map((e) => (
              <span
                key={e.inscripcionId}
                onClick={() => onNavigate({ name: 'inscripcion', id: e.inscripcionId })}
                style={{ fontSize: 14, cursor: 'pointer', width: 'fit-content' }}
              >{e.personaNombre}</span>
            ))}
          </div>
        )}

        <div style={{ display: 'flex', gap: 9, borderTop: '1px solid var(--ga-line-soft)', paddingTop: 14 }}>
          <Button
            variant="secondary"
            disabled={finalizado || agotado || consumir.isPending}
            onClick={() => consumir.mutate(undefined, {
              onSuccess: (r) => push({ tone: 'ok', title: 'Clase consumida', text: `${r.clasesConsumidas} / ${r.clasesContratadas} clases` }),
              onError: (e) => handle(e, { fixLabel: 'Ampliar cupo', onFix: () => ampliar.mutate(4) }),
            })}
          >Consumir clase</Button>
          <Button
            variant="secondary"
            disabled={finalizado || ampliar.isPending}
            onClick={() => ampliar.mutate(4, {
              onSuccess: (r) => push({ tone: 'ok', title: 'Cupo ampliado', text: `+4 clases. Total ${r.clasesContratadas}.` }),
              onError: (e) => handle(e),
            })}
          >Ampliar cupo +4</Button>
        </div>
      </div>
    </Card>
  );
}

function Seguimiento({ inscripcionId }: { inscripcionId: number }) {
  const { data, isPending, error, refetch } = useSeguimientos(inscripcionId);
  const crear = useCrearSeguimiento(inscripcionId);
  const { handle, fieldErrors } = useApiErrorHandler();
  const { push } = useToasts();
  const [obs, setObs] = React.useState('');

  const submit = () => {
    if (!obs.trim()) { push({ tone: 'warn', title: '400 · Falta la observación', text: 'El campo observacion es obligatorio.' }); return; }
    crear.mutate(
      { fecha: new Date().toISOString().slice(0, 10), observacion: obs.trim() },
      {
        onSuccess: () => { setObs(''); push({ tone: 'ok', title: 'Observación registrada', text: 'Se agregó al seguimiento de la inscripción.' }); },
        onError: (e) => handle(e),
      });
  };

  const items = (data ?? []).slice().sort((a, b) => (a.fecha < b.fecha ? 1 : -1));

  return (
    <Card title="Seguimiento" meta="el avance de una inscripción particular es texto libre, no notas">
      <div style={{ padding: '18px 22px', borderBottom: '1px solid var(--ga-line-soft)', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <Field label="Nueva observación" error={fieldErrors.observacion}>
          <Input
            value={obs}
            onChange={(e) => setObs(e.target.value)}
            invalid={!!fieldErrors.observacion}
            placeholder="Qué se trabajó en la clase, qué quedó pendiente…"
          />
        </Field>
        <Button onClick={submit} disabled={crear.isPending} style={{ width: 'fit-content' }}>Registrar</Button>
      </div>

      {isPending && <SkeletonList rows={3} />}
      {error && <ErrorScreen error={error} onRetry={refetch} entidad="seguimiento" />}
      {!isPending && !error && (items.length === 0
        ? <EmptyState title="Sin observaciones todavía" text="Cada clase deja una línea con fecha y texto libre." />
        : <div style={{ display: 'flex', flexDirection: 'column' }}>
            {items.map((o) => (
              <div key={o.id} style={{ padding: '15px 22px', borderBottom: '1px solid var(--ga-line-soft)', display: 'flex', flexDirection: 'column', gap: 6 }}>
                <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 12.5, color: 'var(--ga-soft)' }}>{o.fecha}</span>
                <span style={{ fontSize: 13.5, lineHeight: 1.55, textWrap: 'pretty' }}>{o.observacion}</span>
              </div>
            ))}
          </div>)}
    </Card>
  );
}
