import type { ErrorResponse } from './types';

const BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';
const TOKEN_KEY = 'ga.jwt';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/** Error tipado con el shape uniforme del backend. La UI decide el tratamiento por `status`. */
export class ApiError extends Error {
  status: number;
  payload: ErrorResponse | null;
  constructor(status: number, payload: ErrorResponse | null, fallback = 'Error de red') {
    super(payload?.message ?? fallback);
    this.status = status;
    this.payload = payload;
    this.name = 'ApiError';
  }
}

/** Se dispara en cualquier 401: el shell escucha y manda a Login limpiando el token. */
export const onUnauthorized = new Set<() => void>();

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = tokenStore.get();
  const res = await fetch(BASE + path, {
    ...init,
    headers: {
      ...(init.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init.headers ?? {}),
    },
  });

  if (res.status === 401) {
    tokenStore.clear();
    onUnauthorized.forEach((fn) => fn());
  }

  if (!res.ok) {
    let payload: ErrorResponse | null = null;
    try { payload = (await res.json()) as ErrorResponse; } catch { /* 500 sin body */ }
    throw new ApiError(res.status, payload);
  }

  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

export const api = {
  get:   <T>(p: string) => request<T>(p),
  post:  <T>(p: string, body?: unknown) => request<T>(p, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }),
  put:   <T>(p: string, body: unknown) => request<T>(p, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <T>(p: string) => request<T>(p, { method: 'PATCH' }),
  del:   (p: string) => request<void>(p, { method: 'DELETE' }),
};
