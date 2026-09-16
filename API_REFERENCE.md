# API Reference — Sistema de Gestión de Alumnos

Contrato completo de la API REST expuesta por el backend (`src/main/java/ar/com/ramallo/gestionalumnos`),
para consumir desde el frontend (`frontend/`) o cualquier otro cliente. Documentado directamente contra
el código de los 12 controllers — no hay endpoints planeados ni aspiracionales acá.

- **Base URL local**: `http://localhost:8080`
- **Formato**: JSON (`application/json`) en request y response
- **Autenticación**: JWT stateless. Todo endpoint requiere `Authorization: Bearer <token>` **excepto**
  `POST /api/auth/login`
- **No hay endpoint de registro**: el único usuario se siembra al arrancar la app (`AdminUserSeeder`,
  credenciales en `application-local.properties`)

---

## Autenticación

### `POST /api/auth/login`

Público, sin token.

**Request**
```json
{ "username": "admin", "password": "..." }
```
`username` y `password`: obligatorios (`@NotBlank`).

**Response 200**
```json
{ "token": "eyJhbGciOi..." }
```

**Errores**: `401` con credenciales inválidas (mensaje genérico "Usuario o contraseña incorrectos" —
no distingue si falló el usuario o la contraseña).

---

## Personas

`Persona` es compartida entre las dos ramas (Escolar/Particular) — es el único punto donde conviven.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/personas` | Crear |
| `GET` | `/api/personas/{id}` | Obtener por id |
| `GET` | `/api/personas` | Listar todas |
| `PUT` | `/api/personas/{id}` | Actualizar |
| `DELETE` | `/api/personas/{id}` | Eliminar (baja física) |

**`PersonaRequest`** (body de `POST`/`PUT`)
```json
{ "nombre": "Lucía Ferreyra", "email": "lucia@mail.com", "telefono": "351...", "documento": "38204115" }
```
`nombre`: obligatorio. `email`: si viene, debe tener formato válido (`@Email`). `telefono`/`documento`: libres, opcionales.

**`PersonaResponse`**
```json
{ "id": 1, "nombre": "Lucía Ferreyra", "email": "lucia@mail.com", "telefono": "351...", "documento": "38204115" }
```

**Nota**: `DELETE` es baja física (`personaRepository.deleteById`), no lógica. Si la persona tiene
inscripciones asociadas y la FK no lo permite, la base va a rechazar el borrado — eso llega como `409`
vía el handler genérico de `DataIntegrityViolationException`.

---

## Instituciones

Entidad educativa externa (rama Escolar). **Solo lectura + alta** — no tiene `PUT` ni `DELETE`
(ver pendiente #6 en `design_handoff_gestion_alumnos/BACKEND.md`).

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/instituciones` | Crear |
| `GET` | `/api/instituciones/{id}` | Obtener por id |
| `GET` | `/api/instituciones` | Listar todas |

**`InstitucionRequest`**
```json
{ "nombre": "CENMA Bº SMATA" }
```
`nombre`: obligatorio.

**`InstitucionResponse`**
```json
{ "id": 1, "nombre": "CENMA Bº SMATA" }
```

---

## Programas

El "producto" ofrecido — define categoría y estrategia de evaluación. **Solo lectura + alta.**

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/programas` | Crear |
| `GET` | `/api/programas/{id}` | Obtener por id |
| `GET` | `/api/programas` | Listar todos (sin filtro por institución ni categoría) |

**`ProgramaRequest`**
```json
{ "nombre": "CENMA Sede", "categoria": "ESCOLAR", "estrategiaEvaluacion": "CENMA_SEDE", "institucionId": 1 }
```
`nombre`, `categoria`, `estrategiaEvaluacion`: obligatorios. `institucionId`: opcional (un programa
Particular no tiene institución).

- `categoria`: `ESCOLAR` | `PARTICULAR`
- `estrategiaEvaluacion`: `CENMA_BASE` | `CENMA_SEDE` | `SEGUIMIENTO_LIBRE` — decide qué UI académica
  usa el frontend (planes+gate+nota final / 3 notas que promedian+integrador / sin notas). No se infiere
  del nombre.

**`ProgramaResponse`**
```json
{ "id": 1, "nombre": "CENMA Sede", "categoria": "ESCOLAR", "estrategiaEvaluacion": "CENMA_SEDE",
  "institucionId": 1, "institucionNombre": "CENMA Bº SMATA" }
```

**Sin contadores**: no trae cantidad de módulos, inscripciones activas ni días de grupo — el cliente
tiene que pedirlos aparte (ver pendiente #4 en `BACKEND.md`).

---

## Módulos

Unidad de contenido dentro de un Programa (rama Escolar). **Solo lectura + alta.**

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/modulos` | Crear |
| `GET` | `/api/modulos/{id}` | Obtener por id |
| `GET` | `/api/modulos?programaId={id}` | Listar por programa, ordenados por `orden` |

`programaId` es **obligatorio** en el listado (a diferencia de `/api/inscripciones` y `/api/contratos`,
que sí filtran opcionalmente).

**`ModuloRequest`**
```json
{ "programaId": 1, "orden": 3, "esSecuencial": true }
```
Los tres campos son obligatorios. `(programaId, orden)` es único — un duplicado devuelve `409`.

**`ModuloResponse`**
```json
{ "id": 5, "programaId": 1, "orden": 3, "esSecuencial": true }
```

---

## Planes

Solo CENMA Sede: define desde qué módulo arranca un alumno según su Plan (A/B/C). **Solo lectura + alta.**

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/planes` | Crear |
| `GET` | `/api/planes/{id}` | Obtener por id |
| `GET` | `/api/planes?programaId={id}` | Listar por programa (sin orden garantizado) |

**`PlanRequest`**
```json
{ "programaId": 1, "codigo": "B", "moduloInicio": 4 }
```
Los tres campos son obligatorios. `(programaId, codigo)` es único — un duplicado devuelve `409`.

**`PlanResponse`**
```json
{ "id": 2, "programaId": 1, "codigo": "B", "moduloInicio": 4 }
```

---

## Grupos

Agrupamiento por día/horario, desacoplado de la lógica académica (puramente informativo — no afecta
el avance de módulos). **Solo lectura + alta.**

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/grupos` | Crear |
| `GET` | `/api/grupos/{id}` | Obtener por id |
| `GET` | `/api/grupos?programaId={id}` | Listar por programa |

**`GrupoRequest`**
```json
{ "programaId": 1, "dia": "Miércoles", "horario": "19-21" }
```
Los tres campos son obligatorios. **Sin constraint único** — se pueden crear dos grupos idénticos para
el mismo programa sin que la base lo rechace.

**`GrupoResponse`**
```json
{ "id": 3, "programaId": 1, "dia": "Miércoles", "horario": "19-21" }
```

---

## Inscripciones

El vínculo Persona–Programa, con máquina de estados. El endpoint más grande de la API.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/inscripciones` | Crear (queda en `ACTIVA`) |
| `GET` | `/api/inscripciones/{id}` | Obtener por id |
| `GET` | `/api/inscripciones?personaId=&categoria=` | Listar — ambos filtros opcionales y combinables |
| `POST` | `/api/inscripciones/{id}/pausar` | `ACTIVA → PAUSADA` |
| `POST` | `/api/inscripciones/{id}/reanudar` | `PAUSADA → ACTIVA` |
| `POST` | `/api/inscripciones/{id}/finalizar` | `ACTIVA → FINALIZADA` |
| `POST` | `/api/inscripciones/{id}/cancelar` | `ACTIVA`/`PAUSADA` `→ CANCELADA` |
| `PATCH` | `/api/inscripciones/{id}/grupo?grupoId={id}` | Cambia el grupo (registra en `HistorialGrupo`) |

**`InscripcionRequest`**
```json
{ "personaId": 1, "programaId": 1, "planId": 2, "grupoId": 3, "fechaInicio": "2026-03-01" }
```
`personaId`, `programaId`, `fechaInicio`: obligatorios. `planId`/`grupoId`: opcionales (un particular
no tiene ninguno de los dos).

**`InscripcionResponse`**
```json
{
  "id": 10, "personaId": 1, "personaNombre": "Lucía Ferreyra",
  "programaId": 1, "programaNombre": "CENMA Sede", "categoria": "ESCOLAR",
  "planCodigo": "B", "grupoDia": "Miércoles", "grupoHorario": "19-21",
  "fechaInicio": "2026-03-01", "fechaFin": null, "estado": "ACTIVA", "contratoId": null,
  "puedeFinalizar": false, "modulosPendientes": 2
}
```
`estado`: `ACTIVA` | `PAUSADA` | `FINALIZADA` | `CANCELADA`. `contratoId`: `null` = sin contrato (así
distingue el cliente si es particular sin facturar todavía). `puedeFinalizar`/`modulosPendientes`: en
particulares siempre `true`/`0`; en escolares, `false` si quedan módulos requeridos sin aprobar —
**el mismo cálculo que usa el propio endpoint de finalizar antes de tirar 422**, no una regla aparte.

### Máquina de estados

| Transición | Validación |
|---|---|
| *(alta)* → `ACTIVA` | Si `categoria=ESCOLAR`: 409 si la persona ya tiene otra inscripción escolar `ACTIVA`/`PAUSADA` |
| `ACTIVA ⇄ PAUSADA` | — |
| `ACTIVA` → `FINALIZADA` | 422 si `puedeFinalizar=false` (quedan módulos sin aprobar) |
| `ACTIVA`/`PAUSADA` → `CANCELADA` | — |

Cualquier transición fuera de esta tabla (ej. `FINALIZADA → ACTIVA`) devuelve **409**
(`"Transicion invalida: X -> Y"`). Los botones inválidos se muestran deshabilitados en el frontend,
nunca ocultos.

---

## Instancias evaluativas

Una nota o instancia evaluativa dentro de un Módulo. Sin `PUT`/`DELETE` — el recuperatorio es un
registro nuevo, no una edición.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/instancias-evaluativas` | Crear |
| `GET` | `/api/instancias-evaluativas?inscripcionId=&moduloId=` | Listar — `inscripcionId` obligatorio, `moduloId` opcional |

**`InstanciaEvaluativaRequest`**
```json
{ "inscripcionId": 10, "moduloId": 5, "tipo": "NOTA", "nota": 8.5, "fecha": "2026-04-10",
  "cuentaParaPromedio": true, "recuperaAId": null }
```
`inscripcionId`, `moduloId`, `tipo`, `fecha`: obligatorios. `nota`: sin `@NotNull` a nivel API (el gate
de Sede — `TP_INTEGRADOR` — hoy se carga igual con nota `10`/`0` en vez de un campo `aprobado` propio;
ver pendiente #7 en `BACKEND.md`, es deuda de modelo conocida). `cuentaParaPromedio`: si se omite,
default `true`. `recuperaAId`: id de la instancia que este registro recupera — si viene, debe existir
(404 si no).

- `tipo`: `NOTA` | `INTEGRADOR` | `TP_INTEGRADOR` | `EVALUACION_FINAL`

**`InstanciaEvaluativaResponse`**
```json
{ "id": 20, "inscripcionId": 10, "moduloId": 5, "tipo": "NOTA", "nota": 8.5,
  "fecha": "2026-04-10", "cuentaParaPromedio": true, "recuperaAId": null }
```

**Sin validación de secuencia**: la API no impide cargar una `EVALUACION_FINAL` sin `TP_INTEGRADOR`
aprobado en Sede. El frontend lo permite y avisa en ámbar, no bloqueante.

---

## Contratos

Facturación de inscripciones Particulares — individual o de Empresa con pool de clases compartido.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/contratos` | Crear individual (1 a 1 con una inscripción) |
| `POST` | `/api/contratos/empresa` | Crear de Empresa (cubre varias inscripciones) |
| `GET` | `/api/contratos/{id}` | Obtener por id |
| `GET` | `/api/contratos` | Listar todos (sin filtros — ver pendiente #5 en `BACKEND.md`) |
| `POST` | `/api/contratos/{id}/consumir-clase` | Descuenta una clase del pool |
| `POST` | `/api/contratos/{id}/ampliar-cupo?clasesAdicionales={n}` | Suma cupo |
| `POST` | `/api/contratos/{id}/finalizar` | Cierra el contrato |

**`ContratoRequest`** (individual)
```json
{ "inscripcionId": 10, "tipoFacturacion": "PAQUETE", "clasesContratadas": 24 }
```

**`ContratoEmpresaRequest`**
```json
{ "empresaId": 3, "tipoFacturacion": "PAQUETE", "clasesContratadas": 40, "inscripcionIds": [10, 11] }
```
`inscripcionIds`: no puede venir vacío (`@NotEmpty`).

- `tipoFacturacion`: `POR_CLASE` | `PAQUETE` | `MENSUAL`

**Validaciones al crear (ambos endpoints)**: 422 si la inscripción no es de categoría `PARTICULAR`
(`CategoriaInvalidaException`); 409 si la inscripción ya tiene un contrato asociado
(`RegistroDuplicadoException`).

**`ContratoResponse`**
```json
{
  "id": 7, "empresaId": 3, "empresaNombre": "Alcor S.A.", "tipoFacturacion": "PAQUETE",
  "clasesContratadas": 40, "clasesConsumidas": 19, "estado": "ACTIVO",
  "inscripciones": [
    { "inscripcionId": 10, "personaNombre": "Lucía Ferreyra", "programaNombre": "Inglés IT", "estado": "ACTIVA" }
  ]
}
```
`empresaId`/`empresaNombre`: `null` en un contrato individual. `estado`: `ACTIVO` | `FINALIZADO`.

**El tope de `consumir-clase` solo aplica con `PAQUETE`**: ahí, consumir de más devuelve **409**
(`LimiteClasesExcedidoException`, mensaje incluye el límite exacto). Con `POR_CLASE`/`MENSUAL`,
`clasesContratadas` es puramente referencial — consumir de más nunca devuelve 409. Consumir de un
contrato ya `FINALIZADO` también es 409, sin importar el tipo de facturación.

---

## Empresas

Pagadora de contratos corporativos. **Solo lectura + alta** — mismo caso que Instituciones (sin
`PUT`/`DELETE`, pendiente #6 en `BACKEND.md`).

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/empresas` | Crear |
| `GET` | `/api/empresas/{id}` | Obtener por id |
| `GET` | `/api/empresas` | Listar todas |

**`EmpresaRequest`**
```json
{ "nombre": "Alcor S.A.", "contacto": "compras@alcor.com" }
```
`nombre`: obligatorio. `contacto`: libre, opcional.

**`EmpresaResponse`**
```json
{ "id": 3, "nombre": "Alcor S.A.", "contacto": "compras@alcor.com" }
```

---

## Seguimientos

Observaciones de progreso en texto libre (rama Particular, `estrategiaEvaluacion=SEGUIMIENTO_LIBRE`).
Sin `PUT`/`DELETE` — es un historial cronológico, no se edita.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/seguimientos` | Crear |
| `GET` | `/api/seguimientos?inscripcionId=&limit=` | Listar — ambos opcionales |

Sin `inscripcionId`: devuelve **todos** los seguimientos del sistema, ordenados por `fecha` desc,
`id` desc — pensado para una vista tipo "últimas observaciones" (`?limit=4`). `limit` tiene un tope de
100 (`@Max(100)`, devuelve 400 si se excede).

**`SeguimientoRequest`**
```json
{ "inscripcionId": 10, "fecha": "2026-04-10", "observacion": "Buen progreso en fluidez oral." }
```
Los tres campos son obligatorios.

**`SeguimientoResponse`**
```json
{ "id": 15, "inscripcionId": 10, "personaNombre": "Lucía Ferreyra",
  "programaNombre": "Inglés IT", "fecha": "2026-04-10", "observacion": "Buen progreso en fluidez oral." }
```

---

## Códigos de error

Todo error de la API devuelve el mismo shape (`ErrorResponse`):

```json
{ "timestamp": "2026-09-16T14:32:00Z", "status": 409, "error": "Conflict", "message": "..." }
```

| Código | Cuándo | Excepción / origen |
|---|---|---|
| `400` | Body inválido (`@NotNull`/`@NotBlank`/`@Email` fallido, o un `@RequestParam` fuera de rango como `limit > 100`) | `MethodArgumentNotValidException` / `ConstraintViolationException` — `message` viene como `"campo: mensaje; campo: mensaje"` |
| `401` | Login inválido, o token vencido/ausente en cualquier request | `AuthenticationException` |
| `404` | Id que no existe (persona, programa, módulo, plan, grupo, inscripción, contrato, empresa) | `RecursoNoEncontradoException` |
| `409` | Transición de estado inválida; duplicado (unique constraint de Módulo/Plan, o de cualquier otra entidad vía `DataIntegrityViolationException`); contrato ya asociado a una inscripción; límite de clases del pool excedido | `EstadoInvalidoException` / `RegistroDuplicadoException` / `DataIntegrityViolationException` / `LimiteClasesExcedidoException` |
| `422` | Regla académica incompleta al finalizar una inscripción escolar; categoría inválida para crear un contrato | `RequisitosAcademicosIncompletosException` / `CategoriaInvalidaException` |
| `500` | Cualquier excepción no mapeada explícitamente | handler genérico — nunca expone el detalle interno, mensaje fijo `"Error interno del servidor"` |

`message` es siempre texto plano pensado para mostrar directo en la UI (no un código a mapear a otro
texto) — así lo consume `ErrorSurface.tsx` en el frontend.

---

## Contratos que un cliente debe dar por firmes

(Ver también la sección "Contratos que el frontend da por firmes" en
`design_handoff_gestion_alumnos/BACKEND.md`, que es la misma lista desde el lado del consumidor.)

1. El shape de error de arriba es el mismo en los 6 códigos.
2. `categoria` y `contratoId` en `InscripcionResponse` son la fuente de verdad de la rama — nunca inferir
   del nombre del programa.
3. `estrategiaEvaluacion` decide qué UI académica corresponde, no el nombre del programa.
4. El tope de pool de un Contrato solo aplica con `tipoFacturacion=PAQUETE`.
5. Agregar campos a un record es seguro (los DTOs se mapean por nombre); renombrarlos no.

---

## Pendiente (no rompe nada de lo de arriba)

Ver `design_handoff_gestion_alumnos/BACKEND.md` para el detalle completo: contadores en
`ProgramaResponse`, filtros en `GET /api/contratos`, `PUT` de Institución/Empresa, campo `aprobado`
propio para el gate de Sede, y un endpoint de métricas para Inicio.
