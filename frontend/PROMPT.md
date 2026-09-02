# Prompt inicial para Claude Code

Pegar esto en la raíz del repo `gestion-alumnos`, con la carpeta
`design_handoff_gestion_alumnos/` presente:

---

Vas a implementar el frontend de este proyecto en `/frontend` (Vite + React + TypeScript
+ TanStack Query). El backend es el de este mismo repo: Java 21 / Spring Boot 4.1.1,
API REST con JWT en `http://localhost:8080`.

Leé primero `design_handoff_gestion_alumnos/README.md` y `SCREENS.md` completos.
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

Orden de trabajo sugerido:
1. Scaffold de `/frontend` + `tokens.css` + `ToastProvider` + `AppShell` + Login.
2. Personas (listado + ficha) — `PersonasScreen.tsx` ya está hecha como referencia.
3. Inscripciones (listados por rama + detalle con transiciones).
4. Evaluaciones por módulo (con la advertencia ámbar de secuencia) y Seguimiento.
5. Programas con pestañas, Instituciones, Contratos, Empresas.
6. Inicio (KPIs) al final, porque depende de todo lo anterior.

Antes de empezar, decime cuál de los tres gaps del README querés que resuelva agregando
endpoints al backend (listado global de inscripciones, listado de contratos, `categoria` +
`contratoId` en `InscripcionResponse`) y cuál dejo con el workaround client-side.
