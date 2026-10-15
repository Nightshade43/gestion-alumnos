# Benchmark de carga con datos sintéticos

Mide cuánto tardan (y cuántas consultas SQL cuestan) los listados que usan las pantallas cuando el
sistema tiene cientos de alumnos. Sirve para decidir con datos si hace falta optimizar
(`GET /api/inscripciones` evalúa el estado académico de cada inscripción; ver `BACKEND.md`, ítem 8).

Requisitos: Python 3.8 o superior (solo librería estándar), PostgreSQL local y el repo con el backend.

## Por qué una base aparte

El script crea miles de registros y la API casi no tiene bajas. Por eso corre contra **otra base**
(`gestion_alumnos_bench`) y **otro puerto** (8081), con el perfil `bench`. Tu base y tu backend normales
no se tocan, y limpiar es un `DROP DATABASE`. Si apuntás el script a una base con datos, se niega a
sembrar.

## Pasos (PowerShell, desde la raíz del repo)

1. Crear la base de benchmark (una sola vez):

   ```powershell
   psql -U postgres -c "CREATE DATABASE gestion_alumnos_bench;"
   ```

2. Levantar el backend de benchmark (deja el normal donde está; este usa el puerto 8081):

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "local,bench"
   ./mvnw spring-boot:run
   ```

   Hibernate crea las tablas solo (`ddl-auto=update`) y el seeder crea el usuario admin con las mismas
   credenciales de `application-local.properties`. El perfil `bench` apaga `show-sql` (imprimir cada
   consulta distorsiona los tiempos) y activa las estadísticas de Hibernate; el conteo de consultas sale de
   `bench.log`.

3. En otra terminal, también desde la raíz del repo:

   ```powershell
   python scripts/benchmark/bench_carga.py
   ```

   Toma las credenciales de `login.json` (el mismo archivo que usás para el login con curl) o de
   `--usuario` / `--clave`. Por defecto siembra hasta 50, 150 y 300 alumnos y mide en cada hito.

4. Limpiar: detener el backend de benchmark y

   ```powershell
   psql -U postgres -c "DROP DATABASE gestion_alumnos_bench;"
   Remove-Item bench.log*
   ```

## Cómo leer el resultado

Por cada hito imprime una tabla con, para cada request: cantidad de elementos, tamaño, primera corrida
(en frío), **mediana / p95 / máximo en caliente** y **cantidad de sentencias SQL** de esa request. La fila
`TOTAL Inicio` suma las tres requests que dispara la pantalla de Inicio (`inscripciones` + `contratos` +
`seguimientos`), que es el peor caso: el navegador las lanza en paralelo.

El veredicto compara esa suma contra `--umbral-ms` (500 ms por defecto) en el hito `--objetivo`
(150 alumnos), informa el crecimiento medido por alumno y extrapola linealmente cuándo se cruzaría el
umbral. Si la cantidad de consultas SQL crece más rápido que la de alumnos, la extrapolación es optimista:
confirmar con un hito mayor (`--hitos 100,300,600`).

## Qué datos genera

Mezcla por defecto: 60 % Sede, 25 % Base, 15 % particulares (`--pct-sede`, `--pct-base`). Todo lo creado
lleva el prefijo `BENCH`.

- **Base** (`CENMA_BASE`): 6 módulos. Cada alumno completó un número al azar de módulos: 3 notas que
  promedian + 1 integrador por módulo, y 0 a 2 notas en el módulo en curso.
- **Sede** (`CENMA_SEDE`): 6 módulos, planes A (desde el módulo 1) y B (desde el 4). Por módulo completado,
  un `TP_INTEGRADOR` aprobado (en el 20 % de los casos precedido por uno desaprobado) y una
  `EVALUACION_FINAL`.
- **Particulares**: 0 a 5 seguimientos; 50 % con contrato individual (`PAQUETE`, 10 clases), 30 % en un
  contrato de empresa (grupos de 4) y 20 % sin contrato.
- `--peor-caso`: todos los alumnos escolares con **todos** los módulos evaluados (máxima carga de
  `evaluarEstadoAcademico`).
- `--modulos-base`, `--modulos-sede`, `--empresas` ajustan la estructura; `--semilla` hace reproducible la
  corrida.

Los supuestos (6 módulos por programa, la mezcla de alumnos) son estimaciones: conviene ajustarlos a tus
números reales.

## Medir la base real

Cuando la base real tenga los 150 alumnos, `--solo-medir` hace únicamente `GET` (no crea nada) y emite el
mismo informe. Para el conteo de SQL, el backend real tendría que arrancar con
`spring.jpa.properties.hibernate.generate_statistics=true` y `logging.file.name=bench.log`.

```powershell
python scripts/benchmark/bench_carga.py --base http://localhost:8080 --solo-medir
```
