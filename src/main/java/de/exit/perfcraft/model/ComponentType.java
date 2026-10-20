package de.exit.perfcraft.model;

/**
 * Supported electronic components and mechanical devices.
 */
public enum ComponentType {
    TACTILE_SWITCH("Taster", "SW"),
    RESISTOR_AXIAL("Widerstand", "R"),
    CAPACITOR_CERAMIC("Kondensator", "C"),
    CAPACITOR_ELECTROLYTIC("Elko", "C"),
    DIODE("Diode", "D"),
    LED("LED", "LED"),
    TRANSISTOR_TO92("Transistor", "Q"),
    IC_DIP("IC", "IC"),
    PIN_HEADER("Stiftleiste", "J"),
    TERMINAL_BLOCK("Schraubklemme", "TB");

    private final String displayName;
    private final String defaultPrefix;

    ComponentType(String displayName, String defaultPrefix) {
        this.displayName = displayName;
        this.defaultPrefix = defaultPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultPrefix() {
        return defaultPrefix;
    }
}
