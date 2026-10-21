@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "INSTALL_DIR=%USERPROFILE%\perfcraft"
set "SM_LNK=%APPDATA%\Microsoft\Windows\Start Menu\Programs\perfcraft.lnk"

echo ==================================================================
echo   perfcraft - Windows Uninstaller
echo ==================================================================

:: 1. Startmenue- und Desktop-Verknuepfungen loeschen
if exist "%SM_LNK%" (
    echo [*] Entferne Startmenue-Eintrag...
    del /f /q "%SM_LNK%"
)

for /f "usebackq tokens=*" %%d in (`powershell -NoProfile -Command "[Environment]::GetFolderPath('Desktop')"`) do (
    if exist "%%d\perfcraft.lnk" (
        echo [*] Entferne Desktop-Verknuepfung...
        del /f /q "%%d\perfcraft.lnk"
    )
)

:: 2. Registry Dateizuordnung .perf loeschen
echo [*] Entferne .perf Dateityp-Registrierung...
powershell -NoProfile -Command ^
    "Remove-Item -Path 'HKCU:\Software\Classes\.perf' -Recurse -ErrorAction SilentlyContinue; " ^
    "Remove-Item -Path 'HKCU:\Software\Classes\PerfCraft.Project' -Recurse -ErrorAction SilentlyContinue; "

:: 3. Programmordner %USERPROFILE%\perfcraft loeschen
if exist "%INSTALL_DIR%" (
    echo [*] Loesche Programmordner: %INSTALL_DIR%
    cd /d "%TEMP%"
    rmdir /s /q "%INSTALL_DIR%"
) else (
    echo [i] Programmordner %INSTALL_DIR% wurde bereits entfernt.
)

echo ==================================================================
echo   [OK] perfcraft wurde vollstaendig deinstalliert!
echo ==================================================================
pause
