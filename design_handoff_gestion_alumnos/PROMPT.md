# Prompt inicial para Claude Code

Pegar esto en la raíz del repo `gestion-alumnos`, con la carpeta
`design_handoff_gestion_alumnos/` presente:

---

Vas a implementar el frontend de este proyecto en `/frontend` (Vite + React 19 + TypeScript
+ TanStack Query, npm). El backend es el de este mismo repo: Java 21 / Spring Boot 4.1.1,
API REST con JWT en `http://localhost:8080`, con datos de prueba cargados por `seed-data.sh`.

En desarrollo las requests salen relativas (`/api/...`) y las reenvía el **proxy de Vite**:
no toques la config de CORS del backend — eso queda para el deploy.

Leé primero `design_handoff_gestion_alumnos/README.md` y `SCREENS.md` completos.
`BACKEND.md` lista 8 pendientes de backend: **ninguno bloquea el frontend**, no los
implementes desde el cliente ni inventes campos que todavía no existen.
Los archivos `design/*.dc.html` son **referencias visuales en HTML**, no código a copiar:
recreá esos diseños en React usando los tokens y primitivas del paquete.

Reglas:
1. `tokens/tokens.css` es la única fuente de colores, tipografía, espaciado, radios y
   sombras. No inventes valores nuevos.
2. `src/api/*` está escrito contra los DTOs reales (`src/main/java/.../web/dto`).
   Si tocás el contrato del backend, actualizá `types.ts` en el mismo commit.
3. No agregues entidades, campos ni endpoints que no existan en el backend.
4. Respetá la máquina de estados de `InscripcionService`: botones inválidos deshabilitados,
   no ocultos.
5. Un solo componente de error (`ErrorSurface.tsx`) para los seis códigos. No inventes
   mensajes: mostrá `message` de la API donde el diseño lo indica.
6. Sin responsive: es una app de escritorio. No agregues media queries.

Antes de escribir código: copiá `design_handoff_gestion_alumnos/CLAUDE.md` a la raíz del
repo y seguí `design_handoff_gestion_alumnos/scaffold/README.md` para armar `/frontend`
(los comandos exactos están ahí; gestor de paquetes: **npm**).

Orden de trabajo sugerido:
1. Montar `/frontend` con `scaffold/` + `src/` + `tokens/` y dejar `npm run dev` andando.
   Único ajuste de rutas: el import de `tokens.ts` en `primitives.tsx` pasa a `'../styles/tokens'`.
2. Levantar `ToastProvider` + `AppShell` + Login y verificar el guard de 401 contra el backend.
3. Verificar pantalla por pantalla contra `design/App.dc.html`, en el orden del sidebar.
4. Recién después, refactors propios del proyecto (routing con URL real, tests, code splitting).

Los tres gaps originales ya están resueltos en el backend (`GET /api/inscripciones` con
`personaId`/`categoria` opcionales, `GET /api/contratos`, y `categoria` + `contratoId` en
`InscripcionResponse`), así que no queda ningún fan-out y la rama se lee del campo `categoria`.

**Las 11 pantallas vienen escritas y ruteadas** en `src/App.tsx`: Login, Inicio, Personas,
Persona (ficha), Inscripciones escolares, Inscripciones particulares, Inscripción (detalle),
Programas, Programa (pestañas), Instituciones, Contratos y Empresas — con sus modales de
alta, las confirmaciones irreversibles, los estados vacíos y el error de red.
El trabajo es **portarlas al scaffold y verificarlas contra el backend corriendo**, no
escribirlas de nuevo. Si algo no coincide con `design/App.dc.html`, gana el mockup.

Heurísticas de cliente marcadas en el código, por si el backend cambia (las tres están
justificadas en `BACKEND.md`):
- el tope del pool solo aplica con `PAQUETE`;
- "módulo aprobado" lo calcula el front (`evaluarModulo` en `InscripcionScreen.tsx`);
- el gate de Sede se guarda como `nota` 10/0 porque no existe un campo `aprobado`;
- "Últimas observaciones" de Inicio consulta 6 inscripciones (`useSeguimientosDe`) porque
  no hay listado global de seguimientos.

Si el backend implementa alguno de los 8 puntos de `BACKEND.md`, borrá el workaround
correspondiente en el mismo commit y actualizá `types.ts`.
