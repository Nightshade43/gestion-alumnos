# Guía visual

Capturas del prototipo `design/App.dc.html` (estado de demo `datos`, zoom 72% para que
entre la pantalla completa). Son **referencia de layout y contenido**, no assets a recortar:
la fuente definitiva sigue siendo el prototipo navegable, que además tiene los hovers, los
toasts y las transiciones de estado.

| Archivo | Pantalla | Qué mirar |
|---|---|---|
| `01-inicio.png` | Inicio | 4 KPI cards, "Requiere atención" con los tres tipos de aviso, "Últimas observaciones" |
| `02-personas.png` | Personas (listado) | tabla de 5 columnas, buscador, header de tabla en mono |
| `03-persona-ficha.png` | Persona (ficha) | avatar de iniciales, línea mono de contacto, filas con las **dos ramas** conviviendo |
| `04-inscripciones-escolares.png` | Inscripciones escolares | columnas Plan y Grupo, filtro de estado |
| `05-inscripcion-escolar-evaluaciones.png` | Inscripción (detalle escolar) | acciones de transición, meta card de 5 celdas, card por módulo con chip y promedio |
| `06-inscripciones-particulares.png` | Inscripciones particulares | columnas Contrato y Clases — no es la misma tabla que la escolar |
| `07-inscripcion-particular-contrato-seguimiento.png` | Inscripción (detalle particular) | dos columnas: contrato con `PoolBar` + seguimiento cronológico |
| `08-programas.png` | Programas | filtro segmentado, chip mono de estrategia |
| `09-programa-pestanias.png` | Programa (detalle) | pestañas; en Base **no hay pestaña Planes** |
| `10-instituciones.png` | Instituciones | filas colapsadas con avatar y resumen |
| `11-instituciones-desplegada.png` | Instituciones (abierta) | tarjetas de programa sobre `--ga-row-alt` |
| `12-contratos.png` | Contratos | grilla de tarjetas, barra roja del pool agotado |
| `13-contrato-detalle.png` | Contrato (modal 580px) | pool, empleados cubiertos, las 3 acciones con "Consumir clase" deshabilitado |
| `14-empresas.png` | Empresas | filas con contacto mono y contadores |
| `15-empresas-desplegada.png` | Empresas (abierta) | `PoolBar` de 220px + empleados cubiertos navegables |
| `16-login.png` | Login | columna de 376px, sin sidebar |

No están capturados (mejor verlos en el prototipo, son interacciones): los 6 modales de
alta, la confirmación irreversible, el modal 422, el toast 409 con acción, los estados
vacíos y el error de red. Se llegan con los tweaks **Estados de demo → vacio / error** y
con los botones de alta de cada pantalla.
