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
   consulta distorsiona los tiempos) y activa un filtro (`BenchRequestMetricsFilter`) que escribe en
   `bench.log`, por request, la cantidad de sentencias SQL y los milisegundos en el servidor. De ahí salen
   las columnas `SQL` y `srv ms`.

3. En otra terminal, también desde la raíz del repo:

   ```powershell
   python scripts/benchmark/bench_carga.py
   ```

   Toma las credenciales de `login.json` (el mismo archivo que usás para el login con curl) o de
   `--usuario` / `--clave`, y busca `login.json` y `bench.log` en la carpeta actual o en la raíz del repo.
   Por defecto siembra hasta 50, 150 y 300 alumnos y mide en cada hito.

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

La columna `srv ms` es el tiempo que el servidor tardó en esa request (sin red ni cliente) y sirve para
contrastar con la mediana medida desde afuera.

El veredicto compara esa suma contra `--umbral-ms` (500 ms por defecto) en el hito `--objetivo`
(150 alumnos) y estima en qué cantidad de alumnos se cruzaría el umbral (interpolando entre hitos o
extrapolando con el último tramo). Es **DIFERIR** solo si el umbral no se cruza antes de `--margen` veces el
objetivo (2 por defecto): el listado incluye inscripciones finalizadas y canceladas, que se acumulan año
a año, así que no alcanza con mirar el punto del objetivo. Si el tiempo crece peor que lineal, la
estimación es optimista: confirmar con un hito mayor (`--hitos 100,300,600`).

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

## Repetir la medición sobre los mismos datos (antes y después de optimizar)

La base de benchmark conserva los datos entre corridas. Para comparar una optimización sin volver a
sembrar, reiniciá el backend de benchmark con el código nuevo y corré:

```powershell
python scripts/benchmark/bench_carga.py --solo-medir
```

Mide los mismos listados sobre los mismos datos. Para empezar de cero (otros hitos, otros supuestos),
detené el backend, hacé `DROP DATABASE` y `CREATE DATABASE` de `gestion_alumnos_bench` y repetí los pasos.

## Medir la base real

Cuando la base real tenga los 150 alumnos, `--solo-medir` hace únicamente `GET` (no crea nada) y emite el
mismo informe. El conteo de SQL y `srv ms` solo existen si el backend corre con el perfil `bench`; contra
el backend normal esas columnas salen con `-`.

```powershell
python scripts/benchmark/bench_carga.py --base http://localhost:8080 --solo-medir
```
