import React from 'react';
import { ApiError } from '../api/client';
import { EmptyState, Button, Modal } from './primitives';
import { useToasts } from './Toasts';

/**
 * Componente único de manejo de error, con una variante visual por código HTTP.
 * Toda la API devuelve el mismo shape ErrorResponse { timestamp, status, error, message }.
 *
 *  400 → inline junto al campo (no pasa por acá: usar <Field error=... />)
 *  401 → redirección a Login (la dispara client.ts vía onUnauthorized)
 *  404 → estado vacío "no encontrado" en la pantalla de detalle
 *  409 → toast de conflicto, persistente, con acción de salida
 *  422 → modal explicativo de regla de negocio
 *  500 → toast genérico con reintento
 */

/** Extrae el mensaje de campo de un 400: "email: formato inválido; nombre: no puede estar vacío". */
export function parseFieldErrors(err: unknown): Record<string, string> {
  if (!(err instanceof ApiError) || err.status !== 400 || !err.payload) return {};
  return err.payload.message.split(';').reduce<Record<string, string>>((acc, part) => {
    const [field, ...rest] = part.split(':');
    if (field && rest.length) acc[field.trim()] = rest.join(':').trim();
    return acc;
  }, {});
}

/** Superficie de error a nivel pantalla: 404 como estado vacío, resto como bloque. */
export function ErrorScreen({ error, onRetry, entidad = 'recurso' }: { error: unknown; onRetry?: () => void; entidad?: string }) {
  const api = error instanceof ApiError ? error : null;

  if (api?.status === 404) {
    return (
      <EmptyState
        tone="danger"
        title={`Ese ${entidad} ya no existe`}
        code={api.payload?.message}
        action={onRetry ? <Button variant="ghost" onClick={onRetry}>Volver al listado</Button> : undefined}
      />
    );
  }

  return (
    <EmptyState
      tone="danger"
      title="No se pudo cargar la información"
      text={api?.status === 500 ? 'Ocurrió un error, intentá de nuevo.' : api?.payload?.message}
      code={api ? `${api.status} · ${api.payload?.error ?? ''}` : undefined}
      action={onRetry ? <Button onClick={onRetry}>Reintentar</Button> : undefined}
    />
  );
}

/** Modal de 422: la regla de negocio necesita explicación y un próximo paso. */
export function BusinessRuleModal({ error, onClose, onGo, goLabel }: {
  error: ApiError; onClose: () => void; onGo?: () => void; goLabel?: string;
}) {
  return (
    <Modal
      kicker={`${error.status} · Regla de negocio`}
      title="La operación no se puede completar"
      onClose={onClose}
      width={460}
      footer={<>
        <Button variant="secondary" onClick={onClose}>Cerrar</Button>
        {onGo && <Button onClick={onGo}>{goLabel ?? 'Ver detalle'}</Button>}
      </>}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <p style={{ margin: 0, fontSize: 14.5, lineHeight: 1.55, color: 'var(--ga-muted)' }}>
          El backend rechazó la request por una regla de negocio. Resolvé lo que falta y volvé a intentar.
        </p>
        <div style={{ background: 'var(--ga-surface-2)', border: '1px solid #EDE5D9', borderRadius: 10, padding: '10px 12px', fontFamily: 'var(--ga-font-mono)', fontSize: 12, color: 'var(--ga-muted)' }}>
          message: "{error.payload?.message}"
        </div>
      </div>
    </Modal>
  );
}

/**
 * Hook central para errores de mutación: enruta el error al tratamiento correcto.
 * Devuelve el 422 para que la pantalla lo muestre como modal, y los 400 como
 * errores de campo; 409 y 500 los resuelve solo con toasts.
 */
export function useApiErrorHandler() {
  const { push } = useToasts();
  const [ruleError, setRuleError] = React.useState<ApiError | null>(null);
  const [fieldErrors, setFieldErrors] = React.useState<Record<string, string>>({});

  const handle = React.useCallback((error: unknown, ctx?: { onFix?: () => void; fixLabel?: string }) => {
    if (!(error instanceof ApiError)) {
      push({ tone: 'err', title: 'Sin conexión con el servidor', text: 'Revisá que el backend esté corriendo en localhost:8080.' });
      return;
    }
    switch (error.status) {
      case 400: setFieldErrors(parseFieldErrors(error)); break;
      case 401: break; // client.ts ya limpió el token y disparó onUnauthorized
      case 409: push({ tone: 'warn', title: `409 · Conflicto`, text: error.payload?.message ?? '', action: ctx?.onFix, actionLabel: ctx?.fixLabel }); break;
      case 422: setRuleError(error); break;
      default:  push({ tone: 'err', title: 'Ocurrió un error', text: 'Intentá de nuevo en unos segundos. (500)' });
    }
  }, [push]);

  return { handle, ruleError, clearRuleError: () => setRuleError(null), fieldErrors, clearFieldErrors: () => setFieldErrors({}) };
}
