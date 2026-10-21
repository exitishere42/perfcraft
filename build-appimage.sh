#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="/tmp/perfcraft-appimage"
JAR_SRC=$(ls "$PROJECT_DIR"/target/perfcraft-*-all.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_SRC" ] || [ ! -f "$JAR_SRC" ]; then
    echo "[!] Fat JAR nicht gefunden unter $PROJECT_DIR/target/. Bitte zuerst 'mvn package' ausführen."
    exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/AppDir/usr/lib/perfcraft"
mkdir -p "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$BUILD_DIR/AppDir/usr/share/applications"

cp "$JAR_SRC" "$BUILD_DIR/AppDir/usr/lib/perfcraft/perfcraft.jar"
cp "$PROJECT_DIR/perfcraft.png" "$BUILD_DIR/AppDir/perfcraft.png"
cp "$PROJECT_DIR/perfcraft.png" "$BUILD_DIR/AppDir/.DirIcon"
cp "$PROJECT_DIR/perfcraft.png" "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps/perfcraft.png"
cp "$PROJECT_DIR/perfcraft.svg" "$BUILD_DIR/AppDir/perfcraft.svg"

cat > "$BUILD_DIR/AppDir/perfcraft.desktop" << 'EOF'
[Desktop Entry]
Type=Application
Name=PerfCraft
Comment=Perfboard & Stripboard CAD Designer
Exec=AppRun %f
Icon=perfcraft
Categories=Development;Engineering;Electronics;
Terminal=false
StartupNotify=true
StartupWMClass=de.exit.perfcraft.PerfCraftApp
MimeType=application/x-perfcraft-project;
EOF
cp "$BUILD_DIR/AppDir/perfcraft.desktop" "$BUILD_DIR/AppDir/usr/share/applications/perfcraft.desktop"

cat > "$BUILD_DIR/AppDir/AppRun" << 'EOF'
#!/usr/bin/env bash
HERE="$(dirname "$(readlink -f "${0}")")"
JAR_FILE="$HERE/usr/lib/perfcraft/perfcraft.jar"
if ! command -v java &> /dev/null; then
    echo "[!] Fehler: Java 21+ (java) wurde nicht gefunden. Bitte openjdk-21-jre installieren." >&2
    exit 1
fi
exec java -jar "$JAR_FILE" "$@"
EOF
chmod +x "$BUILD_DIR/AppDir/AppRun"

curl -fsSL -o "$BUILD_DIR/runtime-x86_64" "https://github.com/AppImage/type2-runtime/releases/download/continuous/runtime-x86_64"
mksquashfs "$BUILD_DIR/AppDir" "$BUILD_DIR/app.squashfs" -root-owned -noappend -comp gzip > /dev/null
cat "$BUILD_DIR/runtime-x86_64" "$BUILD_DIR/app.squashfs" > "$PROJECT_DIR/perfcraft-x86_64.AppImage"
chmod +x "$PROJECT_DIR/perfcraft-x86_64.AppImage"
echo "[OK] AppImage erfolgreich erstellt: $PROJECT_DIR/perfcraft-x86_64.AppImage"
