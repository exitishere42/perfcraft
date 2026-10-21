# PerfCraft

[![Release](https://img.shields.io/github/v/release/exitishere42/perfcraft?style=flat-square&color=03DAC6)](https://github.com/exitishere42/perfcraft/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![JavaFX 21](https://img.shields.io/badge/JavaFX-21-orange?style=flat-square)](https://openjfx.io)

An intuitive, fast, and modern Perfboard & Stripboard CAD designer built with Java 21 LTS and JavaFX, featuring Material Design 2 Dark theme.

Inspired by LochMaster, Fritzing, and KiCad, PerfCraft focuses on speed, clarity, and instant usability without visual clutter.

---

## Features

- **3-Tool Workflow**:
  - `V` **Select Tool**: Click or box-select components and wire segments; drag to reposition or press `Delete` to remove.
  - `W` **Wire / Jumper Tool**: Click pad-to-pad to route tinned copper wires or colored insulated jumper wires with live preview.
  - `C` **Component Placer Tool**: Quick component placement with automatic IEC 60062 resistor color code bands, tactile switch 4-pin footprints, capacitors, diodes, LEDs, and DIP ICs.
- **Realistic Vector Perfboard Canvas**:
  - High-visibility silver pads (`#CFD8DC`) with clear drill holes.
  - Standard 2.54 mm (100 mil) pitch grid with alphanumeric coordinates (`A-Z`, `1-99`).
  - Smooth pan (`Middle Click` or `Right Click Drag`) and zoom (`Mouse Wheel`, 50% to 300%).
- **Interactive Electronic Components**:
  - **Resistors**: Automatic 4-band / 5-band color rendering based on ohmic value (e.g. 10 kΩ, 220 Ω, 4.7 kΩ).
  - **Tactile Switches**: 6x6 mm 4-pin momentary push buttons with internal pin pairings.
  - **LEDs & Diodes**: Directional polarity markings (anode/cathode) and selectable LED lens colors.
  - **Capacitors**: Ceramic disc & polarized electrolytic packages.
  - **ICs (DIP)**: Configurable DIP packages (DIP-8, DIP-14, DIP-16, DIP-28, etc.) with pin 1 index notch.
- **KiCad-Style Connectivity & DRC**:
  - **Net Highlighting**: Click any pad or wire to highlight all connected copper traces, jumpers, and component pins using real-time graph traversal (BFS).
  - **Design Rule Check (DRC)**: Instant validation against off-board pins and component overlaps.
- **Project Storage & Production Export**:
  - Clean human-readable JSON project format (`.perf`).
  - **Bill of Materials (BOM)**: One-click export to CSV sorted by component type, designator, and value.
  - **High-Resolution PNG Render**: Export finished perfboard layouts at 1x, 2x, or 4x scale.
- **Cross-Platform & Bilingual**:
  - Built-in English (default) and German (`DE`) localization with live switching.
  - Native performance on Windows and Linux (Ubuntu/Debian, Fedora, Arch).

---

## Keyboard Shortcuts

| Key | Action |
| --- | --- |
| `V` | Select Tool |
| `W` | Wire / Jumper Tool |
| `C` | Component Placer Tool |
| `R` | Rotate active component (0°, 90°, 180°, 270°) |
| `Delete` / `Backspace` | Delete selected elements |
| `Ctrl + N` | New Project |
| `Ctrl + O` | Open Project (`.perf`) |
| `Ctrl + S` | Save Project (`.perf`) |
| `Ctrl + E` | Export high-res PNG image |
| `Ctrl + B` | Export Bill of Materials (CSV) |
| `Ctrl + +` / `Ctrl + -` | Zoom in / Zoom out |
| `Ctrl + 0` | Reset Zoom (100%) |
| `Middle Click` / `Right Drag` | Pan canvas |

---

## Quick Installation

### Windows

Download and run `install.bat`:

```powershell
powershell -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/exitishere42/perfcraft/main/install.bat' -OutFile 'install.bat'; .\install.bat"
```

Or download the pre-packaged `perfcraft-1.0.0-all.jar` from the [Latest Release](https://github.com/exitishere42/perfcraft/releases/latest) and launch with:

```cmd
javaw -jar perfcraft-1.0.0-all.jar
```

### Linux (Debian, Ubuntu, Fedora, Arch)

Run the one-line installer:

```bash
curl -fsSL https://raw.githubusercontent.com/exitishere42/perfcraft/main/install.sh | bash
```

Or run directly with Java 21+:

```bash
java -jar perfcraft-1.0.0-all.jar
```

---

## Building from Source

Requirements: Java Development Kit (JDK) 21 LTS and Apache Maven 3.8+.

```bash
git clone https://github.com/exitishere42/perfcraft.git
cd perfcraft
mvn clean test
mvn clean package
```

Run directly via Maven:

```bash
mvn exec:java
```

Or run the built Fat-JAR:

```bash
java -jar target/perfcraft-1.0.0-all.jar
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
