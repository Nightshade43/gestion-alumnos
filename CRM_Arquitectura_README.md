# Sistema de Gestión de Alumnos — Arquitectura y Plan de Trabajo

> Este documento reemplaza una versión anterior basada en Python/Flask/SQLite. El proyecto se re-inició con la arquitectura descripta a continuación.

## 1. Objetivo

Sistema unificado para gestionar tanto alumnos de instituciones educativas (escuelas secundarias para adultos) como clientes de cursos particulares (inglés IT, turismo, gastronomía, consultoría en IA educativa).

Cubre actualmente:

- CENMA Bº SMATA — Base
- CENMA Bº SMATA — Sede
- Clientes particulares

Etapa inicial: aplicación local. Arquitectura pensada para exponerse como aplicación web sin reescribir la lógica de dominio.

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
| `Contrato` | Facturación (1 a 1 con Inscripcion): tipo, clases contratadas/consumidas | Particular |
| `Seguimiento` | Observaciones de progreso en texto libre (futuro: nivel MCER) | Particular |

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
- Individuales únicamente en esta versión (sin entidad Empresa — ver sección 6).
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

- Empresa como pagador de un Contrato cubriendo varios empleados.
- Nivel MCER (A1–C2) como campo adicional en `Seguimiento`.
- Registro de sesión individual (fecha/hora) si se necesita trazabilidad fina.
- Pagos/facturación real (emisión de comprobantes).

---

## 7. Distribución de paquetes

```
ar.com.ramallo.gestionalumnos
├── GestionAlumnosApplication.java
├── domain/
│   ├── Persona, Institucion, Programa, Modulo, Plan, Grupo,
│   │   Inscripcion, InstanciaEvaluativa, Contrato, Seguimiento, HistorialGrupo
│   └── enums/
│       ├── CategoriaPrograma, EstrategiaEvaluacion, EstadoInscripcion,
│       │   TipoInstanciaEvaluativa, TipoFacturacion, EstadoContrato
├── repository/       (pendiente)
├── service/
│   └── evaluacion/   (pendiente — Strategy pattern CENMA Base / Sede / Seguimiento libre)
├── controller/       (pendiente — vacío hasta exponer REST)
├── exception/        (pendiente)
└── config/           (pendiente)
```

---

## 8. Estado actual

**Completo y testeado:**
- Las 11 entidades del modelo de dominio, persistidas contra PostgreSQL real (no H2), con tests de integración (`@DataJpaTest` + `@AutoConfigureTestDatabase(Replace.NONE)`) cubriendo: mapeo de relaciones, unique constraints compuestas, recuperatorio auto-referenciado, defaults de builder, y ambas ramas de negocio (escolar y particular) de punta a punta.

**Pendiente:**
- `repository/`: interfaces `JpaRepository` por entidad.
- `service/`: `InscripcionService` (máquina de estados, validación de unicidad ESCOLAR) y `service/evaluacion/` (Strategy pattern por `estrategiaEvaluacion`).
- `controller/`, `exception/`, `config/`: sin empezar.

### Próximo entregable

Capa de repositorios (`PersonaRepository`, `ProgramaRepository`, `InscripcionRepository`, etc.), seguida de `InscripcionService`.
