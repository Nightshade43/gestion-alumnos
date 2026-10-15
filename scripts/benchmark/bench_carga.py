#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Carga sintética y medición de los listados del backend (sin dependencias: solo la librería estándar).

Qué hace
  1. Verifica que la base esté VACÍA (protección: esto crea miles de registros y la API casi no tiene bajas).
  2. Crea la estructura (institución, programas Base / Sede / Particular, módulos, planes, grupos, empresas).
  3. Siembra alumnos hasta cada hito (por defecto 50, 150 y 300) con notas, gates de Sede, contratos y
     seguimientos según las reglas reales del dominio.
  4. En cada hito mide los endpoints que usan las pantallas: tiempos en caliente (mediana, p95, máximo),
     tamaño de la respuesta y, si hay bench.log, la cantidad de sentencias SQL por request.
  5. Informa si conviene optimizar ahora o diferir, y cuándo se cruzaría el umbral (extrapolación lineal).

Con --solo-medir no crea nada (solo hace GET): sirve para medir la base REAL cuando ya tenga los 150 alumnos.

Uso típico (ver README.md): backend con SPRING_PROFILES_ACTIVE=local,bench y, desde la raíz del repo,
  python scripts/benchmark/bench_carga.py
"""

import argparse
import http.client
import json
import math
import os
import random
import re
import socket
import statistics
import sys
import time
from datetime import date, timedelta
from urllib.parse import urlparse

PREFIJO = "BENCH"

# Windows (consola cp1252): nunca romper por un carácter que la consola no sabe mostrar.
try:
    sys.stdout.reconfigure(errors="replace")
except AttributeError:  # pragma: no cover
    pass


class ErrorApi(Exception):
    pass


# ---------------------------------------------------------------------------------------------
# Cliente HTTP mínimo con conexión persistente (como un navegador)
# ---------------------------------------------------------------------------------------------

class Api:
    def __init__(self, base):
        u = urlparse(base)
        self.host = u.hostname or "localhost"
        self.port = u.port or 80
        self.token = None
        self.conn = None
        self.requests = 0

    def _conectar(self):
        self.conn = http.client.HTTPConnection(self.host, self.port, timeout=120)
        self.conn.connect()
        # Sin Nagle: http.client manda cabeceras y cuerpo por separado y, con Nagle, cada POST puede esperar ~40 ms.
        self.conn.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

    def request(self, metodo, ruta, cuerpo=None):
        """Devuelve (estado, bytes, milisegundos). Reintenta una vez si el servidor cerró la conexión."""
        cabeceras = {"Accept": "application/json"}
        datos = None
        if cuerpo is not None:
            datos = json.dumps(cuerpo).encode("utf-8")
            cabeceras["Content-Type"] = "application/json"
        if self.token:
            cabeceras["Authorization"] = "Bearer " + self.token
        for intento in (1, 2):
            if self.conn is None:
                self._conectar()
            try:
                t0 = time.perf_counter()
                self.conn.request(metodo, ruta, body=datos, headers=cabeceras)
                resp = self.conn.getresponse()
                crudo = resp.read()
                ms = (time.perf_counter() - t0) * 1000
                break
            except (http.client.HTTPException, OSError):
                self.conn = None
                if intento == 2:
                    raise
        self.requests += 1
        return resp.status, crudo, ms

    def enviar(self, metodo, ruta, cuerpo=None, esperado=(200, 201)):
        estado, crudo, _ = self.request(metodo, ruta, cuerpo)
        if estado not in esperado:
            detalle = crudo[:300].decode("utf-8", "replace")
            raise ErrorApi("%s %s -> %s: %s" % (metodo, ruta, estado, detalle))
        return json.loads(crudo) if crudo else None


def autenticar(api, args):
    usuario = args.usuario or os.environ.get("BENCH_USER")
    clave = args.clave or os.environ.get("BENCH_PASS")
    if (not usuario or not clave) and os.path.exists(args.credenciales):
        with open(args.credenciales, encoding="utf-8-sig") as f:
            cred = json.load(f)
        usuario = usuario or cred.get("username")
        clave = clave or cred.get("password")
    if not usuario or not clave:
        raise ErrorApi("Faltan credenciales: usar --usuario/--clave, BENCH_USER/BENCH_PASS o un login.json "
                       "con {\"username\": ..., \"password\": ...}.")
    respuesta = api.enviar("POST", "/api/auth/login", {"username": usuario, "password": clave})
    api.token = respuesta["token"]


# ---------------------------------------------------------------------------------------------
# Siembra
# ---------------------------------------------------------------------------------------------

def verificar_base_vacia(api):
    for ruta in ("/api/personas", "/api/programas"):
        if api.enviar("GET", ruta):
            raise ErrorApi(
                "La base NO esta vacia (%s devolvio datos). Este script crea miles de registros y la API casi "
                "no tiene bajas: usalo solo contra la base de benchmark (perfil 'bench', puerto 8081). Para medir "
                "una base con datos sin crear nada: --solo-medir." % ruta)


def crear_estructura(api, a):
    def programa(nombre, categoria, estrategia, institucion_id):
        cuerpo = {"nombre": nombre, "categoria": categoria, "estrategiaEvaluacion": estrategia}
        if institucion_id:
            cuerpo["institucionId"] = institucion_id
        return api.enviar("POST", "/api/programas", cuerpo)["id"]

    def modulos(programa_id, cantidad, secuencial):
        return [api.enviar("POST", "/api/modulos",
                           {"programaId": programa_id, "orden": orden, "esSecuencial": secuencial})["id"]
                for orden in range(1, cantidad + 1)]

    def grupos(programa_id, definiciones):
        return [api.enviar("POST", "/api/grupos",
                           {"programaId": programa_id, "dia": dia, "horario": horario})["id"]
                for dia, horario in definiciones]

    institucion = api.enviar("POST", "/api/instituciones", {"nombre": PREFIJO + " CENMA"})["id"]

    base = programa(PREFIJO + " Ingles Base", "ESCOLAR", "CENMA_BASE", institucion)
    sede = programa(PREFIJO + " Ingles Sede", "ESCOLAR", "CENMA_SEDE", institucion)
    particular = programa(PREFIJO + " Ingles Particular", "PARTICULAR", "SEGUIMIENTO_LIBRE", None)

    mitad = a.modulos_sede // 2 + 1
    planes = []
    for codigo, inicio in (("A", 1), ("B", mitad)):
        plan_id = api.enviar("POST", "/api/planes",
                             {"programaId": sede, "codigo": codigo, "moduloInicio": inicio})["id"]
        planes.append({"id": plan_id, "inicio": inicio})

    empresas = [api.enviar("POST", "/api/empresas",
                           {"nombre": "%s Empresa %02d" % (PREFIJO, i), "contacto": "bench@example.com"})["id"]
                for i in range(1, a.empresas + 1)]

    return {
        "base": {"id": base, "modulos": modulos(base, a.modulos_base, False),
                 "grupos": grupos(base, [("Lunes", "18:00"), ("Miercoles", "20:00")])},
        "sede": {"id": sede, "modulos": modulos(sede, a.modulos_sede, True), "planes": planes,
                 "grupos": grupos(sede, [("Martes", "09:00"), ("Sabado", "09:00")])},
        "particular": {"id": particular},
        "empresas": empresas,
    }


class Sembrador:
    """Siembra alumnos de forma incremental y reproducible (misma --semilla, mismos datos)."""

    def __init__(self, api, est, a):
        self.api = api
        self.est = est
        self.a = a
        self.rng = random.Random(a.semilla)
        self.sembrados = 0
        self.instancias = {}          # inscripcionId -> cantidad de instancias evaluativas cargadas
        self.escolares = []           # ids de inscripciones escolares
        self.empresa_siguiente = 0
        # Patrón de 20 plazas con la proporción pedida, barajado una vez: el reparto es estable entre hitos.
        sede = round(20 * a.pct_sede / 100)
        base = round(20 * a.pct_base / 100)
        self.plazas = ["sede"] * sede + ["base"] * base + ["particular"] * max(0, 20 - sede - base)
        self.rng.shuffle(self.plazas)
        self.dia = 0

    # -- utilidades -------------------------------------------------------------------------
    def nota(self):
        return round(self.rng.uniform(6, 10), 2)

    def fecha(self):
        self.dia += 1
        return (date(2026, 3, 2) + timedelta(days=self.dia % 200)).isoformat()

    def persona(self):
        n = self.sembrados + 1
        return self.api.enviar("POST", "/api/personas", {
            "nombre": "%s Alumno %04d" % (PREFIJO, n),
            "documento": "%s%06d" % (PREFIJO, n),
            "telefono": "351-555-%04d" % n,
        })["id"]

    def inscribir(self, persona_id, programa_id, plan_id=None, grupo_id=None):
        cuerpo = {"personaId": persona_id, "programaId": programa_id, "fechaInicio": "2026-03-02"}
        if plan_id:
            cuerpo["planId"] = plan_id
        if grupo_id:
            cuerpo["grupoId"] = grupo_id
        return self.api.enviar("POST", "/api/inscripciones", cuerpo)["id"]

    def instancia(self, inscripcion_id, modulo_id, tipo, nota=None, aprobado=None):
        cuerpo = {"inscripcionId": inscripcion_id, "moduloId": modulo_id, "tipo": tipo, "fecha": self.fecha()}
        if nota is not None:
            cuerpo["nota"] = nota
        if aprobado is not None:
            cuerpo["aprobado"] = aprobado
        self.api.enviar("POST", "/api/instancias-evaluativas", cuerpo)
        self.instancias[inscripcion_id] = self.instancias.get(inscripcion_id, 0) + 1

    # -- tipos de alumno --------------------------------------------------------------------
    def alumno_base(self):
        e = self.est["base"]
        insc = self.inscribir(self.persona(), e["id"], grupo_id=self.rng.choice(e["grupos"]))
        self.escolares.append(insc)
        mods = e["modulos"]
        hechos = len(mods) if self.a.peor_caso else self.rng.randint(0, len(mods))
        for mid in mods[:hechos]:
            for _ in range(3):                                   # minimo 3 notas que promedian
                self.instancia(insc, mid, "NOTA", nota=self.nota())
            self.instancia(insc, mid, "INTEGRADOR", nota=self.nota())   # no promedia
        if hechos < len(mods):                                   # modulo en curso
            for _ in range(self.rng.randint(0, 2)):
                self.instancia(insc, mods[hechos], "NOTA", nota=self.nota())

    def alumno_sede(self):
        e = self.est["sede"]
        plan = self.rng.choice(e["planes"])
        insc = self.inscribir(self.persona(), e["id"], plan_id=plan["id"], grupo_id=self.rng.choice(e["grupos"]))
        self.escolares.append(insc)
        requeridos = e["modulos"][plan["inicio"] - 1:]
        hechos = len(requeridos) if self.a.peor_caso else self.rng.randint(0, len(requeridos))
        for mid in requeridos[:hechos]:
            if self.rng.random() < 0.2:                          # un TP desaprobado antes del aprobado
                self.instancia(insc, mid, "TP_INTEGRADOR", aprobado=False)
            self.instancia(insc, mid, "TP_INTEGRADOR", aprobado=True)
            self.instancia(insc, mid, "EVALUACION_FINAL", nota=self.nota())
        if hechos < len(requeridos) and self.rng.random() < 0.5:  # modulo en curso con el gate ya aprobado
            self.instancia(insc, requeridos[hechos], "TP_INTEGRADOR", aprobado=True)

    def alumno_particular(self, pendientes_empresa):
        insc = self.inscribir(self.persona(), self.est["particular"]["id"])
        for _ in range(self.rng.randint(0, 5)):
            self.api.enviar("POST", "/api/seguimientos", {
                "inscripcionId": insc, "fecha": self.fecha(),
                "observacion": "%s observacion de seguimiento de la clase" % PREFIJO})
        r = self.rng.random()
        if r < 0.5:                                              # contrato individual
            self.api.enviar("POST", "/api/contratos", {
                "inscripcionId": insc, "tipoFacturacion": "PAQUETE", "clasesContratadas": 10})
        elif r < 0.8:                                            # va a un contrato de empresa
            pendientes_empresa.append(insc)
        # el resto queda "sin contrato" (genera avisos en Inicio)

    def contratos_de_empresa(self, pendientes):
        for i in range(0, len(pendientes), 4):
            grupo = pendientes[i:i + 4]
            empresa = self.est["empresas"][self.empresa_siguiente % len(self.est["empresas"])]
            self.empresa_siguiente += 1
            self.api.enviar("POST", "/api/contratos/empresa", {
                "empresaId": empresa, "tipoFacturacion": "PAQUETE",
                "clasesContratadas": 40, "inscripcionIds": grupo})

    def hasta(self, total):
        pendientes_empresa = []
        while self.sembrados < total:
            tipo = self.plazas[self.sembrados % len(self.plazas)]
            if tipo == "sede":
                self.alumno_sede()
            elif tipo == "base":
                self.alumno_base()
            else:
                self.alumno_particular(pendientes_empresa)
            self.sembrados += 1
        self.contratos_de_empresa(pendientes_empresa)

    def inscripcion_mas_cargada(self):
        if not self.escolares:
            return None
        return max(self.escolares, key=lambda i: self.instancias.get(i, 0))


# ---------------------------------------------------------------------------------------------
# Medición
# ---------------------------------------------------------------------------------------------

def sentencias_sql(ruta_log, ejecutar):
    """Ejecuta una request y lee de bench.log cuantas sentencias JDBC contó Hibernate (None si no hay log)."""
    if not ruta_log or not os.path.exists(ruta_log):
        return None
    inicio = os.path.getsize(ruta_log)
    ejecutar()
    time.sleep(0.6)  # la sesion se cierra (y se loguea) al terminar la request
    with open(ruta_log, "rb") as f:
        f.seek(inicio)
        texto = f.read().decode("utf-8", "replace")
    cantidades = [int(x) for x in re.findall(r"executing (\d+) JDBC statements", texto)]
    return max(cantidades) if cantidades else None


def percentil(valores, p):
    ordenados = sorted(valores)
    return ordenados[min(len(ordenados) - 1, math.ceil(p * len(ordenados)) - 1)]


def medir_endpoint(api, a, nombre, ruta, inicio=False):
    estado, crudo, frio = api.request("GET", ruta)
    if estado != 200:
        return {"nombre": nombre, "ruta": ruta, "error": "HTTP %s" % estado, "inicio": inicio}
    tiempos = []
    for _ in range(a.repeticiones):
        estado, crudo, ms = api.request("GET", ruta)
        tiempos.append(ms)
    cuerpo = json.loads(crudo) if crudo else None
    return {
        "nombre": nombre, "ruta": ruta, "inicio": inicio,
        "elementos": len(cuerpo) if isinstance(cuerpo, list) else 1,
        "kb": len(crudo) / 1024.0,
        "frio": frio,
        "mediana": statistics.median(tiempos),
        "p95": percentil(tiempos, 0.95),
        "maximo": max(tiempos),
        "sql": sentencias_sql(a.log, lambda: api.request("GET", ruta)),
    }


def medir(api, a, inscripcion_detalle):
    endpoints = [
        ("Inscripciones (todas)        [Inicio]", "/api/inscripciones", True),
        ("Contratos ACTIVO             [Inicio]", "/api/contratos?estado=ACTIVO", True),
        ("Seguimientos recientes (4)   [Inicio]", "/api/seguimientos?limit=4", True),
        ("Inscripciones ESCOLAR", "/api/inscripciones?categoria=ESCOLAR", False),
        ("Inscripciones PARTICULAR", "/api/inscripciones?categoria=PARTICULAR", False),
        ("Programas (con contadores)", "/api/programas", False),
        ("Personas", "/api/personas", False),
        ("Empresas", "/api/empresas", False),
    ]
    if inscripcion_detalle:
        endpoints += [
            ("Detalle de inscripcion cargada", "/api/inscripciones/%s" % inscripcion_detalle, False),
            ("Instancias de esa inscripcion", "/api/instancias-evaluativas?inscripcionId=%s" % inscripcion_detalle, False),
        ]
    return [medir_endpoint(api, a, n, r, i) for n, r, i in endpoints]


def total_inicio(filas):
    inicio = [f for f in filas if f.get("inicio") and "error" not in f]
    return {
        "mediana": sum(f["mediana"] for f in inicio),
        "p95": sum(f["p95"] for f in inicio),
        "sql": sum(f["sql"] for f in inicio) if inicio and all(f["sql"] is not None for f in inicio) else None,
    }


def imprimir_tabla(titulo, filas):
    print()
    print(titulo)
    print("-" * len(titulo))
    cab = "%-38s %9s %8s %9s %9s %9s %9s %8s" % (
        "Request", "elementos", "KB", "frio ms", "mediana", "p95", "maximo", "SQL")
    print(cab)
    for f in filas:
        if "error" in f:
            print("%-38s %s" % (f["nombre"], f["error"]))
            continue
        sql = "-" if f["sql"] is None else str(f["sql"])
        print("%-38s %9d %8.1f %9.1f %9.1f %9.1f %9.1f %8s" % (
            f["nombre"], f["elementos"], f["kb"], f["frio"], f["mediana"], f["p95"], f["maximo"], sql))
    t = total_inicio(filas)
    sql = "-" if t["sql"] is None else str(t["sql"])
    print("%-38s %9s %8s %9s %9.1f %9.1f %9s %8s" % (
        "TOTAL Inicio (suma de sus 3)", "", "", "", t["mediana"], t["p95"], "", sql))


def veredicto(resultados, a):
    hitos = sorted(resultados)
    objetivo = min(hitos, key=lambda h: abs(h - a.objetivo))
    t = total_inicio(resultados[objetivo])
    print()
    print("=" * 72)
    print("VEREDICTO (con %d alumnos, umbral %d ms sobre la suma de Inicio)" % (objetivo, a.umbral_ms))
    print("=" * 72)
    print("Inicio, mediana: %.0f ms   p95: %.0f ms" % (t["mediana"], t["p95"]))
    if t["mediana"] <= a.umbral_ms:
        print("-> DIFERIR la optimizacion: con %d alumnos queda por debajo del umbral." % objetivo)
    else:
        print("-> OPTIMIZAR ahora: con %d alumnos supera el umbral." % objetivo)
    if len(hitos) >= 2:
        h0, h1 = hitos[0], hitos[-1]
        t0, t1 = total_inicio(resultados[h0])["mediana"], total_inicio(resultados[h1])["mediana"]
        pendiente = (t1 - t0) / float(h1 - h0)
        print("Crecimiento medido entre %d y %d alumnos: %.2f ms por alumno." % (h0, h1, pendiente))
        if t1 >= a.umbral_ms:
            print("El umbral ya se supera dentro del rango medido.")
        elif pendiente <= 0:
            print("Sin crecimiento apreciable entre hitos: el tiempo no depende (todavia) de la cantidad de alumnos.")
        else:
            cruce = h1 + (a.umbral_ms - t1) / pendiente
            if cruce > 20 * h1:
                print("Extrapolacion lineal: el umbral quedaria recien por encima de los %d alumnos." % (20 * h1))
            else:
                print("Extrapolacion lineal: el umbral se cruzaria cerca de los %d alumnos." % round(cruce))
        s0, s1 = total_inicio(resultados[h0])["sql"], total_inicio(resultados[h1])["sql"]
        if s0 is not None and s1 is not None and h1 > h0:
            print("Consultas SQL de Inicio: %d -> %d (%.1f por alumno)." % (s0, s1, (s1 - s0) / float(h1 - h0)))
    print("Nota: es una extrapolacion lineal; si las consultas crecen peor que lineal (p. ej. N+1 anidado) "
          "el cruce llega antes. Confirmar con un hito mayor (--hitos).")


# ---------------------------------------------------------------------------------------------
# Programa principal
# ---------------------------------------------------------------------------------------------

def parsear():
    p = argparse.ArgumentParser(description="Carga sintetica y medicion de listados (ver README.md).")
    p.add_argument("--base", default="http://localhost:8081", help="URL del backend de benchmark (def. 8081)")
    p.add_argument("--usuario"); p.add_argument("--clave")
    p.add_argument("--credenciales", default="login.json", help="JSON {username, password} (def. login.json)")
    p.add_argument("--hitos", default="50,150,300", help="cantidades acumuladas de alumnos donde medir")
    p.add_argument("--objetivo", type=int, default=150, help="hito sobre el que se emite el veredicto")
    p.add_argument("--umbral-ms", type=int, default=500, help="umbral sobre la suma de las 3 requests de Inicio")
    p.add_argument("--repeticiones", type=int, default=10, help="mediciones en caliente por request")
    p.add_argument("--pct-sede", type=int, default=60); p.add_argument("--pct-base", type=int, default=25)
    p.add_argument("--modulos-base", type=int, default=6); p.add_argument("--modulos-sede", type=int, default=6)
    p.add_argument("--empresas", type=int, default=5)
    p.add_argument("--peor-caso", action="store_true",
                   help="todos los alumnos escolares con todos los modulos evaluados")
    p.add_argument("--semilla", type=int, default=42)
    p.add_argument("--log", default="bench.log", help="log del backend para contar SQL (def. bench.log)")
    p.add_argument("--salida", help="guardar los resultados en un JSON")
    p.add_argument("--solo-medir", action="store_true", help="no crea nada: solo mide la base actual")
    a = p.parse_args()
    if a.pct_sede + a.pct_base > 100:
        p.error("--pct-sede + --pct-base no puede superar 100")
    return a


def main():
    a = parsear()
    api = Api(a.base)
    try:
        autenticar(api, a)
        resultados = {}
        sembrador = None
        if a.solo_medir:
            lista = api.enviar("GET", "/api/inscripciones?categoria=ESCOLAR")
            detalle = lista[0]["id"] if lista else None
            filas = medir(api, a, detalle)
            n = len(api.enviar("GET", "/api/personas"))
            imprimir_tabla("Base actual (%d personas)" % n, filas)
            resultados[n] = filas
            a.objetivo = n
        else:
            verificar_base_vacia(api)
            t0 = time.perf_counter()
            estructura = crear_estructura(api, a)
            sembrador = Sembrador(api, estructura, a)
            print("Estructura creada en %.1f s." % (time.perf_counter() - t0))
            for hito in sorted(int(h) for h in a.hitos.split(",")):
                t0 = time.perf_counter()
                antes = api.requests
                sembrador.hasta(hito)
                print("\nSembrados %d alumnos (%d requests, %.1f s)." % (
                    hito, api.requests - antes, time.perf_counter() - t0))
                filas = medir(api, a, sembrador.inscripcion_mas_cargada())
                imprimir_tabla("Con %d alumnos" % hito, filas)
                resultados[hito] = filas
        if a.log and not os.path.exists(a.log):
            print("\n(No se encontro %s: sin conteo de SQL. Correr desde la raiz del repo y con el perfil 'bench'.)" % a.log)
        veredicto(resultados, a)
        if a.salida:
            with open(a.salida, "w", encoding="utf-8") as f:
                json.dump({str(k): v for k, v in resultados.items()}, f, indent=2, ensure_ascii=False)
            print("Resultados guardados en %s." % a.salida)
    except ErrorApi as e:
        print("\nERROR: %s" % e, file=sys.stderr)
        sys.exit(1)
    except OSError as e:
        print("\nNo se pudo conectar con %s (%s). ¿Esta corriendo el backend con el perfil 'bench'?" % (a.base, e),
              file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
