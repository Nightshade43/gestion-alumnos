#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests del importador de Sede con datos inventados (librería estándar).

Desde la raíz del repo:  python -m unittest discover -s scripts/importadores -v
"""

import os
import sys
import unittest
from datetime import date

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import importar_sede as imp  # noqa: E402

JS = "%s 00:00:00 GMT-0300 (hora estándar de Argentina)"


def modulo(estado="pendiente", nota=None, tpi="", ev=""):
    return {"estado": estado, "nota": nota, "tpi_fecha": tpi, "eval_fecha": ev}


def alumno(modulos, plan="A", activo=True, egresado=False, dni="30111222", id_="id-1"):
    modulos = modulos + [modulo() for _ in range(7 - len(modulos))]
    return {"id": id_, "nombre": "Prueba,  Ana", "dni": dni, "grupo": "Miércoles", "plan": plan,
            "activo": activo, "egresado": egresado, "eliminado": False, "modulos": modulos}


class ParseFecha(unittest.TestCase):
    def test_fecha_de_javascript(self):
        self.assertEqual(imp.parse_fecha(JS % "Wed Mar 12 2025"), date(2025, 3, 12))

    def test_iso_en_utc_se_pasa_a_hora_argentina(self):
        self.assertEqual(imp.parse_fecha("2025-06-10T03:00:00.000Z"), date(2025, 6, 10))
        self.assertEqual(imp.parse_fecha("2025-06-10T02:00:00.000Z"), date(2025, 6, 9))

    def test_texto_que_no_es_fecha(self):
        for texto in ("", "pendiente", "Invalid Date", "9"):
            self.assertIsNone(imp.parse_fecha(texto))


class ParseNota(unittest.TestCase):
    def test_nota_normal_y_vacia(self):
        self.assertEqual(imp.parse_nota(8), (8, None))
        self.assertEqual(imp.parse_nota(None), (None, None))

    def test_nota_guardada_como_fecha_de_sheets(self):
        nota, aviso = imp.parse_nota(-2208368592000)   # 08/01/1900 en hora local de 1900 = serial 9
        self.assertEqual(nota, 9)
        self.assertIn("Sheets", aviso)
        self.assertEqual(imp.parse_nota(-2208282192000)[0], 10)

    def test_nota_irrecuperable(self):
        nota, aviso = imp.parse_nota(1700000000000)
        self.assertIsNone(nota)
        self.assertIn("irrecuperable", aviso)


class FilaCorrida(unittest.TestCase):
    def test_reconstruye_estado_fecha_y_nota(self):
        m, avisos = imp.normalizar_modulo(
            {"estado": "2025-11-05T03:00:00.000Z", "nota": None, "tpi_fecha": "9", "eval_fecha": "aprobado"})
        self.assertEqual(m, {"estado": "aprobado", "nota": 9, "fecha_final": date(2025, 11, 5), "fecha_tp": None})
        self.assertIn("fila corrida", avisos[0])

    def test_fila_corrida_con_fecha_de_tp(self):
        m, _ = imp.normalizar_modulo(
            {"estado": "2025-11-05T03:00:00.000Z", "nota": None, "tpi_fecha": JS % "Mon Oct 20 2025",
             "eval_fecha": "en_curso"})
        self.assertEqual((m["estado"], m["nota"], m["fecha_tp"]), ("en_curso", None, date(2025, 10, 20)))


class Plan(unittest.TestCase):
    def mods(self, eximidos):
        return [{"estado": "eximido" if i in eximidos else "pendiente"} for i in range(1, 8)]

    def test_plan_del_export_manda(self):
        self.assertEqual(imp.plan_de({"plan": "C"}, self.mods([])), ("C", "export"))

    def test_plan_inferido_por_eximidos(self):
        self.assertEqual(imp.plan_de({"plan": ""}, self.mods([])), ("A", "inferido"))
        self.assertEqual(imp.plan_de({"plan": ""}, self.mods([1, 2, 3])), ("B", "inferido"))
        self.assertEqual(imp.plan_de({"plan": ""}, self.mods([1, 2, 3, 4, 5])), ("C", "inferido"))

    def test_eximidos_que_no_encajan_quedan_sin_plan(self):
        self.assertEqual(imp.plan_de({"plan": ""}, self.mods([1, 2])), (None, "sin plan"))
        self.assertEqual(imp.plan_de({"plan": ""}, self.mods([2, 3, 4])), (None, "sin plan"))


class PlanificarAlumno(unittest.TestCase):
    def test_aprobado_sin_tp_crea_tp_inferido_antes_que_la_final(self):
        p = imp.planificar_alumno(alumno([modulo("aprobado", 8, "", JS % "Tue Jul 01 2025")]))
        self.assertEqual([(i["tipo"], i.get("inferido", False)) for i in p["instancias"]],
                         [("TP_INTEGRADOR", True), ("EVALUACION_FINAL", False)])
        self.assertEqual(p["fecha_inicio"], date(2025, 7, 1))

    def test_tp_con_fecha_propia_y_fecha_inicio_la_mas_antigua(self):
        p = imp.planificar_alumno(alumno([
            modulo("aprobado", 9, JS % "Mon Apr 07 2025", JS % "Mon May 05 2025"),
            modulo("aprobado", 7, "", JS % "Mon Jun 02 2025")]))
        self.assertEqual(p["fecha_inicio"], date(2025, 4, 7))
        self.assertFalse(p["instancias"][0].get("inferido", False))

    def test_eximidos_no_cargan_nada(self):
        p = imp.planificar_alumno(alumno([modulo("eximido"), modulo("eximido"), modulo("eximido")], plan="B"))
        self.assertEqual(p["instancias"], [])

    def test_sin_fechas_usa_el_inicio_del_ciclo_segun_activo(self):
        self.assertEqual(imp.planificar_alumno(alumno([], activo=True))["fecha_inicio"], date(2026, 3, 1))
        self.assertEqual(imp.planificar_alumno(alumno([], activo=False))["fecha_inicio"], date(2025, 3, 1))

    def test_estado_de_la_inscripcion(self):
        self.assertEqual(imp.planificar_alumno(alumno([]))["estado_inscripcion"], "ACTIVA")
        self.assertEqual(imp.planificar_alumno(alumno([], activo=False))["estado_inscripcion"], "PAUSADA")
        egresado = imp.planificar_alumno(alumno([], activo=False, egresado=True))
        self.assertEqual(egresado["estado_inscripcion"], "ACTIVA")
        self.assertTrue(any("egresado" in a for a in egresado["avisos"]))

    def test_aprobado_sin_nota_va_a_revisar_sin_inventar_nada(self):
        p = imp.planificar_alumno(alumno([modulo("aprobado", None, "", "")]))
        self.assertEqual(p["instancias"], [])
        self.assertTrue(any(a.startswith("REVISAR") for a in p["avisos"]))

    def test_nombre_sin_espacios_dobles(self):
        self.assertEqual(imp.planificar_alumno(alumno([]))["nombre"], "Prueba, Ana")


class Planificar(unittest.TestCase):
    def test_dni_repetido_se_importa_una_vez(self):
        export = {"alumnos": [alumno([], dni="30111222", id_="a"), alumno([], dni="30111222", id_="b")]}
        alumnos, repetidos = imp.planificar(export)
        self.assertEqual([a["id_export"] for a in alumnos], ["a"])
        self.assertEqual(repetidos[0][0]["id_export"], "b")


if __name__ == "__main__":
    unittest.main()
