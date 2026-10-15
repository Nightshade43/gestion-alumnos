@echo off
title Gestion de Alumnos
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\launcher\lanzador.ps1" -Accion iniciar %*
if errorlevel 1 pause
