import React from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { tokenStore, onUnauthorized } from './api/client';
import { AppShell } from './shell/AppShell';
import type { Route } from './shell/AppShell';
import { ToastProvider } from './components/Toasts';
import { LoginScreen } from './screens/LoginScreen';
import { PersonasScreen } from './screens/PersonasScreen';
import { InscripcionesScreen } from './screens/InscripcionesScreen';
import { InscripcionScreen } from './screens/InscripcionScreen';
import { InicioScreen } from './screens/InicioScreen';
import { PersonaScreen } from './screens/PersonaScreen';
import { ProgramasScreen } from './screens/ProgramasScreen';
import { ProgramaScreen } from './screens/ProgramaScreen';
import { InstitucionesScreen } from './screens/InstitucionesScreen';
import { ContratosScreen } from './screens/ContratosScreen';
import { EmpresasScreen } from './screens/EmpresasScreen';

/**
 * Guard de sesión + router mínimo.
 *
 * No hay react-router: la app tiene 11 pantallas y navegación de sidebar, así que
 * el estado de ruta es un discriminated union (`Route`). `from` recuerda de dónde
 * se entró a un detalle para que el "volver" sea contextual.
 *
 * La sesión arranca del token guardado. Cualquier 401 de la API dispara
 * `onUnauthorized` (lo escucha AppShell) y también este listener, que devuelve
 * el árbol a Login: no hay pantalla autenticada sin token.
 */

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Un 401 lo resuelve el guard, no tiene sentido reintentarlo.
      retry: (count, error: any) => (error?.status === 401 ? false : count < 1),
    },
  },
});

export function App() {
  const [autenticado, setAutenticado] = React.useState(() => !!tokenStore.get());

  React.useEffect(() => {
    const fn = () => setAutenticado(false);
    onUnauthorized.add(fn);
    return () => { onUnauthorized.delete(fn); };
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        {autenticado
          ? <AutenticadoApp onLogout={() => { tokenStore.clear(); queryClient.clear(); setAutenticado(false); }} />
          : <LoginScreen onAuthenticated={() => setAutenticado(true)} />}
      </ToastProvider>
    </QueryClientProvider>
  );
}

function AutenticadoApp({ onLogout }: { onLogout: () => void }) {
  const [route, setRoute] = React.useState<Route>({ name: 'inicio' });
  const [from, setFrom] = React.useState<Route | undefined>();

  const navigate = (r: Route) => {
    // Al abrir un detalle guardamos la ruta de origen para el "volver" contextual.
    if (r.name === 'inscripcion') setFrom(route);
    setRoute(r);
  };

  return (
    <AppShell route={route} onNavigate={navigate} onLogout={onLogout}>
      {render(route, navigate, from)}
    </AppShell>
  );
}

function render(route: Route, navigate: (r: Route) => void, from?: Route) {
  switch (route.name) {
    case 'inicio':
      return <InicioScreen onNavigate={navigate} />;
    case 'persona':
      return <PersonaScreen id={route.id} onNavigate={navigate} />;
    case 'programas':
      return <ProgramasScreen onNavigate={navigate} />;
    case 'programa':
      return <ProgramaScreen id={route.id} onNavigate={navigate} />;
    case 'personas':
      return <PersonasScreen onNavigate={navigate} />;
    case 'instituciones':
      return <InstitucionesScreen onNavigate={navigate} />;
    case 'contratos':
      return <ContratosScreen abrirContratoId={route.contratoId} onNavigate={navigate} />;
    case 'empresas':
      return <EmpresasScreen onNavigate={navigate} />;
    case 'inscripciones':
      return <InscripcionesScreen rama={route.rama} onNavigate={navigate} />;
    case 'inscripcion':
      return <InscripcionScreen id={route.id} from={from} onNavigate={navigate} />;
    default:
      // Las 11 pantallas están implementadas: este ramal solo cubre una ruta futura.
      return <Pendiente route={route} />;
  }
}

function Pendiente({ route }: { route: Route }) {
  return (
    <div style={{ padding: '58px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10, textAlign: 'center' }}>
      <span style={{ fontSize: 16, fontWeight: 600 }}>Pantalla no implementada todavía</span>
      <span style={{ fontSize: 14, color: 'var(--ga-muted)', maxWidth: '42ch', lineHeight: 1.5 }}>
        Ruta <code style={{ fontFamily: 'var(--ga-font-mono)' }}>{route.name}</code>. La especificación está en
        {' '}<code style={{ fontFamily: 'var(--ga-font-mono)' }}>SCREENS.md</code> y el mockup navegable en
        {' '}<code style={{ fontFamily: 'var(--ga-font-mono)' }}>design/App.dc.html</code>.
      </span>
    </div>
  );
}
