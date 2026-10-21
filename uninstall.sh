#!/usr/bin/env bash
# ==============================================================================
# perfcraft - Linux Deinstallations-Skript (Uninstaller)
# Entfernt den Programmordner (~/perfcraft), das App-Logo und den Desktop-Entry.
# ==============================================================================

set -e

INSTALL_DIR="${INSTALL_DIR:-$HOME/perfcraft}"
DESKTOP_FILE="$HOME/.local/share/applications/perfcraft.desktop"
ICON_PNG="$HOME/.local/share/icons/hicolor/256x256/apps/perfcraft.png"
ICON_SVG="$HOME/.local/share/icons/hicolor/scalable/apps/perfcraft.svg"
MIME_FILE="$HOME/.local/share/mime/packages/perfcraft.xml"
MIME_ICON="$HOME/.local/share/icons/hicolor/scalable/mimetypes/application-x-perfcraft-project.svg"

echo "=================================================================="
echo "  perfcraft - Linux Uninstaller"
echo "=================================================================="

# 1. Laufende Instanzen beenden
if pgrep -f "perfcraft.*\.jar|perfcraft.*\.AppImage" &> /dev/null; then
    echo "[*] Beende laufende perfcraft-Instanz..."
    pkill -f "perfcraft.*\.jar|perfcraft.*\.AppImage" || true
    sleep 1
fi

# 2. Desktop-Entry, MIME-Type und Icons entfernen
if [ -f "$DESKTOP_FILE" ]; then
    echo "[*] Entferne Desktop-Entry: $DESKTOP_FILE"
    rm -f "$DESKTOP_FILE"
fi

rm -f "$HOME/Desktop/perfcraft.desktop" "$HOME/Schreibtisch/perfcraft.desktop" 2>/dev/null || true

if [ -f "$ICON_PNG" ] || [ -f "$ICON_SVG" ]; then
    echo "[*] Entferne App-Icons..."
    rm -f "$ICON_PNG" "$ICON_SVG"
fi

if [ -f "$MIME_FILE" ]; then
    echo "[*] Entferne .perf MIME-Type..."
    rm -f "$MIME_FILE" "$MIME_ICON" 2>/dev/null || true
fi

if command -v update-mime-database &> /dev/null; then
    update-mime-database "$HOME/.local/share/mime" &> /dev/null || true
fi
if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$HOME/.local/share/applications" &> /dev/null || true
fi
if command -v gtk-update-icon-cache &> /dev/null; then
    gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" &> /dev/null || true
fi

# 3. Programmordner loeschen
if [ -d "$INSTALL_DIR" ]; then
    echo "[*] Loesche Programmordner: $INSTALL_DIR"
    rm -rf "$INSTALL_DIR"
else
    echo "[i] Programmordner $INSTALL_DIR existiert bereits nicht mehr."
fi

echo "=================================================================="
echo "  [OK] perfcraft wurde vollstaendig deinstalliert!"
echo "=================================================================="
