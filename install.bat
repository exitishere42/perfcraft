@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "REPO=exitishere42/perfcraft"
set "INSTALL_DIR=%USERPROFILE%\perfcraft"
set "JAR_NAME=perfcraft-1.0.8-all.jar"
set "RELEASE_URL=https://github.com/%REPO%/releases/latest/download"
set "RAW_URL=https://raw.githubusercontent.com/%REPO%/main"

echo ==================================================================
echo   perfcraft - Windows Installer
echo   Zielordner: %INSTALL_DIR%
echo ==================================================================

:: ------------------------------------------------------------------------------
:: 1. Pruefen ob Java 21+ installiert ist (wenn ja -> skip, wenn nein -> installieren)
:: ------------------------------------------------------------------------------
set "JAVA_OK=0"
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%v in ('powershell -NoProfile -Command "$v = (java -version 2>&1 | Select-Object -First 1); if ($v -match '\"(\d+)') { [int]$Matches[1] } else { 0 }"') do (
        if %%v GEQ 21 set "JAVA_OK=1"
    )
)

if "%JAVA_OK%"=="1" (
    echo [OK] Java 21+ ist bereits installiert - ueberspringe Java-Installation.
) else (
    echo [*] Java 21+ nicht gefunden. Installiere neuestes OpenJDK 21 LTS...
    where winget >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        winget install --id EclipseAdoptium.Temurin.21.JRE -e --accept-source-agreements --accept-package-agreements
    ) else (
        echo [*] Lade OpenJDK 21 MSI direkt von Adoptium herunter...
        powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile '$env:TEMP\temurin21.msi'; Start-Process msiexec.exe -ArgumentList '/i', '$env:TEMP\temurin21.msi', '/passive', '/norestart' -Wait"
    )
    echo [OK] Java 21+ Installation abgeschlossen.
)

:: ------------------------------------------------------------------------------
:: 2. Programmdateien nach %USERPROFILE%\perfcraft herunterladen
:: ------------------------------------------------------------------------------
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo [*] Lade perfcraft und Logos herunter...
powershell -NoProfile -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "Invoke-WebRequest -Uri '%RELEASE_URL%/%JAR_NAME%' -OutFile '%INSTALL_DIR%\%JAR_NAME%'; " ^
    "Invoke-WebRequest -Uri '%RELEASE_URL%/perfcraft.exe' -OutFile '%INSTALL_DIR%\perfcraft.exe'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/perfcraft.ico' -OutFile '%INSTALL_DIR%\perfcraft.ico'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/perfcraft.png' -OutFile '%INSTALL_DIR%\perfcraft.png'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/perf-doc.ico' -OutFile '%INSTALL_DIR%\perf-doc.ico'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/perf-doc.png' -OutFile '%INSTALL_DIR%\perf-doc.png'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/uninstall.bat' -OutFile '%INSTALL_DIR%\uninstall.bat'"

if not exist "%INSTALL_DIR%\%JAR_NAME%" (
    echo [!] Fehler beim Herunterladen von %JAR_NAME%.
    pause
    exit /b 1
)

:: Startskript im Zielordner anlegen
(
echo @echo off
echo cd /d "%%~dp0"
echo start "" javaw -jar "%%~dp0%JAR_NAME%" %%*
) > "%INSTALL_DIR%\run.bat"

:: ------------------------------------------------------------------------------
:: 3. Startmenue- und Desktop-Verknuepfung mit Logo erstellen
:: ------------------------------------------------------------------------------
echo [*] Erstelle Startmenue- und Desktop-Eintrag mit Logo...
powershell -NoProfile -Command ^
    "$ws = New-Object -ComObject WScript.Shell; " ^
    "$exePath = '%INSTALL_DIR%\perfcraft.exe'; " ^
    "$smPath = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\perfcraft.lnk'; " ^
    "$dtPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'perfcraft.lnk'; " ^
    "foreach ($lnkPath in @($smPath, $dtPath)) { " ^
    "  $s = $ws.CreateShortcut($lnkPath); " ^
    "  $s.TargetPath = $exePath; " ^
    "  $s.WorkingDirectory = '%INSTALL_DIR%'; " ^
    "  $s.IconLocation = '%INSTALL_DIR%\perfcraft.ico,0'; " ^
    "  $s.Description = 'PerfCraft - Perfboard & Stripboard CAD Designer'; " ^
    "  $s.Save(); " ^
    "}"

:: ------------------------------------------------------------------------------
:: 4. .perf Dateityp und Dokumenten-Icon in der Windows Registry registrieren
:: ------------------------------------------------------------------------------
echo [*] Registriere .perf Dateityp mit eigenem Dokumenten-Icon...
powershell -NoProfile -Command ^
    "$exe = '%INSTALL_DIR%\perfcraft.exe'; " ^
    "$docIcon = '%INSTALL_DIR%\perf-doc.ico,0'; " ^
    "New-Item -Path 'HKCU:\Software\Classes\.perf' -Value 'PerfCraft.Project' -Force | Out-Null; " ^
    "New-Item -Path 'HKCU:\Software\Classes\PerfCraft.Project' -Value 'PerfCraft Project' -Force | Out-Null; " ^
    "New-Item -Path 'HKCU:\Software\Classes\PerfCraft.Project\DefaultIcon' -Value $docIcon -Force | Out-Null; " ^
    "New-Item -Path 'HKCU:\Software\Classes\PerfCraft.Project\shell\open\command' -Value ('\"' + $exe + '\" \"%1\"') -Force | Out-Null; " ^
    "New-Item -Path 'HKCU:\Software\Classes\perf_auto_file\DefaultIcon' -Value $docIcon -Force | Out-Null; " ^
    "New-Item -Path 'HKCU:\Software\Classes\Applications\perfcraft.exe\DefaultIcon' -Value $docIcon -Force | Out-Null; " ^
    "New-Item -Path 'HKCU:\Software\Classes\Applications\perfcraft.exe\SupportedTypes' -Force | Out-Null; " ^
    "New-ItemProperty -Path 'HKCU:\Software\Classes\Applications\perfcraft.exe\SupportedTypes' -Name '.perf' -Value '' -Force | Out-Null; " ^
    "$code = 'using System; using System.Runtime.InteropServices; public class S { [DllImport(\"shell32.dll\")] public static extern void SHChangeNotify(int e, uint f, IntPtr i1, IntPtr i2); }'; " ^
    "Add-Type -TypeDefinition $code; " ^
    "[S]::SHChangeNotify(0x08000000, 0, [IntPtr]::Zero, [IntPtr]::Zero); " ^
    "Write-Output '[OK] .perf Dokumenten-Icon registriert und Explorer benachrichtigt.'"

echo ==================================================================
echo   [OK] Installation erfolgreich abgeschlossen!
echo   Ordner:       %INSTALL_DIR%
echo   Dateityp:     .perf ist mit PerfCraft und eigenem Icon verknuepft!
echo   Startmenue:   perfcraft (mit Logo direkt im Startmenue & Desktop)
echo ==================================================================
pause
