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
Implementada en `src/screens/LoginScreen.tsx` (guard en `src/App.tsx`). Dibujada en `App.dc.html` (el "Salir" del sidebar vuelve acá).
- Columna centrada de 376px sobre `--ga-bg`, sin sidebar: marca arriba (cuadro 34px radio 10
  `--ga-primary-600` con "G" + nombre 17px/600), card blanca borde `--ga-line` radio 16
  padding 28px, y pie 12.5px `--ga-subtle` centrado ("El token se guarda en memoria; un 401
  en cualquier request vuelve a esta pantalla").
- Card: título "Ingresar" 21px/600 + bajada 13.5px "Acceso único de administración. Las
  cuentas las siembra el sistema."; dos `Field` (Usuario, Contraseña `type=password`) con
  `:focus` = borde `--ga-primary-600` + halo `0 0 0 3px rgba(93,74,135,.12)`; botón primario
  full-width 12px de alto.
- Sin registro ni "olvidé mi contraseña" (el admin lo siembra `AdminUserSeeder`).
- Error 401: bloque rojo dentro de la card, arriba del botón — icono circular 18px,
  "Credenciales inválidas" 13.5px/600 y "401 · Revisá usuario y contraseña. No distinguimos
  cuál de los dos falló." No distinguir usuario de contraseña.
- `POST /api/auth/login` → guardar `token`. Un 401 en cualquier request posterior
  (interceptor de `client.ts`) devuelve a esta pantalla.

## 2. Inicio
Implementada en `src/screens/InicioScreen.tsx`.
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
- Los KPIs se calculan client-side sobre `GET /api/inscripciones` + `GET /api/contratos`
  (ambos listados globales ya existen); una request por recurso, sin fan-out.

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
Implementada en `src/screens/PersonaScreen.tsx`.
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
Implementada en `src/screens/InscripcionesScreen.tsx` — un componente con `rama` como prop.
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
- Datos: `GET /api/inscripciones?categoria=ESCOLAR|PARTICULAR` — el backend filtra la rama.
  El estado se filtra en cliente sobre ese resultado. "Sin contrato" = `contratoId === null`.

## 6. Inscripción (detalle)
Implementada en `src/screens/InscripcionScreen.tsx`.
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
Implementada en `src/screens/ProgramasScreen.tsx`.
- Bajada: "Módulos, planes y grupos viven adentro de cada programa, no en el sidebar."
- Filtro segmentado (Todos / Escolar / Particular): pista `#F0EAE1` radio 9 padding 3,
  opción activa en blanco con `shadow-sm`.
- Tabla: Programa · Categoría (punto + label) · Estrategia (chip mono) · Institución ·
  Inscripciones (conteo).

## 8. Programa (detalle) — pestañas
Implementada en `src/screens/ProgramaScreen.tsx`.
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
Implementada en `src/screens/InstitucionesScreen.tsx`.
- Lista con desplegable (no pantalla propia: son pocas y de uso poco frecuente).
- Fila: avatar 34px `--ga-escolar-bg`/`fg` con iniciales, nombre 15px, sub-línea con los
  programas, y a la derecha "N programas · N inscripciones activas" + chevron.
- Abierta: fondo `--ga-row-alt`, tarjetas de programa en grid
  `1.5fr 1fr 1fr 1fr` (nombre + estrategia mono · N módulos · días de grupo · N activas),
  click va a la ficha del programa.

## 10. Contratos
Implementada en `src/screens/ContratosScreen.tsx`.
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
- **Detalle** (modal 580px al hacer click en la card; `GET /api/contratos/{id}`): cabecera con
  `#id` mono + badge de estado + × de cierre, título (empresa o "Contrato individual") con
  chip de facturación, y bajada según sea de empresa ("Pool compartido: cualquier empleado
  cubierto consume del mismo total") o individual ("Cubre una única inscripción particular").
  Después: bloque de pool (`PoolBar` + total mono 19px + línea de estado), bloque
  "Empleados cubiertos" / "Inscripción cubierta" con filas navegables a la inscripción, y pie
  con tres acciones — **Consumir clase** (`POST /{id}/consumir-clase`), **Ampliar cupo +4**
  (`POST /{id}/ampliar-cupo`) y **Finalizar contrato** (`POST /{id}/finalizar`, estilo
  destructivo). Deshabilitadas según estado: consumir cae con pool `PAQUETE` agotado o
  contrato `FINALIZADO`; ampliar y finalizar caen solo con `FINALIZADO`.
- La card de la grilla es clickeable (hover: borde `#CDBFE5` + sombra suave); "Pool agotado"
  en Inicio abre directamente este modal.

## 11. Empresas
Implementada en `src/screens/EmpresasScreen.tsx`.
- Fila desplegable: avatar `--ga-particular-bg`, nombre, contacto (mono), "N contratos
  activos", "N empleados cubiertos", chevron.
- Abierta: por cada contrato, card con id + chip de facturación + badge, `PoolBar` a la
  izquierda (220px) y a la derecha los empleados cubiertos (punto ámbar + nombre + programa +
  badge de estado), cada uno navegable a su inscripción.

---

## Nota sobre el tope del pool
El backend **solo topea el pool cuando `tipoFacturacion === 'PAQUETE'`**
(`ContratoService.consumirClase`): con `POR_CLASE` el total es referencial y consumir por
encima de `clasesContratadas` no da 409. La UI refleja eso: barra roja y "Consumir clase"
deshabilitado solo en `PAQUETE` agotado; en `POR_CLASE` la barra queda en el color normal y
la línea dice "sin tope en el backend". El único 409 restante es el de contrato
`FINALIZADO`, que aplica a los dos tipos.

## Nota sobre `categoria` en el cliente
`InscripcionResponse` **ya trae `categoria`** (y `contratoId`): la rama se lee del campo,
sin cruzar con `/api/programas` y sin inferirla del nombre del programa. El
`includes('CENMA')` provisorio de `PersonasScreen.tsx` fue reemplazado por
`i.categoria === 'ESCOLAR'`.


---

# Piezas agregadas (cierre de huecos de diseño)

Todas dibujadas en `design/App.dc.html`. Los seis modales anteriores no cambiaron.

## 12. Modal "Cargar nota" (520px)
Se abre desde el botón del header de **Evaluaciones**, en el detalle de una inscripción escolar.
- Bajada: "Queda asociada a la inscripción y al módulo. El promedio del módulo se recalcula
  sobre las instancias que promedian."
- Cuatro campos: **Módulo*** (select con los módulos de esa inscripción) · **Tipo***
  (`NOTA` / `TP_INTEGRADOR` / `EVALUACION_FINAL`, cada opción con su glosa) ·
  **Fecha*** (mono) · **Nota*** (mono, placeholder "0.00 – 10.00").
- Con `TP_INTEGRADOR` el cuarto campo **cambia de control**: pasa a select
  `Aprobado` / `No aprobado` y la etiqueta a "Resultado". Es el único tipo que no promedia.
- Panel violeta, tres variantes: gate ("El TP integrador no promedia: es el gate que
  habilita el módulo siguiente…"), nota común ("…menos de 6 se muestra en rojo y deja el
  módulo sin aprobar") y **programa sin módulos** ("…cargalos desde el programa antes de
  evaluar").
- Al guardar: recalcula promedio (2 decimales) y `aprobado` en cliente
  (promedio ≥ 6 **y** integradores aprobados) y toast `201`. Sin nota → 400 inline por toast.

## 13. Modal "Editar datos" de persona (520px)
Desde el botón secundario de la ficha. Mismos cuatro campos que el alta, precargados.
Bajada: "Reemplaza el registro completo de la persona. No toca sus inscripciones ni su
historial." Guardar propaga el nombre a las inscripciones y al seguimiento (200).

## 14. Modal de alta genérico (540px)
Un solo shell para **programa, programa (edición), institución, empresa, módulo, plan y
grupo**: header con título + bajada, grid de 2 columnas de campos declarados por descriptor
(`label`, `placeholder`, `hint`, select vs input, `span 2`, mono), panel violeta con la regla
de negocio y footer Cancelar / botón primario.
- **Programa**: Nombre (span) · Categoría · Estrategia · Institución (hint "Obligatoria en la
  rama escolar, vacía en particular"). Aviso: qué habilita cada estrategia.
- **Editar programa**: la Estrategia aparece **no editable** ("Fija desde el alta"); el aviso
  explica que cambiarla rompería el historial y que la salida es crear otro programa.
- **Institución**: solo Nombre. **Empresa**: Nombre + Contacto, con el aviso de que crear la
  empresa no crea contrato.
- **Módulo**: Orden (mono) + Secuencia. **Plan**: Código + Módulo de inicio (solo
  `CENMA_SEDE`). **Grupo**: Día + Horario, "no afecta el avance".
- El botón de alta de cada pestaña del programa abre la variante correspondiente a la pestaña
  activa.

## 15. Modal de confirmación destructiva (470px, z-index 80)
Se interpone en **Cancelar inscripción** y **Finalizar contrato** (las dos únicas acciones sin
vuelta atrás). Kicker mono rojo "Acción irreversible", título, párrafo con la consecuencia
real, bloque mono con el endpoint, y footer **Volver** / botón sólido `#9E3A38`.
Sale por encima del modal de detalle de contrato. El resto de las transiciones
(Pausar, Reanudar, Finalizar inscripción) siguen siendo directas.

## 16. Vacíos del resto de pantallas
Card blanca centrada (cuadro 44px con borde punteado, título 16px, texto 14px de máx 44ch,
CTA primario), con copy propio en **Programas** ("El programa define la estrategia de
evaluación…"), **Instituciones**, **Contratos** ("Sin contrato, una inscripción particular no
puede registrar clases consumidas") y **Empresas**. Se ven con el tweak
`Estados de demo → vacio`, que ahora vacía los listados de esas cuatro pantallas.

## 17. Error de red / 500
Tweak `Estados de demo → error`. Reemplaza el cuerpo de **cualquier** pantalla (el sidebar
sigue navegable) por el título de la pantalla + card de borde `#EDC9C6`: icono circular rojo,
"No pudimos cargar los datos", explicación de que **no es un 401** y por eso no volvés a
Login, bloque mono con la request que falló (una por pantalla) y dos botones:
**Reintentar** (primario) y **Ir a Inicio**.

## 18. Ancho angosto (notebook)
Sin media queries: el layout se adapta por grid intrínseco.
- `main`: `padding: 34px clamp(18px,3.2vw,40px) 80px`, `min-width:0`; la grilla raíz es
  `248px minmax(0,1fr)`.
- KPIs `repeat(auto-fit,minmax(196px,1fr))`; Inicio y el detalle particular
  `repeat(auto-fit,minmax(330px,1fr))` y `minmax(320px,1fr)` — a un ancho chico las dos
  columnas se apilan. Meta card `minmax(132px,1fr)`; grilla de contratos `minmax(330px,1fr)`.
- Las tablas tienen `min-width` (700px las de listado, 620px las de evaluaciones) y su card
  pasó a `overflow-x:auto`: scrollean en lugar de comprimir columnas.
- Los headers de pantalla llevan `flex-wrap:wrap`, así el buscador y los botones bajan de
  línea antes de aplastar el título.


---

# Reglas diferenciadas Sede / Base

`CENMA_SEDE` y `CENMA_BASE` son dos líneas académicas distintas, no dos variantes de la
misma. La UI las separa en el alta y en la carga de notas. La rama se lee del programa
(`estrategiaEvaluacion`), no del nombre.

## 19. Alta de inscripción
| | Sede | Base | Particular |
|---|---|---|---|
| Plan | **Sí** — A/B/C, campo obligatorio, hint "Exime los módulos anteriores al de inicio" | **No se renderiza** | No |
| Grupo | 2 opciones (miércoles / jueves) | **1 opción** (lunes 19–21), hint "Único grupo del ciclo lectivo" | No |
| Extra | — | Panel neutro: "Base no usa planes… `planId` se manda vacío" | — |

El select de programa muestra la estrategia en cada opción y, debajo, una línea que resume la
línea elegida (9 módulos secuenciales / 2 módulos por ciclo / seguimiento libre). El aviso
violeta cambia por estrategia y en las dos escolares recuerda el 409 por inscripción escolar
activa o pausada duplicada.

## 20. Carga de nota
El modal cambia de tipos según la línea; no hay un select universal de 4 tipos.
- **Sede** — `TP_INTEGRADOR` (gate, control Aprobado / No aprobado, no promedia) y
  `EVALUACION_FINAL` (nota numérica). Aviso: "la final ES la nota del módulo, no se promedia
  con el TP" y que la API no valida la secuencia.
- **Base** — `NOTA` (promedia) e `INTEGRADOR` (interdisciplinario,
  `cuentaParaPromedio = false`). Bajo el select, contador vivo: "Este módulo tiene 2 de las 3
  notas mínimas (el integrador no cuenta)".
- **Recupera a** (solo Base, solo tipo `NOTA`, solo si ya hay notas): select con las notas
  vigentes del módulo. Al guardar, la original pasa a *no promedia* con la observación
  "Recuperada por la del DD/MM" y la nueva queda como "Recupera a la del DD/MM" — el par
  sigue visible en la tabla.

## 21. Cómo se cierra el módulo
- **Sede**: el header dice **"Nota del módulo"** (no "Promedio") y muestra la última
  `EVALUACION_FINAL`. Aprobado = final ≥ 6 **y** TP integrador aprobado.
- **Base**: el header dice **"Promedio"** (2 decimales) sobre las notas que promedian, más un
  chip con el conteo de notas mínimas (ámbar mientras falten, neutro al cumplirse).
  Aprobado = promedio ≥ 6 **y** 3 notas cargadas **y** integrador aprobado.
- La bajada de la sección Evaluaciones y la meta card también cambian: Sede muestra **Plan**,
  Base muestra **Estrategia: CENMA_BASE** en ese lugar (no tiene plan que mostrar).


## 22. Estado del código TSX

Sincronizado con §19–21:
- `queries.ts` — nuevos `usePrograma(id)` (la estrategia rige toda la UI académica, así que
  el detalle escolar pide el programa) y `useCrearInstancia(inscripcionId)`.
- `InscripcionScreen.tsx` — `evaluarModulo(instancias, estrategia)` bifurcado: Sede toma la
  última `EVALUACION_FINAL` como nota del módulo; Base promedia las `NOTA` con
  `cuentaParaPromedio` **descontando las recuperadas por `recuperaAId`**, exige 3 notas y el
  integrador aprobado. Header por línea ("Nota del módulo" / "Promedio" + chip de conteo),
  modal **Cargar nota** con tipos por estrategia y campo *Recupera a*, y confirmación
  destructiva en **Cancelar**.
- `InscripcionesScreen.tsx` — columna Plan muestra "Sin plan" en Base, panel informativo de
  Base en el alta, y el pie del formulario cambia por estrategia.

**Deuda con backend**: el gate (`TP_INTEGRADOR` / `INTEGRADOR`) no tiene campo booleano; se
guarda como `nota: 10 | 0` con `cuentaParaPromedio: false` y la UI lo lee como
aprobado / no aprobado. Si el backend agrega un campo propio, ese mapeo se borra (está
marcado con comentario en `NotaModal`).

## 23. Dos huecos de API que la UI resuelve explícitamente

1. **Sin listado global de seguimientos.** `GET /api/seguimientos` solo acepta
   `?inscripcionId=`, así que "Últimas observaciones" de Inicio consulta un conjunto
   **acotado** (las 6 inscripciones activas más recientes, vía `useSeguimientosDe`) en lugar
   de hacer fan-out sobre todas. Si el backend expone el listado sin filtro, pasa a ser una
   sola request y el hook se borra.
2. **El aviso de "escolar con módulos sin aprobar" no se puede calcular en Inicio.** Depende
   de las instancias evaluativas de cada inscripción (una request por inscripción escolar).
   La card lo dice en una línea al pie y el 422 se sigue explicando en el detalle, donde los
   datos ya están cargados. Se resolvería con un flag `puedeFinalizar` en
   `InscripcionResponse`.

Alta de programa: la estrategia se elige con su consecuencia escrita en cada opción
(Sede / Base / Seguimiento libre) y se filtra por categoría — un programa particular no
puede quedar con estrategia CENMA.

El alta de inscripción se reusa desde la ficha de persona con `rama={null}` y
`personaFija`: la ficha es transversal, así que la rama se elige en el modal.

---

# Huecos de API para trabajar en backend

Cerrado el frontend de las 11 pantallas, esta es la lista completa de lo que la UI
resuelve hoy con workarounds. Cada punto dice qué hace el cliente y qué endpoint o
campo lo reemplazaría.

| # | Hueco | Workaround actual | Propuesta de backend |
|---|---|---|---|
| 1 | Sin listado global de seguimientos: `GET /api/seguimientos` exige `inscripcionId` | Inicio pide los de las **6** inscripciones activas más recientes (`useSeguimientosDe`) | `GET /api/seguimientos` sin filtro, orden por fecha desc + `?limit=` |
| 2 | No se puede saber si una escolar puede finalizar sin traer todas sus instancias | El aviso no se muestra en Inicio (se declara al pie de la card); el 422 se explica en el detalle | `puedeFinalizar: boolean` en `InscripcionResponse`, o `modulosPendientes: int` |
| 3 | `EmpleadoCubierto` solo trae `inscripcionId` + `personaNombre` | Contratos y Empresas cruzan contra `GET /api/inscripciones?categoria=PARTICULAR` para mostrar programa y estado | agregar `programaNombre` y `estado` a `EmpleadoCubierto` |
| 4 | Módulos, planes y grupos solo se consultan por `programaId` | Instituciones los pide **solo al desplegar** una fila: 1 request por programa abierto | `cantidadModulos` y `dias` en `ProgramaResponse`, o `GET /api/programas?institucionId=` con contadores |
| 5 | `GET /api/contratos` no acepta filtros | Contratos y Empresas filtran estado y empresa en cliente | `?empresaId=` y `?estado=`, mismo criterio que `/api/inscripciones` |
| 6 | `InstitucionController` y `EmpresaController` solo tienen list/get/create | La UI no ofrece editar ni dar de baja instituciones ni empresas | `PUT /{id}` en ambos; baja lógica cuando hay contratos o programas asociados |
| 7 | El gate de Sede (TP integrador aprobado / no aprobado) no tiene campo propio | Se guarda como `nota` 10 / 0 y la UI lo lee como booleano | `aprobado: boolean` en `InstanciaEvaluativa` para `TP_INTEGRADOR` |
| 8 | Sin endpoint de métricas | Inicio calcula los 4 KPIs en cliente sobre dos listados completos | opcional: `GET /api/metricas`, solo si los listados crecen |

Los tres primeros son los que cambian la UI de forma visible. 6 y 7 son deuda de modelo.
El 8 no es urgente: con el volumen actual dos requests alcanzan.

## Estado final del código TSX
Las 11 pantallas están implementadas y ruteadas en `src/App.tsx`; el ramal `Pendiente`
queda solo como red de contención para una ruta futura.
La ruta `contratos` acepta `contratoId` opcional: el aviso "Pool agotado" de Inicio y las
tarjetas de Empresas abren directamente el detalle del contrato.
