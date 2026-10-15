@echo off
title Gestion de Alumnos - construir
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\launcher\lanzador.ps1" -Accion construir
pause
