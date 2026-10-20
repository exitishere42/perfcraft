# Projektanweisung: PerfCraft (Perfboard Designer)

> **An den ausführenden Entwickler-Agenten:**
> Dieses Dokument ist deine verbindliche Spezifikation.
> Arbeite die Roadmap strikt Phase für Phase ab.
> Führe nach jedem Schritt die Verifikations-Befehle aus. Weiche nicht eigenmächtig von Stack oder Architektur ab.
> Halte dich strikt an die Material Design 2 Dark Standards und die Cross-Platform-Vorgaben (Windows & Linux).

---

## 1. Projektübersicht & Scope

### 1.1 Problem & Ziel
**PerfCraft** ist eine spezialisierte, intuitive Desktop-Applikation zur Konzeption, Verdrahtung und Dokumentation von Lochraster-Schaltungen (Perfboards / Pad-per-Hole Prototypenplatinen). 
Klassische EDA-Programme (wie KiCad oder Eagle) sind für Lochraster-Prototypen oft zu überladen, während allgemeine Zeichenprogramme kein Verständnis für Lochabstände (Pitch), Bauteil-Footprints und Lötbrücken haben. 
PerfCraft schließt diese Lücke durch ein hochpräzises, interaktives Lochraster-Canvas mit Bauteil-Bibliothek, Lötbrücken-Routing, automatischer Stücklistenerstellung (BOM) und Unterstützung für ein eigenes Projekt-Dateiformat (`.perf`).

### 1.2 In-Scope (MVP v1.0)
- **Start- & Projektverwaltung:**
  - Willkommensansicht mit "Neues Board", "Projekt öffnen" und Liste zuletzt geöffneter Projekte.
  - Dialog zur Erstellung eines neuen Boards: X- und Y-Lochrastergröße (z. B. Standard 24x18, Min 5x5, Max 200x200), Lochabstand 2.54 mm (0.1"), Projektname.
  - Speichern & Laden im Projektdateiformat `*.perf` (strukturiertes JSON).
  - Export: Hochauflösendes PNG des Boards sowie automatisierte Stückliste (BOM) als CSV/Text.
- **Interaktiver Canvas & Rendering:**
  - Substrat-Rendering im authentischen Perfboard-Grün mit realistischen Kupfer-Pads und Bohrlöchern.
  - Flüssiger Pan (Mausrad-Drag oder mittlere Maustaste) und stufenloser Zoom (Mausrad) mit Zentrierung.
  - Koordinatenbeschriftung (Spalten 1..N / A..Z, Zeilen 1..N).
  - 2-Layer System: Umschaltbar zwischen **Top Layer** (Bauteile, Jumper-Drähte oben) und **Bottom Layer** (Kupferbahnen, Lötbrücken, Lötstellen unten), inklusive halbtransparenter Durchschein-Ansicht ("Beide Ebenen").
- **Leiterbahnen, Lötbrücken & Jumper:**
  - Ziehen von Punkt-zu-Punkt-Leiterbahnen und Lötbrücken zwischen benachbarten oder entfernten Löchern auf dem Bottom-Layer.
  - Ziehen von isolierten Jumper-Drähten auf dem Top-Layer mit wählbaren Drahtfarben.
- **Bauteil-Bibliothek & Platzierung:**
  - Grundbauteile: Widerstände (axial), Keramikkondensatoren, Elektrolytkondensatoren, LEDs, Dioden, Transistoren (TO-92), ICs / DIL-Sockel (DIP-8, DIP-14, DIP-16, DIP-28), Stiftleisten (1 bis 40 Pins), Taster / Pushbuttons.
  - Automatischer Widerstands-Farbcode basierend auf dem Bauteilwert (z. B. 1kΩ, 10kΩ, 470Ω).
  - Drehen von Bauteilen um 90° (Taste `R`), Verschieben und Löschen (Taste `Entf`).
  - Automatische Referenz-Bezeichnung (R1, R2, C1, D1, IC1...).
- **Workflow & Usability:**
  - Vollständige Undo / Redo Historie (Strg+Z / Strg+Y).
  - Eigenschafts-Inspektor für markierte Bauteile (Wert, Einheit, Designator, Pins, Farbe).
  - Tastatur-Shortcuts für schnelles Arbeiten (V = Auswählen, W = Draht, T = Leiterbahn, R = Drehen, Entf = Löschen).

### 1.3 Out-of-Scope (Ausdrücklich nicht in v1.0)
- Keine SPICE-Schaltungssimulation (keine Spannungs-/Stromberechnung).
- Kein automatisches Autorouting von Netzen.
- Keine 3D-Modell-Ansicht (reine 2D-High-Precision-Vektorgrafik).
- Kein Streifenraster (Stripboard) mit Leiterbahn-Unterbrechern (fokussiert rein auf Pad-per-Hole Perfboards).
- Keine Cloud-Anbindung / kein Account-Zwang.

---

## 2. Tech-Stack & Feste Leitplanken

### 2.1 Plattform & Basis-Stack
- **Runtime:** Java 21 LTS (OpenJDK / Eclipse Temurin).
- **Betriebssysteme:** Windows 10/11 & Linux (Debian, Ubuntu, GNOME, X11, Wayland).
- **Build-Tool:** Apache Maven 3.9+ (`pom.xml`).
- **UI-Framework:** JavaFX 21 (Controls, Graphics, FXML, CSS).
- **JSON-Serialisierung:** Jackson Databind 2.17+ für `*.perf`-Dateien.
- **Test-Framework:** JUnit 5 (Jupiter).

### 2.2 Maven- & Package-Vorgaben (Strikte Richtlinie)
- **GroupId:** `de.exit`
- **ArtifactId:** `perfcraft`
- **Version:** `1.0.0-SNAPSHOT`
- **Root-Package:** `de.exit.perfcraft`
- **Verzeichnisstruktur:** Alle Java-Klassen liegen zwingend unter `src/main/java/de/exit/perfcraft/...`.
- **Plugins:**
  - `exec-maven-plugin` konfiguriert für `mvn exec:java` mit Main-Class `de.exit.perfcraft.Main`.
  - `maven-shade-plugin` für ein ausführbares Fat-JAR (`perfcraft-1.0.0-all.jar`).

### 2.3 UI-Design & Styling-Leitplanken
- **Material Design 2 Dark Standard:**
  - Hintergründe / Elevation: Base `#121212`, Surface `#1E1E1E`, Elevated `#242424`, Hover `#2C2C2C`.
  - Karten- & Button-Rundungen: `4px`.
  - Trennlinien / Borders: `1px solid #2C2C2C`.
  - **Primärfarbe (PCB Green):** `#00C853` (Akzent) / `#43A047` mit Text-Kontrast gemäß WCAG AA (>= 4.5:1).
  - **Perfboard Canvas Farbpalette:**
    - PCB Substrat: `#1B4D2E` (Dunkles Platinengrün).
    - Kupfer-Pads: `#D4AF37` / `#C87D2E` (Kupfer/Gold mit Innenkreis `#121212`).
    - Top Layer Jumper-Drähte: Wählbare Farben (Rot, Schwarz, Gelb, Blau, Grün, Weiß).
    - Bottom Layer Leiterbahnen / Lötbrücken: Silbernes Lötzinn (`#B0BEC5` / `#ECEFF1`) mit feinem Glanz.
    - Bauteil-Silkscreen: Hochweißes / Gelbliches Overlay (`#FFFFFF` / `#FFF9C4`).
- **Keine Klammerzusätze & keine Erklärungen in Labels:**
  - Buttons und Menüs heißen prägnant: `"Neu"`, `"Öffnen"`, `"Speichern"`, `"Top Layer"`, `"Bottom Layer"`, `"Widerstand"`, `"Kondensator"`. Keine Zusätze wie `"Top Layer (Bauteilseite)"`.

### 2.4 Anti-Halluzinations-Regeln
- ❌ **Kein Gradle** – Ausschließlich Apache Maven mit `pom.xml`.
- ❌ **Kein AWT / Swing** – Kein `java.awt.Desktop`, keine Swing-Komponenten; ausschließlich JavaFX.
- ❌ **Keine ungeprüften Bibliotheken** – Nur JavaFX, Jackson und JUnit einbinden.
- ❌ **Keine globalen statischen God-Klassen** – Klare Trennung von Datenmodell, State-Management und Canvas-Renderer.
- ❌ **Kein Overengineering** – Saubere, modulare Klassen unter 250 Zeilen.

---

## 3. UI-Layout nach LochMaster-Vorbild (CAD-Workbench)

PerfCraft orientiert sich strukturell und ergonomisch am bewährten **LochMaster 4.0 Layout**, übersetzt in das moderne **Material Design 2 Dark Theme**:

```text
+-----------------------------------------------------------------------------------------------+
| [Menüleiste] Datei  Bearbeiten  Ansicht  Platine  Bauteile  Hilfe                             |
+-----------------------------------------------------------------------------------------------+
| [Befehlsleiste] [Neu] [Öffnen] [Speichern] | [Undo] [Redo] | [Top] [Bottom] [X-Ray] | [+][-][Fit]|
+---+---------------------------------------------------------------------+---------------------+
| T | [Horizontales Lineal / Rulers: Loch-Index 1..N & mm-Skala mit Fadenkreuz-Marker]          |
| O +---+-----------------------------------------------------------------+ [Rechte Dock-Leiste]|
| O | V |                                                                 | Tabs / Accordion:   |
| L | e |                                                                 | 1. Bauteil-Katalog  |
| B | r |                                                                 |    - Widerstände    |
| O | t |                    ZENTRALER PERFBOARD CANVAS                   |    - Kondensatoren  |
| X | . |                                                                 |    - Halbleiter/ICs |
|   |   |        (Fotorealistisches Lochraster, Kupfer-Pads,             |    - Steckverbinder |
| [S| R |         Leiterbahnen, Bauteile mit Farbringen,                  | 2. Inspektor        |
| e | u |         stufenloser Zoom & Pan)                                 |    - Designator (R1)|
| l | l |                                                                 |    - Wert (10k Ω)   |
| e | e |                                                                 |    - Pins / Raster  |
| c | r |                                                                 | 3. Projekt-Baum     |
| t] |                                                                 |    - Alle Komponenten|
+---+---+-----------------------------------------------------------------+---------------------+
| [Statusleiste] Loch: X: 14, Y: 8 | Maß: 35.56 x 20.32 mm | Ebene: Bauteilseite | Zoom: 125% [====] |
+-----------------------------------------------------------------------------------------------+
```

### Die 6 Hauptbereiche der LochMaster-Architektur:
1. **Hauptmenüleiste & Schnell-Befehlsleiste (Top):**
   - Menüs: Datei, Bearbeiten, Ansicht, Platine, Extras.
   - Schnell-Befehlsleiste: Aktionen (Neu, Öffnen, Speichern), Historie (Rückgängig, Wiederholen), Ebenen-Umschalter (**Bauteilseite**, **Lötseite**, **Röntgen / X-Ray**) und Zoom-Buttons.
2. **Linke CAD-Werkzeugleiste (Toolbox):**
   - Schlanke Vertikalleiste für schnelle Werkzeugwechsel:
     - **Auswahl** (`V`) – Bauteile/Leiterbahnen markieren, verschieben.
     - **Drahtbrücke** (`W`) – Isolierte Jumper-Drähte auf Bauteilseite ziehen.
     - **Lötpunkt / Lötbrücke** (`T`) – Zinnbrücken auf Lötseite verbinden.
     - **Bauteil platzieren** (`C`) – Direktes Platzieren aus der Schnellauswahl.
     - **Beschriftung / Text** (`X`) – Freie Notizen & Markierungen auf der Platine.
     - **Löschen** (`Entf`) – Markierte Objekte entfernen.
3. **Dynamische Lineale (Rulers - Horizontal & Vertikal):**
   - Exakte Skalierung passend zum 2.54 mm Raster (gleichzeitige Anzeige von **Loch-Nummern 1..N** und Millimeter-Marken).
   - Ein aktiver Kontrast-Marker auf beiden Linealen folgt der aktuellen Mauszeigerposition in Echtzeit.
4. **Zentraler High-DPI Canvas:**
   - Dunkelgrünes PCB-Substrat (`#1B4D2E`), goldglänzende Kupferpads (`#D4AF37`) und schwarze Bohrungen.
   - Nahtloser Zoom (zentriert um Mauszeiger) und Pan via Mausrad-Klick/Drag.
   - Im **X-Ray / Röntgen-Modus** werden beide Ebenen gleichzeitig dargestellt: Bauteile halbtransparent auf der Oberseite, Lötbahnen auf der Unterseite.
5. **Rechte Dock-Seitenleiste (3-teilig):**
   - **Tab 1: Bauteil-Katalog:** Baumstruktur mit Kategorien (Passiv, Aktiv, ICs/Sockel, Stecker, Schalter) inklusive Suchfeld und Bauteil-Vorschau.
   - **Tab 2: Eigenschafts-Inspektor:** Ändern von Bauteilwerten (z. B. 10kΩ, 100nF), Designator (R1, C1), Lochabstand und Ausrichtung (0°, 90°, 180°, 270°).
   - **Tab 3: Projekt-Baum (Explorer):** Hierarchische Liste aller verbauten Komponenten und Brücken zur schnellen Selektion auch verdeckter Teile.
6. **Echtzeit-Statusleiste (Bottom):**
   - Live-Anzeige der Lochkoordinate (`Loch: X: 14, Y: 8`), Realmaß in mm (`35.56 mm, 20.32 mm`), aktive Ebene und stufenloser Zoom-Schieberegler.

---

## 4. Architektur & Dateistruktur

```text
perfcraft/
├── pom.xml
├── .gitignore
├── README.md
├── icon.svg
├── icon.png
├── icon.ico
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── de/
│   │   │       └── exit/
│   │   │           └── perfcraft/
│   │   │               ├── Main.java                 # Startklasse / Launcher
│   │   │               ├── PerfCraftApp.java         # JavaFX Application
│   │   │               │
│   │   │               ├── model/                    # Reines Datenmodell
│   │   │               │   ├── BoardConfig.java      # Rastergröße (X, Y, Pitch)
│   │   │               │   ├── HoleCoord.java        # X, Y Lochkoordinaten (0-basiert)
│   │   │               │   ├── ComponentType.java    # RESISTOR, CAPACITOR, LED, IC, etc.
│   │   │               │   ├── PlacedComponent.java  # Position, Pins, Rotation, Wert, Label
│   │   │               │   ├── WireTrace.java        # Start, Ende, Layer (TOP/BOTTOM), Farbe
│   │   │               │   ├── BoardLayer.java       # TOP, BOTTOM, BOTH (X-Ray)
│   │   │               │   └── PerfProject.java      # Wurzelobjekt für .perf JSON
│   │   │               │
│   │   │               ├── service/                  # Logik & I/O
│   │   │               │   ├── ProjectStorageService.java # Laden & Speichern von .perf
│   │   │               │   ├── ExportService.java         # PNG Canvas Export & BOM Export
│   │   │               │   ├── ResistorColorCode.java    # Farbcode-Berechnung (4/5 Ringe)
│   │   │               │   └── CommandHistory.java       # Undo/Redo Command-Pattern
│   │   │               │
│   │   │               ├── ui/                       # Präsentation & Controls
│   │   │               │   ├── theme/
│   │   │               │   │   └── MaterialTheme.java    # Farbdefinitionen & CSS-Generierung
│   │   │               │   ├── workbench/            # LochMaster-Workbench Layout
│   │   │               │   │   ├── WorkbenchView.java    # Hauptlayout (Top, Left, Center, Right, Bottom)
│   │   │               │   │   ├── TopCommandBar.java    # Menü & Schnell-Aktionen (Undo, Redo, Zoom)
│   │   │               │   │   ├── ToolboxBar.java       # Linke CAD-Leiste (Select, Wire, Solder, etc.)
│   │   │               │   │   ├── LayerControlBar.java  # Ebenen-Wahlschalter (Top / Bottom / X-Ray)
│   │   │               │   │   ├── StatusBar.java        # Loch-Koordinaten, mm-Maße, Layer, Zoom
│   │   │               │   │   └── sidebar/
│   │   │               │   │       ├── SidebarContainer.java # Tabs für Katalog, Inspektor, Baum
│   │   │               │   │       ├── ComponentCatalogPanel.java # Bauteil-Baum nach Kategorien
│   │   │               │   │       ├── InspectorPanel.java        # Eigenschaften des markierten Objekts
│   │   │               │   │       └── ProjectTreePanel.java      # Hierarchischer Objektbaum
│   │   │               │   ├── canvas/
│   │   │               │   │   ├── PerfBoardCanvas.java  # Hauptzeichenfläche (Pan, Zoom, Grid)
│   │   │               │   │   ├── RulerCanvas.java      # Dynamische Lochraster-Lineale mit Marker
│   │   │               │   │   ├── ComponentRenderer.java# Zeichnet Widerstände, ICs etc.
│   │   │               │   │   └── TraceRenderer.java    # Zeichnet Lötbrücken & Jumper
│   │   │               │   └── dialogs/
│   │   │               │       ├── NewBoardDialog.java   # Dialog für Board-Dimensionen
│   │   │               │       └── ComponentEditDialog.java # Wert & Eigenschaften anpassen
│   │   │               │
│   │   │               └── state/
│   │   │                   └── EditorState.java      # Aktives Tool, Selektion, aktiver Layer
│   │   │
│   │   └── resources/
│   │       ├── css/
│   │       │   └── material-dark.css                 # Dark-Theme Stylesheet
│   │       └── icons/                                # App- & Tool-Icons (SVG/PNG/ICO)
│   │
│   └── test/
│       └── java/
│           └── de/
│               └── exit/
│                   └── perfcraft/
│                       ├── model/
│                       │   └── PerfProjectTest.java   # Serialisierungstest
│                       └── service/
│                           ├── ResistorColorCodeTest.java # Farbcode-Prüfung
│                           └── CommandHistoryTest.java    # Undo/Redo Logik-Test
```

---

## 5. Datenmodell & Dateiformat (`.perf`)

### 4.1 JSON-Spezifikation (`.perf`)
```json
{
  "version": 1,
  "name": "Audio-Vorverstaerker",
  "board": {
    "columns": 24,
    "rows": 18,
    "pitchMm": 2.54
  },
  "components": [
    {
      "id": "c-1",
      "type": "RESISTOR",
      "designator": "R1",
      "value": "10k",
      "unit": "Ω",
      "start": { "x": 4, "y": 2 },
      "end": { "x": 8, "y": 2 },
      "rotation": 0,
      "properties": {
        "colorCode": ["BROWN", "BLACK", "ORANGE", "GOLD"]
      }
    },
    {
      "id": "c-2",
      "type": "IC_DIP",
      "designator": "IC1",
      "value": "NE5532",
      "start": { "x": 10, "y": 6 },
      "pinCount": 8,
      "rotation": 0
    }
  ],
  "traces": [
    {
      "id": "t-1",
      "layer": "BOTTOM",
      "start": { "x": 4, "y": 2 },
      "end": { "x": 4, "y": 6 },
      "color": "#B0BEC5"
    },
    {
      "id": "t-2",
      "layer": "TOP",
      "start": { "x": 8, "y": 2 },
      "end": { "x": 10, "y": 6 },
      "color": "#FF5252"
    }
  ]
}
```

---

## 6. Detaillierte Features mit "Definition of Done" (DoD)

### Feature 1: Neues Board & Startansicht
- **Beschreibung:** Startansicht mit Aktionen "Neues Board", "Öffnen" und Dialog zur Abfrage von Spalten (5..200), Zeilen (5..200) und Projektname.
- **Definition of Done:**
  - Klick auf "Neu" öffnet Modal mit validierten Eingabefeldern (Zahlen 5–200).
  - Nach Klick auf "Erstellen" initialisiert sich der Canvas mit exakt der gewünschten Matrix.
  - Standard-Größe bei Öffnung: 24 Spalten x 18 Zeilen.

### Feature 2: CAD-Workbench mit Rulers, Zoom & Pan (LochMaster-Stil)
- **Beschreibung:** Flüssiges Rendering der Platine mit Kupferringen, Bohrungen und umgebenden dynamischen Loch-/mm-Linealen (Rulers) mit mitfahrendem Mauszeiger-Marker.
- **Definition of Done:**
  - Drehen am Mausrad zoomt stufenlos mit Fokuspunkt unter dem Mauszeiger.
  - Horizontale und vertikale Lineale zeigen Loch-Nummern (1..N) und mm-Werte passend zum aktuellen Zoom und Pan.
  - Marker auf beiden Linealen visualisieren in Echtzeit die aktuelle Cursorposition.
  - Ziehen mit mittlerer Maustaste oder Leertaste+Klick verschiebt das Board flüssig (Pan).

### Feature 3: Linke CAD-Toolbox & Bauteil-Platzierung
- **Beschreibung:** Schnellauswahl über die linke Werkzeugleiste (`V` = Auswählen, `W` = Draht, `T` = Lötbrücke, `C` = Bauteil, `X` = Text, `Entf` = Löschen) und Platzierung auf den Board-Löchern.
- **Definition of Done:**
  - Vorschau des Werkzeugs / Bauteils folgt dem Cursor mit magnetischem Einrasten auf den Pads.
  - Klick platziert das Bauteil und vergibt fortlaufende Nummer (R1, R2, etc.).
  - Taste `R` dreht das Bauteil um 90 Grad.
  - Selektiertes Bauteil zeigt Eigenschaften im rechten Inspektor.

### Feature 4: Widerstands-Farbcode-Generator
- **Beschreibung:** Bei Eingabe gängiger Widerstandswerte (z. B. `470`, `1k`, `4.7k`, `10k`, `100k`, `1M`) berechnet das System die genauen Farbbänder und rendert sie direkt auf dem Bauteilkörper.
- **Definition of Done:**
  - Eingabe von "10k" erzeugt Ringe: Braun (1), Schwarz (0), Orange (10^3), Gold (5%).
  - Eigene Unit-Tests decken alle Standard-E12/E24-Werte fehlerfrei ab.

### Feature 5: Lötbrücken & Jumper-Verdrahtung mit Layer-Steuerung
- **Beschreibung:** Ziehen von Leiterbahnen / Lötbrücken auf dem Bottom-Layer und isolierten Drähten auf dem Top-Layer mit Ebenen-Umschaltung (Top, Bottom, Röntgen / X-Ray).
- **Definition of Done:**
  - Im Bottom-Layer gezeichnete Bahnen werden als silberne Lötbahn dargestellt.
  - Im Top-Layer gezeichnete Bahnen werden als farbige Jumper-Drähte mit End-Pins dargestellt.
  - Schalter in der Befehlsleiste ("Top Layer", "Bottom Layer", "Röntgen") schaltet zwischen reiner Oberseite, reiner Unterseite und durchscheinender Röntgen-Ansicht um.

### Feature 6: Rechte Dock-Leiste & Projekt-Explorer
- **Beschreibung:** 3-teiliges Seitenpanel mit Katalog (kategorisierte Bauteile mit Suche), Inspektor (Werte & Drehung ändern) und hierarchischem Projekt-Objektbaum.
- **Definition of Done:**
  - Klick im Projekt-Baum selektiert das Bauteil auf dem Canvas und hebt es optisch hervor.
  - Änderungen im Inspektor aktualisieren das Bauteil auf dem Canvas sofort.

### Feature 7: Speichern, Laden & Export
- **Beschreibung:** Persistenz im `.perf`-Format sowie Bildexport und BOM.
- **Definition of Done:**
  - Datei -> Speichern (`Strg+S`) schreibt eine gültige `.perf`-JSON-Datei auf Festplatte.
  - Datei -> Öffnen (`Strg+O`) stellt den Board-Zustand 1:1 wieder her.
  - Datei -> Export PNG rendert das gesamte Board ohne UI-Rahmen in eine PNG-Datei.
  - Datei -> Export Stückliste (BOM) exportiert CSV mit Spalten: `Designator, Typ, Wert, Anzahl`.

---

## 7. Fehlermatrix & Edge Cases

| Situation / Ausnahme | Systemverhalten | Feedback an Nutzer |
| :--- | :--- | :--- |
| Ungültige Board-Größe (<5 oder >200) | Feld wird rot markiert, "Erstellen" deaktiviert | Tooltip mit Hinweis auf Wertebereich 5–200 |
| Bauteil ragt über Board-Rand hinaus | Platzierung wird blockiert (roter Cursor) | Subtile optische Rückmeldung, kein Crash |
| Ungültiges / korruptes `.perf` File | Fehler abfangen, Fallback auf leeres Projekt | Material-Dialog: "Datei konnte nicht gelesen werden" |
| Ungültiger Widerstandswert (z. B. "abc") | Behält vorherigen Wert bei oder markiert neutral | Keine Farbbänder, neutraler Widerstandskörper |
| Nicht gespeicherte Änderungen bei Beenden | Dialogabfrage: "Möchten Sie vor dem Schließen speichern?" | Optionen: Speichern / Verwerfen / Abbrechen |

---

## 8. Phasenweise Implementierungs-Roadmap

### Phase 1: Setup & Maven-Infrastruktur
- [ ] Maven `pom.xml` mit Java 21, JavaFX 21, Jackson Databind, JUnit 5 einrichten.
- [ ] Grundstruktur anlegen (`de.exit.perfcraft`).
- [ ] Material Design 2 Dark CSS Grundgerüst (`material-dark.css`) bereitstellen.
- [ ] App-Icon in Fensterleiste einbinden.
- [ ] **Verifikation:** `mvn clean test` und `mvn compile` laufen fehlerfrei durch.

### Phase 2: Datenmodell & Serialisierung
- [ ] Kern-Modellklassen (`HoleCoord`, `BoardConfig`, `PlacedComponent`, `WireTrace`, `PerfProject`) implementieren.
- [ ] `ProjectStorageService` mit Jackson Databind erstellen.
- [ ] Unit-Tests für Serialisierung und Deserialisierung schreiben.
- [ ] **Verifikation:** 100% bestandene Unit-Tests in `PerfProjectTest`.

### Phase 3: Canvas-Engine & LochMaster-Rulers
- [ ] `PerfBoardCanvas` mit stufenlosem Zoom und flüssigem Pan implementieren.
- [ ] Grid-Rendering (PCB-Grün, Kupfer-Ringe, Loch-Masken).
- [ ] `RulerCanvas` (Horizontales & vertikales Loch-Lineal mit mitfahrendem Cursor-Marker).
- [ ] Ebenen-Umschaltung (Top-Layer, Bottom-Layer, Röntgen/X-Ray) mit Alpha-Transparenz.
- [ ] **Verifikation:** Interaktiver Start via `mvn exec:java`, flüssiges Zoomen, Pannen und Lineal-Tracking.

### Phase 4: Werkzeuge, Bauteile & Leiterbahnen
- [ ] Bauteil-Renderer für Widerstände (inkl. `ResistorColorCode`), Kondensatoren, LEDs, ICs und Stiftleisten.
- [ ] Linke Toolbox (`ToolboxBar`) für schnellen Werkzeugwechsel (Select, Wire, Solder, Component, Text, Delete).
- [ ] Leiterbahn- & Jumper-Tool (Lötbrücken auf Bottom, isolierte Drähte auf Top).
- [ ] Selektions- und Verschiebe-Werkzeug, Drehung (`R`), Löschen (`Entf`).
- [ ] **Verifikation:** Alle Bauteil-Typen lassen sich platzieren, drehen, verschieben und mit Traces verbinden.

### Phase 5: Workbench-Layout, Dock-Sidebar & Undo/Redo
- [ ] LochMaster-Workbench-Layout (`WorkbenchView` mit TopCommandBar, ToolboxBar, Rulers, Canvas, Sidebar, StatusBar).
- [ ] Rechte Dock-Sidebar (`SidebarContainer` mit Katalog, Inspektor, Projektbaum).
- [ ] Echtzeit-Statusleiste (Loch-Koordinaten, mm-Werte, Layer, Zoom-Slider).
- [ ] `CommandHistory` für Undo/Redo (`Strg+Z`, `Strg+Y`) einbinden.
- [ ] PNG-Export und Stücklisten-Export (BOM) implementieren.
- [ ] **Verifikation:** Kompletter End-to-End Workflow (Neu -> Zeichnen -> Speichern -> Laden -> Export).

---

## 9. Start- und Testbefehle
- **Kompilieren:** `mvn clean compile`
- **Unit-Tests ausführen:** `mvn test`
- **Anwendung starten:** `mvn exec:java`
- **Fat-JAR bauen:** `mvn clean package`
- **Ausführen des JARs:** `java -jar target/perfcraft-1.0.0-all.jar`
