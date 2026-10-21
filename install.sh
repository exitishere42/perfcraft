#!/usr/bin/env bash
# ==============================================================================
# perfcraft - Automatischer Linux Installer
# Laedt perfcraft nach ~/perfcraft,
# prueft/installiert Java 21+ und erstellt den Desktop-Eintrag mit Logo im Dash.
# ==============================================================================

set -e

REPO="exitishere42/perfcraft"
INSTALL_DIR="${INSTALL_DIR:-$HOME/perfcraft}"
APP_NAME="perfcraft"
JAR_NAME="perfcraft-1.2.0-all.jar"
DETECTED_JAR=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" 2>/dev/null | grep -o '"name": *"perfcraft-[^"]*-all\.jar"' | head -n 1 | cut -d '"' -f 4 || true)
if [ -n "$DETECTED_JAR" ]; then
    JAR_NAME="$DETECTED_JAR"
fi
APPIMAGE_NAME="perfcraft-x86_64.AppImage"
RELEASE_BASE_URL="https://github.com/$REPO/releases/latest/download"
RAW_BASE_URL="https://raw.githubusercontent.com/$REPO/main"

echo "=================================================================="
echo "  perfcraft - Linux Installer"
echo "  Zielordner: $INSTALL_DIR"
echo "=================================================================="

# ------------------------------------------------------------------------------
# 1. Pruefen ob Java 21+ installiert ist
# ------------------------------------------------------------------------------
check_java_version() {
    if command -v java &> /dev/null; then
        local ver
        ver=$(java -version 2>&1 | head -n 1 | sed -E 's/.*"([0-9]+).*/\1/')
        if [[ "$ver" =~ ^[0-9]+$ ]] && [ "$ver" -ge 21 ]; then
            return 0
        fi
    fi
    return 1
}

if check_java_version; then
    JAVA_VER_STR=$(java -version 2>&1 | head -n 1)
    echo "[OK] Passendes Java bereits installiert ($JAVA_VER_STR) -> ueberspringe Java-Installation."
else
    echo "[*] Java 21+ wurde nicht gefunden. Installiere neuestes OpenJDK..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get update -y
        if apt-cache show openjdk-21-jre &> /dev/null; then
            sudo apt-get install -y openjdk-21-jre
        else
            echo "[*] Fuege Eclipse Adoptium Repository fuer OpenJDK 21 hinzu..."
            sudo apt-get install -y wget apt-transport-https gpg
            wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
            echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list > /dev/null
            sudo apt-get update -y
            sudo apt-get install -y temurin-21-jre
        fi
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y java-21-openjdk
    elif command -v pacman &> /dev/null; then
        sudo pacman -Sy --noconfirm jre-openjdk
    elif command -v zypper &> /dev/null; then
        sudo zypper install -y java-21-openjdk
    else
        echo "[!] Konnte Paketmanager nicht erkennen. Bitte OpenJDK 21+ manuell installieren."
        exit 1
    fi
    echo "[OK] Java 21+ erfolgreich installiert."
fi

# ------------------------------------------------------------------------------
# 2. Projektdateien in Zielordner herunterladen
# ------------------------------------------------------------------------------
echo "[*] Erstelle Programmordner: $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"

download_file() {
    local url="$1"
    local dest="$2"
    if command -v curl &> /dev/null; then
        curl -fL --progress-bar -o "$dest" "$url"
    elif command -v wget &> /dev/null; then
        wget -q --show-progress -O "$dest" "$url"
    else
        echo "[!] Weder curl noch wget installiert."
        exit 1
    fi
}

echo "[*] Lade Programmdateien und Logos herunter..."
download_file "$RELEASE_BASE_URL/$JAR_NAME" "$INSTALL_DIR/$JAR_NAME"
download_file "$RELEASE_BASE_URL/$APPIMAGE_NAME" "$INSTALL_DIR/$APPIMAGE_NAME" || true
download_file "$RAW_BASE_URL/perfcraft.png" "$INSTALL_DIR/perfcraft.png"
download_file "$RAW_BASE_URL/perfcraft.svg" "$INSTALL_DIR/perfcraft.svg"
download_file "$RAW_BASE_URL/perf-doc.png" "$INSTALL_DIR/perf-doc.png"
download_file "$RAW_BASE_URL/perf-doc.svg" "$INSTALL_DIR/perf-doc.svg"
download_file "$RAW_BASE_URL/uninstall.sh" "$INSTALL_DIR/uninstall.sh" || true
chmod +x "$INSTALL_DIR/uninstall.sh" 2>/dev/null || true

if [ -f "$INSTALL_DIR/$APPIMAGE_NAME" ]; then
    chmod +x "$INSTALL_DIR/$APPIMAGE_NAME"
fi

# Startskript run.sh im Zielordner erzeugen
cat > "$INSTALL_DIR/run.sh" << EOF
#!/usr/bin/env bash
SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
cd "\$SCRIPT_DIR"

JAR_PATH="\$SCRIPT_DIR/$JAR_NAME"
if [ ! -f "\$JAR_PATH" ]; then
    JAR_PATH="\$(ls -t "\$SCRIPT_DIR"/perfcraft-*-all.jar 2>/dev/null | head -n 1)"
fi

if [ -f "\$JAR_PATH" ]; then
    exec java -cp "\$JAR_PATH" de.exit.perfcraft.Main "\$@" 2>/dev/null || exec java -jar "\$JAR_PATH" "\$@"
fi

if [ -f "\$SCRIPT_DIR/$APPIMAGE_NAME" ] && [ -x "\$SCRIPT_DIR/$APPIMAGE_NAME" ]; then
    exec "\$SCRIPT_DIR/$APPIMAGE_NAME" "\$@"
fi

echo "[FEHLER] Keine ausfuehrbare PerfCraft JAR gefunden in \$SCRIPT_DIR"
exit 1
EOF
chmod +x "$INSTALL_DIR/run.sh"

# ------------------------------------------------------------------------------
# 3. MIME-Type fuer .perf Dateien und eigenes Dokumenten-Icon registrieren
# ------------------------------------------------------------------------------
echo "[*] Registriere .perf MIME-Type und Dokumenten-Icon..."
MIME_DIR="$HOME/.local/share/mime/packages"
MIME_ICON_DIR="$HOME/.local/share/icons/hicolor/scalable/mimetypes"
mkdir -p "$MIME_DIR" "$MIME_ICON_DIR"

cat > "$MIME_DIR/perfcraft.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<mime-info xmlns="http://www.freedesktop.org/standards/shared-mime-info">
  <mime-type type="application/x-perfcraft-project">
    <comment>PerfCraft CAD Project</comment>
    <glob pattern="*.perf"/>
    <icon name="application-x-perfcraft-project"/>
  </mime-type>
</mime-info>
EOF

cp "$INSTALL_DIR/perf-doc.svg" "$MIME_ICON_DIR/application-x-perfcraft-project.svg" 2>/dev/null || true

# ------------------------------------------------------------------------------
# 4. Desktop-Entry & Icon fuer das App-Dash / Anwendungsmenue einrichten
# ------------------------------------------------------------------------------
echo "[*] Richte Desktop-Entry und App-Logo fuer das Dash ein..."
ICON_DIR_PNG="$HOME/.local/share/icons/hicolor/256x256/apps"
ICON_DIR_SVG="$HOME/.local/share/icons/hicolor/scalable/apps"
APPS_DIR="$HOME/.local/share/applications"

mkdir -p "$ICON_DIR_PNG" "$ICON_DIR_SVG" "$APPS_DIR"

cp "$INSTALL_DIR/perfcraft.png" "$ICON_DIR_PNG/perfcraft.png"
cp "$INSTALL_DIR/perfcraft.svg" "$ICON_DIR_SVG/perfcraft.svg"

DESKTOP_FILE="$APPS_DIR/perfcraft.desktop"
cat > "$DESKTOP_FILE" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=PerfCraft
Comment=Perfboard & Stripboard CAD Designer
Exec=$INSTALL_DIR/run.sh %f
Icon=$INSTALL_DIR/perfcraft.png
Path=$INSTALL_DIR
Terminal=false
Categories=Development;Engineering;Electronics;
StartupNotify=true
StartupWMClass=de.exit.perfcraft.Main
MimeType=application/x-perfcraft-project;
EOF
chmod +x "$DESKTOP_FILE"
cp "$DESKTOP_FILE" "$INSTALL_DIR/perfcraft.desktop"

if command -v update-mime-database &> /dev/null; then
    update-mime-database "$HOME/.local/share/mime" &> /dev/null || true
fi
if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$APPS_DIR" &> /dev/null || true
fi
if command -v gtk-update-icon-cache &> /dev/null; then
    gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" &> /dev/null || true
fi

echo "=================================================================="
echo "  [OK] Installation abgeschlossen!"
echo "  Ordner:        $INSTALL_DIR"
echo "  Startbefehl:   $INSTALL_DIR/run.sh"
echo "  Dateityp:      .perf Projekte oeffnen sich automatisch mit PerfCraft!"
echo "  Dash-Eintrag:  PerfCraft ist jetzt direkt im App-Menue verfuegbar."
echo "=================================================================="
