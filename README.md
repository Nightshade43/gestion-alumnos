# Sistema de Gestión de Alumnos

**Proyecto personal de aprendizaje y portfolio.** Backend de un CRM para gestionar tanto alumnos de instituciones educativas para adultos (CENMA Bº SMATA — Base y Sede) como clientes de cursos particulares (inglés para IT, turismo, gastronomía, consultoría en IA educativa).

Desarrollado como ejercicio de diseño de dominio y arquitectura backend con Java y Spring Boot, priorizando reglas de negocio reales (dos programas educativos con lógicas de evaluación distintas, facturación variable, contratos corporativos con pool de clases compartido) por sobre un CRUD genérico.

> Documentación técnica completa: arquitectura y modelo de dominio en [`CRM_Arquitectura_README.md`](./CRM_Arquitectura_README.md), contrato de la API en [`API_REFERENCE.md`](./API_REFERENCE.md).

---

## Estado del proyecto — V0.75

Backend prácticamente completo: **121/121 tests pasando**.

- ✅ Dominio (13 entidades) con tests de integración `@DataJpaTest` contra PostgreSQL real
- ✅ Capa de repositorios (Spring Data JPA)
- ✅ Capa de servicios — máquina de estados de Inscripción, Strategy pattern para las dos lógicas de evaluación (CENMA Base / CENMA Sede), gestión de Contratos (individuales y de Empresa con pool compartido)
- ✅ 12 controllers REST + manejo de errores centralizado
- ✅ Autenticación JWT stateless, con test de integración end-to-end

Pendiente (no bloqueante, ver sección 6 de `CRM_Arquitectura_README.md`): nivel MCER en Seguimiento, registro de sesión individual, pagos reales, roles/multi-usuario, CORS, logging estructurado.

**Próximo hito:** interfaz web de frontend, consumiendo esta API tal como está documentada en `API_REFERENCE.md`.

---

## Stack técnico

- Java 21
- Spring Boot 4.1.1 (Maven)
- Spring Data JPA / Hibernate
- PostgreSQL
- Lombok
- JWT (jjwt) para autenticación stateless
- JUnit 5 + AssertJ

## Requisitos

- JDK 21
- Maven (o el wrapper incluido: `./mvnw` / `mvnw.cmd`)
- PostgreSQL corriendo localmente

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

   Este archivo está en `.gitignore` — nunca se versiona, cada quien tiene el suyo con sus propias credenciales locales.

3. Ejecutar la aplicación desde IntelliJ (clase `GestionAlumnosApplication`) o por línea de comandos:

   ```bash
   ./mvnw spring-boot:run
   ```

`application.properties` activa el profile `local` (`spring.profiles.active=local`), que hace que Spring Boot cargue automáticamente `application-local.properties`. Con `spring.jpa.hibernate.ddl-auto=update`, el esquema se genera y actualiza automáticamente a partir de las entidades — no requiere migraciones manuales en esta etapa.

Una vez levantado el backend, la API queda disponible en `http://localhost:8080`. El usuario admin se siembra automáticamente al arrancar (`AdminUserSeeder`) — no hay endpoint de registro. Login vía `POST /api/auth/login`; el resto de la API requiere el header `Authorization: Bearer <token>`.

## Tests

```bash
./mvnw test
```

Los tests de persistencia usan `@DataJpaTest` con `@AutoConfigureTestDatabase(replace = Replace.NONE)`, es decir, corren contra la PostgreSQL real configurada en `application-local.properties`, no contra una base embebida.

## Estructura del proyecto

```
src/main/java/ar/com/ramallo/gestionalumnos/
├── domain/          Entidades JPA y enums (13 entidades)
├── repository/      Interfaces JpaRepository
├── service/         Lógica de negocio + Strategy pattern de evaluación
├── security/        Autenticación JWT
├── web/             Controllers REST + manejo de errores + DTOs
├── exception/       Excepciones de negocio mapeadas a HTTP
└── config/          AdminUserSeeder
```

## Documentación

- [`CRM_Arquitectura_README.md`](./CRM_Arquitectura_README.md) — modelo de dominio, reglas de negocio por línea de producto, máquina de estados, decisiones de diseño
- [`API_REFERENCE.md`](./API_REFERENCE.md) — contrato completo de la API (endpoints, DTOs, códigos de error) para consumo desde un frontend
