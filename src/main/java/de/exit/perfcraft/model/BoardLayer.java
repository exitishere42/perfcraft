package de.exit.perfcraft.model;

/**
 * Active layer or view plane for the circuit board.
 */
public enum BoardLayer {
    TOP("Bauteilseite"),
    BOTTOM("Lötseite"),
    BOTH("Beide Ebenen");

    private final String displayName;

    BoardLayer(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
