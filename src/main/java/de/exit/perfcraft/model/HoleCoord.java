package de.exit.perfcraft.model;

/**
 * Represents a zero-based coordinate on the perfboard grid.
 */
public record HoleCoord(int x, int y) {

    public double toMmX(double pitch) {
        return x * pitch;
    }

    public double toMmY(double pitch) {
        return y * pitch;
    }

    public boolean isAdjacentTo(HoleCoord other) {
        int dx = Math.abs(x - other.x);
        int dy = Math.abs(y - other.y);
        return (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
    }
}
