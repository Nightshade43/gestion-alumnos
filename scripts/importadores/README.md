# Importadores de datos históricos

Cargan en gestion-alumnos los alumnos de los sistemas anteriores. Solo librería estándar de Python
(3.8 o superior). Se construyen por capas y cada una se prueba antes de sumar la siguiente.

| Importador | Fuente | Estado |
|---|---|---|
| `importar_sede.py` | Export JSON de la app CENMA (Google Sheets) | Capas 1 y 2: lectura, normalización y pasada en seco contra la API (no crea nada). Falta la carga (`--confirmar`) |
| Base | Excel de CENMA Base | Pendiente |

**Datos reales:** los exports tienen nombres, DNI y notas de alumnos. Van en la carpeta `Datos/` de la
raíz del repo, que está en `.gitignore`. El repo es público: nunca los guardes en otra carpeta.

## CENMA Sede (`importar_sede.py`)

Desde la raíz del repo, con el backend levantado:

```
python scripts/importadores/importar_sede.py Datos/cenma_ingles_2026-11-26.json --usuario <admin> --clave <clave>
```

Es una **pasada en seco**: solo hace GET. Verifica en gestion-alumnos que exista lo que la importación
necesita y marca los alumnos cuyo DNI ya está cargado, que se saltearían. No crea nada.

- **Estructura:** un programa con estrategia `CENMA_SEDE` (si hay más de uno, elegilo con
  `--programa-id`), sus módulos 1 a 7, los planes que usa el export con su módulo de inicio
  (A = 1, B = 4, C = 6) y los grupos Miércoles y Jueves (sin importar acentos ni mayúsculas). Lo que
  falte se lista como `FALTA:` y el script termina con código 2: se crea desde las pantallas y se
  vuelve a correr.
- **Credenciales:** `--usuario`/`--clave`, las variables `GA_USUARIO`/`GA_CLAVE` o un `login.json`
  con `{"username": ..., "password": ...}` en la carpeta actual o en la raíz del repo (`/login.json`
  está en `.gitignore`).
- **Sin backend:** `--sin-api` analiza solo el export (capa 1).
- Otro backend: `--base http://localhost:8081`.

Por consola muestra solo cantidades. El detalle por alumno (nombre, DNI, plan, fecha de inicio, cada
instancia que se cargaría y los avisos) queda en `Datos/reporte-sede-AAAAMMDD-HHMMSS.txt`. Buscá
`REVISAR` en el reporte para los casos que necesitan una decisión a mano.

### Cómo se traduce el export

- **Persona:** `nombre` tal cual ("Apellido, Nombre"), `documento` = `dni`. Un DNI repetido en el
  export se importa una sola vez (el primero) y el otro va al reporte.
- **Inscripción** en el programa CENMA_SEDE:
  - Plan: el del export. Si está vacío, se infiere por los módulos eximidos (ninguno → A, 1 a 3 → B,
    1 a 5 → C). Otro patrón queda sin plan y va a REVISAR.
  - Grupo: Miércoles o Jueves.
  - Estado: activo → `ACTIVA`, inactivo → `PAUSADA`. Marcado como egresado → `ACTIVA` y a REVISAR
    (con módulos pendientes no se puede finalizar).
  - `fechaInicio`: la fecha más antigua de sus instancias. Sin ninguna fecha: 01/03/2026 si está
    activo, 01/03/2025 si no.
- **Instancias**, por módulo no eximido, el TP antes que la final:
  - `tpi_fecha` válida → `TP_INTEGRADOR` aprobado con esa fecha.
  - Nota y fecha de final → `EVALUACION_FINAL`. Si el módulo está aprobado y no tiene fecha de TP, se
    crea el `TP_INTEGRADOR` aprobado con la fecha de la final (marcado como inferido).
  - Un módulo aprobado sin nota o sin fecha no inventa nada: va a REVISAR.

### Datos sucios que se corrigen solos (y quedan en el reporte)

- **Fila corrida:** el estado trae una fecha, `eval_fecha` trae el estado y `tpi_fecha` la nota. Se
  reconstruye.
- **Fechas:** `Date.toString()` de JavaScript (medianoche de Argentina) o ISO en UTC (se pasa a hora
  argentina antes de tomar el día).
- **Nota guardada como fecha de Sheets** (año 1900): se recupera como días desde el 30/12/1899.
- `tpi_fecha` = `"Invalid Date"`: se trata como TP sin fecha.

### Tests

```
python -m unittest discover -s scripts/importadores -v
```

Usan datos inventados; no necesitan el export ni el backend.
