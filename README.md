# Sistema de Gestión de Alumnos

Sistema unificado para gestionar alumnos de instituciones educativas (CENMA Bº SMATA — Base y Sede) y clientes de cursos particulares (inglés IT, turismo, gastronomía, consultoría en IA educativa).

Backend en Java / Spring Boot, desarrollado como proyecto personal. Etapa actual: aplicación local, con arquitectura preparada para exponerse como aplicación web más adelante.

Para el detalle completo de arquitectura, modelo de dominio, reglas de negocio y roadmap, ver [`CRM_Arquitectura_README.md`](./CRM_Arquitectura_README.md).

## Stack

- Java 21
- Spring Boot 4.1.1 (Maven)
- Spring Data JPA + Hibernate
- PostgreSQL
- Lombok
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
2. Copiar `src/main/resources/application-local.properties.example` a `src/main/resources/application-local.properties.example` y completar con tus credenciales reales:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_alumnos
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   ```
   Este archivo está en `.gitignore` — nunca se versiona, cada quien tiene el suyo con sus propias credenciales locales.
3. Ejecutar la aplicación desde IntelliJ (clase `GestionAlumnosApplication`) o por línea de comandos:
   ```
   ./mvnw spring-boot:run
   ```

`application.properties` activa el profile `local` (`spring.profiles.active=local`), que hace que Spring Boot cargue automáticamente `application-local.properties.example` además del archivo base. Con `spring.jpa.hibernate.ddl-auto=update`, el esquema se genera y actualiza automáticamente a partir de las entidades — no requiere migraciones manuales en esta etapa.

## Tests

```
./mvnw test
```

Los tests de persistencia usan `@DataJpaTest` con `@AutoConfigureTestDatabase(replace = Replace.NONE)`, es decir, corren contra la PostgreSQL real configurada en `application.properties`, no contra una base embebida.

## Estructura del proyecto

```
src/main/java/ar/com/ramallo/gestionalumnos/
├── domain/          Entidades JPA y enums
├── repository/       (pendiente)
├── service/          (pendiente)
├── controller/        (pendiente)
├── exception/        (pendiente)
└── config/            (pendiente)
```

## Estado

Modelo de dominio completo (11 entidades) y validado con tests de integración. Próximo paso: capa de repositorios y servicios (ver estado detallado en `CRM_Arquitectura_README.md`, sección 8).
