import React from 'react';
import { useLogin } from '../api/queries';
import { ApiError } from '../api/client';
import { Button, Field, Input } from '../components/primitives';

/**
 * Autenticación única. No hay registro ni recupero: las cuentas las siembra
 * AdminUserSeeder en el backend.
 *
 * Un 401 se muestra inline y NO distingue si falló el usuario o la contraseña
 * (es el mensaje genérico que ya devuelve GlobalExceptionHandler).
 */
export function LoginScreen({ onAuthenticated }: { onAuthenticated: () => void }) {
  const login = useLogin();
  const [username, setUsername] = React.useState('');
  const [password, setPassword] = React.useState('');
  const [error, setError] = React.useState<string | null>(null);

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) { setError('generic'); return; }
    setError(null);
    login.mutate({ username, password }, {
      onSuccess: () => onAuthenticated(),   // el token ya quedó en tokenStore
      onError: (err) => setError(err instanceof ApiError && err.status >= 500 ? 'server' : 'generic'),
    });
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 20px', boxSizing: 'border-box' }}>
      <div style={{ width: '100%', maxWidth: 376, display: 'flex', flexDirection: 'column', gap: 22 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
          <div style={{ width: 34, height: 34, borderRadius: 10, background: 'var(--ga-primary-600)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 16, fontWeight: 700 }}>G</div>
          <span style={{ fontSize: 17, fontWeight: 600, letterSpacing: '-.01em' }}>Gestión de Alumnos</span>
        </div>

        <form
          onSubmit={submit}
          style={{ background: 'var(--ga-surface)', border: '1px solid var(--ga-line)', borderRadius: 'var(--ga-radius-xl)', padding: 28, display: 'flex', flexDirection: 'column', gap: 18, boxShadow: 'var(--ga-shadow-sm)' }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <h1 style={{ margin: 0, fontSize: 21, fontWeight: 600, letterSpacing: '-.02em' }}>Ingresar</h1>
            <span style={{ fontSize: 13.5, color: 'var(--ga-muted)', lineHeight: 1.5 }}>
              Acceso único de administración. Las cuentas las siembra el sistema.
            </span>
          </div>

          <Field label="Usuario">
            <Input
              value={username}
              onChange={(e) => { setUsername(e.target.value); setError(null); }}
              autoComplete="username"
              placeholder="admin"
            />
          </Field>
          <Field label="Contraseña">
            <Input
              type="password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setError(null); }}
              autoComplete="current-password"
              placeholder="••••••••"
            />
          </Field>

          {error && (
            <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start', background: 'var(--ga-danger-bg)', border: '1px solid var(--ga-danger-line)', borderRadius: 10, padding: '11px 13px' }}>
              <span style={{ width: 18, height: 18, borderRadius: 999, flex: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, background: '#F2D2CF', color: 'var(--ga-danger-fg)', marginTop: 1 }}>×</span>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <span style={{ fontSize: 13.5, fontWeight: 600, color: 'var(--ga-danger-fg)' }}>
                  {error === 'server' ? 'No se pudo conectar' : 'Credenciales inválidas'}
                </span>
                <span style={{ fontSize: 12.5, color: '#8A4744', lineHeight: 1.45 }}>
                  {error === 'server'
                    ? 'Revisá que el backend esté corriendo en localhost:8080.'
                    : '401 · Revisá usuario y contraseña. No distinguimos cuál de los dos falló.'}
                </span>
              </div>
            </div>
          )}

          <Button type="submit" disabled={login.isPending} style={{ padding: '12px 15px', fontSize: 14.5 }}>
            {login.isPending ? 'Ingresando…' : 'Ingresar'}
          </Button>
        </form>

        <span style={{ fontSize: 12.5, color: 'var(--ga-soft)', textAlign: 'center', lineHeight: 1.5 }}>
          El token se guarda en <code style={{ fontFamily: 'var(--ga-font-mono)' }}>localStorage</code>; un 401 en cualquier request vuelve a esta pantalla.
        </span>
      </div>
    </div>
  );
}
