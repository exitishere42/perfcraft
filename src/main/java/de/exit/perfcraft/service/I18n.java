package de.exit.perfcraft.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lightweight localization service with English as default and German support.
 */
public class I18n {

    public enum Language {
        EN("English"),
        DE("Deutsch");

        private final String label;
        Language(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private static Language currentLanguage = Language.EN;
    private static final Map<String, String> EN_MAP = new HashMap<>();
    private static final Map<String, String> DE_MAP = new HashMap<>();
    private static final java.util.List<Consumer<Language>> LISTENERS = new java.util.ArrayList<>();

    static {
        // App
        EN_MAP.put("app.title", "PerfCraft - Perfboard Designer");
        DE_MAP.put("app.title", "PerfCraft - Lochraster Designer");

        // Menu & Header
        EN_MAP.put("menu.file", "File");
        DE_MAP.put("menu.file", "Datei");
        EN_MAP.put("menu.new", "New");
        DE_MAP.put("menu.new", "Neu");
        EN_MAP.put("menu.open", "Open");
        DE_MAP.put("menu.open", "Öffnen");
        EN_MAP.put("menu.save", "Save");
        DE_MAP.put("menu.save", "Speichern");
        EN_MAP.put("menu.export", "Export");
        DE_MAP.put("menu.export", "Exportieren");
        EN_MAP.put("menu.settings", "Settings");
        DE_MAP.put("menu.settings", "Einstellungen");
        EN_MAP.put("menu.exit", "Exit");
        DE_MAP.put("menu.exit", "Beenden");

        // Layers
        EN_MAP.put("layer.top", "Top Layer");
        DE_MAP.put("layer.top", "Bauteilseite");
        EN_MAP.put("layer.bottom", "Bottom Layer");
        DE_MAP.put("layer.bottom", "Lötseite");
        EN_MAP.put("layer.both", "Both (X-Ray)");
        DE_MAP.put("layer.both", "Beide (Röntgen)");

        // Tools
        EN_MAP.put("tool.select", "Select");
        DE_MAP.put("tool.select", "Wählen");
        EN_MAP.put("tool.wire", "Wire");
        DE_MAP.put("tool.wire", "Leitung");
        EN_MAP.put("tool.component", "Component");
        DE_MAP.put("tool.component", "Bauteil");
        EN_MAP.put("tool.rotate", "Rotate");
        DE_MAP.put("tool.rotate", "Drehen");
        EN_MAP.put("tool.delete", "Delete");
        DE_MAP.put("tool.delete", "Löschen");

        // Wire Kinds
        EN_MAP.put("wire.cable", "Insulated Cable");
        DE_MAP.put("wire.cable", "Isoliertes Kabel");
        EN_MAP.put("wire.silver", "Silver Wire");
        DE_MAP.put("wire.silver", "Silberdraht");
        EN_MAP.put("wire.color", "Color:");
        DE_MAP.put("wire.color", "Farbe:");

        // Components
        EN_MAP.put("comp.resistor", "Resistor");
        DE_MAP.put("comp.resistor", "Widerstand");
        EN_MAP.put("comp.switch", "Tactile Switch");
        DE_MAP.put("comp.switch", "Taster");
        EN_MAP.put("comp.capacitor", "Capacitor");
        DE_MAP.put("comp.capacitor", "Kondensator");
        EN_MAP.put("comp.led", "LED");
        DE_MAP.put("comp.led", "LED");
        EN_MAP.put("comp.diode", "Diode");
        DE_MAP.put("comp.diode", "Diode");
        EN_MAP.put("comp.ic", "IC (DIP)");
        DE_MAP.put("comp.ic", "IC (DIP)");

        // Sidebar & Properties
        EN_MAP.put("sidebar.components", "1. COMPONENT LIBRARY");
        DE_MAP.put("sidebar.components", "1. BAUTEIL-AUSWAHL");
        EN_MAP.put("sidebar.properties", "2. PROPERTIES");
        DE_MAP.put("sidebar.properties", "2. EIGENSCHAFTEN");
        EN_MAP.put("prop.designator", "Designator");
        DE_MAP.put("prop.designator", "Bezeichner");
        EN_MAP.put("prop.value", "Value / Type");
        DE_MAP.put("prop.value", "Wert / Typ");
        EN_MAP.put("prop.bands", "Color Bands (IEC 60062):");
        DE_MAP.put("prop.bands", "Farbringe (IEC 60062):");
        EN_MAP.put("prop.orientation", "Orientation");
        DE_MAP.put("prop.orientation", "Ausrichtung");

        // Statusbar
        EN_MAP.put("stat.hole", "Hole:");
        DE_MAP.put("stat.hole", "Loch:");
        EN_MAP.put("stat.pos", "Pos:");
        DE_MAP.put("stat.pos", "Pos:");
        EN_MAP.put("stat.layer", "Layer:");
        DE_MAP.put("stat.layer", "Ebene:");
        EN_MAP.put("stat.grid", "Grid: 2.54 mm (0.1\")");
        DE_MAP.put("stat.grid", "Raster: 2.54 mm (0.1\")");

        // Dialogs
        EN_MAP.put("dialog.new.title", "New Perfboard");
        DE_MAP.put("dialog.new.title", "Neues Lochrasterboard");
        EN_MAP.put("dialog.new.cols", "Columns (X):");
        DE_MAP.put("dialog.new.cols", "Spalten (X):");
        EN_MAP.put("dialog.new.rows", "Rows (Y):");
        DE_MAP.put("dialog.new.rows", "Zeilen (Y):");
        EN_MAP.put("dialog.new.name", "Project Name:");
        DE_MAP.put("dialog.new.name", "Projektname:");
        EN_MAP.put("dialog.create", "Create");
        DE_MAP.put("dialog.create", "Erstellen");
        EN_MAP.put("dialog.cancel", "Cancel");
        DE_MAP.put("dialog.cancel", "Abbrechen");

        // Welcome Screen / Hauptmenü
        EN_MAP.put("welcome.title", "Welcome to PerfCraft");
        DE_MAP.put("welcome.title", "Willkommen bei PerfCraft");
        EN_MAP.put("welcome.subtitle", "Design circuits on perfboards with authentic leads, components, and continuity.");
        DE_MAP.put("welcome.subtitle", "Entwirf Schaltungen auf Lochrasterplatinen mit echten Drähten, Bauteilen und Durchgangsprüfung.");
        EN_MAP.put("welcome.new", "New Project");
        DE_MAP.put("welcome.new", "Neues Projekt");
        EN_MAP.put("welcome.open", "Open Existing (.perf)");
        DE_MAP.put("welcome.open", "Projekt öffnen (.perf)");
        EN_MAP.put("welcome.recent", "Quick Start Presets:");
        DE_MAP.put("welcome.recent", "Schnellstart-Vorlagen:");
        EN_MAP.put("welcome.preset.small", "Small (16 x 12 holes)");
        DE_MAP.put("welcome.preset.small", "Klein (16 x 12 Löcher)");
        EN_MAP.put("welcome.preset.standard", "Standard (24 x 18 holes)");
        DE_MAP.put("welcome.preset.standard", "Standard (24 x 18 Löcher)");
        EN_MAP.put("welcome.preset.large", "Large (36 x 24 holes)");
        DE_MAP.put("welcome.preset.large", "Groß (36 x 24 Löcher)");

        // Settings
        EN_MAP.put("dialog.settings.title", "Settings");
        DE_MAP.put("dialog.settings.title", "Einstellungen");
        EN_MAP.put("dialog.settings.lang", "Language:");
        DE_MAP.put("dialog.settings.lang", "Sprache:");
        EN_MAP.put("dialog.save", "Save");
        DE_MAP.put("dialog.save", "Speichern");
    }

    public static String get(String key) {
        if (currentLanguage == Language.DE) {
            return DE_MAP.getOrDefault(key, EN_MAP.getOrDefault(key, key));
        }
        return EN_MAP.getOrDefault(key, key);
    }

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(Language lang) {
        currentLanguage = lang;
        for (Consumer<Language> l : LISTENERS) {
            l.accept(lang);
        }
    }

    public static void addListener(Consumer<Language> listener) {
        LISTENERS.add(listener);
    }
}
