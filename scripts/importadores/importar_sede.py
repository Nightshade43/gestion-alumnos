#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Importador de CENMA Sede desde el export JSON de la app CENMA (solo librería estándar).

Capa 1: lee el export, normaliza los datos sucios y arma, para cada alumno, lo que se cargaría en
gestion-alumnos (persona, inscripción e instancias evaluativas).
Capa 2: pasada en seco contra la API (solo GET). Verifica que exista la estructura de CENMA Sede
(programa, módulos 1-7, planes y grupos que el export usa) y marca los alumnos cuyo DNI ya existe, que
se saltearían. No crea nada. Con --sin-api se queda en la capa 1.
La carga real (--confirmar) es la capa siguiente.

Uso, desde la raíz del repo y con el backend levantado:
  python scripts/importadores/importar_sede.py Datos/cenma_ingles_AAAA-MM-DD.json
Credenciales: --usuario/--clave, las variables GA_USUARIO/GA_CLAVE o un login.json con
{"username": ..., "password": ...} en la carpeta actual o en la raíz del repo (está en .gitignore).

El reporte (con nombres y DNI) queda en Datos/, que está en .gitignore. La consola no muestra datos
personales para que la salida se pueda copiar sin cuidado.

Mapeo y decisiones: claude/importador-sede-mapeo.md (proyecto) y scripts/importadores/README.md.
"""

import argparse
import http.client
import json
import os
import re
import sys
import unicodedata
import urllib.parse
from collections import Counter
from datetime import date, datetime, timedelta, timezone

# Windows (consola cp1252): nunca romper por un carácter que la consola no sabe mostrar.
try:
    sys.stdout.reconfigure(errors="replace")
except AttributeError:  # pragma: no cover
    pass

ESTADOS = {"aprobado", "pendiente", "en_curso", "eximido"}
INICIO_PLAN = {"A": 1, "B": 4, "C": 6}
# Plan que corresponde a "eximidos del 1 al k" (decisión de Lucas, 26/11/2026).
PLAN_POR_EXIMIDOS = {0: "A", 3: "B", 5: "C"}
NOTA_APROBACION = 6

# Alumnos sin ninguna fecha en el export: inicio del ciclo lectivo. Los activos están cursando este
# ciclo; los inactivos, el anterior (entre los que sí tienen fechas, la gran mayoría sigue ese patrón).
INICIO_SIN_FECHA_ACTIVO = date(2026, 3, 1)
INICIO_SIN_FECHA_INACTIVO = date(2025, 3, 1)

MESES = {m: i for i, m in enumerate("Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split(), 1)}
JS_DATE = re.compile(r"^\w{3} (\w{3}) (\d{2}) (\d{4}) \d{2}:\d{2}:\d{2} GMT")
ISO_UTC = re.compile(r"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(\.\d+)?Z$")
ARGENTINA = timezone(timedelta(hours=-3))
# Hora local de Córdoba en 1900 (LMT, -4:16:48): Sheets convierte un número a fecha con esa zona.
LMT_1900 = timezone(-timedelta(hours=4, minutes=16, seconds=48))
SERIAL_CERO = date(1899, 12, 30)
EPOCA = datetime(1970, 1, 1, tzinfo=timezone.utc)


# ---------------------------------------------------------------------------------------------
# Campos sueltos
# ---------------------------------------------------------------------------------------------

def parse_fecha(texto):
    """Fecha del export o None. Dos formatos: Date.toString() de JavaScript (siempre medianoche de
    Argentina, así que vale la fecha tal cual) e ISO en UTC (se pasa a hora argentina antes de tomar el día)."""
    if not texto:
        return None
    m = JS_DATE.match(texto)
    if m:
        return date(int(m.group(3)), MESES[m.group(1)], int(m.group(2)))
    m = ISO_UTC.match(texto)
    if m:
        utc = datetime.fromisoformat(m.group(1)).replace(tzinfo=timezone.utc)
        return utc.astimezone(ARGENTINA).date()
    return None


def parse_nota(valor):
    """(nota, aviso). Una nota fuera de 0–10 es casi siempre un número que Sheets guardó como fecha:
    llega en milisegundos desde 1970 y se recupera contando los días desde el 30/12/1899."""
    if valor is None:
        return None, None
    if 0 <= valor <= 10:
        return valor, None
    # Época + timedelta y no datetime.fromtimestamp: en Windows, fromtimestamp no acepta valores anteriores
    # a 1970 (OSError: Invalid argument), y estas notas caen en 1900.
    fecha_local = (EPOCA + timedelta(milliseconds=valor)).astimezone(LMT_1900).date()
    dias = (fecha_local - SERIAL_CERO).days
    if 0 <= dias <= 10:
        return dias, "nota guardada como fecha de Sheets (%s): se leyó %d" % (fecha_local.isoformat(), dias)
    return None, "nota irrecuperable (%r): no se importa" % valor


def normalizar_modulo(m):
    """Devuelve el módulo con campos limpios y la lista de avisos.

    Fila corrida: en algunas filas cada valor quedó una columna a la derecha. `estado` trae la fecha de
    la final (ISO), `eval_fecha` trae el estado y `tpi_fecha` trae la nota (o la fecha del TP)."""
    avisos = []
    if m.get("estado") in ESTADOS:
        estado, texto_final, texto_tp, nota_cruda = m["estado"], m["eval_fecha"], m["tpi_fecha"], m["nota"]
    else:
        avisos.append("fila corrida: se reconstruyó (estado <- eval_fecha, fecha final <- estado, nota <- tpi_fecha)")
        estado, texto_final = m.get("eval_fecha", ""), m.get("estado", "")
        texto_tp, nota_cruda = m.get("tpi_fecha", ""), m.get("nota")
        if texto_tp.isdigit():
            nota_cruda, texto_tp = int(texto_tp), ""
    if texto_tp == "Invalid Date":
        avisos.append('tpi_fecha "Invalid Date": se trata como TP sin fecha')
    nota, aviso_nota = parse_nota(nota_cruda)
    if aviso_nota:
        avisos.append(aviso_nota)
    return {
        "estado": estado,
        "nota": nota,
        "fecha_final": parse_fecha(texto_final),
        "fecha_tp": parse_fecha(texto_tp),
    }, avisos


def plan_de(alumno, modulos):
    """(plan o None, origen). Sin plan en el export, se infiere por los eximidos: tienen que ser
    consecutivos desde el 1 (ninguno → A, 1–3 → B, 1–5 → C). Cualquier otro patrón queda sin plan."""
    if alumno.get("plan") in INICIO_PLAN:
        return alumno["plan"], "export"
    eximidos = [orden for orden, m in enumerate(modulos, 1) if m["estado"] == "eximido"]
    k = max(eximidos) if eximidos else 0
    if eximidos == list(range(1, k + 1)) and k in PLAN_POR_EXIMIDOS:
        return PLAN_POR_EXIMIDOS[k], "inferido"
    return None, "sin plan"


# ---------------------------------------------------------------------------------------------
# Alumno completo
# ---------------------------------------------------------------------------------------------

def planificar_alumno(alumno):
    """Qué se cargaría para un alumno: persona, inscripción, instancias (en orden de carga) y avisos.

    Las instancias de cada módulo van con TP antes que final: el TP es el gate que habilita la final."""
    avisos = []
    modulos = []
    for orden, crudo in enumerate(alumno["modulos"], 1):
        m, avisos_modulo = normalizar_modulo(crudo)
        modulos.append(m)
        avisos += ["módulo %d: %s" % (orden, a) for a in avisos_modulo]

    plan, origen_plan = plan_de(alumno, modulos)
    if origen_plan == "inferido":
        avisos.append("plan %s inferido por los módulos eximidos" % plan)
    elif plan is None:
        avisos.append("REVISAR: sin plan y eximidos que no corresponden a A, B ni C; se importa sin plan (se exigen todos)")
    inicio = INICIO_PLAN.get(plan)

    instancias = []
    for orden, m in enumerate(modulos, 1):
        estado = m["estado"]
        if estado not in ESTADOS:
            avisos.append("REVISAR: módulo %d con estado irreconocible (%r): no se importa" % (orden, estado))
            continue
        if estado == "eximido":
            if inicio is not None and orden >= inicio:
                avisos.append("REVISAR: módulo %d marcado eximido pero el plan %s lo exige; se ignora la marca" % (orden, plan))
            continue

        tiene_final = m["nota"] is not None and m["fecha_final"] is not None
        if m["fecha_tp"]:
            instancias.append({"modulo": orden, "tipo": "TP_INTEGRADOR", "aprobado": True, "fecha": m["fecha_tp"]})
        elif tiene_final and estado == "aprobado":
            instancias.append({"modulo": orden, "tipo": "TP_INTEGRADOR", "aprobado": True,
                               "fecha": m["fecha_final"], "inferido": True})
        elif tiene_final:
            avisos.append("REVISAR: módulo %d con final pero sin TP ni estado aprobado: no se crea TP" % orden)

        if tiene_final:
            instancias.append({"modulo": orden, "tipo": "EVALUACION_FINAL", "nota": m["nota"], "fecha": m["fecha_final"]})
            if m["nota"] >= NOTA_APROBACION and estado != "aprobado":
                avisos.append("REVISAR: módulo %d con final %d pero estado '%s' en el export" % (orden, m["nota"], estado))
            if m["nota"] < NOTA_APROBACION and estado == "aprobado":
                avisos.append("REVISAR: módulo %d 'aprobado' en el export con final %d" % (orden, m["nota"]))
        elif estado == "aprobado":
            avisos.append("REVISAR: módulo %d 'aprobado' sin nota o sin fecha de final: no se crea la final" % orden)

    fechas = [i["fecha"] for i in instancias]
    if fechas:
        fecha_inicio = min(fechas)
    else:
        fecha_inicio = INICIO_SIN_FECHA_ACTIVO if alumno.get("activo") else INICIO_SIN_FECHA_INACTIVO
        avisos.append("sin fechas en el export: fechaInicio = %s (inicio del ciclo lectivo)" % fecha_inicio.isoformat())

    if alumno.get("egresado"):
        estado_inscripcion = "ACTIVA"
        avisos.append("REVISAR: marcado como egresado; se importa ACTIVA para decidir a mano")
    elif alumno.get("activo"):
        estado_inscripcion = "ACTIVA"
    else:
        estado_inscripcion = "PAUSADA"

    return {
        "id_export": alumno["id"],
        "nombre": " ".join(alumno["nombre"].split()),
        "dni": alumno["dni"].strip(),
        "grupo": alumno["grupo"],
        "plan": plan,
        "origen_plan": origen_plan,
        "fecha_inicio": fecha_inicio,
        "estado_inscripcion": estado_inscripcion,
        "instancias": instancias,
        "avisos": avisos,
        "estados_export": [m["estado"] for m in modulos],
    }


def planificar(export):
    """Planifica todos los alumnos. Un DNI repetido en el export se importa una sola vez (el primero):
    en gestion-alumnos el documento es único."""
    vistos = {}
    alumnos, repetidos = [], []
    for alumno in export["alumnos"]:
        if alumno.get("eliminado"):
            continue
        p = planificar_alumno(alumno)
        if p["dni"] in vistos:
            repetidos.append((p, vistos[p["dni"]]))
            continue
        vistos[p["dni"]] = p
        alumnos.append(p)
    return alumnos, repetidos


# ---------------------------------------------------------------------------------------------
# API (capa 2: solo lectura)
# ---------------------------------------------------------------------------------------------

RAIZ_REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))


class ErrorApi(Exception):
    pass


class Api:
    """Cliente HTTP mínimo, como el del benchmark: una conexión, JSON de ida y vuelta, token JWT."""

    def __init__(self, base):
        u = urllib.parse.urlparse(base)
        self.host, self.port = u.hostname or "localhost", u.port or 80
        self.token = None

    def enviar(self, metodo, ruta, cuerpo=None):
        cabeceras = {"Accept": "application/json"}
        datos = None
        if cuerpo is not None:
            datos = json.dumps(cuerpo).encode("utf-8")
            cabeceras["Content-Type"] = "application/json"
        if self.token:
            cabeceras["Authorization"] = "Bearer " + self.token
        conn = http.client.HTTPConnection(self.host, self.port, timeout=60)
        try:
            conn.request(metodo, ruta, body=datos, headers=cabeceras)
            resp = conn.getresponse()
            crudo = resp.read()
        except OSError as e:
            raise ErrorApi("No se pudo conectar con el backend en %s:%s (%s). ¿Está levantado? "
                           "Para analizar el export sin API: --sin-api." % (self.host, self.port, e))
        finally:
            conn.close()
        if not 200 <= resp.status < 300:
            raise ErrorApi("%s %s -> %s: %s" % (metodo, ruta, resp.status, crudo[:300].decode("utf-8", "replace")))
        return json.loads(crudo) if crudo else None


def ubicar(nombre):
    for carpeta in (os.getcwd(), RAIZ_REPO):
        candidato = os.path.join(carpeta, nombre)
        if os.path.exists(candidato):
            return candidato
    return None


def autenticar(api, args):
    usuario = args.usuario or os.environ.get("GA_USUARIO")
    clave = args.clave or os.environ.get("GA_CLAVE")
    archivo = args.credenciales or ubicar("login.json")
    if (not usuario or not clave) and archivo:
        with open(archivo, encoding="utf-8-sig") as f:
            cred = json.load(f)
        usuario, clave = usuario or cred.get("username"), clave or cred.get("password")
    if not usuario or not clave:
        raise ErrorApi("Faltan credenciales: --usuario/--clave, GA_USUARIO/GA_CLAVE o un login.json "
                       "con {\"username\": ..., \"password\": ...}.")
    api.token = api.enviar("POST", "/api/auth/login", {"username": usuario, "password": clave})["token"]


def sin_acentos(texto):
    """'Miércoles' y 'miercoles' tienen que encontrar el mismo grupo."""
    return "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn").strip().lower()


def verificar_estructura(api, alumnos, programa_id=None):
    """Busca en gestion-alumnos lo que la importación necesita, sin crear nada.

    Devuelve (estructura, problemas). `estructura` tiene los ids a usar en la carga; `problemas` lo que
    falta o no coincide. Solo exige los planes y grupos que el export realmente usa."""
    problemas = []
    estructura = {"programa": None, "modulos": {}, "planes": {}, "grupos": {}}

    sede = [p for p in api.enviar("GET", "/api/programas") if p["estrategiaEvaluacion"] == "CENMA_SEDE"]
    if programa_id is not None:
        sede = [p for p in sede if p["id"] == programa_id]
        if not sede:
            return estructura, ["no hay un programa CENMA_SEDE con id %d" % programa_id]
    elif not sede:
        return estructura, ["no hay ningún programa con estrategia CENMA_SEDE: crealo desde Programas"]
    elif len(sede) > 1:
        return estructura, ["hay %d programas CENMA_SEDE (ids %s): elegí uno con --programa-id"
                            % (len(sede), ", ".join(str(p["id"]) for p in sede))]
    programa = sede[0]
    estructura["programa"] = programa
    pid = programa["id"]

    ordenes = {m["orden"]: m for m in api.enviar("GET", "/api/modulos?programaId=%d" % pid)}
    necesarios = sorted({i["modulo"] for a in alumnos for i in a["instancias"]} | set(range(1, 8)))
    for orden in necesarios:
        if orden in ordenes:
            estructura["modulos"][orden] = ordenes[orden]["id"]
        else:
            problemas.append("falta el módulo %d en el programa '%s'" % (orden, programa["nombre"]))

    planes = {p["codigo"].strip().upper(): p for p in api.enviar("GET", "/api/planes?programaId=%d" % pid)}
    for codigo in sorted({a["plan"] for a in alumnos if a["plan"]}):
        plan = planes.get(codigo)
        if plan is None:
            problemas.append("falta el plan %s (inicia en el módulo %d)" % (codigo, INICIO_PLAN[codigo]))
        elif plan["moduloInicio"] != INICIO_PLAN[codigo]:
            problemas.append("el plan %s inicia en el módulo %d y debería iniciar en el %d"
                             % (codigo, plan["moduloInicio"], INICIO_PLAN[codigo]))
        else:
            estructura["planes"][codigo] = plan["id"]

    grupos = {sin_acentos(g["dia"]): g for g in api.enviar("GET", "/api/grupos?programaId=%d" % pid)}
    for dia in sorted({a["grupo"] for a in alumnos}):
        grupo = grupos.get(sin_acentos(dia))
        if grupo is None:
            problemas.append("falta el grupo '%s'" % dia)
        else:
            estructura["grupos"][dia] = grupo["id"]

    return estructura, problemas


def marcar_existentes(api, alumnos):
    """Marca los alumnos cuyo DNI ya está en gestion-alumnos: se saltean (así una segunda corrida no duplica)."""
    existentes = {(p.get("documento") or "").strip() for p in api.enviar("GET", "/api/personas")}
    for a in alumnos:
        a["ya_existe"] = a["dni"] in existentes
    return sum(a["ya_existe"] for a in alumnos)


# ---------------------------------------------------------------------------------------------
# Salida
# ---------------------------------------------------------------------------------------------

def resumen(alumnos, repetidos):
    c = Counter()
    for a in alumnos:
        if a.get("ya_existe"):
            c["ya existen en gestion-alumnos (se saltean)"] += 1
            continue
        c["alumnos"] += 1
        c["inscripción " + a["estado_inscripcion"]] += 1
        c["plan %s (%s)" % (a["plan"] or "-", a["origen_plan"])] += 1
        for i in a["instancias"]:
            c[i["tipo"] + (" inferido" if i.get("inferido") else "")] += 1
        if any(x.startswith("REVISAR") for x in a["avisos"]):
            c["alumnos con algo para REVISAR"] += 1
    c["DNI repetidos (no se importan)"] = len(repetidos)
    return c


def escribir_reporte(ruta, origen, alumnos, repetidos, verificacion=None):
    with open(ruta, "w", encoding="utf-8") as f:
        f.write("Importador CENMA Sede, %s\nExport: %s\nGenerado: %s\n\n"
                % ("pasada en seco contra la API (no se creó nada)" if verificacion else "análisis sin API",
                   origen, datetime.now().isoformat(timespec="seconds")))
        if verificacion:
            estructura, problemas = verificacion
            f.write("== Estructura en gestion-alumnos ==\n")
            if estructura["programa"]:
                f.write("Programa: %s (id %d)\nMódulos: %s\nPlanes: %s\nGrupos: %s\n"
                        % (estructura["programa"]["nombre"], estructura["programa"]["id"],
                           estructura["modulos"], estructura["planes"], estructura["grupos"]))
            for problema in problemas:
                f.write("FALTA: %s\n" % problema)
            f.write("\n")
        f.write("== Resumen ==\n")
        for k, v in sorted(resumen(alumnos, repetidos).items()):
            f.write("%5d  %s\n" % (v, k))
        f.write("\n== DNI repetidos (se importa el primero) ==\n")
        for p, original in repetidos:
            f.write("- DNI %s: '%s' (%s) repite a '%s' (%s)\n"
                    % (p["dni"], p["nombre"], p["id_export"], original["nombre"], original["id_export"]))
        f.write("\n== Alumnos ==\n")
        for a in alumnos:
            if a.get("ya_existe"):
                f.write("\n%s | DNI %s | YA EXISTE en gestion-alumnos: se saltea | id %s\n"
                        % (a["nombre"], a["dni"], a["id_export"]))
                continue
            f.write("\n%s | DNI %s | %s | plan %s (%s) | inicio %s | %s | id %s\n"
                    % (a["nombre"], a["dni"], a["grupo"], a["plan"] or "-", a["origen_plan"],
                       a["fecha_inicio"].isoformat(), a["estado_inscripcion"], a["id_export"]))
            f.write("  export: %s\n" % " ".join(a["estados_export"]))
            for i in a["instancias"]:
                detalle = "nota %d" % i["nota"] if i["tipo"] == "EVALUACION_FINAL" else "aprobado"
                f.write("  M%d %-16s %s %s%s\n" % (i["modulo"], i["tipo"], i["fecha"].isoformat(), detalle,
                                                  " (inferido)" if i.get("inferido") else ""))
            for aviso in a["avisos"]:
                f.write("  ! %s\n" % aviso)


def parsear(argv=None):
    p = argparse.ArgumentParser(description="Importador de CENMA Sede (pasada en seco: no crea nada).")
    p.add_argument("export", help="JSON exportado de la app CENMA (va en Datos/, que no se versiona)")
    p.add_argument("--reporte", help="ruta del reporte (def. junto al export: reporte-sede-AAAAMMDD-HHMMSS.txt)")
    p.add_argument("--sin-api", action="store_true", help="solo analizar el export, sin conectarse al backend")
    p.add_argument("--base", default="http://localhost:8080", help="URL del backend (def. http://localhost:8080)")
    p.add_argument("--programa-id", type=int, help="id del programa CENMA_SEDE si hay más de uno")
    p.add_argument("--usuario")
    p.add_argument("--clave")
    p.add_argument("--credenciales", help="JSON {username, password} (def. login.json en la carpeta actual o la raíz del repo)")
    return p.parse_args(argv)


def main(argv=None):
    a = parsear(argv)
    with open(a.export, encoding="utf-8-sig") as f:
        export = json.load(f)
    alumnos, repetidos = planificar(export)

    verificacion = None
    if not a.sin_api:
        api = Api(a.base)
        try:
            autenticar(api, a)
            verificacion = verificar_estructura(api, alumnos, a.programa_id)
            marcar_existentes(api, alumnos)
        except ErrorApi as e:
            print("ERROR: %s" % e)
            return 1

    ruta = a.reporte or os.path.join(os.path.dirname(os.path.abspath(a.export)),
                                     "reporte-sede-%s.txt" % datetime.now().strftime("%Y%m%d-%H%M%S"))
    escribir_reporte(ruta, a.export, alumnos, repetidos, verificacion)
    for k, v in sorted(resumen(alumnos, repetidos).items()):
        print("%5d  %s" % (v, k))
    if verificacion:
        estructura, problemas = verificacion
        print("\nEstructura en gestion-alumnos: %s" % ("OK" if not problemas else "%d problema(s)" % len(problemas)))
        for problema in problemas:
            print("  FALTA: %s" % problema)
        print("Pasada en seco: no se creó nada.")
    print("\nReporte con el detalle por alumno: %s" % ruta)
    return 2 if verificacion and verificacion[1] else 0


if __name__ == "__main__":
    sys.exit(main())
