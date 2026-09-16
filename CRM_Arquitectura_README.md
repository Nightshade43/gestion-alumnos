# Sistema de Gestión de Alumnos — Arquitectura y Plan de Trabajo

> Este documento reemplaza una versión anterior basada en Python/Flask/SQLite. El proyecto se re-inició con la arquitectura descripta a continuación.

## 1. Objetivo

Sistema unificado para gestionar tanto alumnos de instituciones educativas (escuelas secundarias para adultos) como clientes de cursos particulares (inglés IT, turismo, gastronomía, consultoría en IA educativa).

Cubre actualmente:

- CENMA Bº SMATA — Base
- CENMA Bº SMATA — Sede
- Clientes particulares

Backend expuesto como API REST (JWT), consumido por el frontend propio en `frontend/` (Vite + React). Arquitectura pensada para no reescribir la lógica de dominio al agregar clientes.

---

## 2. Stack técnico

| Componente | Elección |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Build | Maven |
| Persistencia | Spring Data JPA (Hibernate) |
| Base de datos | PostgreSQL |
| Boilerplate | Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`, con `@Builder.Default` donde hay valores iniciales) |
| Testing | JUnit 5, AssertJ, `@DataJpaTest` |
| IDE | IntelliJ (Community) |

### Nota importante sobre Spring Boot 4

Este proyecto usa Spring Boot 4, que modularizó el framework (ver [guía oficial de migración](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)). Esto afecta directamente cómo se escriben imports y dependencias, y la mayoría del contenido en internet todavía asume Spring Boot 3. Puntos a recordar:

- Cada starter tiene su starter de test compañero: `spring-boot-starter-<tecnologia>` + `spring-boot-starter-<tecnologia>-test`.
- Los paquetes de las anotaciones de test cambiaron de raíz. Ejemplos usados en este proyecto:
  - `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
  - `@AutoConfigureTestDatabase` → `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
- `spring-boot-starter-web` fue renombrado a `spring-boot-starter-webmvc`.
- `@MockBean`/`@SpyBean` fueron removidos — usar `@MockitoBean`/`@MockitoSpyBean`.
- `@SpringBootTest` ya no configura MockMvc automáticamente — requiere `@AutoConfigureMockMvc` explícito.
- La traducción de excepciones nativas de Hibernate a `DataAccessException` de Spring solo aplica sobre beans `@Repository` (ej. `JpaRepository`), no al usar `EntityManager` directamente — con `EntityManager` crudo, las excepciones nativas de Hibernate (ej. `ConstraintViolationException`) llegan sin traducir.
- Con `GenerationType.IDENTITY`, el INSERT se ejecuta en el momento del `persist()`, no se puede diferir hasta el `flush()` como con `SEQUENCE` — importante al testear violaciones de constraints con `assertThrows`.

---

## 3. Modelo de dominio

Eje central: `Persona → Inscripcion → Programa`, con ramas específicas según `Programa.categoria`.

### 3.1 Entidades

| Entidad | Rol | Rama |
|---|---|---|
| `Persona` | Datos personales únicos por individuo | Compartida |
| `Institucion` | Entidad educativa externa (ej. CENMA Bº SMATA) | Escolar |
| `Programa` | El "producto" ofrecido; `categoria` (ESCOLAR/PARTICULAR) y `estrategiaEvaluacion` | Ambas |
| `Modulo` | Unidad de contenido dentro de un Programa | Escolar |
| `Plan` | Solo CENMA Sede: define desde qué módulo inicia un alumno (A/B/C) | Escolar |
| `Grupo` | Agrupamiento por horario/día, desacoplado de la lógica académica | Escolar |
| `Inscripcion` | Vínculo Persona–Programa (y opcionalmente Plan/Grupo), con máquina de estados | Ambas |
| `InstanciaEvaluativa` | Nota o instancia evaluativa dentro de un Módulo, con recuperatorio auto-referenciado | Escolar |
| `HistorialGrupo` | Auditoría de cambios de grupo dentro de la misma escuela | Escolar |
| `Contrato` | Facturación (1 a 1 con Inscripcion): tipo, clases contratadas/consumidas; opcionalmente asociado a una `Empresa` | Particular |
| `Seguimiento` | Observaciones de progreso en texto libre (futuro: nivel MCER) | Particular |
| `Empresa` | Pagadora de contratos corporativos: cubre las inscripciones de varios empleados bajo un mismo `Contrato` | Particular |
| `Usuario` | Cuenta de acceso a la API (autenticación JWT); sembrada por `AdminUserSeeder`, sin endpoint de alta | Transversal |

Todas las relaciones son **unidireccionales** (el lado "muchos" conoce al "uno", no al revés) por decisión explícita de simplicidad de mantenimiento en desarrollo individual.

### 3.2 Convenciones de entidad (aplicadas a las 11)

- `@Getter @Setter` explícitos, nunca `@Data` (rompe con colecciones lazy y relaciones en `toString`/`equals`/`hashCode`).
- `equals`/`hashCode` basados únicamente en `id`, con `hashCode` constante (`getClass().hashCode()`) para no romper `HashSet` cuando Hibernate asigna el id tras persistir.
- `@NoArgsConstructor` (requerido por JPA), `@AllArgsConstructor` y `@Builder` en todas.
- `@Builder.Default` obligatorio en todo campo con valor inicial (`estado = ACTIVA`, `cuentaParaPromedio = true`, `clasesConsumidas = 0`) — sin esto, Lombok ignora el inicializador cuando se construye vía builder.
- `@Enumerated(EnumType.STRING)` en todos los enums (nunca `ORDINAL`, que corrompe datos si se reordena el enum).
- `GenerationType.IDENTITY` para todos los IDs (volumen de datos bajo en esta etapa; migrar a `SEQUENCE` si se necesitan inserts masivos).

---

## 4. Reglas de negocio por línea

### 4.1 CENMA Bº SMATA — Base
- 1 clase semanal, único curso (1er año), único Grupo por ciclo lectivo (lunes 19–21).
- 2 módulos por ciclo lectivo, cada uno con actividad integradora interdisciplinaria (`cuentaParaPromedio = false`).
- Mínimo 3 notas por módulo (sin contar el integrador). Aprobación: nota ≥ 6.
- Recuperatorio: la nota reemplaza a la original en el promedio; el original se conserva vía `recuperaA`.
- Promedio de módulo y de ciclo lectivo con 2 decimales (`BigDecimal`, no `double`).

### 4.2 CENMA Bº SMATA — Sede
- Sin ciclo lectivo fijo: 9 módulos consecutivos, 1–7 a cargo del docente.
- 3 planes (A/B/C) con eximición: A desde módulo 1, B desde 4, C desde 6.
- Por módulo: `TP_INTEGRADOR` (gate aprobado/no aprobado) → habilita `EVALUACION_FINAL` (nota ≥ 6, es directamente la nota del módulo, sin promediar con el TP).
- Módulos secuenciales (`esSecuencial = true`): debe aprobarse uno para avanzar al siguiente.
- 2 grupos de agenda (miércoles/jueves), puramente informativos — no afectan el avance.

### 4.3 Clientes particulares
- Individuales o corporativos: un `Contrato` puede ser individual o estar asociado a una `Empresa`, que cubre las inscripciones de varios empleados con pool de clases compartido (`ContratoService`, alta vía `POST /api/contratos/empresa`).
- Sin horario recurrente fijo (sin `Grupo`), coordinación clase a clase.
- Facturación variable vía `Contrato`: por clase, por paquete, o mensual. Sin registro de pagos ni de sesión individual — solo contador agregado de clases consumidas.
- Progreso vía `Seguimiento`: observaciones de texto libre, sin nota numérica.

---

## 5. Máquina de estados de `Inscripcion`

| Transición | Disparador | Validación |
|---|---|---|
| *(alta)* → `ACTIVA` | Nueva Inscripción | Si `categoria = ESCOLAR`: no puede existir otra Inscripción de la misma Persona en `ACTIVA`/`PAUSADA` con categoría `ESCOLAR` |
| `ACTIVA` → `PAUSADA` | Interrupción temporal | — |
| `PAUSADA` → `ACTIVA` | Retoma actividad | — |
| `ACTIVA` → `FINALIZADA` | Cumple condiciones de cierre según `estrategiaEvaluacion` | Terminal |
| `ACTIVA` → `CANCELADA` | Abandono sin completar | Terminal |
| `PAUSADA` → `CANCELADA` | Abandono definitivo tras pausa | Terminal |

`PAUSADA → FINALIZADA` no es un caso de negocio real.

**Complementarias:**
- Cambio de Grupo dentro de la misma escuela: actualiza `Inscripcion.grupo` y registra en `HistorialGrupo`.
- Cambio de categoría: se cierra la Inscripción anterior explícitamente y se crea una nueva independiente.
- Esta máquina de estados vive en `InscripcionService`, no en la entidad — el objeto de dominio no depende de repositorios para validarse.

---

## 6. Puntos abiertos / extensiones futuras

- Nivel MCER (A1–C2) como campo adicional en `Seguimiento`.
- Registro de sesión individual (fecha/hora) si se necesita trazabilidad fina.
- Pagos/facturación real (emisión de comprobantes).
- Roles / multi-usuario (hoy un único usuario admin sembrado por `AdminUserSeeder`, sin endpoint de alta).
- CORS para el backend (el frontend ya lee `VITE_API_BASE`; en desarrollo el proxy de Vite lo evita, pero producción con front y back en orígenes distintos lo necesita).
- Logging estructurado.
- Los 5 puntos de `design_handoff_gestion_alumnos/BACKEND.md` todavía sin resolver (contadores de programa, filtros en `GET /api/contratos`, `PUT` de Institución/Empresa, campo `aprobado` para el gate de Sede, endpoint de métricas de Inicio).
- Migraciones versionadas (Flyway/Liquibase) — hoy el esquema se genera con `ddl-auto=update`, razonable en desarrollo individual pero sin control de versión del esquema.
- Spring Actuator / endpoint de salud, necesario para desplegar en cualquier PaaS.
- `InstitucionController` es el único de los 12 controllers sin test dedicado.

---

## 7. Distribución de paquetes

```
ar.com.ramallo.gestionalumnos
├── GestionAlumnosApplication.java
├── domain/
│   ├── Persona, Institucion, Programa, Modulo, Plan, Grupo,
│   │   Inscripcion, InstanciaEvaluativa, Contrato, Seguimiento,
│   │   HistorialGrupo, Empresa, Usuario
│   └── enums/
│       ├── CategoriaPrograma, EstrategiaEvaluacion, EstadoInscripcion,
│       │   TipoInstanciaEvaluativa, TipoFacturacion, EstadoContrato
├── repository/       interfaces JpaRepository, una por entidad
├── service/
│   ├── InscripcionService, ContratoService, SeguimientoService
│   └── evaluacion/   Strategy pattern: CenmaBaseEvaluacionService, CenmaSedeEvaluacionService,
│                      EvaluacionServiceFactory
├── security/         JwtService, JwtAuthenticationFilter, JwtAuthenticationEntryPoint,
│                      SecurityConfig, UsuarioDetailsService
├── web/               12 controllers REST + GlobalExceptionHandler + ErrorResponse
│   └── dto/           un Request/Response por entidad expuesta
├── exception/        excepciones de negocio mapeadas a HTTP (400/404/409/422)
└── config/           AdminUserSeeder
```

---

## 8. Estado actual

**Completo y testeado (121 tests, `./mvnw test`):**
- Las 13 entidades del modelo de dominio, persistidas contra PostgreSQL real (no H2), con tests de integración (`@DataJpaTest` + `@AutoConfigureTestDatabase(Replace.NONE)`) cubriendo: mapeo de relaciones, unique constraints compuestas, recuperatorio auto-referenciado, defaults de builder, y ambas ramas de negocio (escolar y particular) de punta a punta.
- `repository/`: interfaces `JpaRepository` completas.
- `service/`: `InscripcionService` (máquina de estados, validación de unicidad ESCOLAR), `ContratoService` (individuales y de Empresa con pool compartido), `SeguimientoService`, y `service/evaluacion/` (Strategy pattern por `estrategiaEvaluacion`).
- `web/`: 12 controllers REST, autenticación JWT stateless (`AuthController` + filtro), manejo de errores centralizado con el shape único `{ timestamp, status, error, message }`.
- `frontend/`: las 11 pantallas (Vite + React 19 + TypeScript + TanStack Query) consumiendo esta API, con tema visual propio (Dark CENMA) documentado en `design_handoff_gestion_alumnos/`.

**Pendiente** (detalle en la sección 6 de este documento y en `design_handoff_gestion_alumnos/BACKEND.md`):
- 5 de los 8 puntos de `BACKEND.md` (contadores de programa, filtros de contratos, `PUT` de Institución/Empresa, campo `aprobado` del gate de Sede, métricas de Inicio); los otros 3 ya están en `master` pero el frontend todavía no los adoptó.
- `API_REFERENCE.md` (contrato completo de la API) — referenciado desde el README raíz pero todavía no escrito.
- `InstitucionControllerTest` — el único controller sin test dedicado.
- CI (no hay `.github/workflows`), CORS, Dockerfile/deploy, Actuator, migraciones versionadas.

### Próximo entregable

`API_REFERENCE.md` (documentar el contrato real de los 12 controllers) y, del lado del frontend, adoptar los 3 puntos de `BACKEND.md` ya resueltos en el backend (listado global de seguimientos, `puedeFinalizar`, `EmpleadoCubierto` sin el cruce redundante).

