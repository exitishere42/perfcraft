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
    private boolean flippedX = false;
    private int boardCols = 24;

    public CoordinateTransform() {
        this(DEFAULT_PIXELS_PER_MM);
    }

    public CoordinateTransform(double pixelsPerMm) {
        this.pixelsPerMm = pixelsPerMm;
    }

    public void setFlippedX(boolean flippedX, int boardCols) {
        this.flippedX = flippedX;
        this.boardCols = Math.max(1, boardCols);
    }

    public boolean isFlippedX() {
        return flippedX;
    }

    public int getBoardCols() {
        return boardCols;
    }

    public double getHoleSpacingPx() {
        return PITCH_MM * pixelsPerMm * scale;
    }

    public double gridToScreenX(int gridX) {
        int effX = flippedX ? (boardCols - 1 - gridX) : gridX;
        return (effX * PITCH_MM * pixelsPerMm * scale) + translateX;
    }

    public double gridToScreenY(int gridY) {
        return (gridY * PITCH_MM * pixelsPerMm * scale) + translateY;
    }

    public HoleCoord screenToGrid(double screenX, double screenY) {
        double worldMmX = (screenX - translateX) / (pixelsPerMm * scale);
        double worldMmY = (screenY - translateY) / (pixelsPerMm * scale);
        int gx = (int) Math.round(worldMmX / PITCH_MM);
        int gy = (int) Math.round(worldMmY / PITCH_MM);
        if (flippedX) {
            gx = (boardCols - 1) - gx;
        }
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
        this.boardCols = Math.max(1, boardCols);
        double pitchPx = PITCH_MM * pixelsPerMm;
        double boardWidthPx = (boardCols - 1) * pitchPx;
        double boardHeightPx = (boardRows - 1) * pitchPx;
        double totalW = boardWidthPx + pitchPx * 3.4;
        double totalH = boardHeightPx + pitchPx * 5.0;
        double scaleX = (canvasWidth - 40.0) / Math.max(1.0, totalW);
        double scaleY = (canvasHeight - 40.0) / Math.max(1.0, totalH);
        this.scale = Math.clamp(Math.min(scaleX, scaleY), 0.2, 8.0);
        this.translateX = (canvasWidth - (boardWidthPx * scale)) / 2.0;
        this.translateY = (canvasHeight - (boardHeightPx * scale)) / 2.0;
    }

    public double getScale() { return scale; }
    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }
}
