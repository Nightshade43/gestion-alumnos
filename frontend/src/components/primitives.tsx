import React from 'react';
import { statusTone } from '../../tokens/tokens';
import type { EstadoInscripcion, EstadoContrato, CategoriaPrograma } from '../api/types';

/* ─────────────── Button ─────────────── */
type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';
const BTN: Record<ButtonVariant, React.CSSProperties> = {
  primary:   { background: 'var(--ga-primary-600)', color: '#fff', border: 'none', boxShadow: 'var(--ga-shadow-sm)', fontWeight: 600 },
  secondary: { background: '#fff', color: 'var(--ga-ink)', border: '1px solid var(--ga-line-strong)', fontWeight: 500 },
  ghost:     { background: 'transparent', color: 'var(--ga-muted)', border: 'none', fontWeight: 500 },
  danger:    { background: '#fff', color: 'var(--ga-danger-fg)', border: '1px solid var(--ga-danger-line)', fontWeight: 600 },
};

export function Button({ variant = 'primary', children, ...rest }:
  React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant }) {
  const disabled = rest.disabled;
  return (
    <button
      {...rest}
      style={{
        borderRadius: 'var(--ga-radius-sm)', padding: '10px 15px', fontSize: 14,
        fontFamily: 'inherit', cursor: disabled ? 'not-allowed' : 'pointer',
        ...BTN[variant],
        ...(disabled ? { background: '#F0EAE1', color: '#B3A99C', border: '1px solid transparent', boxShadow: 'none' } : null),
        ...rest.style,
      }}
    >
      {children}
    </button>
  );
}

/* ─────────────── Field / Input / Select ───────────────
   Regla de diseño: el 400 de la API se muestra inline, debajo del campo,
   y el formulario NO se cierra ni pierde lo cargado. */
export function Field({ label, required, hint, error, children }: {
  label: string; required?: boolean; hint?: string; error?: string; children: React.ReactNode;
}) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--ga-ink-soft)' }}>
        {label}{required ? ' *' : ''}
      </span>
      {children}
      {error && <span style={{ fontSize: 12, color: 'var(--ga-danger-fg)' }}>{error}</span>}
      {!error && hint && <span style={{ fontSize: 12, color: 'var(--ga-soft)' }}>{hint}</span>}
    </label>
  );
}

export const inputStyle = (opts?: { invalid?: boolean; mono?: boolean }): React.CSSProperties => ({
  fontFamily: opts?.mono ? 'var(--ga-font-mono)' : 'inherit',
  fontSize: 14.5, padding: '10px 12px', background: opts?.invalid ? '#FFFBFA' : '#fff',
  color: 'var(--ga-ink)', outline: 'none', borderRadius: 'var(--ga-radius-sm)',
  border: `1px solid ${opts?.invalid ? '#DE9C95' : 'var(--ga-line-strong)'}`,
});

export function Input({ invalid, mono, ...rest }: React.InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean; mono?: boolean }) {
  return (
    <input
      {...rest}
      onFocus={(e) => { e.currentTarget.style.borderColor = 'var(--ga-primary-600)'; e.currentTarget.style.boxShadow = 'var(--ga-focus-ring)'; rest.onFocus?.(e); }}
      onBlur={(e) => { e.currentTarget.style.borderColor = invalid ? '#DE9C95' : 'var(--ga-line-strong)'; e.currentTarget.style.boxShadow = 'none'; rest.onBlur?.(e); }}
      style={{ ...inputStyle({ invalid, mono }), ...rest.style }}
    />
  );
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} style={{ ...inputStyle(), ...props.style }} />;
}

/* ─────────────── Badges ─────────────── */
export function StatusBadge({ estado }: { estado: EstadoInscripcion | EstadoContrato }) {
  const t = statusTone[estado];
  return (
    <span style={{
      background: t.bg, color: t.fg, border: `1px solid ${t.line}`,
      fontSize: 11.5, fontWeight: 600, letterSpacing: '.03em',
      padding: '4px 9px', borderRadius: 'var(--ga-radius-pill)', width: 'fit-content',
    }}>{estado}</span>
  );
}

export function RamaTag({ categoria, dot = true }: { categoria: CategoriaPrograma; dot?: boolean }) {
  const esc = categoria === 'ESCOLAR';
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 7, fontSize: 13, color: esc ? 'var(--ga-escolar-fg)' : 'var(--ga-particular-fg)' }}>
      {dot && <span style={{ width: 8, height: 8, borderRadius: 3, background: esc ? 'var(--ga-escolar)' : 'var(--ga-particular)' }} />}
      {esc ? 'Escolar' : 'Particular'}
    </span>
  );
}

export function MonoChip({ children }: { children: React.ReactNode }) {
  return (
    <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 12, color: 'var(--ga-chip-text)', background: 'var(--ga-chip-bg)', padding: '3px 8px', borderRadius: 'var(--ga-radius-xs)' }}>
      {children}
    </span>
  );
}

/* ─────────────── Card / Table ─────────────── */
export function Card({ title, meta, actions, children, padded }: {
  title?: string; meta?: string; actions?: React.ReactNode; children?: React.ReactNode; padded?: boolean;
}) {
  return (
    <section style={{ background: 'var(--ga-surface)', border: '1px solid var(--ga-line)', borderRadius: 'var(--ga-radius-lg)', overflow: 'hidden' }}>
      {(title || actions) && (
        <header style={{ padding: '15px 22px', borderBottom: '1px solid var(--ga-line-soft)', display: 'flex', alignItems: 'center', gap: 10 }}>
          {title && <h2 style={{ margin: 0, fontSize: 16, fontWeight: 600 }}>{title}</h2>}
          {meta && <span style={{ fontSize: 13, color: 'var(--ga-soft)' }}>{meta}</span>}
          {actions && <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>{actions}</div>}
        </header>
      )}
      <div style={padded ? { padding: '18px 22px' } : undefined}>{children}</div>
    </section>
  );
}

export const thStyle: React.CSSProperties = {
  padding: '11px 16px', textAlign: 'left', fontSize: 11, letterSpacing: '.1em',
  textTransform: 'uppercase', color: 'var(--ga-muted)', fontWeight: 600, fontFamily: 'var(--ga-font-mono)',
};
export const tdStyle: React.CSSProperties = { padding: '13px 16px', fontSize: 14.5 };

export function DataTable<Row>({ rows, columns, onRowClick, empty }: {
  rows: Row[];
  columns: { key: string; header: string; render: (r: Row) => React.ReactNode; mono?: boolean }[];
  onRowClick?: (r: Row) => void;
  empty?: React.ReactNode;
}) {
  if (rows.length === 0 && empty) return <>{empty}</>;
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
      <thead>
        <tr style={{ background: 'var(--ga-surface-2)' }}>
          {columns.map((c) => <th key={c.key} style={thStyle}>{c.header}</th>)}
        </tr>
      </thead>
      <tbody>
        {rows.map((r, i) => (
          <tr
            key={i}
            onClick={onRowClick ? () => onRowClick(r) : undefined}
            style={{ borderTop: '1px solid var(--ga-line-soft)', cursor: onRowClick ? 'pointer' : 'default' }}
            onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--ga-primary-50)')}
            onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
          >
            {columns.map((c) => (
              <td key={c.key} style={{ ...tdStyle, fontFamily: c.mono ? 'var(--ga-font-mono)' : 'inherit', fontSize: c.mono ? 13.5 : 14.5, color: c.mono ? 'var(--ga-muted)' : undefined }}>
                {c.render(r)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/* ─────────────── Estados de carga / vacío ─────────────── */
export function SkeletonList({ rows = 5 }: { rows?: number }) {
  const widths = ['70%', '92%', '55%', '80%', '64%'];
  return (
    <div style={{ padding: 22, display: 'flex', flexDirection: 'column', gap: 14 }}>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} style={{
          height: 13, width: widths[i % widths.length], borderRadius: 5,
          background: 'linear-gradient(90deg,#F0EAE1 25%,#F8F3EC 50%,#F0EAE1 75%)',
          backgroundSize: '420px 100%', animation: 'ga-shimmer 1.4s infinite linear',
        }} />
      ))}
    </div>
  );
}

export function Spinner() {
  return <span style={{ width: 13, height: 13, border: '2px solid var(--ga-line)', borderTopColor: 'var(--ga-primary-600)', borderRadius: 999, display: 'inline-block', animation: 'ga-spin .8s linear infinite' }} />;
}

export function EmptyState({ title, text, action, tone = 'neutral', code }: {
  title: string; text?: string; action?: React.ReactNode; tone?: 'neutral' | 'danger'; code?: string;
}) {
  return (
    <div style={{ padding: '58px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, textAlign: 'center' }}>
      <div style={{
        width: 46, height: 46, borderRadius: 13,
        background: tone === 'danger' ? 'var(--ga-danger-bg)' : 'var(--ga-surface-2)',
        border: tone === 'danger' ? 'none' : '1px dashed var(--ga-line-strong)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: 'var(--ga-danger-fg)', fontSize: 19, fontWeight: 600,
      }}>{tone === 'danger' ? '?' : ''}</div>
      <span style={{ fontSize: 16, fontWeight: 600 }}>{title}</span>
      {text && <span style={{ fontSize: 14, color: 'var(--ga-muted)', maxWidth: '38ch', lineHeight: 1.5 }}>{text}</span>}
      {code && <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 12, color: 'var(--ga-muted)', background: 'var(--ga-surface-2)', border: '1px solid #EDE5D9', borderRadius: 8, padding: '7px 10px' }}>{code}</span>}
      {action}
    </div>
  );
}

/* ─────────────── Modal ─────────────── */
export function Modal({ title, kicker, children, footer, onClose, width = 520 }: {
  title: string; kicker?: string; children: React.ReactNode; footer?: React.ReactNode; onClose: () => void; width?: number;
}) {
  return (
    <div onClick={onClose} style={{ position: 'fixed', inset: 0, background: 'rgba(43,39,36,.42)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 60 }}>
      <div onClick={(e) => e.stopPropagation()} style={{ width, background: '#fff', borderRadius: 'var(--ga-radius-xl)', boxShadow: 'var(--ga-shadow-lg)', overflow: 'hidden' }}>
        <div style={{ padding: '22px 24px 4px', display: 'flex', flexDirection: 'column', gap: 8 }}>
          {kicker && <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 11, letterSpacing: '.1em', textTransform: 'uppercase', color: 'var(--ga-warn-fg)' }}>{kicker}</span>}
          <h3 style={{ margin: 0, fontSize: 19, fontWeight: 600, letterSpacing: '-.01em' }}>{title}</h3>
        </div>
        <div style={{ padding: '16px 24px' }}>{children}</div>
        {footer && <div style={{ padding: '6px 24px 22px', display: 'flex', gap: 10, justifyContent: 'flex-end' }}>{footer}</div>}
      </div>
    </div>
  );
}

/* ─────────────── Progreso de pool de clases ─────────────── */
export function PoolBar({ consumidas, contratadas }: { consumidas: number; contratadas: number }) {
  const pct = Math.min(100, Math.round((consumidas / Math.max(1, contratadas)) * 100));
  const agotado = consumidas >= contratadas;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'baseline' }}>
        <span style={{ fontFamily: 'var(--ga-font-mono)', fontSize: 10.5, letterSpacing: '.12em', textTransform: 'uppercase', color: 'var(--ga-soft)' }}>Pool de clases</span>
        <span className="ga-num" style={{ marginLeft: 'auto', fontSize: 15, fontWeight: 600 }}>{consumidas} / {contratadas}</span>
      </div>
      <div style={{ height: 9, borderRadius: 999, background: 'var(--ga-line-soft)', overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${pct}%`, borderRadius: 999, background: agotado ? 'var(--ga-danger-fg)' : 'var(--ga-primary-600)' }} />
      </div>
      <span style={{ fontSize: 12.5, color: 'var(--ga-soft)' }}>
        {agotado ? 'Pool agotado — consumir devuelve 409' : `${contratadas - consumidas} clases disponibles en el pool`}
      </span>
    </div>
  );
}
