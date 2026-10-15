# Sistema de Gestión de Alumnos

[![CI](https://img.shields.io/github/actions/workflow/status/Nightshade43/gestion-alumnos/ci.yml?branch=master&label=CI&logo=githubactions&logoColor=white)](https://github.com/Nightshade43/gestion-alumnos/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.7-3178C6?logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-6-646CFF?logo=vite&logoColor=white)
[![Versión](https://img.shields.io/github/v/release/Nightshade43/gestion-alumnos?label=versi%C3%B3n)](https://github.com/Nightshade43/gestion-alumnos/releases)
![License](https://img.shields.io/badge/license-Apache%202.0-blue)

**Proyecto personal de aprendizaje y portfolio.** CRM (backend en Java/Spring Boot y frontend de escritorio en React/TypeScript) para gestionar tanto alumnos de instituciones educativas para adultos (CENMA Bº SMATA — Base y Sede) como clientes de cursos particulares (inglés para IT, turismo, gastronomía, consultoría en IA educativa).

Desarrollado como ejercicio de diseño de dominio y arquitectura backend con Java y Spring Boot (con un frontend que consume la API tal como está documentada), priorizando reglas de negocio reales (dos programas educativos con lógicas de evaluación distintas, facturación variable, contratos corporativos con pool de clases compartido) por sobre un CRUD genérico.

> Documentación técnica completa: arquitectura y modelo de dominio en [`CRM_Arquitectura_README.md`](./CRM_Arquitectura_README.md), contrato de la API en [`API_REFERENCE.md`](./API_REFERENCE.md) y diseño del frontend en [`design_handoff_gestion_alumnos/`](./design_handoff_gestion_alumnos/README.md).

---

## Estado del proyecto — V1.0

Backend y frontend funcionales: **201/201 tests de backend pasando**, con integración continua en cada push a `master`.

**Backend**

- ✅ Dominio (13 entidades) con tests de integración `@DataJpaTest` contra PostgreSQL real
- ✅ Capa de repositorios (Spring Data JPA)
- ✅ Capa de servicios — máquina de estados de Inscripción, Strategy pattern para las dos lógicas de evaluación (CENMA Base / CENMA Sede), gestión de Contratos (individuales y de Empresa con pool compartido)
- ✅ 12 controllers REST + manejo de errores centralizado: los errores del cliente (cuerpo ilegible, parámetro faltante o inválido, ruta inexistente, método o `Content-Type` no soportado) responden 400, 404, 405 o 415 con el mismo formato de error, no 500
- ✅ Autenticación JWT stateless, con test de integración end-to-end
- ✅ Listados con costo constante en consultas: `GET /api/inscripciones` evalúa el estado académico de todo el listado por lotes (3 consultas SQL con 300 alumnos, medido con datos sintéticos; ver [Benchmark de carga](#benchmark-de-carga))

**Frontend** (`/frontend`)

- ✅ 11 pantallas: inicio, login, personas, inscripciones (escolares y particulares), contratos, programas, instituciones y empresas
- ✅ Consume la API sin endpoints ni campos propios: los tipos de `src/api/types.ts` están escritos 1:1 contra los DTOs del backend
- ⚪ Sin tests automatizados; en CI se verifica que los tipos y el build pasen

**Herramientas**

- ✅ CI en GitHub Actions: tests de backend contra PostgreSQL y chequeo de tipos y build del frontend
- ✅ Benchmark de carga con datos sintéticos (`scripts/benchmark/`)
- ✅ Launcher local de doble clic (`iniciar.bat`): verifica Java y PostgreSQL, crea la base si falta y abre el sistema en el navegador

Pendiente (no bloqueante, ver sección 6 de `CRM_Arquitectura_README.md`): nivel MCER en Seguimiento, registro de sesión individual, pagos reales, roles/multi-usuario, CORS, logging estructurado. Para un despliegue: CORS, Docker y Actuator.

---

## Stack técnico

**Backend**

- Java 21
- Spring Boot 4.1.1 (Maven)
- Spring Data JPA / Hibernate
- PostgreSQL
- Lombok
- JWT (jjwt) para autenticación stateless
- JUnit 5 + AssertJ + Mockito

**Frontend**

- React 19 + TypeScript
- Vite
- TanStack Query

**Integración continua**

- GitHub Actions

## Requisitos

- JDK 21
- Maven (o el wrapper incluido: `./mvnw` / `mvnw.cmd`)
- PostgreSQL corriendo localmente
- Node.js 22 (para compilar y desarrollar el frontend; para usar el sistema ya construido no hace falta)

## Configuración local

1. Crear la base de datos:

   ```sql
   CREATE DATABASE gestion_alumnos;
   ```

2. Copiar `src/main/resources/application-local.properties.example` a `src/main/resources/application-local.properties` y completar con tus credenciales reales:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_alumnos
   spring.datasource.username=postgres
   spring.datasource.password=postgres

   jwt.secret=<una clave de al menos 32 caracteres>
   admin.username=<usuario admin>
   admin.password=<password admin>
   ```

   Para depurar consultas, agregá en ese mismo archivo `spring.jpa.show-sql=true` (y `spring.jpa.properties.hibernate.format_sql=true`). Están apagadas por defecto: imprimir cada SQL llena la consola y distorsiona los tiempos.

   Este archivo está en `.gitignore` — nunca se versiona, cada quien tiene el suyo con sus propias credenciales locales.

3. Ejecutar la aplicación desde IntelliJ (clase `GestionAlumnosApplication`) o por línea de comandos:

   ```bash
   ./mvnw spring-boot:run
   ```

`application.properties` activa el profile `local` (`spring.profiles.active=local`), que hace que Spring Boot cargue automáticamente `application-local.properties`. Con `spring.jpa.hibernate.ddl-auto=update`, el esquema se genera y actualiza automáticamente a partir de las entidades — no requiere migraciones manuales en esta etapa.

Una vez levantado el backend, la API queda disponible en `http://localhost:8080`. El usuario admin se siembra automáticamente al arrancar (`AdminUserSeeder`) — no hay endpoint de registro. Login vía `POST /api/auth/login`; el resto de la API requiere el header `Authorization: Bearer <token>`.

Para cargar datos de prueba (CENMA Base, CENMA Sede y particulares), copiá `seed-data.sh.example` a `seed-data.sh` (que está en `.gitignore`), completá tu usuario y clave de admin y corré `bash seed-data.sh` con el backend levantado.

## Uso diario: abrir el sistema con doble clic (Windows)

Requisitos: JDK 21 y PostgreSQL con el servicio iniciado. Node.js solo hace falta cuando hay que construir.

1. **Primera vez:** hacé doble clic en `iniciar.bat`. Si falta `application-local.properties`, el launcher lo crea a partir del ejemplo y lo abre en el Bloc de notas: completá la contraseña de PostgreSQL, `jwt.secret` y las credenciales de admin, guardá y volvé a abrir `iniciar.bat`.
2. **`iniciar.bat`** hace todo lo demás:
   - verifica que haya Java 21 o superior;
   - verifica que PostgreSQL responda y **crea la base `gestion_alumnos` si no existe** (usa `psql`, que se instala junto con PostgreSQL; si el launcher no lo encuentra, agregá la ruta en `application-local.properties` con `launcher.psql=C:\Program Files\PostgreSQL\16\bin\psql.exe`);
   - construye el sistema si todavía no está construido o si hay cambios nuevos (por ejemplo después de un `git pull` en la otra computadora): compila el frontend y lo empaqueta dentro de un único jar;
   - levanta el sistema y abre el navegador en `http://localhost:8080`.
3. **No cierres la ventana de consola mientras uses el sistema:** al cerrarla se detiene.

Otros comandos:

- `construir.bat` reconstruye a mano (instala dependencias del frontend, lo compila y genera `target\gestion-alumnos-*.jar`). Es lo único que usa Node.
- `iniciar.bat -Reconstruir` fuerza la reconstrucción aunque no haya cambios.
- Para tener un acceso en el escritorio, clic derecho sobre `iniciar.bat` → *Enviar a* → *Escritorio (crear acceso directo)*.

En este modo el backend sirve la interfaz en el mismo puerto: no hay proxy ni CORS de por medio. El empaquetado del frontend dentro del jar está en el profile `con-frontend` del `pom.xml`, que no se activa en `./mvnw test` ni en el CI.

### Solución de problemas: Windows Defender

Al clonar el repositorio en una computadora nueva, Windows Defender puede poner en cuarentena `iniciar.bat` y `scripts\launcher\lanzador.ps1`. El launcher invoca PowerShell con `-ExecutionPolicy Bypass`, un patrón que algunos antivirus marcan como sospechoso aunque el script sea inofensivo. El síntoma es que `iniciar.bat` falla con el mensaje *"El argumento '...\lanzador.ps1' para el parámetro -File no existe"* y `git status` muestra esos archivos como `deleted`.

Para resolverlo:

1. En *Seguridad de Windows* → *Protección contra virus y amenazas* → *Historial de protección*, restaurá los archivos detectados.
2. Agregá una exclusión para la carpeta del repositorio (*Configuración de Protección contra virus y amenazas* → *Administrar la configuración* → *Exclusiones*).
3. Recuperá lo que falte con `git restore iniciar.bat scripts/launcher/lanzador.ps1`.

Esto solo aplica al uso local en Windows con el launcher. Un despliegue en servidor no usa `iniciar.bat` y no lo necesita.

## Versiones publicadas (jar de release)

Cada tag `v*` (por ejemplo `v1.0.0`) publica en [GitHub Releases](https://github.com/Nightshade43/gestion-alumnos/releases) un jar autocontenido, con el backend y el frontend ya compilado, y su checksum `.sha256`. Sirve para usar el sistema en otra computadora sin clonar el repositorio ni instalar Node: alcanza con JDK 21 y PostgreSQL.

1. Creá la base: `CREATE DATABASE gestion_alumnos;`
2. Guardá el jar en una carpeta y creá junto a él un `application-local.properties` con el mismo contenido que el ejemplo (`src/main/resources/application-local.properties.example`).
3. Ejecutá `java -jar gestion-alumnos-<versión>.jar` y abrí `http://localhost:8080`.

Para publicar una versión nueva:

1. Subí `<version>` en `pom.xml` (por ejemplo a `1.0.1`), hacé commit y push, y esperá a que el CI esté en verde.
2. Etiquetá ese commit y subí el tag: `git tag v1.0.1` y `git push origin v1.0.1`.
3. El workflow [`release.yml`](./.github/workflows/release.yml) construye el jar y crea el release. Si el tag no coincide con la versión del `pom.xml`, se detiene sin publicar.

El workflow no corre los tests (ya corrieron en el CI del push) y también se puede lanzar a mano desde la pestaña Actions: en ese caso deja el jar como artefacto del run, sin crear un release.

## Frontend (desarrollo)

Con el backend corriendo en `localhost:8080`:

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173
```

El proxy de Vite reenvía `/api` al backend, así que no hace falta CORS mientras se desarrolla. Ingresá con el usuario admin que sembró el backend. Para verificar tipos y generar el build de producción: `npm run build`.

## Tests

```bash
./mvnw test
```

Los tests de persistencia usan `@DataJpaTest` con `@AutoConfigureTestDatabase(replace = Replace.NONE)`, es decir, corren contra la PostgreSQL real configurada en `application-local.properties`, no contra una base embebida. Cada caso crea sus propios datos y se revierte al terminar, así que conviven con datos reales.

Corré los tests en una terminal **sin** `SPRING_PROFILES_ACTIVE`: esa variable de entorno pisa el profile `local` y los tests que levantan Spring no pueden cargar el contexto.

### Integración continua

El workflow [`.github/workflows/ci.yml`](./.github/workflows/ci.yml) corre en cada push a `master` (y a mano desde la pestaña Actions):

- **Backend:** `./mvnw test` con Java 21 y un contenedor de PostgreSQL 16. La configuración que en local vive en `application-local.properties` llega por variables de entorno con valores descartables (datasource, `JWT_SECRET`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`).
- **Frontend:** `npm ci` y `npm run build` (chequeo de tipos más build de Vite) con Node 22.

## Benchmark de carga

`scripts/benchmark/` siembra datos sintéticos en una base aparte (`gestion_alumnos_bench`, puerto 8081, profile `bench`) y mide tiempos y cantidad de consultas SQL de los listados que usan las pantallas. Sirvió para decidir con datos qué optimizar: con 300 alumnos, `GET /api/inscripciones` pasó de ≈2570 consultas y ≈960 ms a 3 consultas y ≈50 ms. Instrucciones y cómo leer el resultado: [`scripts/benchmark/README.md`](./scripts/benchmark/README.md).

## Estructura del proyecto

```
src/main/java/ar/com/ramallo/gestionalumnos/
├── domain/          Entidades JPA y enums (13 entidades)
├── repository/      Interfaces JpaRepository
├── service/         Lógica de negocio + Strategy pattern de evaluación
├── security/        Autenticación JWT
├── web/             Controllers REST + manejo de errores + DTOs
├── exception/       Excepciones de negocio mapeadas a HTTP
└── config/          AdminUserSeeder y, solo con el profile bench, BenchRequestMetricsFilter

frontend/                        App React + TypeScript (Vite)
iniciar.bat / construir.bat      Launcher local (la lógica está en scripts/launcher/lanzador.ps1)
design_handoff_gestion_alumnos/  Diseño del frontend: pantallas, tokens y pendientes de backend
scripts/benchmark/               Benchmark de carga con datos sintéticos
.github/workflows/               Integración continua (ci.yml) y jar de release (release.yml)
```

## Documentación

- [`CRM_Arquitectura_README.md`](./CRM_Arquitectura_README.md) — modelo de dominio, reglas de negocio por línea de producto, máquina de estados, decisiones de diseño
- [`API_REFERENCE.md`](./API_REFERENCE.md) — contrato completo de la API (endpoints, DTOs, códigos de error) para consumo desde un frontend
- [`design_handoff_gestion_alumnos/`](./design_handoff_gestion_alumnos/README.md) — diseño del frontend: visión general, especificación pantalla por pantalla (`SCREENS.md`) y pendientes de backend con su estado (`BACKEND.md`)
- [`scripts/benchmark/README.md`](./scripts/benchmark/README.md) — cómo correr el benchmark de carga y leer sus resultados

## Licencia

Este proyecto está bajo la [Apache License 2.0](./LICENSE).
