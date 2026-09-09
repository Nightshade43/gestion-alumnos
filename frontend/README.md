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

## Estado de la API

**Los 3 gaps de la primera revisión están resueltos en `master`** (detalle abajo). De la
implementación de las 11 pantallas salieron **8 pendientes nuevos**, ninguno bloqueante:
están especificados uno por uno, con contrato propuesto y criterio de aceptación, en
**[BACKEND.md](./BACKEND.md)**. El frontend funciona sin ellos; lo que cambian es cantidad
de requests, exactitud de dos avisos de Inicio y una deuda de modelo (el gate de Sede
guardado como nota 10/0).

### Gaps de la primera revisión — resueltos
Los tres gaps de la primera lectura ya están cerrados en `master`. El paquete quedó
sincronizado: **no hay más workarounds client-side**.

| # | Gap original | Cómo quedó | Efecto en el paquete |
|---|---|---|---|
| 1 | `GET /api/inscripciones` exigía `personaId` | `listar(personaId?, categoria?)`, ambos opcionales y combinables | `useTodasLasInscripciones()` **eliminado**. Ahora `useInscripciones({ categoria })`: una sola request por pantalla, el filtro de rama lo hace el backend. |
| 2 | `ContratoController` sin listado | `GET /api/contratos` devuelve todos con sus `inscripciones` | `useContratos()`. La pantalla Contratos y el KPI "Contratos activos" ya tienen origen de datos. |
| 3 | `InscripcionResponse` sin `categoria`/`contratoId` | ambos campos agregados (`categoria` del programa, `contratoId` null si no tiene) | Se fue el cruce con `/api/programas` y el `includes('CENMA')` provisorio de `PersonasScreen`. "Sin contrato" ahora es `contratoId === null`. |

Sobre el orden de parámetros del record: `types.ts` mapea **por nombre de campo**
(interfaces TS sobre JSON), así que la nueva posición de `categoria` (6º) y `contratoId`
(último) en el constructor canónico no afecta al front. Nada en el paquete depende del
orden — sí conviene revisarlo en los tests de backend que construyan el record posicionalmente.

Verificado contra `master` el 2026-09-07: `InscripcionController` expone un único
`@GetMapping` (`listar(personaId?, categoria?)`) y `ContratoController` tiene los seis
mappings completos (`GET /`, `GET /{id}`, `POST /`, `POST /empresa`, `consumir-clase`,
`ampliar-cupo`, `finalizar`).
Complemento ya conocido: la API **no valida secuencia académica** al cargar notas
(se puede cargar `EVALUACION_FINAL` sin `TP_INTEGRADOR` aprobado). La UI muestra una
**advertencia ámbar no bloqueante**, no lo impide.

---

## Screens / Views
La especificación pantalla por pantalla (layout, componentes, copy exacto, endpoints que
consume cada una y estados) está en **[SCREENS.md](./SCREENS.md)**.
Mapa corto:

Estado del código de referencia: **11 de 11 pantallas escritas y ruteadas** en `App.tsx`,
más el guard de sesión, los 6 modales, las confirmaciones destructivas, los vacíos y el
error de red. No queda ninguna pantalla en placeholder.

| Pantalla | Propósito | Endpoints |
|---|---|---|
| Login | Autenticación única | `POST /api/auth/login` |
| Inicio | KPIs + "Requiere atención" + últimas observaciones | `GET /api/personas|programas|inscripciones|contratos` |
| Personas (listado) | Buscar y crear personas | `GET/POST /api/personas` |
| Persona (ficha) | Datos + inscripciones de ambas ramas | `GET /api/personas/{id}`, `GET /api/inscripciones?personaId=` |
| Inscripciones escolares | Listado con Plan/Grupo, filtro por estado | `GET /api/inscripciones?categoria=ESCOLAR` |
| Inscripciones particulares | Listado con contrato y clases | `GET /api/inscripciones?categoria=PARTICULAR` |
| Inscripción (detalle) | Máquina de estados + evaluaciones o seguimiento/contrato | transiciones, instancias-evaluativas, seguimientos, contratos |
| Programas (listado) | Filtro por categoría | `GET /api/programas` |
| Programa (detalle) | Pestañas Módulos · Planes · Grupos | `GET /api/modulos|planes|grupos?programaId=` |
| Instituciones | Listado con desplegable de sus programas | `GET /api/instituciones`, `GET /api/programas` |
| Contratos | Pool de clases, alta individual y de empresa | `GET/POST /api/contratos`, `POST /api/contratos/empresa` |
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
├─ SCREENS.md                     ← especificación pantalla por pantalla (+ huecos de API)
├─ BACKEND.md                     ← los 8 pendientes de backend, con contrato y aceptación
├─ PROMPT.md                      ← prompt inicial para Claude Code
├─ tokens/tokens.css              ← variables CSS + keyframes (importar una vez)
├─ tokens/tokens.ts               ← espejo tipado
├─ src/api/types.ts               ← tipos 1:1 con los records de web/dto
├─ src/api/client.ts              ← fetch + JWT + interceptor 401 + ApiError
├─ src/api/endpoints.ts           ← superficie completa de la API
├─ src/api/queries.ts             ← hooks TanStack Query (sin workarounds)
├─ src/components/primitives.tsx  ← Button, Field/Input/Select, badges, Card, DataTable,
│                                    Skeleton, EmptyState, Modal, PoolBar
├─ src/components/Toasts.tsx      ← ToastProvider + useToasts
├─ src/components/ErrorSurface.tsx← componente único de error (400→500) + useApiErrorHandler
├─ src/App.tsx                    ← guard de sesión + router mínimo (Route union)
├─ src/shell/AppShell.tsx         ← sidebar + layout de escritorio + ScreenHeader
├─ src/screens/LoginScreen.tsx        ← auth + 401 inline
├─ src/screens/InicioScreen.tsx       ← 4 KPIs + requiere atención + últimas observaciones
├─ src/screens/PersonasScreen.tsx     ← listado + alta
├─ src/screens/PersonaScreen.tsx      ← ficha: datos + inscripciones de las dos ramas + edición
├─ src/screens/InscripcionesScreen.tsx← listado por rama + alta (reusada desde la ficha)
├─ src/screens/InscripcionScreen.tsx  ← detalle: transiciones, evaluaciones, contrato, seguimiento
├─ src/screens/ProgramasScreen.tsx    ← índice + filtro segmentado + alta con estrategia
├─ src/screens/ProgramaScreen.tsx     ← pestañas Módulos · Planes (solo Sede) · Grupos
├─ src/screens/InstitucionesScreen.tsx← lista desplegable con sus programas
├─ src/screens/ContratosScreen.tsx    ← grilla de pools + detalle 580px + altas individual/empresa
├─ src/screens/EmpresasScreen.tsx     ← lista desplegable con contratos y empleados cubiertos
└─ design/                        ← mockups HTML (referencia visual, no código)
   ├─ Fundamentos.dc.html         ← paleta, tipografía, componentes, nav, estados
   ├─ App.dc.html                 ← todas las pantallas navegables
   └─ screens/                    ← 16 capturas PNG + índice (guía visual rápida)
```

Abrir los dos `.dc.html` en el navegador: son la referencia visual definitiva.
En `App.dc.html` se puede navegar todo el flujo real (transiciones de estado, 409 de cupo,
modal 422, altas de contrato).
