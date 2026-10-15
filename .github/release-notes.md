## Cómo usar este jar

El jar es autocontenido: incluye el backend y el frontend ya compilado, en un solo puerto. No necesitás clonar el repositorio ni instalar Node.

**Requisitos:** JDK 21 y PostgreSQL en marcha.

1. Creá la base de datos: `CREATE DATABASE gestion_alumnos;`
2. Guardá `gestion-alumnos-__VERSION__.jar` en una carpeta y creá, **en esa misma carpeta**, un archivo `application-local.properties` con tus datos (el modelo está en `src/main/resources/application-local.properties.example` del repositorio):

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/gestion_alumnos
   spring.datasource.username=postgres
   spring.datasource.password=<tu contraseña>

   jwt.secret=<una clave de al menos 32 caracteres>
   admin.username=<usuario admin>
   admin.password=<clave del admin>
   ```

3. Ejecutá `java -jar gestion-alumnos-__VERSION__.jar` y abrí http://localhost:8080 en el navegador.

El archivo `.sha256` permite verificar que la descarga no se corrompió: `sha256sum -c gestion-alumnos-__VERSION__.jar.sha256` (en PowerShell: `Get-FileHash gestion-alumnos-__VERSION__.jar` y comparar con el contenido del archivo).

---
