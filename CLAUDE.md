# gestion-alumnos — reglas del proyecto

Copiar este archivo a la **raíz del repo** (junto al `pom.xml`). Aplica a todo el trabajo de
frontend en `/frontend`.

## Contexto

Monorepo: backend Java 21 / Spring Boot 4.1.1 en `src/main/java/ar/com/ramallo/gestionalumnos`,
frontend Vite + React 19 + TypeScript + TanStack Query en `/frontend`.
Herramienta interna de escritorio, un solo usuario (docente/consultor), JWT.

Dominio: **Persona → Inscripción → Programa**, con dos ramas que se ven distintas y no se
mezclan: **Escolar** (CENMA Base / CENMA Sede — instituciones, programas, módulos, planes,
grupos, notas) y **Particular** (clases individuales y de empresa — contratos con pool de
clases, empresas, seguimiento en texto libre).

Documentación de diseño en `design_handoff_gestion_alumnos/`:
`README.md` (visión general), `SCREENS.md` (spec pantalla por pantalla),
`BACKEND.md` (pendientes de backend), `design/App.dc.html` (prototipo navegable),
`design/screens/` (capturas).

## Reglas de frontend

1. **`src/styles/tokens.css` es la única fuente** de colores, tipografía, espaciado, radios
   y sombras. No inventar valores nuevos ni agregar colores fuera de la paleta.
2. **No agregar entidades, campos ni endpoints que el backend no tenga.** `src/api/types.ts`
   está escrito 1:1 contra los records de `web/dto`. Si cambia el contrato del backend,
   actualizar `types.ts` en el mismo commit.
3. **Un solo componente de error**: `components/ErrorSurface.tsx` para los seis códigos
   (400 inline, 401 → Login, 404 vacío, 409 toast persistente, 422 modal, 500 toast).
   No inventar mensajes: mostrar el `message` de la API donde el diseño lo indica.
4. **La máquina de estados de `InscripcionService` manda**: `ACTIVA ⇄ PAUSADA`,
   `ACTIVA/PAUSADA → CANCELADA`, `ACTIVA → FINALIZADA`. Los botones inválidos se muestran
   **deshabilitados, no ocultos**.
5. **`estrategiaEvaluacion` decide la UI académica**, nunca el nombre del programa:
   `CENMA_SEDE` (planes + TP integrador que habilita la final + la final ES la nota del
   módulo), `CENMA_BASE` (mínimo 3 notas que promedian + integrador que no promedia),
   `SEGUIMIENTO_LIBRE` (sin notas).
6. **El tope del pool solo aplica con `PAQUETE`** (`ContratoService.consumirClase`). Con
   `POR_CLASE` / `MENSUAL` el total es referencial y consumir de más no devuelve 409.
7. **Sin responsive, pero fluido**: es una app de escritorio; no agregar media queries, pero
   el layout tiene que aguantar un notebook angosto (grillas con `minmax(0,1fr)`,
   `min-width: 0`, sin anchos fijos en cajas con texto).
8. **Router mínimo**: la ruta es un discriminated union (`Route` en `shell/AppShell.tsx`),
   sin react-router. Si en algún momento se necesitan URLs compartibles, se cambia entonces
   — no antes.
9. **Estilos inline con los tokens**, como el código existente. No introducir Tailwind,
   CSS-in-JS ni una carpeta de CSS modules sin acordarlo.
10. **No agregar dependencias** más allá de React, React DOM y TanStack Query sin preguntar.

## Convenciones de código

- Componentes de pantalla en `src/screens/<Nombre>Screen.tsx`, un default export nombrado
  (`export function XScreen`). Los modales de esa pantalla viven en el mismo archivo, abajo.
- Hooks de datos **solo** en `src/api/queries.ts`. Las pantallas no llaman a `endpoints.ts`
  directamente.
- Toda mutación que cambia estado hace `setQueryData` del recurso + `invalidateQueries` del
  listado que lo contiene.
- Copy en **español rioplatense**, sin emoji. Los textos del diseño son los definitivos:
  si algo suena raro, avisar antes de reescribirlo.
- Los comentarios explican **por qué** (una regla del backend, un workaround), no qué hace
  la línea.

## Pendientes de backend

`BACKEND.md` lista 8 puntos con contrato propuesto y criterio de aceptación. **Ninguno
bloquea el frontend.** No implementarlos desde el cliente ni asumir campos que todavía no
existen. Si se implementa alguno en el backend, borrar el workaround correspondiente en el
mismo commit.

Workarounds vigentes marcados en el código:
- `useSeguimientosDe` (Inicio consulta 6 inscripciones porque no hay listado global de seguimientos);
- `evaluarModulo` en `InscripcionScreen.tsx` (la API no expone "módulo aprobado");
- el gate de Sede se guarda como `nota` 10/0 porque no existe un campo `aprobado`;
- el cruce contra `/api/inscripciones` en Contratos y Empresas para completar `EmpleadoCubierto`.

## Desarrollo

```bash
cd frontend && npm run dev     # http://localhost:5173
```

El proxy de Vite reenvía `/api` a `localhost:8080`, así que **no hace falta CORS** mientras
se desarrolle. Backend corriendo y datos de `seed-data.sh` cargados antes de probar.
CORS se configura al desplegar, junto con `VITE_API_BASE`.
