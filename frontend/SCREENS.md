# Especificación de pantallas

Referencia visual: `design/App.dc.html` (navegable) y `design/Fundamentos.dc.html`
(paleta, tipografía, componentes, estados). Los valores acá son los mismos de
`tokens/tokens.css`; cuando digo "primario" es `#5D4A87`.

**Layout común**: sidebar 248px (fijo, `#2B2724`) + `<main>` con
`padding: 34px 40px 80px`, `max-width: 1220px`. Todas las pantallas apilan sus bloques con
`display:flex; flex-direction:column; gap:22px`. Título de pantalla: 30px/600/-.025em, con
bajada de 14px en `--ga-muted`.

---

## 1. Login
- Formulario centrado: usuario + contraseña → `POST /api/auth/login` → guardar `token`.
- Sin registro (el admin lo siembra `AdminUserSeeder`).
- Error: 401 con mensaje genérico ("Usuario o contraseña incorrectos", texto que ya devuelve
  `GlobalExceptionHandler`), inline abajo del formulario. No distinguir usuario de contraseña.

## 2. Inicio
- **4 KPI cards** en `grid-template-columns: repeat(4,1fr); gap:16px`. Cada card:
  fondo blanco, borde `--ga-line`, radio 14, padding `18px 20px`; etiqueta mono 10.5px
  uppercase `.12em`, valor 34px/600/-.03em con `tabular-nums`, detalle 13px.
  Hover: `border-color:#C9BFDD`. Click navega a la pantalla relacionada.
  1. **Inscripciones activas** — detalle "N escolares · N particulares"
  2. **Pausadas**
  3. **Contratos activos** — "N de empresa · N individuales"
  4. **Clases del pool** — `consumidas/contratadas` global, detalle "N disponibles en total"
- **Requiere atención** (col. izquierda, `1.15fr`): filas con icono circular de 20px y dos
  líneas. Se arma anticipando los rechazos de la API:
  - pool agotado → "Consumir otra clase devuelve 409" (icono rojo)
  - inscripción particular sin contrato → ámbar
  - inscripción escolar con módulos sin aprobar → "finalizar devuelve 422" (ámbar)
  - vacío: "Nada pendiente / Todos los contratos tienen cupo…"
- **Últimas observaciones** (col. derecha): 4 seguimientos más recientes, persona + fecha
  mono a la derecha + texto 13.5px. Click abre la inscripción.
- Todo se calcula client-side; no requiere endpoints nuevos más allá de los gaps del README.

## 3. Personas (listado)
Implementada en `src/screens/PersonasScreen.tsx`.
- Header con buscador (250px, placeholder "Buscar por nombre o documento…") + botón primario
  "Nueva persona". Filtrado client-side por nombre o documento.
- Tabla: **Nombre** (500) · **Documento** (mono) · **Teléfono** (mono) · **Inscripciones**
  (resumen "2 escolares · 1 particular" o "Sin inscripciones") · celda final "Ver ficha"
  en primario 600.
- Header de tabla: fondo `--ga-surface-2`, mono 11px uppercase `.1em`, color `--ga-muted`.
- Estados: skeleton de 5 líneas / vacío "Todavía no hay personas cargadas" con CTA /
  búsqueda sin resultados: línea centrada 14px "Ninguna persona coincide con la búsqueda."
- **Alta** (modal 520px): Nombre* (span 2), Documento, Teléfono, Email (span 2, hint
  "Opcional. Si viene, la API valida el formato (400 inline)"). `PersonaRequest`.

## 4. Persona (ficha)
- Volver "← Personas" (13px, `--ga-muted`).
- Encabezado: avatar 54px radio 16 con iniciales sobre `--ga-primary-100`/`700`;
  nombre 30px; línea mono 13.5px con documento · teléfono · email.
  Acciones: "Editar datos" (secundario) + "Nueva inscripción" (primario).
- **Inscripciones** (card): filas en
  `grid-template-columns: 14px 1.6fr 1.1fr 130px 110px`, más recientes primero.
  Punto de rama (9px, radio 3) · programa + rama · detalle corto
  (escolar: "Plan B · Miércoles 19–21"; particular: "Contrato Alcor S.A." o
  "Contrato individual") · fecha mono · badge de estado.
  Es la **única pantalla donde conviven las dos ramas**.
- Datos: `GET /api/personas/{id}` + `GET /api/inscripciones?personaId={id}`.

## 5. Inscripciones escolares / particulares (dos ítems de sidebar, misma plantilla)
- Título con punto de rama al lado. Bajadas:
  escolar "CENMA Base y CENMA Sede — avance por módulos y notas.";
  particular "Clases individuales y de empresa — contratos y seguimiento."
- Filtro `<select>` de estado (Todos / Activas / Pausadas / Finalizadas / Canceladas) +
  botón "Nueva inscripción".
- Columnas escolares: Alumno · Programa · **Plan** · **Grupo** (mono) · Inicio · Estado.
- Columnas particulares: Cliente · Programa · **Contrato** (empresa o "Individual") ·
  **Clases** (`18 / 24`, mono) · Inicio · Estado.
  No se mezclan en una tabla universal con columnas condicionales.
- Vacío con filtro aplicado: "Ninguna inscripción con ese estado".
- Datos: ver gaps #1 y #3 del README.

## 6. Inscripción (detalle)
- Volver contextual: "← Inscripciones escolares" o "← Ficha de <nombre>" según de dónde se entró.
- Encabezado: nombre 30px + badge de estado; debajo el programa en 15px `--ga-muted`.
- **Acciones de transición** a la derecha, en este orden: `Pausar`/`Reanudar` (la etiqueta
  cambia con el estado), `Finalizar`, `Cancelar` (variante danger). Habilitación:
  - ACTIVA → Pausar ✓, Finalizar ✓, Cancelar ✓
  - PAUSADA → Reanudar ✓, Finalizar ✗, Cancelar ✓
  - FINALIZADA / CANCELADA → todas ✗ (deshabilitadas, visibles)
- **Meta card**: `repeat(5,1fr)`, etiquetas mono 10.5px uppercase. Escolar: Rama · Plan ·
  Grupo · Inicio · Fin. Particular: Rama · Facturación · Contrato · Inicio · Fin.
- **Si es escolar → Evaluaciones**: una card por módulo.
  Header del módulo: título 15px/600 + chip `APROBADO`/`EN CURSO` + "Promedio 8.25"
  (mono, 2 decimales siempre — el backend usa `BigDecimal`).
  Banda de advertencia ámbar (`--ga-warn-panel`) cuando hay `EVALUACION_FINAL` sin
  `TP_INTEGRADOR` aprobado: "La API no lo bloquea; revisá si es correcto." **No bloqueante.**
  Tabla interna: Tipo (chip mono) · Fecha (mono) · Nota (mono 15px/600; roja si desaprueba
  o si es fuera de secuencia) · Promedia (Sí/No, desde `cuentaParaPromedio`) · Observación
  (usar `recuperaAId` para "Recupera a la del DD/MM").
  Botón "Cargar nota" (secundario) en el header de la sección.
- **Si es particular → dos columnas `360px 1fr`**:
  - **Contrato**: badge de estado, tipo ("Empresa · PAQUETE" / "Individual · POR_CLASE"),
    `PoolBar` (`consumidas / contratadas`, barra roja si está agotado), lista "Cubre a"
    cuando es de empresa, y botones `Consumir clase` (primario) + `Ampliar cupo` (secundario).
    Sin contrato: texto explicativo + CTA "Crear contrato individual".
  - **Seguimiento**: textarea "Nueva observación…" + botón Agregar sobre fondo `--ga-row-alt`;
    debajo, lista cronológica descendente con fecha mono de 88px a la izquierda.
    Vacío: "Sin observaciones todavía".
- 409 al consumir sin cupo → toast persistente con acción "Ampliar cupo".
  422 al finalizar una escolar incompleta → modal con `message` de la API.

## 7. Programas (listado)
- Bajada: "Módulos, planes y grupos viven adentro de cada programa, no en el sidebar."
- Filtro segmentado (Todos / Escolar / Particular): pista `#F0EAE1` radio 9 padding 3,
  opción activa en blanco con `shadow-sm`.
- Tabla: Programa · Categoría (punto + label) · Estrategia (chip mono) · Institución ·
  Inscripciones (conteo).

## 8. Programa (detalle) — pestañas
- Encabezado: nombre + línea con rama · chip de estrategia · institución.
- **Pestañas**: Módulos · Planes · Grupos. `Planes` **solo se renderiza si
  `estrategiaEvaluacion === 'CENMA_SEDE'`**. Estilo: padding `10px 16px`,
  `border-bottom: 2px solid` primario en la activa, `margin-bottom:-1px` sobre la línea.
- Contenido: card con header (título + bajada + botón de alta) y tabla de 3 columnas:
  - **Módulos**: Orden ("Módulo 3") · Secuencia (Secuencial/Independiente) · Regla
    ("Requiere aprobar el anterior").
  - **Planes**: Código ("Plan B") · Módulo de inicio · Efecto ("Exime los módulos anteriores").
  - **Grupos**: Día · Horario · Observación ("Informativo — no afecta el avance" en Sede).
- Vacíos por pestaña: en programas particulares, "Los programas particulares no usan módulos:
  el progreso se registra en Seguimiento."

## 9. Instituciones
- Lista con desplegable (no pantalla propia: son pocas y de uso poco frecuente).
- Fila: avatar 34px `--ga-escolar-bg`/`fg` con iniciales, nombre 15px, sub-línea con los
  programas, y a la derecha "N programas · N inscripciones activas" + chevron.
- Abierta: fondo `--ga-row-alt`, tarjetas de programa en grid
  `1.5fr 1fr 1fr 1fr` (nombre + estrategia mono · N módulos · días de grupo · N activas),
  click va a la ficha del programa.

## 10. Contratos
- Dos botones: "Contrato individual" (secundario) y "Contrato de empresa" (primario).
- Grid de 2 columnas con una card por contrato: título ("Alcor S.A." o
  "Contrato individual #901"), chip de facturación, badge de estado, `PoolBar` y
  "Cubre a <nombres>".
- **Alta individual** (modal 560px): lista de radio con las inscripciones `PARTICULAR`
  **sin contrato previo**, `TipoFacturacion`, `clasesContratadas` (aviso distinto para
  `POR_CLASE`, donde es opcional). `POST /api/contratos`.
- **Alta de empresa** (modal 600px): Empresa + Facturación + Clases del pool + **checklist
  múltiple** de inscripciones elegibles; pie con "N inscripciones sobre el mismo pool".
  Nota fija: "Una inscripción ya cubierta no puede sumarse a otro pool".
  `POST /api/contratos/empresa`. Vacío en la selección → 400 con `inscripcionIds`.

## 11. Empresas
- Fila desplegable: avatar `--ga-particular-bg`, nombre, contacto (mono), "N contratos
  activos", "N empleados cubiertos", chevron.
- Abierta: por cada contrato, card con id + chip de facturación + badge, `PoolBar` a la
  izquierda (220px) y a la derecha los empleados cubiertos (punto ámbar + nombre + programa +
  badge de estado), cada uno navegable a su inscripción.

---

## Nota sobre `categoria` en el cliente
Mientras `InscripcionResponse` no traiga `categoria` (gap #3), la rama se resuelve cruzando
`programaId` contra `GET /api/programas`. **No** inferirla del nombre del programa: en
`PersonasScreen.tsx` hay un `includes('CENMA')` marcado como provisorio justamente para
reemplazarlo por ese cruce (o por el campo nuevo, si se agrega al backend).
