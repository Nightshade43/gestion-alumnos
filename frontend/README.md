# Handoff: Gestión de Alumnos — Frontend

## Overview
Frontend de escritorio para **Gestión de Alumnos**, herramienta interna de un solo usuario
(docente/consultor) sobre el backend Java 21 + Spring Boot 4.1.1 del repo
`Nightshade43/gestion-alumnos` (branch `master`).

Dominio: **Persona → Inscripción → Programa**, con dos ramas separadas visualmente:
**Escolar** (CENMA Base / CENMA Sede: instituciones, programas, módulos, planes, grupos,
evaluaciones con notas) y **Particular** (clases individuales y de empresa: contratos con
pool de clases compartido, empresas, seguimiento en texto libre).

Destino acordado: **subcarpeta `/frontend` dentro del repo del backend**, Vite + React + TS,
datos con **TanStack Query**. CORS lo configura el usuario en el backend (hoy no está puesto).

## About the Design Files
Los archivos `.dc.html` de `design/` son **referencias de diseño hechas en HTML**:
prototipos que muestran el look y el comportamiento buscado, **no código para copiar**.
La tarea es **recrear esos diseños en React + TypeScript** dentro de `/frontend`, usando
los tokens y las primitivas que vienen en este paquete (`tokens/`, `src/`), que sí son
código de arranque real y están escritos contra los DTOs verdaderos del backend.

## Fidelity
**Alta fidelidad (hifi).** Colores, tipografía, espaciado, radios, sombras y estados están
definidos y deben respetarse. Los valores exactos están en `tokens/tokens.css`
(variables CSS) y `tokens/tokens.ts` (espejo tipado).

---

## ⚠️ Gaps del backend detectados al leer el repo
Estos tres puntos afectan directamente lo que la UI puede pedir. Ninguno es un bug: son
endpoints que todavía no existen. Recomendación: **agregarlos en el backend**; mientras no
estén, el paquete incluye el workaround.

| # | Gap | Impacto en la UI | Workaround incluido |
|---|---|---|---|
| 1 | `GET /api/inscripciones` exige `personaId` (`@RequestParam Long personaId`, no opcional). No hay listado global. | Los listados "Inscripciones escolares" / "Inscripciones particulares" y los KPIs de Inicio necesitan todas las inscripciones. | `useTodasLasInscripciones()` en `src/api/queries.ts`: fan-out `GET /api/personas` + N requests. Aceptable con el volumen actual; borrar cuando exista el listado. |
| 2 | `ContratoController` solo expone `GET /api/contratos/{id}`. No hay listado. | La pantalla **Contratos** y el KPI "Contratos activos" no tienen de dónde leer. | Ninguno real: hoy solo se puede llegar a un contrato desde una inscripción cuyo id se conozca. **Sugerido: `GET /api/contratos` (y opcional `?empresaId=`).** |
| 3 | `InscripcionResponse` no trae `categoria` ni `contratoId`. | Separar por rama y saber si una inscripción particular ya tiene contrato requiere cruzar con `GET /api/programas` en el cliente. | Cruce client-side por `programaId` (los programas sí traen `categoria`). **Sugerido: sumar `categoria` y `contratoId` al response** — evita el join en el front. |

Complemento ya conocido: la API **no valida secuencia académica** al cargar notas
(se puede cargar `EVALUACION_FINAL` sin `TP_INTEGRADOR` aprobado). La UI muestra una
**advertencia ámbar no bloqueante**, no lo impide.

---

## Screens / Views
La especificación pantalla por pantalla (layout, componentes, copy exacto, endpoints que
consume cada una y estados) está en **[SCREENS.md](./SCREENS.md)**.
Mapa corto:

| Pantalla | Propósito | Endpoints |
|---|---|---|
| Login | Autenticación única | `POST /api/auth/login` |
| Inicio | KPIs + "Requiere atención" + últimas observaciones | personas, programas, inscripciones (fan-out), contratos |
| Personas (listado) | Buscar y crear personas | `GET/POST /api/personas` |
| Persona (ficha) | Datos + inscripciones de ambas ramas | `GET /api/personas/{id}`, `GET /api/inscripciones?personaId=` |
| Inscripciones escolares | Listado con Plan/Grupo, filtro por estado | ver gap #1 |
| Inscripciones particulares | Listado con contrato y clases | ver gaps #1 y #3 |
| Inscripción (detalle) | Máquina de estados + evaluaciones o seguimiento/contrato | transiciones, instancias-evaluativas, seguimientos, contratos |
| Programas (listado) | Filtro por categoría | `GET /api/programas` |
| Programa (detalle) | Pestañas Módulos · Planes · Grupos | `GET /api/modulos|planes|grupos?programaId=` |
| Instituciones | Listado con desplegable de sus programas | `GET /api/instituciones`, `GET /api/programas` |
| Contratos | Pool de clases, alta individual y de empresa | `POST /api/contratos`, `POST /api/contratos/empresa` (ver gap #2) |
| Empresas | Listado con desplegable de contratos y empleados | `GET /api/empresas` |

## Interactions & Behavior
- **Navegación**: sidebar fijo de 7 ítems, dos grupos (Escolar / Particular) con "Personas"
  transversal arriba. Módulos/Planes/Grupos son **pestañas de Programa**;
  Evaluaciones/Seguimiento viven en el **detalle de la Inscripción**. Sin responsive.
- **Máquina de estados de Inscripción** (fuente: `InscripcionService`): `ACTIVA ⇄ PAUSADA`,
  `ACTIVA/PAUSADA → CANCELADA`, `ACTIVA → FINALIZADA`. Los botones inválidos se muestran
  **deshabilitados, no ocultos**. Cualquier otra combinación devuelve 409.
- **Filas de tabla**: hover `var(--ga-primary-50)`, cursor pointer, click abre el detalle.
- **Desplegables** (Instituciones / Empresas): chevron rota 180° con `transition .16s`.
- **Toasts**: entran con `ga-toast-in` (.18s ease-out), abajo a la derecha. Los `ok`
  se autodescartan a los 4s; los 409 quedan hasta que el usuario los cierre.
- **Skeleton**: `ga-shimmer` 1.4s linear infinite en listados; spinner solo en acciones puntuales.

## State Management
- **Servidor**: TanStack Query. Claves y hooks en `src/api/queries.ts`; toda transición de
  estado hace `setQueryData` de la inscripción + `invalidateQueries` del listado de su persona.
- **Sesión**: JWT en `localStorage` (`ga.jwt`) vía `tokenStore`. `client.ts` agrega el header
  `Authorization: Bearer` en cada request y, ante cualquier 401, limpia el token y dispara
  `onUnauthorized` — `AppShell` escucha y manda a Login.
- **UI local**: ruta activa, filtro de estado, pestaña de Programa, id abierto en los
  desplegables, texto del buscador, modal abierto.

## Error handling — un componente, seis tratamientos
Todo error de la API tiene el mismo shape (`web/ErrorResponse.java`):
`{ timestamp, status, error, message }`. `src/components/ErrorSurface.tsx` lo enruta:

| Código | Origen en el backend | Tratamiento |
|---|---|---|
| 400 | `MethodArgumentNotValidException` → `"campo: mensaje; campo: mensaje"` | Inline en el campo (`parseFieldErrors`), el form no se cierra |
| 401 | `JwtAuthenticationEntryPoint` / login inválido | Limpia token + redirección a Login |
| 404 | `RecursoNoEncontradoException` | Estado vacío "no encontrado" en la pantalla de detalle |
| 409 | `RegistroDuplicado`, `EstadoInvalido`, `LimiteClasesExcedido`, `DataIntegrityViolation` | Toast persistente con acción de salida ("Ampliar cupo") |
| 422 | `RequisitosAcademicosIncompletos`, `CategoriaInvalida` | Modal explicativo con próximo paso |
| 500 | `handleGeneral` | Toast genérico "Ocurrió un error, intentá de nuevo" |

## Design Tokens
Valores completos en `tokens/tokens.css`. Resumen:
- **Primario (Ciruela)**: `#5D4A87` (600), `#4A3A6E` (700 hover), `#E6E0F1` (100), `#F4F1FA` (50).
- **Neutros cálidos**: canvas `#FAF7F2`, superficie `#FFFFFF`, líneas `#E6DED2` / `#F0EAE1` / `#D6CBBB`, tinta `#2B2724`, apagado `#6F675E` / `#948B80`.
- **Sidebar**: `#2B2724`, hover `#3A3531`, texto `#D3CBC1`, etiquetas `#7C736A`.
- **Estados**: ACTIVA `#2F7A4E`/`#E2F1E7`, PAUSADA `#8F6414`/`#FBEFD8`, FINALIZADA `#3F6183`/`#E4EDF4`, CANCELADA `#9E3A38`/`#F8E3E1`.
- **Rama**: Escolar `#3E6C8A`, Particular `#A8762B` (nunca son el primario).
- **Tipografía**: UI **Instrument Sans**; datos/números/ids/fechas **IBM Plex Mono** con `tabular-nums`. Escala 30 / 22 / 16 / 15 / 14.5 / 13 / 11px.
- **Radios**: 6 / 9 / 12 / 14 / 16 / 999. **Sombras**: sm `0 1px 2px rgba(43,39,36,.12)`, md `0 6px 18px rgba(43,39,36,.07)`, lg `0 22px 54px rgba(43,39,36,.28)`. **Focus**: `0 0 0 3px #E6E0F1`.
- **Layout**: sidebar 248px, contenido máx. 1220px, padding `34px 40px 80px`.

## Assets
Ninguno externo. Solo fuentes de Google Fonts:
`Instrument+Sans:wght@400..700` e `IBM+Plex+Mono:wght@400;500;600`.
Los avatares son iniciales sobre tinte, no imágenes. No hay iconos de librería:
los pocos glifos usados son texto (`✓`, `!`, `×`, `⌄`, `←`).

## Files
```
design_handoff_gestion_alumnos/
├─ README.md                      ← este archivo
├─ SCREENS.md                     ← especificación pantalla por pantalla
├─ PROMPT.md                      ← prompt inicial para Claude Code
├─ tokens/tokens.css              ← variables CSS + keyframes (importar una vez)
├─ tokens/tokens.ts               ← espejo tipado
├─ src/api/types.ts               ← tipos 1:1 con los records de web/dto
├─ src/api/client.ts              ← fetch + JWT + interceptor 401 + ApiError
├─ src/api/endpoints.ts           ← superficie completa de la API
├─ src/api/queries.ts             ← hooks TanStack Query + workaround del gap #1
├─ src/components/primitives.tsx  ← Button, Field/Input/Select, badges, Card, DataTable,
│                                    Skeleton, EmptyState, Modal, PoolBar
├─ src/components/Toasts.tsx      ← ToastProvider + useToasts
├─ src/components/ErrorSurface.tsx← componente único de error (400→500) + useApiErrorHandler
├─ src/shell/AppShell.tsx         ← sidebar + layout de escritorio + ScreenHeader
├─ src/screens/PersonasScreen.tsx ← pantalla de referencia completa
└─ design/                        ← mockups HTML (referencia visual, no código)
   ├─ Fundamentos.dc.html         ← paleta, tipografía, componentes, nav, estados
   └─ App.dc.html                 ← todas las pantallas navegables
```

Abrir los dos `.dc.html` en el navegador: son la referencia visual definitiva.
En `App.dc.html` se puede navegar todo el flujo real (transiciones de estado, 409 de cupo,
modal 422, altas de contrato).
