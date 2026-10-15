<#
  Lanzador local de Gestión de Alumnos (Windows PowerShell 5.1 o superior).
  No se ejecuta a mano: lo llaman iniciar.bat y construir.bat de la raíz del repo.

    -Accion iniciar    Verifica Java y PostgreSQL, crea la base si falta, reconstruye si hay cambios nuevos,
                       levanta el sistema y abre el navegador.
    -Accion construir  Compila el frontend y empaqueta todo en un solo jar (target\gestion-alumnos-*.jar).
    -Reconstruir       Con iniciar: fuerza la reconstrucción aunque no haya cambios.
#>
param(
    [ValidateSet('iniciar', 'construir')][string]$Accion = 'iniciar',
    [switch]$Reconstruir
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$Raiz = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $Raiz

$ArchivoConfig = Join-Path $Raiz 'src\main\resources\application-local.properties'
$ArchivoEjemplo = Join-Path $Raiz 'src\main\resources\application-local.properties.example'

function Info([string]$mensaje) { Write-Host $mensaje -ForegroundColor Cyan }
function Aviso([string]$mensaje) { Write-Host "AVISO: $mensaje" -ForegroundColor Yellow }
function Fallar([string]$mensaje) {
    Write-Host ''
    Write-Host "ERROR: $mensaje" -ForegroundColor Red
    exit 1
}

# Ejecuta un programa nativo capturando su salida. Los errores de un .exe no deben cortar el script
# ($ErrorActionPreference = 'Stop' convertiría su stderr en una excepción en Windows PowerShell 5.1).
function Ejecutar([string]$exe, [string[]]$argumentos) {
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $salida = & $exe @argumentos 2>&1 | ForEach-Object { "$_" }
        $codigo = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $anterior
    }
    [pscustomobject]@{ Salida = (($salida -join "`n").Trim()); Codigo = $codigo }
}

function Verificar-Java {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Fallar 'No se encontró Java. Instalá un JDK 21 (por ejemplo Temurin: https://adoptium.net) y volvé a abrir este archivo.'
    }
    $primera = (Ejecutar 'cmd.exe' @('/c', 'java -version 2>&1')).Salida -split "`n" | Select-Object -First 1
    if ($primera -notmatch 'version "(\d+)') {
        Fallar "No pude leer la versión de Java ('$primera'). Hace falta JDK 21 o superior."
    }
    if ([int]$Matches[1] -lt 21) {
        Fallar "Se encontró Java $($Matches[1]) y hace falta 21 o superior. Instalá un JDK 21 y volvé a abrir este archivo."
    }
}

function Leer-Propiedades([string]$ruta) {
    $propiedades = @{}
    foreach ($linea in Get-Content -Path $ruta -Encoding UTF8) {
        $texto = $linea.Trim()
        if ($texto -eq '' -or $texto.StartsWith('#') -or $texto.StartsWith('!')) { continue }
        $i = $texto.IndexOf('=')
        if ($i -lt 1) { continue }
        $propiedades[$texto.Substring(0, $i).Trim()] = $texto.Substring($i + 1).Trim()
    }
    return $propiedades
}

# Devuelve las propiedades de application-local.properties. Si el archivo no existe lo crea a partir del ejemplo,
# lo abre para que lo completes y corta: hay que volver a abrir el launcher después de guardarlo.
function Leer-Configuracion {
    if (-not (Test-Path $ArchivoConfig)) {
        Copy-Item $ArchivoEjemplo $ArchivoConfig
        Start-Process notepad.exe $ArchivoConfig
        Fallar ("Faltaba application-local.properties. Lo creé a partir del ejemplo y lo abrí en el Bloc de notas: " +
                "completá la contraseña de PostgreSQL, jwt.secret y las credenciales de admin, guardalo y volvé a abrir este archivo.")
    }
    $config = Leer-Propiedades $ArchivoConfig
    foreach ($clave in @('spring.datasource.url', 'spring.datasource.username', 'spring.datasource.password',
                         'jwt.secret', 'admin.username', 'admin.password')) {
        if (-not $config.ContainsKey($clave) -or $config[$clave] -eq '') {
            Fallar "Falta '$clave' en $ArchivoConfig."
        }
    }
    foreach ($clave in @('jwt.secret', 'admin.password')) {
        if ($config[$clave] -like 'CAMBIAME*') {
            Start-Process notepad.exe $ArchivoConfig
            Fallar "'$clave' todavía tiene el valor de ejemplo. Completalo en el archivo que se abrió, guardalo y volvé a abrir este archivo."
        }
    }
    return $config
}

function Buscar-Psql {
    $comando = Get-Command psql -ErrorAction SilentlyContinue
    if ($comando) { return $comando.Source }
    $candidatos = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\psql.exe' -ErrorAction SilentlyContinue
    if (-not $candidatos) { return $null }
    $ordenados = $candidatos | Sort-Object {
        try { [double]($_.Directory.Parent.Name -replace '[^\d.]', '') } catch { 0 }
    } -Descending
    return $ordenados[0].FullName
}

# Verifica que PostgreSQL responda y crea la base si no existe.
function Preparar-Base($config) {
    $url = $config['spring.datasource.url']
    if ($url -notmatch '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([A-Za-z0-9_]+)') {
        Fallar "No entiendo spring.datasource.url ('$url'). Se espera algo como jdbc:postgresql://localhost:5432/gestion_alumnos"
    }
    $servidor = $Matches[1]
    $puerto = if ($Matches[2]) { $Matches[2] } else { '5432' }
    $base = $Matches[3]
    $usuario = $config['spring.datasource.username']

    $psql = Buscar-Psql
    if (-not $psql) {
        Aviso "No encontré psql, así que no puedo verificar ni crear la base '$base'. Si no existe, creala con pgAdmin o con: CREATE DATABASE $base;"
        return
    }

    $env:PGPASSWORD = $config['spring.datasource.password']
    $env:PGCONNECT_TIMEOUT = '5'
    try {
        $consulta = Ejecutar $psql @('-h', $servidor, '-p', $puerto, '-U', $usuario, '-d', 'postgres', '-tAc',
                                      "SELECT 1 FROM pg_database WHERE datname = '$base'")
        if ($consulta.Codigo -ne 0) {
            Fallar ("No pude conectarme a PostgreSQL en ${servidor}:${puerto} con el usuario '$usuario'. " +
                    "Verificá que el servicio de PostgreSQL esté iniciado y que la contraseña de application-local.properties sea correcta.`n" +
                    "Detalle: $($consulta.Salida)")
        }
        if ($consulta.Salida -ne '1') {
            Info "La base '$base' no existe: la creo."
            $crear = Ejecutar $psql @('-h', $servidor, '-p', $puerto, '-U', $usuario, '-d', 'postgres', '-c', "CREATE DATABASE `"$base`"")
            if ($crear.Codigo -ne 0) {
                Fallar "No pude crear la base '$base'. Detalle: $($crear.Salida)"
            }
        }
    } finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
        Remove-Item Env:PGCONNECT_TIMEOUT -ErrorAction SilentlyContinue
    }
}

function Buscar-Jar {
    $jars = Get-ChildItem (Join-Path $Raiz 'target') -Filter 'gestion-alumnos-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*.original' } | Sort-Object LastWriteTime -Descending
    if ($jars) { return $jars[0] }
    return $null
}

# Hay que reconstruir si no hay jar o si algún archivo fuente es más nuevo que él (por ejemplo después de un git pull).
function Necesita-Reconstruir($jar) {
    if (-not $jar) { return $true }
    $fuentes = @(
        Get-ChildItem (Join-Path $Raiz 'src\main') -Recurse -File |
            Where-Object { $_.Name -ne 'application-local.properties' }
        Get-ChildItem (Join-Path $Raiz 'frontend\src') -Recurse -File
        Get-Item (Join-Path $Raiz 'pom.xml')
        Get-Item (Join-Path $Raiz 'frontend\package.json')
    )
    return [bool]($fuentes | Where-Object { $_.LastWriteTime -gt $jar.LastWriteTime } | Select-Object -First 1)
}

function Construir {
    Verificar-Java
    if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
        Fallar 'No se encontró Node.js (npm), que hace falta para compilar el frontend. Instalá Node 22 desde https://nodejs.org y volvé a intentar.'
    }

    Info '[1/3] Instalando las dependencias del frontend...'
    Push-Location (Join-Path $Raiz 'frontend')
    try {
        & npm ci
        if ($LASTEXITCODE -ne 0) { Fallar 'Falló npm ci.' }

        Info '[2/3] Compilando el frontend...'
        & npm run build
        if ($LASTEXITCODE -ne 0) { Fallar 'Falló la compilación del frontend (npm run build).' }
    } finally {
        Pop-Location
    }

    Info '[3/3] Empaquetando el sistema en un solo jar (la primera vez descarga dependencias y puede tardar)...'
    & (Join-Path $Raiz 'mvnw.cmd') -B -Pcon-frontend -DskipTests clean package
    if ($LASTEXITCODE -ne 0) { Fallar 'Falló el empaquetado con Maven.' }

    if (-not (Buscar-Jar)) { Fallar 'Maven terminó sin errores pero no encontré el jar en target\.' }
    Info 'Listo: el sistema está construido.'
}

function Puerto-Abierto([int]$puerto) {
    $cliente = New-Object System.Net.Sockets.TcpClient
    try {
        $espera = $cliente.BeginConnect('127.0.0.1', $puerto, $null, $null)
        return ($espera.AsyncWaitHandle.WaitOne(500) -and $cliente.Connected)
    } catch {
        return $false
    } finally {
        $cliente.Close()
    }
}

# Código HTTP de una URL, o 0 si no responde. Un 401/404 también significa "el servidor está arriba".
function Codigo-Http([string]$url) {
    try {
        return [int](Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3).StatusCode
    } catch {
        # Con un 401/404 la excepción trae la respuesta; sin servidor no hay respuesta.
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    }
}

function Iniciar {
    Verificar-Java
    $config = Leer-Configuracion
    $puerto = if ($config.ContainsKey('server.port')) { [int]$config['server.port'] } else { 8080 }
    $url = "http://localhost:$puerto"

    if (Puerto-Abierto $puerto) {
        Info "Ya hay algo escuchando en el puerto $puerto (¿el sistema ya está abierto?). Abro el navegador."
        Start-Process $url
        return
    }

    Preparar-Base $config

    $jar = Buscar-Jar
    if ($Reconstruir -or (Necesita-Reconstruir $jar)) {
        if ($jar -and -not (Get-Command npm -ErrorAction SilentlyContinue)) {
            Aviso 'Hay cambios nuevos pero falta Node.js para reconstruir: arranco con la versión ya construida.'
        } else {
            Info 'Hay cambios nuevos (o todavía no se construyó): reconstruyo antes de iniciar.'
            Construir
            $jar = Buscar-Jar
        }
    }

    Info "Iniciando Gestión de Alumnos en $url ..."
    Info 'No cierres esta ventana mientras uses el sistema: al cerrarla se detiene.'
    Write-Host ''

    # La configuración se lee del archivo (no de la copia que quedó dentro del jar), así cambiar una clave
    # no obliga a reconstruir.
    $argumentos = @('-jar', ('"' + $jar.FullName + '"'),
                    '--spring.config.additional-location=optional:file:./src/main/resources/')
    $proceso = Start-Process -FilePath 'java' -ArgumentList $argumentos -WorkingDirectory $Raiz -NoNewWindow -PassThru

    $limite = (Get-Date).AddSeconds(120)
    $listo = $false
    while ((Get-Date) -lt $limite) {
        if ($proceso.HasExited) {
            Fallar 'El sistema se detuvo al arrancar. El motivo está en los mensajes de arriba.'
        }
        if ((Codigo-Http "$url/api/personas") -ne 0) { $listo = $true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $listo) { Fallar 'El sistema no respondió en 2 minutos. Revisá los mensajes de arriba.' }

    if ((Codigo-Http "$url/") -ne 200) {
        Aviso 'El servidor está arriba pero no sirve la interfaz: el jar se construyó sin el frontend. Ejecutá construir.bat.'
    }
    Info "Listo. Abriendo $url en el navegador."
    Start-Process $url

    $proceso.WaitForExit()
}

switch ($Accion) {
    'construir' { Construir }
    'iniciar'   { Iniciar }
}
