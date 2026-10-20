# Projektanweisung: PerfCraft (Perfboard CAD Workbench)

> **Verbindliche Spezifikation für den ausführenden Entwickler-Agenten**
> Version: 1.0.0-PROD
> Zielsystem: Java 21 LTS | JavaFX 21 | Apache Maven | Material Design 2 Dark Standard
> Referenz-Vorbild: ABACOM LochMaster 4.0 CAD-Workbench
> Dieses Dokument ist die ausnahmslos gültige Arbeitsgrundlage. Jede Klasse, jedes Interface, jedes Tool und jede Rendering-Schicht muss exakt den hier definierten Vorgaben entsprechen.

---

## Inhaltsverzeichnis
1. [Präambel, Mission & Design-Philosophie](#1-präambel-mission--design-philosophie)
2. [Tech-Stack, Frameworks & Feste Leitplanken](#2-tech-stack-frameworks--feste-leitplanken)
3. [UI-Layout nach LochMaster-Vorbild (CAD-Workbench Architektur)](#3-ui-layout-nach-lochmaster-vorbild-cad-workbench-architektur)
   - 3.1 Gesamtlayout & Responsive Aufteilung
   - 3.2 Menüleiste (Menüs & Aktionen)
   - 3.3 Befehlsleiste (Top Command Bar)
   - 3.4 Ebenen-Steuerung (Bauteilseite, Lötseite, Röntgen/X-Ray)
   - 3.5 Linke CAD-Toolbox (Werkzeugleiste)
   - 3.6 Dynamische Dual-Einheiten-Lineale (Ruler Engine)
   - 3.7 Zentraler Perfboard Canvas & Substrat-Rendering
   - 3.8 Rechte Dock-Sidebar (Katalog, Inspektor, Projekt-Baum)
   - 3.9 Echtzeit-Statusleiste & Messdaten
   - 3.10 KiCad-inspiriertes Appearance- & Sichtbarkeits-Panel
4. [Interaktives Werkzeug-System (CAD Tools & State Machine)](#4-interaktives-werkzeug-system-cad-tools--state-machine)
   - 4.1 Auswahl-Werkzeug (Pointer / Selection Tool)
   - 4.2 Jumper-Draht-Werkzeug (Top Layer Wire Tool)
   - 4.3 Lötbrücken-Werkzeug (Bottom Layer Solder Bridge Tool)
   - 4.4 Bauteil-Platzierungs-Werkzeug (Component Placement Tool)
   - 4.5 Text- & Beschriftungs-Werkzeug (Silkscreen Text Tool)
   - 4.6 Verbindungsprüfer (Continuity & Net Highlighter Tool)
   - 4.7 Lösch-Werkzeug (Eraser / Delete Tool)
   - 4.8 Ratsnest / Airwire-Engine (Logische Netze & Flugdrähte wie in KiCad)
   - 4.9 Design Rule Check (DRC) & Kollisions-Engine (KiCad-Standard)
   - 4.10 Relatives Koordinaten-Messwerkzeug (KiCad Leertasten-Delta-Modus)
5. [Bauteil-Bibliothek & Footprint-Spezifikationen](#5-bauteil-bibliothek--footprint-spezifikationen)
   - 5.1 Passive Bauteile (Widerstände, Kondensatoren, Potentiometer)
   - 5.2 Aktive Bauteile (Dioden, LEDs, Transistoren)
   - 5.3 Integrierte Schaltungen (DIL/DIP-Sockel & ICs)
   - 5.4 Steckverbinder & Mechanik (Stiftleisten, Klemmen, Taster)
6. [Widerstands-Farbcode-Engine (Mathematik & Algorithmus)](#6-widerstands-farbcode-engine-mathematik--algorithmus)
7. [Canvas Rendering-Pipeline & Grafik-Mathematik](#7-canvas-rendering-pipeline--grafik-mathematik)
   - 7.1 Koordinaten-Systeme (Screen vs. World vs. Grid)
   - 7.2 Transformationsmatrix, Zoom & Pan
   - 7.3 Vollständige Implementierung von `CoordinateTransform.java`
   - 7.4 Viewport-Culling für Hochleistungs-Rendering
   - 7.5 Multi-Layer Zeichenschichten & Z-Index
8. [Vollständiges Datenmodell & `.perf` Dateiformat](#8-vollständiges-datenmodell--perf-dateiformat)
   - 8.1 JSON-Schemaspezifikation
   - 8.2 Java-Klassenstruktur im `model`-Package
9. [Undo/Redo Transaktions-Architektur (Command Pattern)](#9-undoredo-transaktions-architektur-command-pattern)
10. [Stücklisten- (BOM) & Export-Engine](#10-stücklisten--bom--export-engine)
11. [Vollständige Tastatur- & Maus-Interaktionsmatrix](#11-vollständige-tastatur--maus-interaktionsmatrix)
12. [Detaillierte Fehlermatrix & Edge Cases](#12-detaillierte-fehlermatrix--edge-cases)
13. [Phasenweise Implementierungs-Roadmap](#13-phasenweise-implementierungs-roadmap)
14. [Start-, Build- und Verifikations-Befehle](#14-start--build--und-verifikations-befehle)

---

## 1. Präambel, Mission & Design-Philosophie

### 1.1 Mission von PerfCraft
PerfCraft ist eine spezialisierte, hochpräzise Desktop-CAD-Anwendung für Elektronikentwickler, Maker und Hobbyisten zur Planung, Verdrahtung und Dokumentation von Lochrasterplatinen (Perfboards / Pad-per-Hole Prototypenboards).

Klassische EDA-Werkzeuge (wie KiCad, Altium oder Eagle) sind primär auf die industrielle Fertigung von Mehrlagen-Leiterplatten (PCBs) ausgelegt. Sie zwingen Nutzer durch komplexe Schematics, Netzlisten, ERC-Prüfungen und Gerber-Exporte. Bei der praktischen Labor- und Prototypenentwicklung auf Lochrasterplatten hingegen zählt die **direkte räumliche Platzierung**, die **mechanische Passgenauigkeit von Bauteilen im 2.54 mm Raster**, die **gezielte Verlegung von isolierten Schaltdrähten auf der Bauteilseite** und das **Ziehen von Zinnbrücken auf der Lötseite**.

PerfCraft verbindet das legendäre Workbench-Workflow-Konzept des Branchenstandards **ABACOM LochMaster 4.0** mit modernen Softwarestandards:
- **Material Design 2 Dark Theme** für ermüdungsfreies Arbeiten im Labor.
- **Flüssige Vektorgrafik-Canvas-Engine** mit stufenlosem Zoom und verzögerungsfreiem Panning.
- **Robuste Java 21 LTS & JavaFX Plattform** mit nativer Ausführbarkeit auf Windows und Linux.
- **Klares, menschenlesbares JSON-Projektformat (`.perf`)** für maximale Zukunfts- und Versionsverwaltungssicherheit.

### 1.2 Die LochMaster-DNA
PerfCraft adaptiert zentrale Kernparadigmen von LochMaster:
1. **Das Board ist die Realität:** Es gibt keine Trennung zwischen Schaltplan und Platinenlayout; der Anwender arbeitet direkt auf dem physikalischen Abbild der Lochrasterplatine.
2. **Physikalische 2-Ebenen-Metapher:**
   - **Bauteilseite (Top Layer):** Hier liegen die Bauteilkörper, Silkscreen-Beschriftungen und isolierte Schaltdrähte / Jumper.
   - **Lötseite (Bottom Layer):** Hier liegen die Kupfer-Pads, blanke Drähte und verlötete Zinnbrücken.
   - **Röntgenansicht (X-Ray Mode):** Gleichzeitiges Betrachten beider Seiten mit transparenter Durchsicht – exakt wie beim Gegenlicht-Halten einer echten Platine.
3. **Dual-Einheiten-Lineale (Rulers):** Gleichzeitige Anzeige von Loch-Koordinaten (Index 1..N) und echten Millimeter-/Zoll-Abmessungen mit mitfahrenden Cursor-Markern.
4. **Haptische CAD-Werkzeugleiste:** Direkter Zugriff auf Auswahl, Draht, Lötbrücke, Bauteil, Text und Prüfwerkzeuge.

---

## 2. Tech-Stack, Frameworks & Feste Leitplanken

### 2.1 Plattform & Basis-Bibliotheken
- **Java-Version:** Java 21 LTS (Oracle OpenJDK, Eclipse Temurin).
- **Betriebssysteme:** Windows 10/11 (x64) & Linux (Debian, Ubuntu, Fedora, GNOME Wayland/X11).
- **Build- & Dependency-Management:** Apache Maven 3.9+ (`pom.xml`).
- **UI-Framework:** JavaFX 21.0.2 (Module: `javafx.controls`, `javafx.graphics`, `javafx.fxml`).
- **Serialisierung & JSON-Parsing:** Jackson Databind 2.17.0 (`com.fasterxml.jackson.core:jackson-databind`).
- **Testing:** JUnit 5.10.2 Jupiter (`org.junit.jupiter:junit-jupiter`).

### 2.2 Strikte Maven- & Code-Konventionen
- **GroupId:** `de.exit`
- **ArtifactId:** `perfcraft`
- **Version:** `1.0.0-SNAPSHOT`
- **Root-Package:** `de.exit.perfcraft`
- **Source-Verzeichnis:** Alle Klassen liegen unter `src/main/java/de/exit/perfcraft/...`.
- **Ressourcen:** Alle Assets (CSS, SVG, Icons) liegen unter `src/main/resources/...`.
- **Verpackung:** `maven-shade-plugin` erzeugt eine ausführbare Fat-JAR unter `target/perfcraft-1.0.0-all.jar`.
- **Direkter Start:** `mvn exec:java` startet die Anwendung via `de.exit.perfcraft.Main`.

### 2.3 UI-Design & Material Design 2 Dark Standard
- **Elevation & Flächenfarben:**
  - Background (Basis): `#121212`
  - Surface Level 1 (Workbench-Rahmen & Toolbars): `#1E1E1E`
  - Surface Level 2 (Dock-Panels, Dialoge & Karten): `#242424`
  - Surface Level 3 (Inputs, Dropdowns & Selektionen): `#2C2C2C`
  - Hover-Zustand: `#383838`
- **Kantenrundungen:** Durchgängig `4px` für Karten, Buttons, Eingabefelder und Tooltips.
- **Trennlinien:** `1px solid #2C2C2C`.
- **Primärfarbe (Perfboard Green):** `#00C853` (Akzent) / `#43A047` (Gedämpft). Text auf Primärfarbe zwingend Kontrastverhältnis >= 4.5:1.
- **Perfboard Canvas Farbpalette:**
  - Substrat dunkelgrün: `#1B4D2E`
  - Substrat-Kantenfase: `#143922`
  - Kupfer-Pads (Außenring): `#D4AF37` (Mattgold) / `#C87D2E` (Kupfer)
  - Pad-Innenbohrung: `#0D1F14` (Lochschatten)
  - Lötbrücken (Lötzinn): `#CFD8DC` mit feinem Glanz `#ECEFF1`
  - Silkscreen-Aufdruck: `#F5F5F5` mit feiner Kontur
- **Strikte Bezeichnungsregel:** Niemals Erklärungen, Klammerzusätze oder Richtwerte in Buttons, Menüs oder Dropdowns rendern (z. B. nur `"Top Layer"`, niemals `"Top Layer (Bauteilseite)"`).

### 2.4 Anti-Halluzinations-Regeln
- ❌ **Kein Gradle** – Ausschließlich Apache Maven mit `pom.xml`.
- ❌ **Kein Swing / AWT** – Keine Verwendung von `java.awt.Desktop` oder Swing-Komponenten; ausschließlich JavaFX.
- ❌ **Keine ungeprüften Bibliotheken** – Keine externen Drawing-Engines einbinden; der Canvas wird nativ mit JavaFX `GraphicsContext` gerendert.
- ❌ **Keine monolithischen Dateien** – Jede Klasse hat eine klare Einzelverantwortung (Single Responsibility) und bleibt überschaubar (< 300 Zeilen).

---

## 3. UI-Layout nach LochMaster-Vorbild (CAD-Workbench Architektur)

### 3.1 Gesamtlayout & Responsive Aufteilung
Das Workbench-Fenster nutzt ein hierarchisches `BorderPane`-Layout, das die Arbeitsfläche in klar definierte Funktionsbereiche gliedert:

```text
+---------------------------------------------------------------------------------------------------------+
| [Menüleiste] Datei   Bearbeiten   Ansicht   Platine   Bauteile   Extras   Hilfe                         |
+---------------------------------------------------------------------------------------------------------+
| [Top Command Bar]                                                                                       |
| [Neu] [Öffnen] [Speichern] | [Undo] [Redo] | [Top] [Bottom] [X-Ray] | [Zoom In] [Zoom Out] [1:1] [Fit]  |
+---+-------------------------------------------------------------------------------+---------------------+
| T | [Horizontales Lineal / Ruler: Loch-Index 1..N & mm-Skala mit Cursor-Marker]   | [Rechte Dock-Leiste]|
| O +---+---------------------------------------------------------------------------+ [Tabs:]             |
| O | V |                                                                           | [Katalog]           |
| L | e |                                                                           | [Inspektor]         |
| B | r |                                                                           | [Projekt-Baum]      |
| O | t |                       ZENTRALER PERFBOARD CANVAS                          +---------------------+
| X | . |                                                                           | AKTIVER TAB-INHALT  |
|   |   |        (Fotorealistisches Lochraster, Kupfer-Pads,                       |                     |
| [S| R |         Leiterbahnen, Bauteile mit Farbringen,                            | - Bauteilsuche      |
| e | u |         stufenloser Zoom & Pan)                                           | - Parameter-Inputs  |
| l | l |                                                                           | - Baum-Hierarchie   |
| e | e |                                                                           |                     |
| c | r |                                                                           |                     |
| t] |                                                                           |                     |
+---+---+---------------------------------------------------------------------------+---------------------+
| [Statusleiste] Loch: X: 14, Y: 8 | Realmaß: 35.56 x 20.32 mm | Ebene: Top Layer | Zoom: 125% [====]     |
+---------------------------------------------------------------------------------------------------------+
```

### 3.2 Menüleiste (Menüs & Aktionen)
Die Menüleiste (`TopMenuBar`) stellt den kompletten Zugriff auf alle Applikationsbefehle bereit:

1. **Datei:**
   - *Neu...* (`Strg+N`): Öffnet den `NewBoardDialog`.
   - *Öffnen...* (`Strg+O`): Lädt eine `.perf`-Projektdatei über den systemnativen FileChooser.
   - *Speichern* (`Strg+S`): Speichert das aktuelle Projekt direkt.
   - *Speichern unter...* (`Strg+Umschalt+S`): Speichert unter neuem Pfad.
   - *Trennlinie*
   - *Exportieren als Bild (PNG)...*: Exportiert die Platine in frei wählbarer Auflösung.
   - *Exportieren als Stückliste (CSV)...*: Erstellt eine konsolidierte Bauteilliste.
   - *Trennlinie*
   - *Beenden* (`Alt+F4`): Prüft auf ungespeicherte Änderungen und schließt PerfCraft.
2. **Bearbeiten:**
   - *Rückgängig* (`Strg+Z`): Macht die letzte Aktion im `CommandHistory` rückgängig.
   - *Wiederholen* (`Strg+Y`): Führt die zuletzt verworfene Aktion wieder aus.
   - *Trennlinie*
   - *Ausschneiden* (`Strg+X`): Kopiert selektierte Komponenten in die Zwischenablage und löscht sie.
   - *Kopieren* (`Strg+C`): Kopiert selektierte Komponenten.
   - *Einfügen* (`Strg+V`): Fügt Komponenten an Mausposition ein.
   - *Löschen* (`Entf`): Löscht alle markierten Objekte.
   - *Alles auswählen* (`Strg+A`): Selektiert alle Bauteile und Leiterbahnen.
3. **Ansicht:**
   - *Bauteilseite (Top Layer)* (`F1`): Zeigt ausschließlich die Oberseite der Platine.
   - *Lötseite (Bottom Layer)* (`F2`): Zeigt ausschließlich die Unterseite der Platine.
   - *Röntgenansicht (X-Ray)* (`F3`): Durchscheinende Ansicht beider Ebenen.
   - *Trennlinie*
   - *Einzoomen* (`Strg++` / Mausrad oben): Erhöht den Zoom-Faktor um 15%.
   - *Auszoomen* (`Strg+-` / Mausrad unten): Verringert den Zoom-Faktor um 15%.
   - *Originalgröße 100%* (`Strg+0`): Setzt den Maßstab exakt auf 1:1 zurück.
   - *Ganzes Board anzeigen* (`Strg+F`): Passt Zoom und Pan so an, dass das Board zentriert den Canvas füllt.
   - *Trennlinie*
   - *Lineale ein-/ausblenden* (`Strg+R`): Schaltet die horizontalen und vertikalen Rulers um.
   - *Gitterbeschriftung umschalten*: Zahlen (1..N) oder Buchstaben/Zahlen (A..Z, 1..N).
4. **Platine:**
   - *Platinengröße ändern...*: Ändert Spalten- und Zeilenzahl des bestehenden Projekts.
   - *Platine um 90° drehen*: Rotiert die gesamte Platine inklusive aller Bauteile im Uhrzeigersinn.
   - *Platine horizontal spiegeln*: Spiegelt das Layout.
5. **Bauteile:**
   - *Bauteil im Uhrzeigersinn drehen* (`R`): Rotiert selektierte Bauteile um 90°.
   - *Widerstands-Farbcode-Rechner...*: Öffnet ein schnelles Hilfstool zur Farbcode-Ermittlung.
   - *Automatisch neu nummerieren*: Vergibt Bezeichner (R1, R2, C1, C2) geordnet von links oben nach rechts unten.
6. **Hilfe:**
   - *Tastaturkürzel*: Übersicht aller Schnelltasten.
   - *Über PerfCraft*: Dialog mit Version, Entwickler-Credits und Lizenzhinweisen.

### 3.3 Befehlsleiste (Top Command Bar)
Die Befehlsleiste (`TopCommandBar`) liegt direkt unter dem Menü und enthält ergonomische Schnellzugriff-Buttons mit 16x16 Lucide-Vektor-Icons:
- **Dateigruppe:** `Neu`, `Öffnen`, `Speichern`.
- **Historiegruppe:** `Rückgängig` (mit Tooltip des letzten Befehls), `Wiederholen`.
- **Ebenengruppe:** Segmentierter 3-Wege-Schalter für `Top Layer`, `Bottom Layer` und `X-Ray`.
- **Zoomgruppe:** `Vergrößern`, `Verkleinern`, `100%`, `Einpassen`.

### 3.4 Ebenen-Steuerung (Bauteilseite, Lötseite, Röntgen/X-Ray)
Die Umschaltung der Arbeits- und Anzeigeebene ist das Herzstück des LochMaster-Prinzips:
1. **Top Layer (Bauteilseite):**
   - Fokus auf Bauteilkörpern, Silkscreen, Beschriftungen und isolierten Jumper-Drähten.
   - Die Lötstellen und Kupfer-Pads werden als Basishintergrund angezeigt.
   - Zinnbrücken auf der Lötseite sind unsichtbar oder maximal mit 10% Deckkraft angedeutet.
2. **Bottom Layer (Lötseite):**
   - Fokus auf Kupfer-Pads, Lötstellen und silbernen Zinnbrücken.
   - Die Bauteile auf der Oberseite werden ausgeblendet oder als feine Silhouetten dargestellt.
   - Die Platine wird horizontal gespiegelt dargestellt oder transparent durchleuchtet, sodass die Orientierung intuitiv bleibt.
3. **X-Ray / Röntgen-Modus:**
   - Beide Ebenen sind voll sichtbar: Die Bauteile auf der Oberseite werden mit 65% Deckkraft semitransparent gerendert.
   - Sämtliche Lötbrücken der Unterseite scheinen klar und kontrastreich durch.
   - Ideal für das Verdrahten komplexer Schaltungen, um Leiterbahn-Kollisionen unter Bauteilen sofort zu erkennen.

### 3.5 Linke CAD-Toolbox (Werkzeugleiste)
Die vertikale Leiste (`ToolboxBar`) am linken Fensterrand ist 48 Pixel breit und enthält 8 hochspezialisierte Werkzeug-Icons:
1. **Auswahl (`V`):** Selektieren, Verschieben, Mehrfachauswahl via Gummiband-Rechteck.
2. **Jumper-Draht (`W`):** Verlegen isolierter Schaltdrähte auf dem Top Layer.
3. **Lötbrücke (`T`):** Ziehen von verlöteten Zinnbrücken auf dem Bottom Layer.
4. **Bauteil (`C`):** Direktes Platzieren des im Katalog aktiven Bauteils.
5. **Leiterbahn-Trenner (`K`):** Lochraster-Trennung (für spätere Streifenraster-Erweiterung vorbereitet).
6. **Text (`X`):** Platzieren von frei konfigurierbaren Beschriftungstexten auf dem Silkscreen.
7. **Verbindungsprüfer (`N`):** Klick auf einen Pin hebt das gesamte elektrisch verbundene Netz farbig hervor.
8. **Löschen (`Entf`):** Chirurgisches Löschen des angeklickten Elements.

### 3.6 Dynamische Dual-Einheiten-Lineale (Ruler Engine)
Um den zentralen Canvas liegen das **horizontale Lineal (`TopRuler`)** und das **vertikale Lineal (`LeftRuler`)**:
- **Abmessung:** 24 Pixel Höhe (horizontal) bzw. 28 Pixel Breite (vertikal).
- **Zwei Skalen gleichzeitig:**
  - **Primärskala (Loch-Index):** Große Zahlen `1, 2, 3 ... N` zentriert über jeder Lochspalte/Lochzeile.
  - **Sekundärskala (Millimeter):** Feine Teilstriche mit mm-Werten (0 mm, 2.54 mm, 5.08 mm, 10.0 mm).
- **Echtzeit-Tracking-Marker:**
  - Ein 2px breiter, leuchtend grüner Akzentstrich (`#00C853`) auf beiden Linealen wandert synchron mit dem Mauszeiger mit.
  - Dadurch sieht der Entwickler sofort und ohne Nachzählen, in welcher Lochspalte und Lochzeile sich der Cursor befindet.
- **Nullpunkt:** Exakt an der oberen linken Ecke des ersten Lochs (Koordinate 0,0).

### 3.7 Zentraler Perfboard Canvas & Substrat-Rendering
Die Zeichenfläche (`PerfBoardCanvas`) rendert die Platine fotorealistisch mit Subpixel-Genauigkeit:
- **Platinen-Körper:** Grünes Epoxid-Substrat (`#1B4D2E`) mit abgerundeten Ecken und umlaufender Fase.
- **Kupfer-Pads:** Ringförmige Pads um jedes Loch mit gold/kupfernem Glanz (`#D4AF37`), feiner dunkler Außenkante (`#8C6D1F`) und Innenkreis für die Bohrung (`#0D1F14`).
- **Snapping-Engine:** Der Mauszeiger rastet magnetisch auf dem nächstgelegenen Loch-Mittelpunkt ein (Fangradius 60% des Lochabstands).

### 3.8 Rechte Dock-Sidebar (Katalog, Inspektor, Projekt-Baum)
Die rechte Seitenleiste ist 280 Pixel breit, einklappbar und über drei Material-Tabs organisiert:
- **Tab 1: Bauteil-Katalog (`CatalogPanel`):**
  - Live-Suchfeld oben mit Schnellfilterung.
  - Akkordeon-Baumstruktur: *Widerstände*, *Kondensatoren*, *Dioden & LEDs*, *Transistoren*, *ICs & Sockel*, *Steckverbinder*, *Mechanik*.
  - Grafische Miniatur-Vorschau des ausgewählten Bauteils mit Maßangabe (z. B. "Raster 4 Löcher = 10.16 mm").
- **Tab 2: Eigenschafts-Inspektor (`InspectorPanel`):**
  - Passt sich dynamisch an das aktuell markierte Objekt an:
    - Bauteil-Bezeichner (z. B. `R1`, `C3`, `IC1`).
    - Elektrischer Wert (z. B. `10k`, `100n`, `NE555`).
    - Einheit (`Ω`, `F`, `H`, etc.).
    - Lochraster-Spannweite (Anzahl Löcher zwischen den Pins).
    - Ausrichtung (Drehung in 90°-Schritten: 0°, 90°, 180°, 270°).
    - Draht-/Körperfarbe.
- **Tab 3: Projekt-Objektbaum (`ProjectTreePanel`):**
  - Hierarchische Auflistung sämtlicher auf dem Board verbauter Elemente.
  - Gruppiert nach Bauteilen, Jumper-Drähten und Lötbrücken.
  - Klick auf ein Element im Baum selektiert und zentriert es sofort auf dem Canvas.

### 3.9 Echtzeit-Statusleiste & Messdaten
Die Fußleiste (`StatusBar`) ist 28 Pixel hoch und liefert permanente Messwerte:
- **Loch-Koordinaten:** `Loch: X: 14, Y: 8` (1-basiert für den Nutzer, 0-basiert intern).
- **Metrische Position:** `Pos: 35.56 mm, 20.32 mm` (bezogen auf den Platinen-Nullpunkt).
- **KiCad-Style Relativmessung:** `ΔX: +4 (10.16 mm) | ΔY: -2 (-5.08 mm) | Dist: 11.36 mm | Winkel: -26.6°` (Nullpunkt setzbar mit `Leertaste`).
- **Aktiver Layer:** `Ebene: Bauteilseite (Top)`.
- **Zoom-Steuerung:** Prozentanzeige (`125%`) mit integriertem Schieberegler.

### 3.10 KiCad-inspiriertes Appearance- & Sichtbarkeits-Panel
Angelehnt an das moderne **KiCad Appearance Panel** verfügt PerfCraft über eine kompakte Sichtbarkeitsleiste mit schnellen Ein-Klick-Augensymbolen (👁) zur selektiven Schaltung visueller Schichten:
- 👁 **Silkscreen (F.SilkS):** Bauteilbeschriftungen, Pin-1-Punkte, Textmarkierungen.
- 👁 **Bauteilkörper (F.Fab):** 2D/3D-Darstellung der Gehäusekörper (Widerstände, ICs, Elkos).
- 👁 **Top Jumper-Drähte (F.Cu Wires):** Isolierte Schaltdrähte der Bauteilseite.
- 👁 **Ratsnest / Airwires:** Gestrichelte logische Fluglinien unvollständiger Netze.
- 👁 **Kupfer-Pads & Bohrungen:** Pads und Löcher des Lochrasters.
- 👁 **Bottom Lötbrücken (B.Cu Traces):** Zinnbrücken und Leiterbahnen der Lötseite.
- 👁 **DRC-Warnmarker:** Rote/Gelbe Fehlerindikatoren bei Kollisionen oder offenen Netzen.

---

## 4. Interaktives Werkzeug-System (CAD Tools & State Machine)

PerfCraft implementiert für die Werkzeugleiste das **State-Pattern**. Die zentrale Schnittstelle `Tool` definiert den einheitlichen Lebenszyklus für alle Benutzerinteraktionen auf der Arbeitsfläche:

```java
package de.exit.perfcraft.ui.tool;

import de.exit.perfcraft.model.HoleCoord;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public interface Tool {
    void onMouseDown(MouseEvent e, HoleCoord gridPos);
    void onMouseMove(MouseEvent e, HoleCoord gridPos);
    void onMouseDragged(MouseEvent e, HoleCoord gridPos);
    void onMouseUp(MouseEvent e, HoleCoord gridPos);
    void onKeyPressed(KeyEvent e);
    void renderOverlay(GraphicsContext gc, CoordinateTransform transform);
    void reset();
    String getStatusMessage();
}
```

### 4.1 Auswahl-Werkzeug (Pointer / Selection Tool - `V`)
- **Modus-Aktivierung:** Taste `V` oder Klick auf das Zeiger-Icon in der Toolbox.
- **Zustände (Internal State Machine):**
  - `IDLE`: Bereit für Klick oder Drag. Hover über Bauteilen hebt diese dezent hervor.
  - `DRAGGING_OBJECT`: Ein oder mehrere selektierte Objekte werden über das Board verschoben.
  - `MARQUEE_SELECTION`: Ziehen ins Leere spannt ein Auswahl-Rechteck auf.
- **Einzelklick auf Objekt:** Selektiert das Bauteil oder die Leiterbahn, zeigt Griffe an den Pins an und aktualisiert den Inspektor.
- **Shift-Klick (Multi-Selektion):** Fügt das angeklickte Objekt zur aktuellen Auswahl hinzu oder entfernt es bei erneutem Klick.
- **Klick ins Leere:** Hebt die gesamte Selektion auf.
- **Gummiband-Rahmen (Marquee Selection):** Ziehen mit gedrückter linker Maustaste spannt ein halbtransparentes grünes Rechteck (`rgba(0, 200, 83, 0.15)`) mit gestrichelter grüner Kontur auf. Alle Objekte, deren Bounding-Box das Rechteck schneidet, werden markiert.
- **Verschieben:** Selektierte Bauteile können per Drag-and-Drop verschoben werden. Das Einrasten erfolgt exakt an ganzzahligen Loch-Koordinaten.
- **Drehen (`R`):** Dreht alle aktuell selektierten Bauteile um ihren Schwerpunkt um 90° im Uhrzeigersinn.
- **Feinverschiebung (Nudging):** Pfeiltasten `Hoch`, `Runter`, `Links`, `Rechts` verschieben selektierte Objekte um genau 1 Loch.

### 4.2 Jumper-Draht-Werkzeug (Top Layer Wire Tool - `W`)
- **Modus-Aktivierung:** Taste `W` oder Klick auf das Draht-Icon.
- **Workflow & Zustände:**
  1. `WAITING_FOR_START`: Mauszeiger rastet magnetisch auf Pads ein. Ein Klick verankert den Startpunkt `HoleCoord start`.
  2. `ROUTING_WIRE`:
     - Während der Mausbewegung wird ein elastischer Vorschau-Draht in gewählter Farbe gerendert.
     - **Orthogonaler Knick (90° Routing):** Standardmäßig wird der Draht rechtwinklig über Zwischenkoordinaten geroutet.
     - Drücken der `Umschalt`-Taste (Shift) erzwingt rein diagonale oder freie Linienführung.
     - Klick auf ein Zwischenloch fixiert einen Knickpunkt (Biegeradius).
  3. `COMPLETED`: Klick auf das Zielloch erstellt den fertigen `WireTrace` (Typ `JUMPER_WIRE`), fügt ihn über das `AddTraceCommand` in die Projektdaten ein und schaltet zurück in den Zustand `WAITING_FOR_START`.
  4. `Escape`: Bricht das aktuelle Verlegen ab und verwirft Zwischenpunkte.
- **Drahtfarben:** Wählbar in der Toolbar über ein Dropdown:
  - Rot (`#E53935` - typisch Spannungsversorgung VCC)
  - Schwarz (`#212121` - typisch Masse GND)
  - Blau (`#1E88E5` - Signale)
  - Gelb (`#FDD835` - Busse / Taktsignale)
  - Grün (`#43A047` - analoge Netze)
  - Weiß (`#FAFAFA` - Sonderleitungen)
- **Rendering:**
  - Isolierter Draht mit 3.5px Linienstärke, rundem Abschluss (`StrokeLineCap.ROUND`).
  - Metallische Anschlusspins (`#CFD8DC`) an den beiden Endlöchern.

### 4.3 Lötbrücken-Werkzeug (Bottom Layer Solder Bridge Tool - `T`)
- **Modus-Aktivierung:** Taste `T` oder Klick auf das Lötzinn-Icon.
- **Zweck:** Verbinden zweier oder mehrerer benachbarter Kupfer-Pads auf der Platinenunterseite durch eine massive Zinnbrücke.
- **Zustände:**
  - `WAIT_PAD`: Maus rastet auf Loch-Pads der Unterseite ein.
  - `DRAG_SOLDER`: Mit gedrückter Maustaste über benachbarte Pads ziehen; das Tool erzeugt kontinuierlich verbundene Zinnbrücken zwischen benachbarten Löchern.
- **Einschränkung:** Lötbrücken dürfen maximal zwischen direkt benachbarten Pads (horizontal, vertikal oder diagonal, $\Delta X \le 1, \Delta Y \le 1$) gezogen werden. Bei größeren Distanzen wird der Nutzer gewarnt oder ein blanker Schaltdraht vorgeschlagen.
- **Rendering:**
  - Zusammenhängende Lötzinnraupe (`#CFD8DC` Basis, `#ECEFF1` metallischer Glanzstrich).
  - Volle Füllung des Pad-Innenraums unter Beibehaltung der Bohrungs-Maske.

### 4.4 Bauteil-Platzierungs-Werkzeug (Component Placement Tool - `C`)
- **Modus-Aktivierung:** Taste `C` oder Auswahl eines Bauteils aus dem rechten Katalog.
- **Ghost-Vorschau (Placement Preview):**
  - Das ausgewählte Bauteil schwebt als halbtransparenter "Geist" mit dem Mauszeiger über das Board.
  - Die Anschlusspins rasten magnetisch auf den Bohrungen ein.
- **Validierungs-Matrix:**
  - Gültige Position (alle Pins liegen im Bereich $0 \le X < \text{Cols}$ und $0 \le Y < \text{Rows}$): Ghost leuchtet in Bauteilfarbe mit 70% Deckkraft.
  - Ungültige Position (Pins außerhalb des Platinenrands): Ghost färbt sich rot (`rgba(255, 82, 82, 0.7)`), Klick wird mit dezentem Tonsignal ignoriert.
- **Interaktive Steuerung während der Platzierung:**
  - Taste `R`: Dreht das Bauteil sofort um 90° im Uhrzeigersinn um seinen Haupt-Pin.
  - Taste `+` / `-`: Vergrößert bzw. verkleinert den Rasterabstand (z. B. bei Widerständen 3, 4, 5 oder 6 Löcher).
  - Linksklick: Platziert das Bauteil, generiert den nächsten Bezeichner (`R1`, `R2`) und bleibt aktiv für die nächste Platzierung.
  - `Escape` oder Rechtsklick: Beendet das Werkzeug und wechselt zum Auswahl-Werkzeug.

### 4.5 Text- & Beschriftungs-Werkzeug (Silkscreen Text Tool - `X`)
- **Modus-Aktivierung:** Taste `X` oder Klick auf das Text-Icon.
- **Workflow:**
  1. Klick auf eine Position der Platine öffnet ein Inline-Eingabefeld direkt auf dem Canvas.
  2. Nutzer tippt den Text ein (z. B. `+12V`, `GND`, `AUDIO-IN`).
  3. `Enter` bestätigt den Text; er wird als Vektor-Silkscreen auf dem Platinen-Substrat verankert.
- **Eigenschaften im Inspektor:**
  - Textinhalt, Schriftgröße (6pt bis 20pt), Rotation (0°, 90°, 180°, 270°), Ebene (Top / Bottom).

### 4.6 Verbindungsprüfer (Continuity & Net Highlighter Tool - `N`)
- **Modus-Aktivierung:** Taste `N` oder Klick auf das Testspitzen-Icon.
- **Funktion:** Simuliert einen akustisch/optischen Durchgangsprüfer (Multimeter im Pieps-Modus).
- **Graph-Traversierungs-Algorithmus (Breadth-First Search):**
  1. Starte an dem angeklickten Pad `HoleCoord start`.
  2. Suche alle Bauteil-Pins, Drahtbrücken-Punkte und Lötbrücken, die mit diesem Pad verbunden sind.
  3. Bei Lötbrücken: Traversiere rekursiv zu allen angrenzenden Pads.
  4. Bei Jumper-Drähten: Traversiere zum Ziel-Pad des Jumpers.
  5. Sammle die gesamte zusammenhängende Netzknoten-Menge in `Set<HoleCoord> activeNet`.
- **Visuelle Rückmeldung:**
  - Das gesamte ermittelte Netz wird mit einem pulsierenden, neongelben Kontur-Glow (`#FFEE58`) umrahmt.
  - Der Rest der Platine wird mit einer leichten dunklen Vignette (40% Alpha) überzogen.
  - Die Statusleiste zeigt: `Netzprüfung: 6 verbundene Pins, 3 Leiterbahnen, Gesamtlänge: 45.7 mm`.

### 4.7 Lösch-Werkzeug (Eraser / Delete Tool - `Entf`)
- **Modus-Aktivierung:** Klick auf das Radiergummi-Icon in der Toolbox oder Taste `Entf` bei selektierten Objekten.
- **Chirurgische Einzel-Löschung:** Klick auf ein beliebiges Bauteil, einen Jumper oder eine Lötbrücke entfernt exakt das getroffene Element.
- **Massen-Löschung:** Sind bereits mehrere Objekte ausgewählt, löscht ein Klick auf `Entf` die gesamte Auswahl über ein einziges `DeleteSelectionCommand` (1-Klick Undo stellt alles wieder her).

### 4.8 Ratsnest / Airwire-Engine (Logische Netze & Flugdrähte wie in KiCad)
Wie im **KiCad PCB Editor** unterstützt PerfCraft ein logisches Netz- und Ratsnest-System:
- **Konzept:** Der Entwickler kann logische Netze definieren (z. B. `VCC`, `GND`, `SDA`, `SCL`, `SIGNAL_IN`). Bauteil-Pins können einem Netz zugewiesen werden.
- **Dynamische Fluglinien (Airwires):**
  - Unverbundene Pins desselben Netzes werden über haardünne, gestrichelte Vektorlinien (`#64B5F6`, 1px gestrichelt) miteinander verbunden.
  - Ein Minimum-Spanning-Tree (MST / Kruskal-Algorithmus) berechnet stets die kürzesten elastischen Linien zwischen den Pins.
- **Automatisches Verschwinden:**
  - Sobald der Entwickler eine physische Verbindung (Zinnbrücke auf der Lötseite oder Jumper-Draht auf der Bauteilseite) zwischen zwei Pins zieht, erkennt die Engine den geschlossenen Strompfad und blendet die entsprechende Ratsnest-Linie **in Echtzeit aus**.
- **Globaler Toggle:** Über Taste `F10` oder das Augensymbol im Appearance-Panel kann das Ratsnest jederzeit ein- und ausgeblendet werden.

### 4.9 Design Rule Check (DRC) & Kollisions-Engine (KiCad-Standard)
Inspiriert vom **KiCad DRC** führt PerfCraft eine automatisierte Layout-Verifikation durch (`Menü: Platine -> Designregeln prüfen (DRC)` oder Taste `F7`):
1. **Bauteil-Kollisionsprüfung (Physical Overlap):**
   - Prüft, ob sich zwei Bauteilkörper physisch überschneiden (z. B. zwei große axiale Elkos oder ICs, die im selben Bauraum platziert wurden).
   - Verwendet 2D-Polygon-SAT (Separating Axis Theorem) für rotierte Bauteil-Körper.
2. **Kurzschluss-Erkennung (Net Short-Circuit Check):**
   - Erkennt, wenn eine Zinnbrücke oder ein Jumper zwei voneinander getrennte, benannte Netze (z. B. `VCC` und `GND`) galvanisch kurzschließt.
3. **Offene Pins (Unconnected Pins Check):**
   - Identifiziert Pins von Komponenten, die keinem Netz angehören und weder mit einem Draht noch einer Zinnbrücke kontaktiert sind.
   - Visuelle Markierung: Ein kleiner orangefarbener Kreis mit Kreuz (`#FF9800`) markiert die unverbundenen Pins.
4. **Platinenrand-Freiraum (Board Edge Clearance):**
   - Warnt, wenn Bauteile oder Zinnbrücken weniger als 1 Lochschritt vom mechanischen Platinenrand entfernt liegen.
5. **Ergebnis-Panel:**
   - Ein aufklappbares Panel listet alle Fehler und Warnungen strukturiert auf. Klick auf eine Zeile zoomt und zentriert die Kamera direkt auf das betroffene Bauteil.

### 4.10 Relatives Koordinaten-Messwerkzeug (KiCad Leertasten-Delta-Modus)
Aus **KiCad** übernommen ist die intuitive Ein-Tasten-Relativmessung:
- Drücken der **`Leertaste` (Space)** setzt an der aktuellen Cursor-Position einen temporären lokalen Nullpunkt $(X_0, Y_0)$.
- Bei jeder Mausbewegung berechnet PerfCraft die relative Differenz und zeigt sie mit extrem hohem Kontrast in der Statusleiste:
  $$\Delta X = X - X_0 \quad (\text{Löcher und mm})$$
  $$\Delta Y = Y - Y_0 \quad (\text{Löcher und mm})$$
  $$\text{Distanz} = \sqrt{\Delta X^2 + \Delta Y^2} \times 2.54\text{ mm}$$
  $$\text{Winkel} = \text{atan2}(-\Delta Y, \Delta X) \times \frac{180}{\pi}$$
- Erneutes Drücken der `Leertaste` setzt den Nullpunkt auf die neue Position; `Escape` kehrt zur absoluten Anzeige zurück.

---

## 5. Bauteil-Bibliothek & Footprint-Spezifikationen

Jedes Bauteil wird in PerfCraft durch einen streng typisierten Footprint definiert, der die mechanische Geometrie im 2.54 mm Lochraster festlegt:

```text
Lochabstand Pitch: 2.54 mm (Standard 0.1 Zoll)
1 Lochschritt = 1 Grid-Unit (GU)
```

### 5.1 Passive Bauteile

#### 5.1.1 Axial-Widerstand (Kohleschicht / Metallschicht 0.25W)
- **Standard-Spannweite:** 4 Löcher (10.16 mm) oder 5 Löcher (12.7 mm) horizontal/vertikal.
- **Körperabmessungen:** Länge 6.5 mm, Durchmesser 2.5 mm, zentriert zwischen den Anschlusspins.
- **Footprint-Geometrie (4 Löcher horizontal):**
  ```text
  (Pin 1)---[===|===|===|===]---(Pin 2)
     X=0          Körper          X=4
  ```
- **Rendering:**
  - Körper in Beige/Hellbraun (`#D7CCC8` für Kohleschicht 5%) oder Hellblau (`#90CAF9` für Metallschicht 1%).
  - Zylindrischer Körper mit abgerundeten Endkappen.
  - Gebogene Drahtanschlüsse (Silber `#B0BEC5`), die rechtwinklig in die Kupferlöcher abtauchen.
  - Vier oder fünf farbige Coderinge direkt auf dem Zylinder.

#### 5.1.2 Keramikkondensator (Scheibe / MLCC)
- **Standard-Spannweite:** 1 Loch (2.54 mm) oder 2 Löcher (5.08 mm).
- **Footprint-Geometrie (2 Löcher):**
  ```text
  (Pin 1)-----[ || ]-----(Pin 2)
     X=0       Scheibe     X=2
  ```
- **Rendering:**
  - Flache gelb-orangefarbene Scheibe (`#FFA726`) oder blauer Block (`#42A5F5`).
  - Aufschrift der Kapazität (z. B. `104` für 100nF).

#### 5.1.3 Elektrolytkondensator (Radial)
- **Standard-Spannweite:** 1 Loch (2.54 mm) bis 3 Löcher (7.62 mm) je nach Baugröße.
- **Footprint-Geometrie:**
  ```text
     (Pin +)       (Pin -)
       X=0           X=1
      [   ( + )   - - -   ]
  ```
- **Rendering:**
  - Kreisförmiger Zylinderkopf (Draufsicht) in Dunkelblau (`#1565C0`) oder Schwarz (`#212121`).
  - Weißer Minuspol-Streifen mit `-`-Symbolen zur visuellen Polaritätsprüfung.
  - Durchmesser: 5 mm, 6.3 mm, 8 mm oder 10 mm.

#### 5.1.4 Trimm-Potentiometer (3-Pin)
- **Pinbelegung:** Dreieckig angeordnet oder linear im 2.54 mm bzw. 5.08 mm Raster.
- **Footprint-Geometrie (Dreieckig):**
  ```text
  (Pin 1)-------(Pin 3)
     \           /
      \  [ (o) ]/
       (Pin 2 / Schleifer)
  ```
- **Rendering:** Blaues Kunststoffgehäuse (`#1976D2`) mit weißem Drehschlitz im Zentrum.

### 5.2 Aktive Bauteile

#### 5.2.1 Diode (1N4148 / 1N4007)
- **Standard-Spannweite:** 3 bis 4 Löcher.
- **Footprint-Geometrie (4 Löcher):**
  ```text
  (Anode)----[ === | == | ]----(Kathode)
    X=0         Körper  Ring     X=4
  ```
- **Rendering:**
  - Kleinsignal (1N4148): Transparent-oranger Glaskörper (`#FF7043`) mit schwarzem Kathodenring.
  - Leistung (1N4007): Schwarzer Kunststoffzylinder (`#212121`) mit silbernem Kathodenring.

#### 5.2.2 Leuchtdiode (LED 3mm / 5mm)
- **Standard-Spannweite:** 1 Loch (2.54 mm) zwischen Anode (+) und Kathode (-).
- **Rendering:**
  - Farbige Kuppel (Rot, Grün, Gelb, Blau, Kaltweiß).
  - Abflachung am Rand zur Kennzeichnung der Kathode.
  - Feiner innerer Reflexionskegel.

#### 5.2.3 Transistor (TO-92 Gehäuse)
- **Pinbelegung:** 3 Pins in einer Reihe (Raster 2.54 mm: Pin 1, 2, 3 z. B. E-B-C).
- **Footprint-Geometrie:**
  ```text
  (E)   (B)   (C)
  X=0   X=1   X=2
  [ (_______) ]  (Halbkreis-Körper)
  ```
- **Rendering:**
  - Halbkreis-förmiges schwarzes Epoxid-Gehäuse mit planer Vorderseite.
  - Beschriftung (z. B. `BC547`, `2N3904`).

### 5.3 Integrierte Schaltungen (DIL/DIP-Sockel & ICs)

PerfCraft unterstützt die gesamte Standard-DIL-Palette mit doppelseitigen Pinreihen:
- **DIP-4, DIP-8, DIP-14, DIP-16, DIP-18, DIP-20, DIP-24 (schmal), DIP-28, DIP-40.**
- **Reihenabstand:**
  - Standard (Narrow): 3 Lochschritte = 7.62 mm (0.3 Zoll) für DIP-4 bis DIP-28.
  - Wide: 6 Lochschritte = 15.24 mm (0.6 Zoll) für DIP-24W, DIP-28W, DIP-40.
- **Footprint-Geometrie (DIP-8 Beispiel):**
  ```text
      (Notch / Kerbe)
         [ ( ) ]
  Pin 1 (o)   (o) Pin 8
  Pin 2 (o)   (o) Pin 7
  Pin 3 (o)   (o) Pin 6
  Pin 4 (o)   (o) Pin 5
         [-----]
  ```
- **Rendering:**
  - Dunkelgrauer IC-Körper (`#263238`) mit matter Textur.
  - Halbkreisförmige Einkerbung (Notch) an Pin 1 / Kopfseite.
  - Weißer Markierungspunkt (Dot) direkt an Pin 1.
  - Silberne Kontakt-Pins (`#CFD8DC`) auf den Pads.
  - Laser-Gravur-Text mittig (z. B. `NE5532P`, `ATmega328P`).

### 5.4 Steckverbinder & Mechanik

#### 5.4.1 Stiftleisten (Pin Headers)
- **Typen:** 1-reihig (1x1 bis 1x40 Pins), 2-reihig (2x2 bis 2x40 Pins).
- **Rendering:** Schwarze quadratische Kunststoffisolatoren mit vergoldeten quadratischen Stiftkontakten (`#FFD54F`).

#### 5.4.2 Schraubklemmen (Terminal Blocks)
- **Rastermaß:** 5.08 mm (2 Lochschritte) für 2-polige und 3-polige Klemmen.
- **Rendering:** Grünes Gehäuse (`#2E7D32`) mit metallischen Schrauböffnungen von oben.

#### 5.4.3 Kurzhubtaster (Tactile Push Button 6x6 mm)
- **Footprint:** 4 Pins im Raster 6.5 mm x 4.5 mm (typisch auf 3x2 Löcher gespreizt).
- **Rendering:** Silberner Metallkragen mit rundem schwarzem Betätigungsknopf in der Mitte.

---

## 6. Widerstands-Farbcode-Engine (Mathematik & Algorithmus)

PerfCraft enthält eine bidirektionale Berechnungs-Engine gemäß **IEC 60062** für 4-Band- und 5-Band-Farbcodierungen.

### 6.1 Die Farbwert-Tabelle

| Farbe | Kennziffer (Band 1-3) | Multiplikator | Toleranz | Hex-Farbcode |
| :--- | :--- | :--- | :--- | :--- |
| **Schwarz** | 0 | $10^0 = 1$ | - | `#212121` |
| **Braun** | 1 | $10^1 = 10$ | $\pm 1\%$ | `#6D4C41` |
| **Rot** | 2 | $10^2 = 100$ | $\pm 2\%$ | `#D32F2F` |
| **Orange** | 3 | $10^3 = 1.000$ | - | `#F57C00` |
| **Gelb** | 4 | $10^4 = 10.000$ | - | `#FBC02D` |
| **Grün** | 5 | $10^5 = 100.000$ | $\pm 0.5\%$ | `#388E3C` |
| **Blau** | 6 | $10^6 = 1.000.000$ | $\pm 0.25\%$ | `#1976D2` |
| **Violett** | 7 | $10^7 = 10.000.000$ | $\pm 0.1\%$ | `#7B1FA2` |
| **Grau** | 8 | $10^8 = 100.000.000$ | $\pm 0.05\%$ | `#616161` |
| **Weiß** | 9 | $10^9 = 1.000.000.000$ | - | `#EEEEEE` |
| **Gold** | - | $10^{-1} = 0.1$ | $\pm 5\%$ | `#C5A059` |
| **Silber** | - | $10^{-2} = 0.01$ | $\pm 10\%$ | `#9E9E9E` |

### 6.2 Parsing-Algorithmus (String zu Ohm)
Die Engine verarbeitet Eingaben flexibel und fehlertolerant:
- `"10k"` -> $10.000\ \Omega$
- `"4k7"` oder `"4.7k"` -> $4.700\ \Omega$
- `"470R"` oder `"470"` -> $470\ \Omega$
- `"1M"` -> $1.000.000\ \Omega$
- `"0R22"` oder `"0.22"` -> $0.22\ \Omega$

### 6.3 Ring-Generierungs-Algorithmus (4-Band Standard)
1. Extrahiere den numerischen Ohmwert $R$.
2. Ermittle die ersten beiden signifikanten Ziffern $d_1$ und $d_2$:
   $$d_1 = \lfloor R / 10^{\lfloor \log_{10}(R) \rfloor} \rfloor$$
   $$d_2 = \lfloor (R / 10^{\lfloor \log_{10}(R) \rfloor - 1}) \rfloor \pmod{10}$$
3. Ermittle den Multiplikator-Exponenten:
   $$exp = \lfloor \log_{10}(R) \rfloor - 1$$
4. Mappe $d_1$, $d_2$, $exp$ und die gewählte Toleranz (Standard Gold = 5%) auf die Farb-Enums.
5. Das Array `[Band1, Band2, Multiplier, Tolerance]` wird direkt an den `ComponentRenderer` übergeben.

---

## 7. Canvas Rendering-Pipeline & Grafik-Mathematik

### 7.1 Koordinaten-Systeme

PerfCraft rechnet durchgängig in drei strikt getrennten Koordinatenräumen:

```text
[Screen Space]  (Pixel auf Monitor: 0 .. CanvasWidth, 0 .. CanvasHeight)
      ▲
      │  Transformation via Matrix (scale, translateX, translateY)
      ▼
[World Space]   (Metrische Platinen-Millimeter: 0.0 mm .. WidthMm)
      ▲
      │  Rasterung via Pitch (2.54 mm)
      ▼
[Grid Space]    (Ganzzahlige Loch-Indizes: X = 0..Cols-1, Y = 0..Rows-1)
```

**Mathematische Umrechnungsformeln:**
$$X_{\text{world}} = X_{\text{grid}} \times 2.54\text{ mm}$$
$$Y_{\text{world}} = Y_{\text{grid}} \times 2.54\text{ mm}$$

$$X_{\text{screen}} = (X_{\text{world}} \times \text{pixelsPerMm} \times \text{scale}) + \text{translateX}$$
$$Y_{\text{screen}} = (Y_{\text{world}} \times \text{pixelsPerMm} \times \text{scale}) + \text{translateY}$$

Umgekehrt für Maus-Picking (Screen zu Grid):
$$X_{\text{world}} = \frac{X_{\text{screen}} - \text{translateX}}{\text{pixelsPerMm} \times \text{scale}}$$
$$X_{\text{grid}} = \text{round}\left(\frac{X_{\text{world}}}{2.54}\right)$$

### 7.2 Transformationsmatrix, Zoom & Pan
- **Zentrierter Zoom um Mauszeiger:**
  Beim Drehen des Mausrads wird nicht der Platinen-Ursprung skaliert, sondern der Punkt unter dem Cursor fixiert:
  $$\text{scale}_{\text{neu}} = \text{scale}_{\text{alt}} \times 1.15^{\text{delta}}$$
  $$\text{translateX} = X_{\text{mouse}} - (X_{\text{mouse}} - \text{translateX}) \times \frac{\text{scale}_{\text{neu}}}{\text{scale}_{\text{alt}}}$$
  $$\text{translateY} = Y_{\text{mouse}} - (Y_{\text{mouse}} - \text{translateY}) \times \frac{\text{scale}_{\text{neu}}}{\text{scale}_{\text{alt}}}$$
- **Pan (Verschieben):**
  Ziehen mit mittlerer Maustaste oder Leertaste addiert das Maus-Delta direkt auf `translateX` und `translateY`.

### 7.3 Vollständige Implementierung von `CoordinateTransform.java`

Die Transformationsklasse kapselt alle Umrechnungen thread-sicher und recheneffizient:

```java
package de.exit.perfcraft.ui.canvas;

import de.exit.perfcraft.model.HoleCoord;
import javafx.geometry.Point2D;

public class CoordinateTransform {
    private static final double DEFAULT_PIXELS_PER_MM = 3.7795; // 96 DPI Basis (1 mm = 3.78 px)
    private static final double PITCH_MM = 2.54;

    private double scale = 1.0;
    private double translateX = 50.0;
    private double translateY = 50.0;
    private final double pixelsPerMm;

    public CoordinateTransform() {
        this(DEFAULT_PIXELS_PER_MM);
    }

    public CoordinateTransform(double pixelsPerMm) {
        this.pixelsPerMm = pixelsPerMm;
    }

    public double getHoleSpacingPx() {
        return PITCH_MM * pixelsPerMm * scale;
    }

    public double gridToScreenX(int gridX) {
        return (gridX * PITCH_MM * pixelsPerMm * scale) + translateX;
    }

    public double gridToScreenY(int gridY) {
        return (gridY * PITCH_MM * pixelsPerMm * scale) + translateY;
    }

    public HoleCoord screenToGrid(double screenX, double screenY) {
        double worldMmX = (screenX - translateX) / (pixelsPerMm * scale);
        double worldMmY = (screenY - translateY) / (pixelsPerMm * scale);
        int gx = (int) Math.round(worldMmX / PITCH_MM);
        int gy = (int) Math.round(worldMmY / PITCH_MM);
        return new HoleCoord(gx, gy);
    }

    public Point2D screenToWorldMm(double screenX, double screenY) {
        double wx = (screenX - translateX) / (pixelsPerMm * scale);
        double wy = (screenY - translateY) / (pixelsPerMm * scale);
        return new Point2D(wx, wy);
    }

    public void applyZoom(double zoomFactor, double pivotX, double pivotY) {
        double newScale = Math.clamp(scale * zoomFactor, 0.15, 12.0);
        double actualFactor = newScale / scale;
        translateX = pivotX - (pivotX - translateX) * actualFactor;
        translateY = pivotY - (pivotY - translateY) * actualFactor;
        scale = newScale;
    }

    public void pan(double deltaX, double deltaY) {
        translateX += deltaX;
        translateY += deltaY;
    }

    public void resetView(double canvasWidth, double canvasHeight, int boardCols, int boardRows) {
        double boardWidthPx = boardCols * PITCH_MM * pixelsPerMm;
        double boardHeightPx = boardRows * PITCH_MM * pixelsPerMm;
        double scaleX = (canvasWidth - 100) / boardWidthPx;
        double scaleY = (canvasHeight - 100) / boardHeightPx;
        this.scale = Math.clamp(Math.min(scaleX, scaleY), 0.3, 3.0);
        this.translateX = (canvasWidth - (boardWidthPx * scale)) / 2.0;
        this.translateY = (canvasHeight - (boardHeightPx * scale)) / 2.0;
    }

    public double getScale() { return scale; }
    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }
}
```

### 7.4 Viewport-Culling für Hochleistungs-Rendering
Um auch bei riesigen Platinen (z. B. 150x150 Löcher = 22.500 Pads) konstante 60 FPS zu garantieren, berechnet der Renderer vor jedem Frame die sichtbare Bounding-Box:
$$\text{col}_{\text{min}} = \max\left(0, \lfloor \text{ScreenToGridX}(0) \rfloor - 1\right)$$
$$\text{col}_{\text{max}} = \min\left(\text{Cols} - 1, \lceil \text{ScreenToGridX}(\text{Width}) \rceil + 1\right)$$
$$\text{row}_{\text{min}} = \max\left(0, \lfloor \text{ScreenToGridY}(0) \rfloor - 1\right)$$
$$\text{row}_{\text{max}} = \min\left(\text{Rows} - 1, \lceil \text{ScreenToGridY}(\text{Height}) \rceil + 1\right)$$
Nur Löcher und Komponenten innerhalb dieses Fensters werden an die GPU übergeben.

### 7.4 Multi-Layer Zeichenschichten & Z-Index
Jeder Zeichenzyklus arbeitet eine streng definierte Ebenenhierarchie ab:
1. **Layer 0 (Substrat):** PCB-Hintergrundfläche mit Kantenfase und Platinenrändern.
2. **Layer 1 (Lötseite - Traces):** Zinnbrücken und Kupferverbindungen auf dem Bottom Layer.
3. **Layer 2 (Pads & Bohrungen):** Kupfer-Ringe und schwarze Bohrlöcher.
4. **Layer 3 (Bauteilseite - Silkscreen):** Textaufdrucke, Umrisse und Bestückungsdruck.
5. **Layer 4 (Bauteilseite - Jumper-Drähte):** Isolierte Schaltdrähte mit Pins.
6. **Layer 5 (Bauteilkörper):** Widerstände, Kondensatoren, ICs, Sockel, Dioden.
7. **Layer 6 (Net Highlighter):** Leuchtende Netzkonturen bei aktivem Verbindungsprüfer.
8. **Layer 7 (Selektions- & Ghost-Overlay):** Transparenter Platzierungs-Geist und Selektionsrahmen.

---

## 8. Vollständiges Datenmodell & `.perf` Dateiformat

### 8.1 JSON-Schemaspezifikation
Das Projekt-Dateiformat `*.perf` ist ein strikt strukturiertes JSON-Dokument:

```json
{
  "formatVersion": 1,
  "generator": "PerfCraft 1.0.0",
  "createdAt": "2026-10-20T21:30:00Z",
  "modifiedAt": "2026-10-20T21:45:00Z",
  "project": {
    "name": "Audio-Vorverstaerker-Mono",
    "description": "Mikrofon-Vorstufe mit NE5532 und passiver Klangregelung",
    "author": "PerfCraft User",
    "board": {
      "columns": 30,
      "rows": 20,
      "pitchMm": 2.54,
      "boardColor": "#1B4D2E",
      "padStyle": "ROUND_PAD"
    },
    "components": [
      {
        "id": "comp-101",
        "type": "RESISTOR_AXIAL",
        "designator": "R1",
        "value": "10k",
        "unit": "Ω",
        "tolerance": "5%",
        "start": { "x": 4, "y": 3 },
        "end": { "x": 8, "y": 3 },
        "rotation": 0,
        "layer": "TOP",
        "colorCode": ["BROWN", "BLACK", "ORANGE", "GOLD"]
      },
      {
        "id": "comp-102",
        "type": "IC_DIP",
        "designator": "IC1",
        "value": "NE5532",
        "unit": "",
        "pinCount": 8,
        "rowSpacingHoles": 3,
        "start": { "x": 12, "y": 6 },
        "rotation": 0,
        "layer": "TOP"
      }
    ],
    "traces": [
      {
        "id": "trace-201",
        "type": "SOLDER_BRIDGE",
        "layer": "BOTTOM",
        "color": "#CFD8DC",
        "points": [
          { "x": 4, "y": 3 },
          { "x": 4, "y": 4 }
        ]
      },
      {
        "id": "trace-202",
        "type": "JUMPER_WIRE",
        "layer": "TOP",
        "color": "#E53935",
        "points": [
          { "x": 8, "y": 3 },
          { "x": 12, "y": 6 }
        ]
      }
    ],
    "texts": [
      {
        "id": "txt-301",
        "text": "+12V",
        "position": { "x": 2, "y": 1 },
        "fontSize": 10,
        "color": "#FFFFFF",
        "layer": "TOP"
      }
    ]
  }
}
```

### 8.2 Java-Klassenstruktur im `model`-Package

1. **`HoleCoord.java` (Record):**
   ```java
   public record HoleCoord(int x, int y) {
       public double toMmX(double pitch) { return x * pitch; }
       public double toMmY(double pitch) { return y * pitch; }
       public boolean isAdjacentTo(HoleCoord other) {
           int dx = Math.abs(x - other.x);
           int dy = Math.abs(y - other.y);
           return (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
       }
   }
   ```
2. **`BoardConfig.java`:**
   Enthält Spalten (5..200), Zeilen (5..200), Pitch (Standard 2.54mm) und Substratfarben.
3. **`ComponentType.java` (Enum):**
   `RESISTOR_AXIAL`, `CAPACITOR_CERAMIC`, `CAPACITOR_ELECTROLYTIC`, `DIODE`, `LED`, `TRANSISTOR_TO92`, `IC_DIP`, `PIN_HEADER`, `TERMINAL_BLOCK`, `TACTILE_SWITCH`.
4. **`PlacedComponent.java`:**
   Repräsentiert ein physisch auf dem Board platziertes Bauteil mit ID, Bezeichner, Wert, Pin-Koordinaten, Drehung und visuellen Attributen.
5. **`WireTrace.java`:**
   Repräsentiert eine Leiterbahn, Zinnbrücke oder einen isolierten Jumper-Draht mit Koordinaten-Liste, Ebene und Farbe.
6. **`BoardLayer.java` (Enum):**
   `TOP` (Bauteilseite), `BOTTOM` (Lötseite), `BOTH` (Röntgen/X-Ray).
7. **`PerfProject.java`:**
   Wurzelklasse mit Projektmetadaten, BoardConfig, Bauteilliste, Leiterbahnen und Textlabels.

---

## 9. Undo/Redo Transaktions-Architektur (Command Pattern)

Sämtliche zustandsverändernden Benutzeraktionen werden über das **Command-Entwurfsmuster** gekapselt:

```text
       CommandHistory
      ┌───────────────┐
      │ - undoStack   │
      │ - redoStack   │
      └───────┬───────┘
              │ execute(cmd)
              ▼
      <<interface>> Command
      ┌─────────────────────────┐
      │ + execute()             │
      │ + undo()                │
      │ + getDescription()      │
      └─────────────────────────┘
              ▲
    ┌─────────┴────────┬─────────────────────┐
    │                  │                     │
PlaceComponentCmd  DeleteSelectionCmd    MoveComponentCmd
```

### Konkrete Command-Implementierungen:
1. **`PlaceComponentCommand`:** Fügt ein Bauteil hinzu; Undo entfernt es restlos.
2. **`DeleteSelectionCommand`:** Löscht mehrere Bauteile und Leiterbahnen auf einmal; Undo stellt die gesamte gelöschte Gruppe mit originalen IDs und Positionen wieder her.
3. **`MoveComponentCommand`:** Speichert Start- und Zielkoordinaten; Undo schiebt das Bauteil exakt zurück.
4. **`RotateComponentCommand`:** Rotiert das Bauteil um 90°; Undo dreht es um 270° (bzw. -90°).
5. **`AddTraceCommand`:** Fügt einen Jumper oder eine Lötbrücke hinzu; Undo löscht die Verbindung.
6. **`EditPropertiesCommand`:** Ändert Wert oder Bezeichner; Undo stellt alte Werte wieder her.

---

## 10. Stücklisten- (BOM) & Export-Engine

### 10.1 Konsolidierungs-Algorithmus (`ExportService`)
Die Stücklisten-Engine aggregiert alle auf dem Board verbauten Komponenten automatisch:
1. Gruppierung nach **Bauteiltyp** und **elektrischem Wert** (z. B. alle Widerstände mit Wert `10k`).
2. Zusammenfassung der Bezeichner zu kommagetrennten Listen (z. B. `R1, R2, R5, R8`).
3. Zählung der Gesamtanzahl (`Quantity`).
4. Ermittlung der Pin-Spannweite (z. B. "Pitch: 10.16 mm / 4 Löcher").

### 10.2 CSV-Export (RFC 4180 konform)
```text
Pos;Bauteiltyp;Wert;Bezeichner;Menge;Rasterabstand
1;Widerstand (Axial);10k Ω;R1, R2;2;10.16 mm
2;Widerstand (Axial);470 Ω;R3;1;10.16 mm
3;Kondensator (Keramik);100n F;C1, C2;2;2.54 mm
4;Kondensator (Elektrolyt);47u F;C3;1;2.54 mm
5;IC (DIP-8);NE5532;IC1;1;7.62 mm
```

### 10.3 Hochauflösender Bildexport (PNG)
Der PNG-Export rendert die gesamte Platine ohne Anwendungs-UI auf einen Off-Screen JavaFX-Canvas:
- Wählbare Auflösung: Standard (150 DPI), Druckqualität (300 DPI) oder Ultra-HD (600 DPI).
- Wählbare Ansicht: Nur Bauteilseite, Nur Lötseite oder Beide Ebenen (Röntgen).
- Wählbarer Hintergrund: Transparent (RGBA) oder Weißes Papier.

---

## 11. Vollständige Tastatur- & Maus-Interaktionsmatrix

| Taste / Geste | Kontext | Ausgelöste Aktion |
| :--- | :--- | :--- |
| `V` | Global | Auswahl-Werkzeug aktivieren |
| `M` | Bei Selektion | **KiCad Move:** Bauteil aufnehmen und frei verschieben |
| `G` | Bei Selektion | **KiCad Drag:** Bauteil verschieben und verbundene Drähte elastisch mitführen |
| `E` | Bei Selektion | **KiCad Edit:** Eigenschafts-Dialog sofort öffnen |
| `D` | Bei Selektion | **KiCad Duplicate:** Bauteil sofort duplizieren |
| `R` | Bei Ghost / Selektion | Bauteil um 90° im Uhrzeigersinn drehen |
| `F` | Bei Selektion | **KiCad Flip:** Bauteil zwischen Top- und Bottom-Layer spiegeln |
| `W` | Global | Jumper-Draht-Werkzeug aktivieren |
| `T` | Global | Lötbrücken-Werkzeug aktivieren |
| `C` | Global | Bauteil-Platzierungs-Werkzeug aktivieren |
| `X` | Global | Text-Werkzeug aktivieren |
| `N` oder `` ` `` (Backtick) | Global | **KiCad Net Highlight:** Netz unter Cursor leuchtend hervorheben |
| `Leertaste` | Canvas | **KiCad Delta-Origin:** Setzt lokalen Nullpunkt für Relativmessung ($\Delta X=0, \Delta Y=0$) |
| `Entf` | Auswahl aktiv | Alle selektierten Bauteile und Leiterbahnen löschen |
| `Strg + Z` | Global | Letzten Befehl rückgängig machen (Undo) |
| `Strg + Y` | Global | Zuletzt verworfenen Befehl wiederholen (Redo) |
| `Strg + S` | Global | Projekt speichern |
| `Strg + O` | Global | Projekt öffnen |
| `Strg + N` | Global | Neues Board erstellen Dialog öffnen |
| `Strg + A` | Global | Alle Objekte auf dem Board selektieren |
| `Strg + 0` | Canvas | Zoom auf 100% (1:1 Originalgröße) |
| `Strg + F` | Canvas | Zoom "Fit to Window" (Platine bildschirmfüllend zentrieren) |
| `F1` | Global | Ebene auf Bauteilseite (Top Layer) umschalten |
| `F2` | Global | Ebene auf Lötseite (Bottom Layer) umschalten |
| `F3` | Global | Ebene auf Röntgenansicht (X-Ray) umschalten |
| `F7` | Global | **KiCad DRC:** Design Rule Check (Kollisionen & Kurzschlüsse) ausführen |
| `F10` | Global | **KiCad Ratsnest:** Gestrichelte Fluglinien (Airwires) umschalten |
| `Mausrad oben/unten` | Canvas | Stufenloser Zoom zentriert um Mauszeiger |
| `Mittlere Maustaste Drag` | Canvas | Platine verschieben (Pan) |
| `Leertaste (gedrückt halten) + Drag`| Canvas | Platine verschieben (Pan-Alternative) |
| `Pfeiltasten` | Auswahl aktiv | Markierte Objekte um genau 1 Loch verschieben |
| `Escape` | Im Werkzeug | Aktiven Zeichen-/Verlege-Vorgang abbrechen oder Netz-Highlighting aufheben |

---

## 12. Detaillierte Fehlermatrix & Edge Cases

| Nr. | Situation / Edge Case | Systemverhalten | Feedback an den Nutzer |
| :--- | :--- | :--- | :--- |
| **E1** | Board-Dimension < 5 oder > 200 im Neudialog | Button "Erstellen" wird deaktiviert, Textfeld rot umrandet | Tooltip: "Zulässiger Bereich: 5 bis 200 Löcher" |
| **E2** | Bauteil ragt beim Platzieren über Platinenrand | Platzierung wird gesperrt, Klick löst keine Aktion aus | Ghost-Vorschau färbt sich rot (`#FF5252`) |
| **E3** | Zwei Bauteile teilen sich denselben Anschluss-Pin | Ist physikalisch auf Lochrastern erlaubt (z. B. 2 Drähte in 1 Loch) | Subtiler gelber Warn-Glow an der betroffenen Bohrung |
| **E4** | Lötbrücke über Distanz > 1 Loch versucht | Aktion wird blockiert (Lötzinn überbrückt nur Nachbarpads) | Hinweis: "Für größere Distanzen Jumper-Draht (W) nutzen" |
| **E5** | Laden einer korrupten oder unvollständigen `.perf`-Datei | JSON-Parser fängt Exception ab, Projekt wird nicht überschrieben | Dialog: "Ungültiges Dateiformat. Die Datei konnte nicht gelesen werden." |
| **E6** | Laden einer Datei mit höherer FormatVersion | Abwärtskompatibilitäts-Check | Warnung: "Datei wurde mit einer neueren PerfCraft-Version erstellt" |
| **E7** | Speichern auf schreibgeschütztes Medium / Verzeichnis | `IOException` wird sauber abgefangen | Rote Fehlermeldung in Statusleiste: "Fehler beim Speichern" |
| **E8** | Ungültiger Widerstandswert eingetippt (z. B. "12xyz") | Vorheriger gültiger Wert bleibt bestehen | Keine Farbcode-Ringe, Textfeld leuchtet kurz orange |
| **E9** | Zoom-Faktor übersteigt 1000% oder fällt unter 10% | Schieberegler und Mausrad werden geclamped | Zoom bleibt exakt auf Maximal-/Minimalwert arretiert |
| **E10**| Schließen des Fensters bei ungespeicherten Änderungen | `WindowCloseRequestHandler` fängt Schließevent ab | Material-Dialog mit drei Optionen: "Speichern", "Verwerfen", "Abbrechen" |
| **E11**| Export einer komplett leeren Platine | Bild wird gerendert (nur Platine ohne Teile) | Stücklisten-Export zeigt Hinweis: "Keine Bauteile auf dem Board" |
| **E12**| Undo-Stack ist leer | Undo-Button und Menüpunkt werden ausgegraut | Klick bewirkt nichts, kein Fehler im Log |

---

## 13. Phasenweise Implementierungs-Roadmap

### Phase 1: Projekt-Setup & Maven-Infrastruktur
- [x] Maven `pom.xml` mit Java 21, JavaFX 21, Jackson Databind und JUnit 5 initialisiert.
- [x] Basis-Package `de.exit.perfcraft` angelegt.
- [x] App-Icons (SVG, PNG, ICO) generiert und in Fensterleiste eingebunden.
- [x] `material-dark.css` Grund-Stylesheet erstellt.
- [x] **Verifikation:** `mvn clean test` läuft fehlerfrei durch.

### Phase 2: Kern-Datenmodell & Serialisierungs-Engine
- [ ] Implementierung der Klassen im Package `de.exit.perfcraft.model`:
  - `HoleCoord` Record mit Nachbarschafts- und Distanzmethoden.
  - `BoardConfig` mit Spalten, Zeilen, Pitch (2.54 mm) und Validierungslogik.
  - `ComponentType` Enum mit allen Gehäusevarianten.
  - `PlacedComponent` mit Pin-Listen, Bezeichner, Wert und Farbcode.
  - `WireTrace` mit Layer (`TOP`/`BOTTOM`), Segmenten und Farb-Attributen.
  - `PerfProject` Wurzelklasse mit Metadaten.
- [ ] Implementierung von `ProjectStorageService` mit Jackson Databind für `.perf`-Dateien.
- [ ] Unit-Tests in `PerfProjectTest` für fehlerfreie Serialisierung und Deserialisierung.
- [ ] **Verifikation:** 100% aller Tests in `mvn test` bestehen.

### Phase 3: High-DPI Canvas & LochMaster-Lineale (Rulers)
- [ ] Implementierung der mathematischen Koordinaten-Engine (`CoordinateTransform`).
- [ ] `PerfBoardCanvas` mit flüssigem Pan (Mausrad-Drag) und stufenlosem Zoom (zentriert um Cursor).
- [ ] Fotorealistisches Rendering von Epoxid-Substrat, Kupfer-Pads und Bohrlöchern mit Viewport-Culling.
- [ ] `RulerCanvas`: Horizontales und vertikales Lineal mit Loch-Indizes, mm-Teilung und synchron mitfahrendem Tracking-Marker.
- [ ] Layer-Rendering: Umschaltung zwischen Top Layer, Bottom Layer und X-Ray Modus.
- [ ] **Verifikation:** Interaktive Platine reagiert latenzfrei auf Zoom, Pan und Mausbewegungen.

### Phase 4: CAD-Werkzeuge & Interaktions-Modi
- [ ] State-Machine für aktive Werkzeuge (`EditorState`).
- [ ] `ToolboxBar`: Linke Werkzeugleiste mit 8 Buttons (`V`, `W`, `T`, `C`, `X`, `N`, `Entf`).
- [ ] Jumper-Draht-Tool (`WireTool`): Verlegen isolierter Schaltdrähte auf dem Top Layer.
- [ ] Lötbrücken-Tool (`SolderTool`): Ziehen von Zinnbrücken auf dem Bottom Layer.
- [ ] Bauteil-Platzierungs-Tool (`PlacementTool`) mit Ghost-Vorschau und Pad-Magnetismus.
- [ ] Selektions- & Verschiebe-Werkzeug mit Gummiband-Rechteck und Drehung (`R`).
- [ ] **Verifikation:** Bauteile und Drähte lassen sich fehlerfrei platzieren, drehen, verschieben und löschen.

### Phase 5: Bauteil-Renderer & Widerstands-Farbcode
- [ ] `ResistorColorCode` Engine mit bidirektionalem Parser (String <-> Farbbänder).
- [ ] `ComponentRenderer` für alle Footprints:
  - Axial-Widerstände mit exakten Farbringen.
  - Keramik- und Elektrolytkondensatoren mit Polaritätsmarkierung.
  - Dioden (1N4148 / 1N4007) mit Kathodenring.
  - LEDs in 5 Farben mit Abflachung.
  - Transistoren (TO-92) mit 3 Pins.
  - DIL/DIP-Sockel & ICs (DIP-8 bis DIP-40) mit Notch und Pin-1 Markierung.
  - Stiftleisten und Schraubklemmen.
- [ ] Unit-Tests in `ResistorColorCodeTest` für alle Standard-E12- und E24-Widerstandswerte.
- [ ] **Verifikation:** Alle Bauteile werden mit fotorealistischer Vektorpräzision gerendert.

### Phase 6: LochMaster-Workbench UI, Dock-Panels & Undo/Redo
- [ ] `WorkbenchView`: Vollständiges CAD-Layout mit TopCommandBar, ToolboxBar, Rulers, Canvas, Sidebar und StatusBar.
- [ ] `SidebarContainer`:
  - `CatalogPanel` mit kategorisierter Bauteil-Bibliothek und Suchfeld.
  - `InspectorPanel` zur Live-Bearbeitung selektierter Komponenten.
  - `ProjectTreePanel` mit hierarchischer Bauteilliste.
- [ ] `StatusBar`: Live-Anzeige von Loch-Koordinaten, mm-Maßen, Layer und Zoom-Schieberegler.
- [ ] `CommandHistory` (Undo/Redo für Platzieren, Verschieben, Löschen, Drehen via `Strg+Z` und `Strg+Y`).
- [ ] `NewBoardDialog` mit Validierung von Spalten und Zeilen (5..200).
- [ ] `ExportService`: PNG-Bildexport und CSV-Stücklistenexport (BOM).
- [ ] **Verifikation:** End-to-End Workflow-Test (Neu -> Entwurf -> Verdrahtung -> Stückliste -> Speichern -> Laden).

---

## 14. Start-, Build- und Verifikations-Befehle

Alle Befehle werden im Projekt-Hauptverzeichnis ausgeführt:

- **Projekt kompilieren:**
  ```powershell
  mvn clean compile
  ```
- **Automatisierte Unit-Tests ausführen:**
  ```powershell
  mvn test
  ```
- **Anwendung interaktiv starten (Entwicklungsmodus):**
  ```powershell
  mvn exec:java
  ```
- **Ausführbares Fat-JAR bauen:**
  ```powershell
  mvn clean package
  ```
- **Fat-JAR testen & starten:**
  ```powershell
  java -jar target/perfcraft-1.0.0-all.jar
  ```
