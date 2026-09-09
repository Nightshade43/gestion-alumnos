# Trabajo pendiente de backend

Repo: `Nightshade43/gestion-alumnos` · branch `master` · paquete
`src/main/java/ar/com/ramallo/gestionalumnos`

Este documento es la contraparte de `SCREENS.md`: **todo lo que el frontend hoy resuelve con
un workaround** porque la API no lo expone. Cada punto trae el estado actual, por qué el
cliente lo necesita, el contrato propuesto (firma concreta) y el criterio de aceptación.

El frontend de las 11 pantallas ya está escrito y **funciona sin ninguno de estos cambios**:
ninguno es un bloqueante. Lo que cambia al implementarlos es cantidad de requests, exactitud
de dos avisos, y deuda de modelo que hoy se está guardando mal.

Orden recomendado: **1 → 3 → 2 → 7 → 5 → 4 → 6 → 8**.

---

## Resumen

| # | Tema | Tipo | Prioridad | Rompe contrato |
|---|---|---|---|---|
| 1 | Listado global de seguimientos | endpoint nuevo | Alta | No |
| 2 | `puedeFinalizar` en `InscripcionResponse` | campo nuevo | Alta | No (aditivo) |
| 3 | `programaNombre` + `estado` en `EmpleadoCubierto` | campos nuevos | Alta | No (aditivo) |
| 4 | Contadores de programa (módulos / días) | campos nuevos | Media | No (aditivo) |
| 5 | Filtros en `GET /api/contratos` | params opcionales | Media | No |
| 6 | `PUT` de Institución y Empresa | endpoints nuevos | Media | No |
| 7 | `aprobado` en instancia evaluativa (gate de Sede) | modelo | Alta | Sí (migración) |
| 8 | Endpoint de métricas de Inicio | endpoint nuevo | Baja | No |

---

## 1. Listado global de seguimientos

**Hoy.** `SeguimientoController` solo expone `GET /api/seguimientos?inscripcionId={id}`
(obligatorio) y `POST /api/seguimientos`.

**Por qué.** La card "Últimas observaciones" de Inicio muestra los 4 seguimientos más
recientes de todo el sistema. Sin listado global, el cliente no tiene forma de saber cuáles
son los más recientes sin recorrer todas las inscripciones.

**Workaround actual.** `useSeguimientosDe()` (en `src/api/queries.ts`) consulta las **6**
inscripciones activas más recientes y ordena en cliente. Son 6 requests en paralelo y el
resultado es **aproximado**: si la observación más nueva pertenece a una inscripción vieja o
pausada, no aparece.

**Contrato propuesto.**

```java
// SeguimientoController
@GetMapping
public List<SeguimientoResponse> listar(
    @RequestParam(required = false) Long inscripcionId,
    @RequestParam(required = false) @Max(100) Integer limit) { ... }
```

- `inscripcionId` pasa a ser **opcional** (mismo criterio que ya se aplicó en
  `InscripcionController.listar`).
- Sin `inscripcionId`: todos los seguimientos, **ordenados por `fecha` desc, `id` desc**.
- `limit` opcional, default sin tope (o 50); el cliente pediría `?limit=4`.
- `SeguimientoResponse` debería sumar `personaNombre` y `programaNombre` (hoy la card los
  obtiene cruzando contra el listado de inscripciones).

**Aceptación.** `GET /api/seguimientos?limit=4` devuelve las 4 observaciones más nuevas del
sistema, de cualquier inscripción y en cualquier estado, sin necesidad de más requests.

**Efecto en el frontend.** Se borra `useSeguimientosDe` y el componente `Observaciones` de
`InicioScreen.tsx` pasa a una sola query. Se elimina la nota "aproximado" de `SCREENS.md` §23.

---

## 2. Saber si una inscripción escolar puede finalizar

**Hoy.** `POST /api/inscripciones/{id}/finalizar` puede fallar con **422**
(`RequisitosAcademicosIncompletos`) cuando la escolar tiene módulos sin aprobar. El cliente
solo se entera **al intentarlo**.

**Por qué.** El diseño de Inicio incluye un aviso en "Requiere atención":
_"inscripción escolar con módulos sin aprobar → finalizar devuelve 422"_. Calcularlo en
cliente exige traer las instancias evaluativas de **cada** inscripción escolar
(1 request por inscripción) y además duplicar la regla académica de Sede/Base en el front.

**Workaround actual.** El aviso **no se muestra** en Inicio. La card lo declara al pie
("detectarlas acá pediría las instancias de cada inscripción") y el 422 se sigue explicando
en el detalle, donde los datos ya están cargados.

**Contrato propuesto.** Campo aditivo en `InscripcionResponse`:

```java
public record InscripcionResponse(
    // … campos actuales …
    boolean puedeFinalizar,        // false si finalizar devolvería 422
    int modulosPendientes          // 0 en particulares
) {}
```

El cálculo es el mismo que ya hace `InscripcionService` antes de tirar el 422 — se trata de
**exponer la evaluación, no de escribir una regla nueva**. En inscripciones particulares
`puedeFinalizar` es siempre `true` y `modulosPendientes` es `0`.

**Aceptación.** Para una escolar con un módulo sin aprobar, `GET /api/inscripciones`
devuelve `puedeFinalizar: false` y `modulosPendientes: 1`; finalizarla devuelve 422.
Los dos valores son coherentes siempre.

**Efecto en el frontend.** Se habilita la tercera fila de "Requiere atención" en Inicio y el
botón **Finalizar** del detalle puede deshabilitarse con explicación previa en lugar de
fallar contra la API.

---

## 3. `EmpleadoCubierto` con programa y estado

**Hoy.**

```java
public record EmpleadoCubierto(Long inscripcionId, String personaNombre) {}
```

**Por qué.** El detalle de contrato (modal de Contratos) y las tarjetas de Empresas muestran
por cada empleado cubierto: **nombre · programa · badge de estado**. Con el record actual
falta la mitad.

**Workaround actual.** `ContratosScreen.tsx` y `EmpresasScreen.tsx` piden además
`GET /api/inscripciones?categoria=PARTICULAR` y cruzan por `inscripcionId` en memoria. Si
una inscripción cubierta quedara fuera de ese listado (por paginado futuro o por estar
cancelada y filtrada), la fila se renderiza sin programa ni estado.

**Contrato propuesto.**

```java
public record EmpleadoCubierto(
    Long inscripcionId,
    String personaNombre,
    String programaNombre,
    EstadoInscripcion estado
) {}
```

Aditivo: el orden de los dos primeros campos no cambia y el cliente mapea por nombre.

**Aceptación.** `GET /api/contratos/{id}` alcanza por sí solo para dibujar el bloque
"Empleados cubiertos" completo, sin una segunda request.

**Efecto en el frontend.** Desaparecen los dos `useInscripciones({ categoria: 'PARTICULAR' })`
que hoy existen solo para ese cruce.

---

## 4. Contadores de programa (módulos y días de grupo)

**Hoy.** `GET /api/modulos`, `/api/planes` y `/api/grupos` solo se consultan por
`programaId`, y `ProgramaResponse` no trae ningún contador.

**Por qué.** En Instituciones, cada programa desplegado muestra
_"N módulos · días de grupo · N activas"_. Y en Programas, la columna "Inscripciones" es un
conteo.

**Workaround actual.** `InstitucionesScreen.tsx` monta los hooks de módulos y grupos **solo
cuando la fila está abierta**: 1 request por programa abierto, nunca en el listado. Es
aceptable (las instituciones son pocas y se abren de a una) pero sigue siendo N+1 en la
apertura.

**Contrato propuesto.** Campos aditivos en `ProgramaResponse`:

```java
public record ProgramaResponse(
    // … campos actuales …
    int cantidadModulos,
    int cantidadInscripcionesActivas,
    List<String> diasDeGrupo      // ["Miércoles", "Sábado"]
) {}
```

Alternativa equivalente si se prefiere no engordar el DTO:
`GET /api/programas?institucionId={id}` con esos contadores en un DTO de resumen aparte.

**Aceptación.** Desplegar una institución con 3 programas no dispara requests adicionales.

---

## 5. Filtros en `GET /api/contratos`

**Hoy.** Devuelve todos los contratos, sin parámetros.

**Por qué.** Contratos filtra por estado y Empresas agrupa por empresa. Además Inicio
necesita solo los activos para el KPI de pool.

**Workaround actual.** Todo el filtrado es en cliente sobre el listado completo. Con el
volumen actual funciona; escala mal si crece.

**Contrato propuesto.**

```java
@GetMapping
public List<ContratoResponse> listar(
    @RequestParam(required = false) Long empresaId,
    @RequestParam(required = false) EstadoContrato estado) { ... }
```

Mismo criterio que `InscripcionController.listar(personaId?, categoria?)`: ambos opcionales
y combinables, sin filtro = todos.

**Aceptación.** `GET /api/contratos?estado=ACTIVO&empresaId=3` devuelve solo los de esa
empresa que están activos.

---

## 6. Edición de Institución y Empresa

**Hoy.** `InstitucionController` y `EmpresaController` exponen solo `list`, `get` y `create`
(a diferencia de `PersonaController`, que ya tiene `PUT` y `DELETE`).

**Por qué.** Un nombre mal cargado o un contacto que cambia no tienen forma de corregirse
desde la app.

**Workaround actual.** La UI **no ofrece** editar ni dar de baja: las dos pantallas son
listado + alta. No hay botones muertos.

**Contrato propuesto.**

```java
@PutMapping("/{id}") InstitucionResponse actualizar(@PathVariable Long id, @Valid @RequestBody InstitucionRequest body);
@PutMapping("/{id}") EmpresaResponse     actualizar(@PathVariable Long id, @Valid @RequestBody EmpresaRequest body);
```

Baja: preferible **no** exponer `DELETE` duro. Si hace falta, baja lógica que devuelva **409**
cuando la institución tiene programas o la empresa tiene contratos activos (el cliente ya
sabe mostrar el 409 como toast persistente).

**Aceptación.** `PUT /api/instituciones/{id}` con nombre vacío devuelve 400 con
`"nombre: …"`; con nombre válido, 200 y el nombre actualizado se propaga a
`ProgramaResponse.institucionNombre`.

---

## 7. El gate de Sede no tiene campo propio (deuda de modelo)

**Hoy.** `InstanciaEvaluativa` guarda `nota` (`BigDecimal`) y `cuentaParaPromedio`. En
**CENMA Sede** el `TP_INTEGRADOR` no es una nota: es un **gate binario** (aprobado / no
aprobado) que habilita la `EVALUACION_FINAL`.

**Workaround actual — el más incómodo de todos.** El modal de carga de nota muestra un
control **Aprobado / No aprobado** y guarda `nota: 10` o `nota: 0`
(ver `NotaModal` en `InscripcionScreen.tsx`). La UI lee de vuelta `nota >= 6` como
"aprobado". Funciona, pero:

- los datos quedan con notas inventadas que nadie puso;
- si mañana el gate se evalúa con otro umbral, los registros viejos son ambiguos;
- un reporte que promedie sin filtrar por tipo va a mezclar el 10/0 del gate.

**Contrato propuesto.**

```java
// InstanciaEvaluativaRequest / Response
Boolean aprobado   // null salvo en instancias de tipo gate (TP_INTEGRADOR / INTEGRADOR)
```

- Para `TP_INTEGRADOR` (Sede) e `INTEGRADOR` (Base): `nota` pasa a ser opcional y `aprobado`
  es el campo que manda.
- `cuentaParaPromedio` sigue en `false` para los gates (ya es así).
- **Migración**: los registros existentes de tipo gate se convierten con
  `aprobado = (nota >= 6)` y, opcionalmente, `nota = null`.

**Aceptación.** Cargar un TP integrador aprobado sin enviar `nota` devuelve 201, y el módulo
queda habilitado para la `EVALUACION_FINAL`. El promedio de Base no cambia (el gate nunca
promedió).

**Efecto en el frontend.** Se borra el mapeo 10/0 y el comentario que lo explica; el control
Aprobado/No aprobado manda `aprobado: true|false`.

---

## 8. Métricas de Inicio (opcional)

**Hoy.** No hay endpoint de métricas.

**Workaround actual.** Inicio calcula los 4 KPIs en cliente sobre **dos** listados completos
(`GET /api/inscripciones` + `GET /api/contratos`). Es una decisión deliberada, no una
carencia: dos requests y cero endpoints nuevos.

**Cuándo hacerlo.** Solo si los listados crecen al punto de que traerlos completos para
contar sea caro. En ese caso:

```java
// GET /api/metricas
public record MetricasResponse(
    int inscripcionesActivas, int activasEscolares, int activasParticulares,
    int inscripcionesPausadas,
    int contratosActivos, int contratosDeEmpresa,
    int clasesConsumidas, int clasesContratadas   // solo PAQUETE
) {}
```

**Nota.** El KPI "Clases del pool" cuenta **solo contratos `PAQUETE`**, porque son los únicos
con tope real en `ContratoService.consumirClase`. Si se agrega el endpoint, tiene que
respetar ese mismo criterio o el número no va a coincidir con las barras de las tarjetas.

---

## Contratos que el frontend da por firmes

Cambiar cualquiera de estos rompe pantallas. Si se toca, avisar:

1. **Shape único de error** — `{ timestamp, status, error, message }` en todos los códigos.
   `ErrorSurface.tsx` enruta por `status` y muestra `message` textual.
2. **Semántica de códigos** — 400 validación de campos con formato `"campo: mensaje; campo: mensaje"`;
   401 sesión; 404 no encontrado; 409 conflicto de estado / duplicado / límite de clases;
   422 regla académica o categoría inválida. El cliente da un tratamiento visual distinto a cada uno.
3. **Máquina de estados de Inscripción** — `ACTIVA ⇄ PAUSADA`, `ACTIVA/PAUSADA → CANCELADA`,
   `ACTIVA → FINALIZADA`. Los botones inválidos se muestran deshabilitados, no ocultos.
4. **`categoria` y `contratoId` en `InscripcionResponse`** — la rama se lee del campo;
   `contratoId === null` es "sin contrato". Sin ellos vuelve el cruce con `/api/programas`.
5. **`estrategiaEvaluacion` decide la UI académica** — `CENMA_SEDE` (planes + gate + nota
   final), `CENMA_BASE` (3 notas que promedian + integrador), `SEGUIMIENTO_LIBRE`
   (sin notas). No se infiere del nombre del programa.
6. **El tope del pool solo aplica con `PAQUETE`** — con `POR_CLASE` / `MENSUAL` el total es
   referencial y consumir de más no devuelve 409. La UI pinta la barra y habilita
   "Consumir clase" según esa regla.
7. **Mapeo por nombre de campo** — `types.ts` son interfaces sobre JSON: agregar campos a un
   record es seguro; **renombrarlos no**. Reordenar el constructor canónico no afecta al
   front, pero sí a los tests de backend que construyan records posicionalmente.

## Sin cambios pendientes

Lo que se pidió en la primera revisión ya está en `master` y el paquete está sincronizado:
`GET /api/inscripciones` con `personaId`/`categoria` opcionales, `GET /api/contratos`,
`categoria` + `contratoId` en `InscripcionResponse`, el `@GetMapping` duplicado de
`InscripcionController` y los tres mappings faltantes de `ContratoController`.
