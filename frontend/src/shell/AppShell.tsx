import React from 'react';
import { onUnauthorized, tokenStore } from '../api/client';

/**
 * Shell de escritorio: sidebar fijo de 248px + contenido de máx. 1220px.
 * Sin responsive por decisión de producto (uso exclusivo en escritorio/notebook).
 *
 * Navegación: 7 ítems para 12 controllers. Módulos/Planes/Grupos NO están en el
 * sidebar (son pestañas de Programa) y Evaluaciones/Seguimiento tampoco
 * (viven en el detalle de la Inscripción, según la rama).
 */

export type Route =
  | { name: 'inicio' }
  | { name: 'personas' }
  | { name: 'persona'; id: number }
  | { name: 'inscripciones'; rama: 'ESCOLAR' | 'PARTICULAR' }
  | { name: 'inscripcion'; id: number }
  | { name: 'programas' }
  | { name: 'programa'; id: number }
  | { name: 'instituciones' }
  | { name: 'contratos' }
  | { name: 'empresas' };

const itemStyle = (active: boolean): React.CSSProperties => ({
  display: 'flex', alignItems: 'center', gap: 10, padding: '9px 11px',
  borderRadius: 'var(--ga-radius-sm)', fontSize: 14, cursor: 'pointer',
  background: active ? 'var(--ga-primary-600)' : 'transparent',
  color: active ? '#fff' : 'var(--ga-nav-text)',
  fontWeight: active ? 600 : 400,
});

const groupLabel: React.CSSProperties = {
  fontFamily: 'var(--ga-font-mono)', fontSize: 10, letterSpacing: '.14em',
  textTransform: 'uppercase', color: 'var(--ga-nav-label)',
  padding: '16px 11px 7px', display: 'flex', alignItems: 'center', gap: 8,
};

export function AppShell({ route, onNavigate, onLogout, children }: {
  route: Route; onNavigate: (r: Route) => void; onLogout: () => void; children: React.ReactNode;
}) {
  React.useEffect(() => {
    const fn = () => { tokenStore.clear(); onLogout(); };
    onUnauthorized.add(fn);
    return () => { onUnauthorized.delete(fn); };
  }, [onLogout]);

  const is = (n: Route['name'], rama?: 'ESCOLAR' | 'PARTICULAR') =>
    route.name === n && (!rama || (route as any).rama === rama);

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'var(--ga-sidebar-w) 1fr', minHeight: '100vh' }}>
      <aside style={{ background: 'var(--ga-nav-bg)', padding: '18px 12px', display: 'flex', flexDirection: 'column', gap: 3, color: '#E8E1D8', position: 'sticky', top: 0, height: '100vh', boxSizing: 'border-box' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 10px 18px' }}>
          <div style={{ width: 26, height: 26, borderRadius: 8, background: 'var(--ga-primary-600)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 13, fontWeight: 700 }}>G</div>
          <span style={{ fontSize: 14.5, fontWeight: 600 }}>Gestión de Alumnos</span>
        </div>

        <div style={itemStyle(is('inicio'))} onClick={() => onNavigate({ name: 'inicio' })}>Inicio</div>
        <div style={itemStyle(is('personas') || is('persona'))} onClick={() => onNavigate({ name: 'personas' })}>Personas</div>

        <div style={groupLabel}><span style={{ width: 7, height: 7, borderRadius: 2, background: '#5E90AF' }} />Escolar</div>
        <div style={itemStyle(is('inscripciones', 'ESCOLAR'))} onClick={() => onNavigate({ name: 'inscripciones', rama: 'ESCOLAR' })}>Inscripciones</div>
        <div style={itemStyle(is('programas') || is('programa'))} onClick={() => onNavigate({ name: 'programas' })}>Programas</div>
        <div style={itemStyle(is('instituciones'))} onClick={() => onNavigate({ name: 'instituciones' })}>Instituciones</div>

        <div style={groupLabel}><span style={{ width: 7, height: 7, borderRadius: 2, background: '#C79240' }} />Particular</div>
        <div style={itemStyle(is('inscripciones', 'PARTICULAR'))} onClick={() => onNavigate({ name: 'inscripciones', rama: 'PARTICULAR' })}>Inscripciones</div>
        <div style={itemStyle(is('contratos'))} onClick={() => onNavigate({ name: 'contratos' })}>Contratos</div>
        <div style={itemStyle(is('empresas'))} onClick={() => onNavigate({ name: 'empresas' })}>Empresas</div>

        <div style={{ marginTop: 'auto', borderTop: '1px solid var(--ga-nav-hover)', paddingTop: 12, display: 'flex', alignItems: 'center', gap: 10, paddingLeft: 11 }}>
          <div style={{ width: 26, height: 26, borderRadius: 999, background: 'var(--ga-nav-hover)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 600, color: 'var(--ga-nav-text)' }}>AD</div>
          <span style={{ fontSize: 13, color: '#B7ADA1' }}>admin</span>
          <span onClick={onLogout} style={{ marginLeft: 'auto', paddingRight: 11, fontSize: 12, color: 'var(--ga-nav-label)', cursor: 'pointer' }}>Salir</span>
        </div>
      </aside>

      <main style={{ padding: 'var(--ga-content-pad)', maxWidth: 'var(--ga-content-max)', width: '100%', boxSizing: 'border-box' }}>
        {children}
      </main>
    </div>
  );
}

export function ScreenHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 16 }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 5 }}>
        <h1 style={{ margin: 0, fontSize: 30, fontWeight: 600, letterSpacing: '-.025em' }}>{title}</h1>
        {subtitle && <span style={{ fontSize: 14, color: 'var(--ga-muted)' }}>{subtitle}</span>}
      </div>
      {actions && <div style={{ marginLeft: 'auto', display: 'flex', gap: 10, alignItems: 'center' }}>{actions}</div>}
    </div>
  );
}
