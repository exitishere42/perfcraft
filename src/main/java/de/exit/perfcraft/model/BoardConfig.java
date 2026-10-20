package de.exit.perfcraft.model;

/**
 * Dimensions and configuration of the perfboard grid.
 */
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
