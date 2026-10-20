# Projektanweisung: PerfCraft (Perfboard Designer)

> **Verbindliche Spezifikation für den ausführenden Entwickler-Agenten**
> Version: 1.0.0-PROD
> Zielsystem: Java 21 LTS | JavaFX 21 | Apache Maven | Material Design 2 Dark Standard
> Leitprinzip: **Klarheit auf den ersten Blick (Zero-Clutter Workbench)**
> Dieses Dokument ist die ausnahmslos gültige Arbeitsgrundlage. Jede Klasse, jedes Interface, jedes Tool und jede Rendering-Schicht muss exakt den hier definierten Vorgaben entsprechen.

---

## Inhaltsverzeichnis
1. [Präambel, Mission & Design-Philosophie (Zero-Clutter CAD)](#1-präambel-mission--design-philosophie-zero-clutter-cad)
2. [Tech-Stack, Frameworks & Feste Leitplanken](#2-tech-stack-frameworks--feste-leitplanken)
3. [Aufgeräumtes UI-Layout (Klar & Sofort Verständlich)](#3-aufgeräumtes-ui-layout-klar--sofort-verständlich)
   - 3.1 Das 4-Zonen-Layout auf einen Blick
   - 3.2 Menüleiste (Schlank & Fokussiert)
   - 3.3 Obere Befehlsleiste (Header-Bar)
   - 3.4 Die 3 Kern-Werkzeuge (Auswahl, Kabel/Silberdraht, Bauteil)
   - 3.5 Rechter Bauteil-Katalog & Eigenschaften-Inspektor
   - 3.6 Der Perfboard Canvas (Clean 2D Flat-Vector Stil)
   - 3.7 Lineale (Rulers) & Statusleiste
4. [Die 3 Kern-Werkzeuge im Detail](#4-die-3-kern-werkzeuge-im-detail)
   - 4.1 Werkzeug 1: Auswahl & Verschieben (`V`)
   - 4.2 Werkzeug 2: Universelles Draht- & Kabel-Werkzeug (`W`) (Kabel / Silberdraht)
   - 4.3 Werkzeug 3: Bauteil-Platzierungs-Werkzeug (`C`)
5. [Bauteil-Bibliothek (Inklusive Taster & Widerstands-Farbbänder)](#5-bauteil-bibliothek-inklusive-taster--widerstands-farbbänder)
   - 5.1 Taster (4-Pin Kurzhubtaster / Tactile Switch 6x6 mm)
   - 5.2 Widerstände (Axial 0207 mit echten Farbringen)
   - 5.3 Kondensatoren (Keramik & Elko mit Minus-Markierung)
   - 5.4 Dioden & LEDs (Mit Kathoden-Markierung)
   - 5.5 Transistoren (TO-92 Dreibeiner)
   - 5.6 Integrierte Schaltungen (DIL/DIP-8 & DIP-16 Sockel/ICs)
   - 5.7 Stiftleisten & Klemmen
6. [Widerstands-Farbcode-Engine (Mathematik & IEC 60062)](#6-widerstands-farbcode-engine-mathematik--iec-60062)
   - 6.1 Farbwert-Tabelle (Exakte Hex-Werte für Canvas)
   - 6.2 Parsing-Algorithmus (String zu Ohm)
   - 6.3 Ring-Generierungs-Algorithmus
7. [Canvas Rendering-Pipeline (Clean 2D Flat-Vector statt Pseudofotorealismus)](#7-canvas-rendering-pipeline-clean-2d-flat-vector-statt-pseudofotorealismus)
   - 7.1 Visuelle Farbabstimmung (Substrat, Pads, Bauteile)
   - 7.2 Koordinatensysteme (Screen vs. World vs. Grid)
   - 7.3 Vollständige Implementierung von `CoordinateTransform.java`
   - 7.4 Zoom, Pan & Magnetisches Pad-Snapping
   - 7.5 Ebenen-Rendering (Bauteilseite, Lötseite, Beide / Röntgen)
8. [Vollständiges Datenmodell & `.perf` Dateiformat](#8-vollständiges-datenmodell--perf-dateiformat)
   - 8.1 JSON-Schemaspezifikation
   - 8.2 Java-Klassenstruktur im `model`-Package
9. [Undo/Redo Transaktions-Architektur (Command Pattern)](#9-undoredo-transaktions-architektur-command-pattern)
10. [Stücklisten- (BOM) & Export-Engine (PNG & CSV)](#10-stücklisten--bom--export-engine-png--csv)
11. [Vollständige Tastatur- & Maus-Interaktionsmatrix](#11-vollständige-tastatur--maus-interaktionsmatrix)
12. [Detaillierte Fehlermatrix & Edge Cases](#12-detaillierte-fehlermatrix--edge-cases)
13. [Phasenweise Implementierungs-Roadmap](#13-phasenweise-implementierungs-roadmap)
14. [Start-, Build- und Verifikations-Befehle](#14-start--build--und-verifikations-befehle)

---

## 1. Präambel, Mission & Design-Philosophie (Zero-Clutter CAD)

### 1.1 Mission von PerfCraft
PerfCraft ist eine intuitive, visuell aufgeräumte Desktop-Applikation zur schnellen Konzeption, Verdrahtung und Dokumentation von Lochraster-Schaltungen (Perfboards / Pad-per-Hole Prototypenboards).

Viele klassische Elektronikprogramme (wie KiCad, Eagle oder LochMaster) wirken auf den ersten Blick überfüllt: Dutzende verschachtelte Untermenüs, unübersichtliche Werkzeugkaskaden, kryptische Icons und übertriebener Pseudofotorealismus erschweren den schnellen Einstieg.

**Die PerfCraft-Philosophie lautet:**
1. **Auf den ersten Blick verständlich:** Keine überfüllten Toolbars. Jeder Anwender – vom Schüler über den Hobby-Bastler bis zum Ingenieur – versteht das Programm innerhalb von 10 Sekunden ohne Handbuch.
2. **Kompaktes, logisches Werkzeug-Set:**
   - **Ein Tool zum Auswählen, Verschieben und Drehen.**
   - **Ein universelles Tool für Leitungen:** Egal ob isoliertes farbiges Kabel oder blanker Silberdraht / Lötbrücke.
   - **Ein Tool zum Platzieren von Bauteilen und Geräten:** Direktes Auswählen und Setzen auf das Lochraster.
3. **Clean 2D Flat-Vector Design statt Kitsch:**
   - Kein pseudofotorealistischer Glanz-Overkill, keine künstlichen Rausch-Texturen.
   - Stattdessen: **Satte, harmonische Farben**, klares Platinengrün, saubere Kupfer-Pads mit Bohrung, eindeutig erkennbare Bauteil-Silhouetten und **echte, exakt berechnete Farbbänder auf den Widerständen**.
4. **Kernbauteile im Fokus:**
   - Neben Widerständen, Kondensatoren, LEDs und ICs sind **Taster (4-Pin Kurzhubtaster)** eine elementare Standard-Komponente.

---

## 2. Tech-Stack, Frameworks & Feste Leitplanken

### 2.1 Plattform & Basis-Bibliotheken
- **Java-Version:** Java 21 LTS (Oracle OpenJDK, Eclipse Temurin).
- **Betriebssysteme:** Windows 10/11 & Linux (vollwertig cross-platform ohne native DLL-Abhängigkeiten).
- **Build-Tool:** Apache Maven 3.9+ (`pom.xml`).
- **UI-Framework:** JavaFX 21.0.2 (`javafx-controls`, `javafx-graphics`, `javafx-fxml`).
- **Serialisierung:** Jackson Databind 2.17.0 (`com.fasterxml.jackson.core:jackson-databind`).
- **Testing:** JUnit 5.10.2 Jupiter (`org.junit.jupiter:junit-jupiter`).

### 2.2 Strikte Maven- & Code-Konventionen
- **GroupId:** `de.exit`
- **ArtifactId:** `perfcraft`
- **Version:** `1.0.0-SNAPSHOT`
- **Root-Package:** `de.exit.perfcraft`
- **Verzeichnisstruktur:** Alle Java-Klassen liegen unter `src/main/java/de/exit/perfcraft/...`.
- **Packaging:** `maven-shade-plugin` erzeugt eine ausführbare Fat-JAR unter `target/perfcraft-1.0.0-all.jar`.
- **Direkter Start:** `mvn exec:java` startet via `de.exit.perfcraft.Main`.

### 2.3 UI-Design & Material Design 2 Dark Standard
- **Elevation-Farben:**
  - Base / Background: `#121212`
  - Surface Level 1 (Toolbars, Leisten): `#1E1E1E`
  - Surface Level 2 (Karten, Dialoge, Sidebar): `#242424`
  - Surface Level 3 (Inputs, aktive Buttons): `#2C2C2C`
  - Hover: `#353535`
- **Kantenrundungen:** Durchgängig `4px`.
- **Trennlinien:** `1px solid #2C2C2C`.
- **Primärfarbe (Perfboard Green):** `#00C853` (Akzent) / `#43A047`.
- **Keine Klammerzusätze in Labels:** Buttons und Auswahlen tragen saubere Fachbegriffe (`"Kabel"`, `"Silberdraht"`, `"Widerstand"`, `"Taster"`), niemals umgangssprachliche Erklärungen im Label.

### 2.4 Anti-Halluzinations-Regeln
- ❌ **Kein Gradle** – Ausschließlich Apache Maven mit `pom.xml`.
- ❌ **Kein Swing / AWT** – Kein `java.awt.Desktop`, ausschließlich JavaFX.
- ❌ **Keine externen Drawing-Libraries** – Nativer JavaFX `GraphicsContext` mit sauberem Vektor-Rendering.
- ❌ **Kein Overengineering** – Übersichtliche, modulare Klassen unter 250 Zeilen.

---

## 3. Aufgeräumtes UI-Layout (Klar & Sofort Verständlich)

### 3.1 Das 4-Zonen-Layout auf einen Blick

Das Layout ist bewusst extrem aufgeräumt gehalten:

```text
+---------------------------------------------------------------------------------------------------------+
| [Menü] Datei   Bearbeiten   Ansicht   Platine   Hilfe                                                   |
+---------------------------------------------------------------------------------------------------------+
| [Header-Bar] [Neu] [Öffnen] [Speichern] | [Undo] [Redo] | Ansicht: [Bauteile] [Lötseite] [Beide] | Zoom |
+---+-------------------------------------------------------------------------------+---------------------+
|   | [Horizontales Lineal: Loch 1..N mit mitfahrendem Zeiger-Marker]               | [Rechte Seitenleiste|
| T +---+---------------------------------------------------------------------------+ [Bauteil-Auswahl]   |
| O | V |                                                                           |                     |
| O | e |                                                                           | (o) Widerstand      |
| L | r |                                                                           | ( ) Kondensator     |
| B | t |                       ZENTRALER PERFBOARD CANVAS                          | ( ) LED             |
| A | . |                                                                           | ( ) Diode           |
| R |   |               - Klares grünes Lochraster                                  | ( ) Transistor      |
|   | R |               - Echte Widerstandsfarbringe                                | ( ) IC (DIP)        |
| [1| u |               - Bunte Kabel & Silberdraht                                 | ( ) Taster          |
|  2| l |               - Flüssiger Zoom & Pan                                      +---------------------+
|  3| e |                                                                           | [Eigenschaften]     |
|  ]| r |                                                                           | Bezeichner: R1      |
|   |   |                                                                           | Wert: 10k Ω         |
|   |   |                                                                           | [Farbringe-Vorschau]|
+---+---+---------------------------------------------------------------------------+---------------------+
| [Statusleiste] Loch: X: 14, Y: 8 | Realmaß: 35.56 x 20.32 mm | Ebene: Bauteilseite | Zoom: 125%          |
+---------------------------------------------------------------------------------------------------------+
```

### 3.2 Menüleiste (Schlank & Fokussiert)
Die Menüleiste enthält nur die wirklich nötigen Funktionen:
- **Datei:** *Neu...* (`Strg+N`), *Öffnen...* (`Strg+O`), *Speichern* (`Strg+S`), *Speichern unter...*, *Export als Bild (PNG)*, *Export als Stückliste (CSV)*, *Beenden*.
- **Bearbeiten:** *Rückgängig* (`Strg+Z`), *Wiederholen* (`Strg+Y`), *Löschen* (`Entf`), *Alles auswählen* (`Strg+A`).
- **Ansicht:** *Bauteilseite (Oben)* (`F1`), *Lötseite (Unten)* (`F2`), *Beide Ebenen* (`F3`), *Originalgröße 100%* (`Strg+0`), *Ganzes Board anzeigen* (`Strg+F`).
- **Platine:** *Größe ändern...* (Spalten & Zeilen), *Platine leeren*.
- **Hilfe:** *Tastenkürzel*, *Über PerfCraft*.

### 3.3 Obere Befehlsleiste (Header-Bar)
Direkt unter dem Menü liegt eine aufgeräumte Befehlsleiste:
- **Projekt:** `Neu`, `Öffnen`, `Speichern`.
- **Verlauf:** `Rückgängig`, `Wiederholen`.
- **Ebenen-Wahlschalter (Segment-Button):**
  - `Bauteilseite`: Zeigt Komponenten und farbige Kabel oben.
  - `Lötseite`: Zeigt Kupfer-Pads und Silberdraht/Lötbrücken unten.
  - `Beide`: Halbtransparente Röntgenansicht beider Seiten.
- **Zoom-Gruppe:** `Vergrößern`, `Verkleinern`, `Einpassen`.

### 3.4 Die 3 Kern-Werkzeuge (Linke Toolbar)
Die linke Werkzeugleiste enthält exakt **3 große, eindeutige Schaltflächen**:
1. **Auswahl & Bewegen (`V`):** Anklicken, Verschieben, Drehen (`R`), Löschen (`Entf`).
2. **Kabel & Silberdraht (`W`):** Zieht Verbindungen von Loch zu Loch. Schnelle Umschaltung direkt in der Leiste zwischen:
   - **Kabel (isoliert, bunt)** für die Oberseite.
   - **Silberdraht (blank)** für die Unterseite / Lötstellen.
3. **Bauteil platzieren (`C`):** Aktiviert das Platzieren des rechts ausgewählten Bauteils.

### 3.5 Rechter Bauteil-Katalog & Eigenschaften-Inspektor
Die rechte Seitenleiste ist in zwei übersichtliche Abschnitte unterteilt:
- **Oben (Bauteil-Wahl):**
  - Schnelle Liste der verfügbaren Komponenten: *Widerstand*, *Kondensator*, *LED*, *Diode*, *Transistor*, *IC (DIP-8/16)*, **Taster (6x6 mm)**.
  - Klick auf einen Bauteiltyp wählt ihn sofort für das Platzierungs-Werkzeug vor.
- **Unten (Eigenschaften des selektierten Objekts):**
  - **Bezeichner:** z. B. `R1`, `C1`, `SW1`, `IC1`.
  - **Wert:** z. B. `10k`, `100n`, `NE555`, `Rot`.
  - **Live-Vorschau der Farbringe:** Bei Widerständen werden die 4 Farbbänder direkt im Inspektor als kleine Grafik gerendert.
  - **Drehung:** Button zum Drehen um 90° (`R`).

### 3.6 Der Perfboard Canvas (Clean 2D Flat-Vector Stil)
Der Canvas verzichtet auf übertriebene 3D-Effekte und setzt auf maximale visuelle Klarheit:
- **Substrat:** Ein angenehmes, sattes Platinengrün (`#1B4D2E`).
- **Kupfer-Pads:** Saubere, gold-kupferne Kreise (`#D4AF37`) mit schwarzem Bohrungs-Zentrum (`#121212`).
- **Bauteilkörper:** Klare Vektorformen mit hohem Kontrast:
  - Widerstand: Beiger Zylinder mit exakten Farbringen.
  - Taster: Quadratischer grauer Rahmen mit zentriertem rundem Tasterknopf und 4 Pins.
  - Kondensator: Runder Zylinder mit weißem Minuspol-Streifen oder gelbe Scheibe.
  - LED: Farbiger transparenter Kreis mit Abflachung.
  - IC: Dunkles Rechteck mit Pin-1-Kerbe.
- **Leitungen:**
  - Isolierte Kabel: Kräftige, satte Farben (Rot, Schwarz, Blau, Gelb, Grün, Weiß).
  - Silberdraht / Lötbrücken: Silberner Vektorstrich (`#CFD8DC`) zwischen benachbarten oder entfernten Löchern.

### 3.7 Lineale (Rulers) & Statusleiste
- **Lineale oben und links:** Zeigen die Loch-Nummern `1, 2, 3 ... N` und feine mm-Marken. Ein kleiner grüner Marker wandert mit dem Mauszeiger mit.
- **Statusleiste unten:**
  - `Loch: X: 14, Y: 8`
  - `Maß: 35.56 mm x 20.32 mm`
  - `Aktives Werkzeug: Kabel (Rot)`
  - `Zoom: 125%`

---

## 4. Die 3 Kern-Werkzeuge im Detail

### 4.1 Werkzeug 1: Auswahl & Verschieben (`V`)
- **Aktivierung:** Taste `V` oder Klick auf das Zeiger-Icon.
- **Bedienung:**
  - **Klick auf Bauteil oder Leitung:** Wählt das Element aus, hebt es mit einer feinen grünen Kontur hervor und zeigt seine Werte im rechten Inspektor an.
  - **Ziehen (Drag & Drop):** Verschiebt das Bauteil flüssig über das Board; die Pins rasten magnetisch auf den Lochmitten ein.
  - **Taste `R`:** Dreht das selektierte Bauteil um 90° im Uhrzeigersinn.
  - **Taste `Entf`:** Löscht das ausgewählte Element sofort.
  - **Gummiband-Rahmen:** Ziehen ins Leere spannt ein grünes Auswahlrechteck auf, um mehrere Bauteile gleichzeitig zu markieren und zu bewegen.

### 4.2 Werkzeug 2: Universelles Draht- & Kabel-Werkzeug (`W`)
Anstatt den Nutzer mit separaten Werkzeugen für Lötbrücken, Drähte, Jumper und Kabel zu verwirren, vereint PerfCraft alle Leitungen in **einem einzigen, intuitiven Werkzeug**:

- **Aktivierung:** Taste `W` oder Klick auf das Leitungs-Icon.
- **Typ-Umschaltung in der Leiste:**
  - **Isoliertes Kabel:**
    - Für die Bauteilseite (Top Layer).
    - Schnellauswahl der Farbe: Rot (VCC), Schwarz (GND), Blau, Gelb, Grün, Weiß.
    - Wird als satter, farbiger Leitungsstrich mit Anschlusspins gezeichnet.
  - **Silberdraht / Brücke:**
    - Für die Lötseite (Bottom Layer) oder blanke Brücken.
    - Wird als silberner Leitungsstrich (`#CFD8DC`) gezeichnet.
- **Workflow:**
  1. Klick auf Start-Loch verankert den Beginn der Leitung.
  2. Mausbewegung zeigt eine elastische Vorschau-Linie mit Einrasten auf Ziel-Löcher.
  3. Optionaler Zwischenklick erzeugt einen 90°-Knick für saubere, rechtwinklige Leitungsverlegung.
  4. Klick auf das Ziel-Loch schließt die Leitung ab.
  5. `Escape` bricht das Ziehen jederzeit ab.

### 4.3 Werkzeug 3: Bauteil-Platzierungs-Werkzeug (`C`)
- **Aktivierung:** Taste `C` oder direkter Klick auf ein Bauteil in der rechten Auswahlliste.
- **Workflow:**
  1. Bauteil rechts anklicken (z. B. *Widerstand 10k* oder *Taster*).
  2. Auf dem Canvas folgt eine saubere, halbtransparente Vorschau (Ghost) dem Mauszeiger und rastet magnetisch auf den Löchern ein.
  3. Taste `R` dreht das Bauteil vor dem Absetzen in die gewünschte Richtung (0°, 90°, 180°, 270°).
  4. Ein Linksklick setzt das Bauteil fest auf das Lochraster.
  5. PerfCraft vergibt automatisch den nächsten Bezeichner (z. B. `R1` -> `R2`, `SW1` -> `SW2`).
  6. Das Werkzeug bleibt aktiv, sodass man sofort weitere gleiche Bauteile platzieren kann; Rechtsklick oder `Escape` beendet das Platzieren.

---

## 5. Bauteil-Bibliothek (Inklusive Taster & Widerstands-Farbbänder)

### 5.1 Taster (4-Pin Kurzhubtaster / Tactile Switch 6x6 mm)
Der Kurzhubtaster ist eine unverzichtbare Komponente für Prototypen und wird in PerfCraft als vollwertiges Standard-Bauteil geführt:
- **Mechanische Geometrie:**
  - Gehäuse: 6.0 mm x 6.0 mm.
  - Pin-Abstand: Spreizung über **3 Löcher horizontal x 2 Löcher vertikal** (oder gedreht 2x3 Löcher).
  - Pins 1 & 2 sind intern gebrückt (Kontaktseite A), Pins 3 & 4 sind intern gebrückt (Kontaktseite B).
- **Clean 2D Rendering:**
  - Quadratischer Gehäusekörper in Dunkelgrau (`#37474F`).
  - Vier metallische Lötfähnchen (`#B0BEC5`) an den Ecken, die in den Loch-Bohrungen verankert sind.
  - Zentrierter, runder Tasterknopf in Schwarz (`#212121`) mit feiner Kontur.
  - Standard-Bezeichner: `SW1`, `SW2` (Switch).

```text
       Pin 1 (o)-------(o) Pin 2  (intern verbunden A)
             |  [ (o) ]  |        (Runder Knopf)
       Pin 3 (o)-------(o) Pin 4  (intern verbunden B)
```

### 5.2 Widerstände (Axial 0207 mit echten Farbringen)
- **Geometrie:** Standard-Spannweite 4 Löcher (10.16 mm) oder 5 Löcher (12.7 mm).
- **Clean 2D Rendering:**
  - Beigefarbener Zylinderkörper (`#D7CCC8`).
  - Zwei silberne Anschlussbeinchen (`#B0BEC5`).
  - **Echte Farbbänder:** Vier exakt berechnete Farbringe entsprechend dem eingestellten Ohm-Wert (z. B. Braun-Schwarz-Orange-Gold für 10kΩ).
  - Standard-Bezeichner: `R1`, `R2`.

```text
(Pin 1)-----[ = | = | = | = ]-----(Pin 2)
  X=0          Farbringe            X=4
```

### 5.3 Kondensatoren (Keramik & Elko)
- **Keramikkondensator (MLCC / Scheibe):**
  - Spannweite: 1 oder 2 Löcher.
  - Gelb-orangefarbener flacher Block (`#FFA726`) mit Kapazitätsaufdruck (z. B. `104`).
- **Elektrolytkondensator (Radialer Elko):**
  - Spannweite: 1 bis 2 Löcher.
  - Runder Zylinderkopf (Draufsicht) in Dunkelblau (`#1565C0`) mit weißem Minuspol-Streifen (`- - -`) zur klaren Polaritätskontrolle.

### 5.4 Dioden & LEDs
- **Diode (1N4148 / 1N4007):**
  - Spannweite: 3 bis 4 Löcher.
  - Zylindrischer Körper mit gut sichtbarem weißem oder schwarzem **Kathodenring**.
- **Leuchtdiode (LED 3mm / 5mm):**
  - Spannweite: 1 Loch zwischen Anode (+) und Kathode (-).
  - Farbiger Kreis (Rot, Grün, Gelb, Blau, Kaltweiß) mit **Abflachung** an der Kathodenseite.

### 5.5 Transistoren (TO-92 Dreibeiner)
- **Geometrie:** 3 Pins in einer Reihe (Lochabstand 2.54 mm: Pin 1, 2, 3 z. B. E-B-C).
- **Rendering:** Schwarzes Halbkreis-Gehäuse mit planer Vorderseite und Beschriftung (z. B. `BC547`).

### 5.6 Integrierte Schaltungen (DIL/DIP-8 & DIP-16 Sockel/ICs)
- **Geometrie:**
  - DIP-8: 2 Reihen à 4 Pins, Reihenabstand 3 Löcher (7.62 mm).
  - DIP-16: 2 Reihen à 8 Pins, Reihenabstand 3 Löcher (7.62 mm).
- **Rendering:**
  - Dunkelgrauer IC-Körper (`#263238`).
  - Halbkreisförmige Kerbe (Notch) an Kopfseite und Punkt an Pin 1.
  - Zentrierte Beschriftung (z. B. `NE5532`, `TL072`).

### 5.7 Stiftleisten & Klemmen
- **Stiftleisten:** 1x2 bis 1x8 Pins mit schwarzen Isolatorkörpern und goldenen Kontakten.
- **Schraubklemmen:** 2-polig und 3-polig im Raster 5.08 mm (2 Löcher) in PCB-Grün mit Schraubenöffnungen.

### 5.8 Konkrete 2D-Zeichenroutinen (`ComponentRenderer.java`)

Die Klasse `ComponentRenderer` setzt das Clean 2D Flat-Vector Prinzip in performantem JavaFX-Code um:

```java
package de.exit.perfcraft.ui.canvas;

import de.exit.perfcraft.model.PlacedComponent;
import de.exit.perfcraft.service.ResistorColorCode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ComponentRenderer {

    /**
     * Rendert einen 4-Pin Kurzhubtaster (6x6 mm) im Clean 2D Vektorstil.
     */
    public static void renderTactileSwitch(GraphicsContext gc, CoordinateTransform ct, PlacedComponent comp) {
        double x1 = ct.gridToScreenX(comp.getStart().x());
        double y1 = ct.gridToScreenY(comp.getStart().y());
        double hs = ct.getHoleSpacingPx();

        // 3x2 Löcher Bounding-Box
        double width = hs * 2.6;
        double height = hs * 1.6;
        double centerX = x1 + (hs * 1.0);
        double centerY = y1 + (hs * 0.5);

        // 1. Metall-Pins an den 4 Ecken
        gc.setFill(Color.web("#B0BEC5"));
        double pinSize = hs * 0.35;
        gc.fillRect(x1 - pinSize/2, y1 - pinSize/2, pinSize, pinSize);
        gc.fillRect(x1 + (hs * 2) - pinSize/2, y1 - pinSize/2, pinSize, pinSize);
        gc.fillRect(x1 - pinSize/2, y1 + hs - pinSize/2, pinSize, pinSize);
        gc.fillRect(x1 + (hs * 2) - pinSize/2, y1 + hs - pinSize/2, pinSize, pinSize);

        // 2. Quadratischer Gehäusekörper
        gc.setFill(Color.web("#37474F"));
        gc.fillRoundRect(centerX - width/2, centerY - height/2, width, height, 4, 4);
        gc.setStroke(Color.web("#546E7A"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(centerX - width/2, centerY - height/2, width, height, 4, 4);

        // 3. Metall-Deckplatte
        double plateW = width * 0.75;
        double plateH = height * 0.75;
        gc.setFill(Color.web("#78909C"));
        gc.fillRoundRect(centerX - plateW/2, centerY - plateH/2, plateW, plateH, 3, 3);

        // 4. Zentrierter runder Tasterknopf
        double btnRadius = Math.min(plateW, plateH) * 0.38;
        gc.setFill(Color.web("#212121"));
        gc.fillOval(centerX - btnRadius, centerY - btnRadius, btnRadius * 2, btnRadius * 2);
        gc.setStroke(Color.web("#424242"));
        gc.setLineWidth(1.0);
        gc.strokeOval(centerX - btnRadius, centerY - btnRadius, btnRadius * 2, btnRadius * 2);

        // 5. Bezeichner Text
        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator(), centerX - 10, centerY - height/2 - 3);
    }

    /**
     * Rendert einen axialen Widerstand mit echten, akkuraten Farbringen.
     */
    public static void renderResistor(GraphicsContext gc, CoordinateTransform ct, PlacedComponent comp) {
        double x1 = ct.gridToScreenX(comp.getStart().x());
        double y1 = ct.gridToScreenY(comp.getStart().y());
        double x2 = ct.gridToScreenX(comp.getEnd().x());
        double y2 = ct.gridToScreenY(comp.getEnd().y());

        // 1. Silberne Anschlussdrähte
        gc.setStroke(Color.web("#B0BEC5"));
        gc.setLineWidth(2.0);
        gc.strokeLine(x1, y1, x2, y2);

        // 2. Widerstandskörper mittig
        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;
        double bodyLength = ct.getHoleSpacingPx() * 2.4;
        double bodyHeight = ct.getHoleSpacingPx() * 0.85;

        // Horizontale Ausrichtung (oder entsprechend Drehung)
        double bx = midX - bodyLength / 2.0;
        double by = midY - bodyHeight / 2.0;

        gc.setFill(Color.web("#D7CCC8")); // Sandfarben / Beige
        gc.fillRoundRect(bx, by, bodyLength, bodyHeight, 4, 4);
        gc.setStroke(Color.web("#A1887F"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(bx, by, bodyLength, bodyHeight, 4, 4);

        // 3. Echte Farbringe
        ResistorColorCode.Bands bands = ResistorColorCode.parseAndCalculate(comp.getValue());
        if (bands != null) {
            double bandW = bodyLength * 0.08;
            double spacing = bodyLength * 0.18;
            double startOffset = bx + bodyLength * 0.20;

            drawBand(gc, startOffset + 0 * spacing, by, bandW, bodyHeight, bands.band1().getHexColor());
            drawBand(gc, startOffset + 1 * spacing, by, bandW, bodyHeight, bands.band2().getHexColor());
            drawBand(gc, startOffset + 2 * spacing, by, bandW, bodyHeight, bands.multiplier().getHexColor());
            drawBand(gc, startOffset + 3.2 * spacing, by, bandW, bodyHeight, bands.tolerance().getHexColor());
        }

        // 4. Bezeichner & Wert
        gc.setFill(Color.web("#E0E0E0"));
        gc.fillText(comp.getDesignator() + " (" + comp.getValue() + ")", bx, by - 4);
    }

    private static void drawBand(GraphicsContext gc, double x, double y, double w, double h, String hexColor) {
        gc.setFill(Color.web(hexColor));
        gc.fillRect(x, y, w, h);
    }
}
```

---

## 6. Widerstands-Farbcode-Engine (Mathematik & IEC 60062)

PerfCraft berechnet die Farbringe vollautomatisch aus dem eingegebenen Widerstandswert.

### 6.1 Farbwert-Tabelle (Exakte Hex-Werte für Canvas)

| Farbe | Ziffer | Multiplikator | Toleranz | Hex-Farbcode auf Canvas |
| :--- | :--- | :--- | :--- | :--- |
| **Schwarz** | 0 | $10^0 = 1$ | - | `#1A1A1A` |
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
Der Parser versteht alle gängigen Schreibweisen:
- `"10k"` oder `"10kΩ"` -> $10.000\ \Omega$
- `"4k7"` oder `"4.7k"` -> $4.700\ \Omega$
- `"470"` oder `"470R"` -> $470\ \Omega$
- `"1M"` -> $1.000.000\ \Omega$
- `"0.22"` oder `"0R22"` -> $0.22\ \Omega$

### 6.3 Vollständige Implementierung von `ResistorColorCode.java`

```java
package de.exit.perfcraft.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResistorColorCode {

    public enum ResistorColor {
        BLACK(0, 1.0, null, "#1A1A1A"),
        BROWN(1, 10.0, 1.0, "#6D4C41"),
        RED(2, 100.0, 2.0, "#D32F2F"),
        ORANGE(3, 1_000.0, null, "#F57C00"),
        YELLOW(4, 10_000.0, null, "#FBC02D"),
        GREEN(5, 100_000.0, 0.5, "#388E3C"),
        BLUE(6, 1_000_000.0, 0.25, "#1976D2"),
        VIOLET(7, 10_000_000.0, 0.1, "#7B1FA2"),
        GRAY(8, 100_000_000.0, 0.05, "#616161"),
        WHITE(9, 1_000_000_000.0, null, "#EEEEEE"),
        GOLD(-1, 0.1, 5.0, "#C5A059"),
        SILVER(-2, 0.01, 10.0, "#9E9E9E");

        private final int digit;
        private final double multiplier;
        private final Double tolerance;
        private final String hexColor;

        ResistorColor(int digit, double multiplier, Double tolerance, String hexColor) {
            this.digit = digit;
            this.multiplier = multiplier;
            this.tolerance = tolerance;
            this.hexColor = hexColor;
        }

        public int getDigit() { return digit; }
        public double getMultiplier() { return multiplier; }
        public Double getTolerance() { return tolerance; }
        public String getHexColor() { return hexColor; }

        public static ResistorColor fromDigit(int d) {
            for (ResistorColor c : values()) {
                if (c.digit == d) return c;
            }
            return BLACK;
        }

        public static ResistorColor fromMultiplierExponent(int exp) {
            return switch (exp) {
                case -2 -> SILVER;
                case -1 -> GOLD;
                case 0 -> BLACK;
                case 1 -> BROWN;
                case 2 -> RED;
                case 3 -> ORANGE;
                case 4 -> YELLOW;
                case 5 -> GREEN;
                case 6 -> BLUE;
                case 7 -> VIOLET;
                case 8 -> GRAY;
                case 9 -> WHITE;
                default -> BLACK;
            };
        }

        public static ResistorColor fromTolerance(double percent) {
            if (Math.abs(percent - 1.0) < 0.01) return BROWN;
            if (Math.abs(percent - 2.0) < 0.01) return RED;
            if (Math.abs(percent - 0.5) < 0.01) return GREEN;
            if (Math.abs(percent - 0.25) < 0.01) return BLUE;
            if (Math.abs(percent - 0.1) < 0.01) return VIOLET;
            if (Math.abs(percent - 10.0) < 0.01) return SILVER;
            return GOLD; // Standard 5%
        }
    }

    public record Bands(ResistorColor band1, ResistorColor band2, ResistorColor multiplier, ResistorColor tolerance) {}

    private static final Pattern RESISTOR_PATTERN = Pattern.compile(
        "^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*([kKMmRΩ]?)\\s*$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SHORT_PREFIX_PATTERN = Pattern.compile(
        "^\\s*([0-9]+)\\s*([kKMmR])\\s*([0-9]+)?\\s*$", Pattern.CASE_INSENSITIVE
    );

    public static Double parseOhms(String input) {
        if (input == null || input.isBlank()) return null;
        String clean = input.trim().replace("Ω", "").replace("ohm", "").trim();

        // Match z. B. "4k7"
        Matcher sm = SHORT_PREFIX_PATTERN.matcher(clean);
        if (sm.matches()) {
            double whole = Double.parseDouble(sm.group(1));
            String prefix = sm.group(2).toUpperCase(Locale.ROOT);
            double fraction = sm.group(3) != null ? Double.parseDouble("0." + sm.group(3)) : 0.0;
            double mult = switch (prefix) {
                case "K" -> 1_000.0;
                case "M" -> 1_000_000.0;
                default -> 1.0;
            };
            return (whole + fraction) * mult;
        }

        // Match z. B. "10k" oder "470"
        Matcher m = RESISTOR_PATTERN.matcher(clean);
        if (m.matches()) {
            double val = Double.parseDouble(m.group(1).replace(",", "."));
            String unit = m.group(2).toUpperCase(Locale.ROOT);
            double mult = switch (unit) {
                case "K" -> 1_000.0;
                case "M" -> 1_000_000.0;
                default -> 1.0;
            };
            return val * mult;
        }
        return null;
    }

    public static Bands calculate4Band(double ohms, double tolerancePercent) {
        if (ohms <= 0) return null;
        int exponent = (int) Math.floor(Math.log10(ohms)) - 1;
        double normalized = ohms / Math.pow(10, exponent);
        int val = (int) Math.round(normalized);
        int d1 = (val / 10) % 10;
        int d2 = val % 10;

        ResistorColor b1 = ResistorColor.fromDigit(d1);
        ResistorColor b2 = ResistorColor.fromDigit(d2);
        ResistorColor mult = ResistorColor.fromMultiplierExponent(exponent);
        ResistorColor tol = ResistorColor.fromTolerance(tolerancePercent);
        return new Bands(b1, b2, mult, tol);
    }

    public static Bands parseAndCalculate(String input) {
        Double ohms = parseOhms(input);
        if (ohms == null) return null;
        return calculate4Band(ohms, 5.0); // 5% Standard Gold
    }
}
```

---

## 7. Canvas Rendering-Pipeline (Clean 2D Flat-Vector statt Pseudofotorealismus)

### 7.1 Visuelle Farbabstimmung (Substrat, Pads, Bauteile)
- **Kein Rauschen / Keine Pseudo-Texturen:** Klare, saubere Flächen für ermüdungsfreies Arbeiten.
- **Substrat:** `#1B4D2E` (Platinengrün) mit feinem Rand `#143922`.
- **Lochraster-Pads:**
  - Außenring: `#D4AF37` (Warmes Kupfer/Gold).
  - Innenkreis: `#121212` (Bohrung).
  - Durchmesser Pad: ca. 75% des Lochabstands (1.9 mm).
  - Durchmesser Bohrung: ca. 35% des Lochabstands (0.9 mm).
- **Kontrast:** Alle Elemente sind so abgestimmt, dass sie sich sofort vom dunkelgrünen Substrat abheben.

### 7.2 Koordinatensysteme (Screen vs. World vs. Grid)
- **Screen-Space:** Pixel auf der Bildschirmoberfläche.
- **World-Space:** Millimeter auf der Platine (Pitch = 2.54 mm).
- **Grid-Space:** Ganzzahlige Lochkoordinaten $X \in [0, \text{Cols}-1], Y \in [0, \text{Rows}-1]$.

### 7.3 Vollständige Implementierung von `CoordinateTransform.java`

```java
package de.exit.perfcraft.ui.canvas;

import de.exit.perfcraft.model.HoleCoord;
import javafx.geometry.Point2D;

public class CoordinateTransform {
    private static final double DEFAULT_PIXELS_PER_MM = 3.7795; // 96 DPI Basis (1 mm = 3.78 px)
    private static final double PITCH_MM = 2.54;

    private double scale = 1.0;
    private double translateX = 40.0;
    private double translateY = 40.0;
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
        double newScale = Math.clamp(scale * zoomFactor, 0.2, 8.0);
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
        double scaleX = (canvasWidth - 80) / boardWidthPx;
        double scaleY = (canvasHeight - 80) / boardHeightPx;
        this.scale = Math.clamp(Math.min(scaleX, scaleY), 0.3, 3.0);
        this.translateX = (canvasWidth - (boardWidthPx * scale)) / 2.0;
        this.translateY = (canvasHeight - (boardHeightPx * scale)) / 2.0;
    }

    public double getScale() { return scale; }
    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }
}
```

### 7.4 Zoom, Pan & Magnetisches Pad-Snapping
- **Stufenloser Zoom:** Mausrad drehen zoomt zentriert um den aktuellen Mauszeigerpunkt.
- **Flüssiger Pan:** Mittlere Maustaste gedrückt halten oder Leertaste + Linke Maustaste verschiebt das Board.
- **Magnetisches Snapping:** Nähert sich der Mauszeiger einem Loch auf weniger als 65% des Lochabstands, springt die Vorschau exakt auf das Pad-Zentrum.

### 7.5 Ebenen-Rendering (Bauteilseite, Lötseite, Beide / Röntgen)
- **Modus 1 (Bauteilseite):** Zeigt das Substrat, die Pads, Bauteilkörper und farbige Kabel oben. Silberdrähte der Unterseite sind ausgeblendet.
- **Modus 2 (Lötseite):** Zeigt das Substrat, die Pads und Silberdrähte/Lötbrücken unten. Bauteilkörper der Oberseite sind ausgeblendet.
- **Modus 3 (Beide / Röntgen):** Zeigt beide Ebenen gleichzeitig: Bauteile auf der Oberseite werden mit 60% Transparenz gerendert, sodass Silberdrähte und Lötpunkte darunter klar durchscheinen.

---

## 8. Vollständiges Datenmodell & `.perf` Dateiformat

### 8.1 JSON-Schemaspezifikation
Das Projekt-Dateiformat `*.perf` speichert alle Daten in einem sauberen, lesbaren JSON-Dokument:

```json
{
  "formatVersion": 1,
  "generator": "PerfCraft 1.0.0",
  "project": {
    "name": "Taster-LED-Schaltung",
    "board": {
      "columns": 24,
      "rows": 18,
      "pitchMm": 2.54
    },
    "components": [
      {
        "id": "c-1",
        "type": "TACTILE_SWITCH",
        "designator": "SW1",
        "value": "Taster 6x6",
        "start": { "x": 5, "y": 4 },
        "rotation": 0,
        "layer": "TOP"
      },
      {
        "id": "c-2",
        "type": "RESISTOR_AXIAL",
        "designator": "R1",
        "value": "10k",
        "unit": "Ω",
        "start": { "x": 10, "y": 4 },
        "end": { "x": 14, "y": 4 },
        "rotation": 0,
        "layer": "TOP"
      },
      {
        "id": "c-3",
        "type": "LED",
        "designator": "LED1",
        "value": "Rot",
        "start": { "x": 16, "y": 4 },
        "end": { "x": 17, "y": 4 },
        "rotation": 0,
        "layer": "TOP"
      }
    ],
    "wires": [
      {
        "id": "w-1",
        "kind": "CABLE",
        "layer": "TOP",
        "color": "#E53935",
        "points": [
          { "x": 5, "y": 4 },
          { "x": 10, "y": 4 }
        ]
      },
      {
        "id": "w-2",
        "kind": "SILVER_WIRE",
        "layer": "BOTTOM",
        "color": "#CFD8DC",
        "points": [
          { "x": 14, "y": 4 },
          { "x": 16, "y": 4 }
        ]
      }
    ]
  }
}
```

### 8.2 Java-Klassenstruktur im `model`-Package

1. **`HoleCoord.java` (Record):**
   ```java
   package de.exit.perfcraft.model;

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

2. **`ComponentType.java` (Enum):**
   ```java
   package de.exit.perfcraft.model;

   public enum ComponentType {
       TACTILE_SWITCH("Taster (6x6 mm)", "SW"),
       RESISTOR_AXIAL("Widerstand (Axial)", "R"),
       CAPACITOR_CERAMIC("Kondensator (Keramik)", "C"),
       CAPACITOR_ELECTROLYTIC("Kondensator (Elko)", "C"),
       DIODE("Diode", "D"),
       LED("LED", "LED"),
       TRANSISTOR_TO92("Transistor (TO-92)", "Q"),
       IC_DIP("IC (DIP)", "IC"),
       PIN_HEADER("Stiftleiste", "J"),
       TERMINAL_BLOCK("Schraubklemme", "TB");

       private final String displayName;
       private final String defaultPrefix;

       ComponentType(String displayName, String defaultPrefix) {
           this.displayName = displayName;
           this.defaultPrefix = defaultPrefix;
       }

       public String getDisplayName() { return displayName; }
       public String getDefaultPrefix() { return defaultPrefix; }
   }
   ```

3. **`WireKind.java` (Enum):**
   ```java
   package de.exit.perfcraft.model;

   public enum WireKind {
       CABLE("Isoliertes Kabel", "#E53935", BoardLayer.TOP),
       SILVER_WIRE("Silberdraht / Bruecke", "#CFD8DC", BoardLayer.BOTTOM);

       private final String displayName;
       private final String defaultColor;
       private final BoardLayer defaultLayer;

       WireKind(String displayName, String defaultColor, BoardLayer defaultLayer) {
           this.displayName = displayName;
           this.defaultColor = defaultColor;
           this.defaultLayer = defaultLayer;
       }

       public String getDisplayName() { return displayName; }
       public String getDefaultColor() { return defaultColor; }
       public BoardLayer getDefaultLayer() { return defaultLayer; }
   }
   ```

4. **`PlacedComponent.java`:**
   ```java
   package de.exit.perfcraft.model;

   import java.util.Objects;

   public class PlacedComponent {
       private String id;
       private ComponentType type;
       private String designator;
       private String value;
       private HoleCoord start;
       private HoleCoord end;
       private int rotation; // 0, 1, 2, 3 (x90 Grad)
       private BoardLayer layer;

       public PlacedComponent() {}

       public PlacedComponent(String id, ComponentType type, String designator, String value,
                              HoleCoord start, HoleCoord end, int rotation, BoardLayer layer) {
           this.id = id;
           this.type = type;
           this.designator = designator;
           this.value = value;
           this.start = start;
           this.end = end;
           this.rotation = rotation;
           this.layer = layer;
       }

       public String getId() { return id; }
       public void setId(String id) { this.id = id; }
       public ComponentType getType() { return type; }
       public void setType(ComponentType type) { this.type = type; }
       public String getDesignator() { return designator; }
       public void setDesignator(String designator) { this.designator = designator; }
       public String getValue() { return value; }
       public void setValue(String value) { this.value = value; }
       public HoleCoord getStart() { return start; }
       public void setStart(HoleCoord start) { this.start = start; }
       public HoleCoord getEnd() { return end; }
       public void setEnd(HoleCoord end) { this.end = end; }
       public int getRotation() { return rotation; }
       public void setRotation(int rotation) { this.rotation = rotation; }
       public BoardLayer getLayer() { return layer; }
       public void setLayer(BoardLayer layer) { this.layer = layer; }

       public void rotate90() {
           this.rotation = (this.rotation + 1) % 4;
       }

       @Override
       public boolean equals(Object o) {
           if (this == o) return true;
           if (o == null || getClass() != o.getClass()) return false;
           PlacedComponent that = (PlacedComponent) o;
           return Objects.equals(id, that.id);
       }

       @Override
       public int hashCode() {
           return Objects.hash(id);
       }
   }
   ```

5. **`WireTrace.java`:**
   ```java
   package de.exit.perfcraft.model;

   import java.util.ArrayList;
   import java.util.List;
   import java.util.Objects;

   public class WireTrace {
       private String id;
       private WireKind kind;
       private BoardLayer layer;
       private String color;
       private List<HoleCoord> points = new ArrayList<>();

       public WireTrace() {}

       public WireTrace(String id, WireKind kind, BoardLayer layer, String color, List<HoleCoord> points) {
           this.id = id;
           this.kind = kind;
           this.layer = layer;
           this.color = color;
           if (points != null) this.points.addAll(points);
       }

       public String getId() { return id; }
       public void setId(String id) { this.id = id; }
       public WireKind getKind() { return kind; }
       public void setKind(WireKind kind) { this.kind = kind; }
       public BoardLayer getLayer() { return layer; }
       public void setLayer(BoardLayer layer) { this.layer = layer; }
       public String getColor() { return color; }
       public void setColor(String color) { this.color = color; }
       public List<HoleCoord> getPoints() { return points; }
       public void setPoints(List<HoleCoord> points) { this.points = points; }

       @Override
       public boolean equals(Object o) {
           if (this == o) return true;
           if (o == null || getClass() != o.getClass()) return false;
           WireTrace wireTrace = (WireTrace) o;
           return Objects.equals(id, wireTrace.id);
       }

       @Override
       public int hashCode() {
           return Objects.hash(id);
       }
   }
   ```

6. **`BoardConfig.java`:**
   ```java
   package de.exit.perfcraft.model;

   public class BoardConfig {
       private int columns = 24;
       private int rows = 18;
       private double pitchMm = 2.54;

       public BoardConfig() {}

       public BoardConfig(int columns, int rows, double pitchMm) {
           this.columns = Math.clamp(columns, 5, 200);
           this.rows = Math.clamp(rows, 5, 200);
           this.pitchMm = pitchMm > 0 ? pitchMm : 2.54;
       }

       public int getColumns() { return columns; }
       public void setColumns(int columns) { this.columns = Math.clamp(columns, 5, 200); }
       public int getRows() { return rows; }
       public void setRows(int rows) { this.rows = Math.clamp(rows, 5, 200); }
       public double getPitchMm() { return pitchMm; }
       public void setPitchMm(double pitchMm) { this.pitchMm = pitchMm; }
   }
   ```

7. **`PerfProject.java`:**
   ```java
   package de.exit.perfcraft.model;

   import java.util.ArrayList;
   import java.util.List;

   public class PerfProject {
       private String name = "Unbenanntes-Projekt";
       private BoardConfig board = new BoardConfig();
       private List<PlacedComponent> components = new ArrayList<>();
       private List<WireTrace> wires = new ArrayList<>();

       public PerfProject() {}

       public PerfProject(String name, BoardConfig board) {
           this.name = name;
           this.board = board;
       }

       public String getName() { return name; }
       public void setName(String name) { this.name = name; }
       public BoardConfig getBoard() { return board; }
       public void setBoard(BoardConfig board) { this.board = board; }
       public List<PlacedComponent> getComponents() { return components; }
       public void setComponents(List<PlacedComponent> components) { this.components = components; }
       public List<WireTrace> getWires() { return wires; }
       public void setWires(List<WireTrace> wires) { this.wires = wires; }
   }
   ```

---

## 9. Undo/Redo Transaktions-Architektur (Command Pattern)

Alle Aktionen werden über das Command-Muster abgesichert:

```java
public interface Command {
    void execute();
    void undo();
    String getDescription();
}
```

### Konkrete Commands:
- **`PlaceComponentCommand`:** Setzt ein Bauteil aufs Board; Undo entfernt es.
- **`AddWireCommand`:** Fügt ein Kabel oder einen Silberdraht hinzu; Undo löscht die Leitung.
- **`MoveComponentCommand`:** Speichert Start- und Zielkoordinate; Undo schiebt das Bauteil exakt zurück.
- **`RotateComponentCommand`:** Dreht das Bauteil um 90°; Undo dreht es um 270°.
- **`DeleteSelectionCommand`:** Löscht markierte Bauteile/Leitungen; Undo stellt alle Objekte wieder her.
- **`ChangePropertyCommand`:** Ändert Wert oder Bezeichner; Undo stellt alte Werte wieder her.

---

## 10. Stücklisten- (BOM) & Export-Engine (PNG & CSV)

### 10.1 Konsolidierter CSV-Export
Die Stücklisten-Engine aggregiert alle Bauteile nach Typ und Wert:
```text
Pos;Bauteiltyp;Wert;Bezeichner;Menge;Rasterabstand
1;Taster (6x6 mm);Kurzhubtaster;SW1;1;3x2 Löcher
2;Widerstand (Axial);10k Ω;R1;1;10.16 mm
3;LED (5mm);Rot;LED1;1;2.54 mm
```

### 10.2 Hochauflösender Bildexport (PNG)
- Rendert das Board ohne Anwendungsrahmen in eine PNG-Datei.
- Wählbare DPI (150 DPI für Bildschirm, 300 DPI für Druck).
- Wählbare Ansicht: Bauteilseite, Lötseite oder Beide Ebenen.

---

## 11. Vollständige Tastatur- & Maus-Interaktionsmatrix

| Taste / Geste | Kontext | Ausgelöste Aktion |
| :--- | :--- | :--- |
| `V` | Global | Auswahl- & Verschiebe-Werkzeug aktivieren |
| `W` | Global | Leitungs-Werkzeug (Kabel & Silberdraht) aktivieren |
| `C` | Global | Bauteil-Platzierungs-Werkzeug aktivieren |
| `R` | Bei Vorschau / Selektion | Bauteil um 90° im Uhrzeigersinn drehen |
| `Entf` | Bei Selektion | Ausgewähltes Bauteil oder Leitung löschen |
| `Strg + Z` | Global | Rückgängig (Undo) |
| `Strg + Y` | Global | Wiederholen (Redo) |
| `Strg + S` | Global | Projekt speichern |
| `Strg + O` | Global | Projekt öffnen |
| `Strg + N` | Global | Neues Board Dialog öffnen |
| `Strg + A` | Global | Alle Objekte auswählen |
| `Strg + 0` | Canvas | Zoom auf 100% zurücksetzen |
| `Strg + F` | Canvas | Board bildschirmfüllend einpassen |
| `F1` | Global | Bauteilseite anzeigen |
| `F2` | Global | Lötseite anzeigen |
| `F3` | Global | Beide Seiten anzeigen (Röntgenansicht) |
| `Mausrad` | Canvas | Stufenloser Zoom zentriert um Cursor |
| `Mittlere Maustaste Drag` | Canvas | Platine verschieben (Pan) |
| `Leertaste + Linke Maus`| Canvas | Platine verschieben (Pan-Alternative) |
| `Pfeiltasten` | Bei Selektion | Selektiertes Bauteil um 1 Loch verschieben |
| `Escape` | Im Werkzeug | Aktiven Vorgang abbrechen / Auswahl aufheben |

---

## 12. Detaillierte Fehlermatrix & Edge Cases

| Nr. | Situation / Edge Case | Systemverhalten | Feedback an den Nutzer |
| :--- | :--- | :--- | :--- |
| **E1** | Ungültige Board-Größe (<5 oder >200) | Button "Erstellen" deaktiviert | Tooltip: "Zulässiger Bereich: 5 bis 200 Löcher" |
| **E2** | Bauteil ragt beim Platzieren über den Platinenrand | Platzierung gesperrt, Klick ignoriert | Vorschau färbt sich rot (`#FF5252`) |
| **E3** | Zwei Bauteile teilen sich denselben Anschluss | Erlaubt (z. B. 2 Anschlüsse in 1 Loch) | Subtiler gelber Warn-Glow an der Bohrung |
| **E4** | Laden einer ungültigen oder korrupten `.perf`-Datei | Parsing-Fehler abgefangen, Fallback | Dialog: "Datei konnte nicht gelesen werden" |
| **E5** | Schreibgeschütztes Dateisystem beim Speichern | Exception abgefangen | Rote Meldung: "Fehler beim Speichern der Datei" |
| **E6** | Ungültiger Widerstandswert (z. B. "xyz") | Vorheriger Wert bleibt erhalten | Keine Farbringe, Eingabefeld leuchtet kurz orange |
| **E7** | Schließen des Fensters mit ungespeicherten Daten | Schließ-Event abgefangen | Dialog: "Möchten Sie vor dem Schließen speichern?" |
| **E8** | Zoom-Grenzen erreicht | Clamping auf 20% bis 800% | Zoom arretiert stabil am Anschlag |

---

## 13. Phasenweise Implementierungs-Roadmap

### Phase 1: Projekt-Setup & Maven-Infrastruktur
- [x] Maven `pom.xml` mit Java 21, JavaFX 21, Jackson Databind und JUnit 5 eingerichtet.
- [x] Basis-Package `de.exit.perfcraft` angelegt.
- [x] App-Icons (SVG, PNG, ICO) generiert und in Fensterleiste eingebunden.
- [x] `material-dark.css` bereitgestellt.
- [x] **Verifikation:** `mvn clean test` läuft fehlerfrei durch.

### Phase 2: Kern-Datenmodell & Serialisierung
- [ ] Datenmodellklassen in `de.exit.perfcraft.model` implementieren:
  - `HoleCoord` Record.
  - `ComponentType` Enum (inkl. `TACTILE_SWITCH`).
  - `WireKind` Enum (`CABLE`, `SILVER_WIRE`).
  - `PlacedComponent` & `WireTrace`.
  - `BoardConfig` & `PerfProject`.
- [ ] `ProjectStorageService` mit Jackson Databind für `.perf`-Dateien erstellen.
- [ ] Unit-Tests in `PerfProjectTest` für fehlerfreie Serialisierung/Deserialisierung.
- [ ] **Verifikation:** 100% bestandene Unit-Tests in `mvn test`.

### Phase 3: Canvas-Engine & Clean 2D Vektor-Rendering
- [ ] `CoordinateTransform` mit Zoom, Pan und magnetischem Pad-Snapping implementieren.
- [ ] `PerfBoardCanvas` mit klarem dunkelgrünem Substrat und Kupferpads erstellen.
- [ ] Horizontales und vertikales Loch-Lineal mit mitfahrendem Zeiger-Marker anbinden.
- [ ] Ebenen-Umschaltung (Bauteilseite, Lötseite, Beide/Röntgen) umsetzen.
- [ ] **Verifikation:** Interaktiver Start via `mvn exec:java`, flüssiges Pannen und Zoomen.

### Phase 4: Die 3 Kern-Werkzeuge & Bauteil-Rendering
- [ ] Auswahl- und Verschiebe-Werkzeug (`V`) mit Rotation (`R`) und Löschen (`Entf`).
- [ ] Leitungs-Werkzeug (`W`) für isolierte bunte Kabel und blanken Silberdraht.
- [ ] Bauteil-Platzierungs-Werkzeug (`C`) mit magnetischer Vorschau.
- [ ] Bauteil-Renderer:
  - Taster (6x6 mm mit 4 Pins).
  - Widerstand mit echten, berechneten Farbringen (`ResistorColorCode`).
  - Kondensatoren, LEDs, Dioden, ICs.
- [ ] **Verifikation:** Bauteile (inkl. Taster) und Leitungen lassen sich intuitiv platzieren und bewegen.

### Phase 5: Aufgeräumte UI, Inspektor & Export
- [ ] Header-Bar mit Neu, Öffnen, Speichern, Undo/Redo und Layer-Wahlschalter.
- [ ] Linke 3-Button-Toolbar (`V`, `W`, `C`).
- [ ] Rechte Seitenleiste mit Bauteil-Liste und Werte-Inspektor (inkl. Farbband-Vorschau).
- [ ] `CommandHistory` für Undo/Redo (`Strg+Z`, `Strg+Y`).
- [ ] PNG-Bildexport und CSV-Stücklistenexport (BOM).
- [ ] **Verifikation:** Kompletter End-to-End Workflow fehlerfrei getestet.

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
