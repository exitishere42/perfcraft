package de.exit.perfcraft.model;

/**
 * Kind of trace or wire connection.
 */
public enum WireKind {
    CABLE("Kabel", "#E53935", BoardLayer.TOP),
    SILVER_WIRE("Silberdraht", "#CFD8DC", BoardLayer.TOP);

    private final String displayName;
    private final String defaultColor;
    private final BoardLayer defaultLayer;

    WireKind(String displayName, String defaultColor, BoardLayer defaultLayer) {
        this.displayName = displayName;
        this.defaultColor = defaultColor;
        this.defaultLayer = defaultLayer;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultColor() {
        return defaultColor;
    }

    public BoardLayer getDefaultLayer() {
        return defaultLayer;
    }
}
