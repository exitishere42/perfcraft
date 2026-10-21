#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

echo ==================================================================
echo   Starte PerfCraft (JavaFX 21 LTS)
echo ==================================================================

JAR=$(ls target/perfcraft-*-all.jar 2>/dev/null | head -n 1)
if [ -n "$JAR" ] && [ -f "$JAR" ]; then
    java -jar "$JAR" "$@"
else
    echo "Fat JAR nicht gefunden. Starte via Maven exec:java..."
    mvn exec:java -Dexec.args="$*"
fi
