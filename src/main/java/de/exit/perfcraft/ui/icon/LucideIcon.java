package de.exit.perfcraft.ui.icon;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Standardkonforme Vektor-Icons aus dem Lucide Icon Set (lucide.dev).
 * Zero-Emoji Standard für saubere CAD-Darstellung.
 */
public class LucideIcon extends Canvas {
    private String name;
    private Color color;
    private final double size;

    public LucideIcon(String name, double size, Color color) {
        super(size, size);
        this.name = name;
        this.size = size;
        this.color = color;
        draw();
    }

    public void setIcon(String name, Color color) {
        this.name = name;
        this.color = color;
        draw();
    }

    public void setColor(Color color) {
        this.color = color;
        draw();
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        double scale = size / 24.0;
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineWidth(2.0 * scale);

        switch (name.toLowerCase()) {
            case "pointer", "mouse-pointer", "select" -> {
                // Lucide mouse-pointer-2: path m3 3 7.07 16.97 2.51-7.39 7.39-2.51L3 3z, path m13 13 6 6
                double[] px = {3 * scale, 10.07 * scale, 12.58 * scale, 19.97 * scale};
                double[] py = {3 * scale, 19.97 * scale, 12.58 * scale, 10.07 * scale};
                gc.strokePolygon(px, py, 4);
                gc.strokeLine(13 * scale, 13 * scale, 19 * scale, 19 * scale);
            }
            case "wire", "cable" -> {
                // Lucide cable: path M4 9a2 2 0 0 1-2-2V5h4a2 2 0 0 1 2 2 10 10 0 0 0 20 0 2 2 0 0 1 2-2h4v2a2 2 0 0 1-2 2
                gc.strokeLine(2 * scale, 12 * scale, 6 * scale, 12 * scale);
                gc.strokeLine(6 * scale, 12 * scale, 9 * scale, 6 * scale);
                gc.strokeLine(9 * scale, 6 * scale, 15 * scale, 18 * scale);
                gc.strokeLine(15 * scale, 18 * scale, 18 * scale, 12 * scale);
                gc.strokeLine(18 * scale, 12 * scale, 22 * scale, 12 * scale);
                gc.fillOval(1 * scale, 10.5 * scale, 3 * scale, 3 * scale);
                gc.fillOval(20 * scale, 10.5 * scale, 3 * scale, 3 * scale);
            }
            case "component", "cpu" -> {
                // Lucide cpu: rect 4,4,16,16 rx=2; rect 9,9,6,6; pins
                gc.strokeRoundRect(4 * scale, 4 * scale, 16 * scale, 16 * scale, 3 * scale, 3 * scale);
                gc.strokeRoundRect(9 * scale, 9 * scale, 6 * scale, 6 * scale, 1 * scale, 1 * scale);
                gc.strokeLine(9 * scale, 2 * scale, 9 * scale, 4 * scale);
                gc.strokeLine(15 * scale, 2 * scale, 15 * scale, 4 * scale);
                gc.strokeLine(9 * scale, 20 * scale, 9 * scale, 22 * scale);
                gc.strokeLine(15 * scale, 20 * scale, 15 * scale, 22 * scale);
                gc.strokeLine(2 * scale, 9 * scale, 4 * scale, 9 * scale);
                gc.strokeLine(2 * scale, 15 * scale, 4 * scale, 15 * scale);
                gc.strokeLine(20 * scale, 9 * scale, 22 * scale, 9 * scale);
                gc.strokeLine(20 * scale, 15 * scale, 22 * scale, 15 * scale);
            }
            case "rotate", "rotate-cw" -> {
                // Lucide rotate-cw: path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8", path d="M21 3v5h-5"
                gc.strokeArc(3 * scale, 3 * scale, 18 * scale, 18 * scale, -20, 270, ArcType.OPEN);
                gc.strokeLine(21 * scale, 3 * scale, 21 * scale, 8 * scale);
                gc.strokeLine(16 * scale, 8 * scale, 21 * scale, 8 * scale);
            }
            case "delete", "trash", "trash-2" -> {
                // Lucide trash-2
                gc.strokeLine(3 * scale, 6 * scale, 21 * scale, 6 * scale);
                gc.strokeRoundRect(5 * scale, 6 * scale, 14 * scale, 15 * scale, 2 * scale, 2 * scale);
                gc.strokeRoundRect(8 * scale, 3 * scale, 8 * scale, 3 * scale, 1 * scale, 1 * scale);
                gc.strokeLine(10 * scale, 10 * scale, 10 * scale, 16 * scale);
                gc.strokeLine(14 * scale, 10 * scale, 14 * scale, 16 * scale);
            }
            case "switch", "toggle-left" -> {
                // Lucide toggle-left: rect 2 6 20 12 rx=6, circle cx=8 cy=12 r=2
                gc.strokeRoundRect(2 * scale, 6 * scale, 20 * scale, 12 * scale, 6 * scale, 6 * scale);
                gc.fillOval(6 * scale, 10 * scale, 4 * scale, 4 * scale);
            }
            case "resistor" -> {
                // IEEE Resistor zigzag
                double[] rx = {2 * scale, 6 * scale, 8.5 * scale, 12 * scale, 15.5 * scale, 18 * scale, 22 * scale};
                double[] ry = {12 * scale, 12 * scale, 6 * scale, 18 * scale, 6 * scale, 12 * scale, 12 * scale};
                for (int i = 0; i < rx.length - 1; i++) {
                    gc.strokeLine(rx[i], ry[i], rx[i + 1], ry[i + 1]);
                }
            }
            case "capacitor" -> {
                // Capacitor parallel plates
                gc.strokeLine(2 * scale, 12 * scale, 9 * scale, 12 * scale);
                gc.strokeLine(9 * scale, 5 * scale, 9 * scale, 19 * scale);
                gc.strokeLine(15 * scale, 5 * scale, 15 * scale, 19 * scale);
                gc.strokeLine(15 * scale, 12 * scale, 22 * scale, 12 * scale);
            }
            case "led" -> {
                // LED: Diode + emission arrows
                double[] dx = {7 * scale, 7 * scale, 14 * scale};
                double[] dy = {6 * scale, 18 * scale, 12 * scale};
                gc.strokePolygon(dx, dy, 3);
                gc.strokeLine(14 * scale, 6 * scale, 14 * scale, 18 * scale);
                gc.strokeLine(2 * scale, 12 * scale, 7 * scale, 12 * scale);
                gc.strokeLine(14 * scale, 12 * scale, 22 * scale, 12 * scale);
                // Emission rays
                gc.strokeLine(15 * scale, 5 * scale, 19 * scale, 1 * scale);
                gc.strokeLine(18 * scale, 1 * scale, 19 * scale, 1 * scale);
                gc.strokeLine(19 * scale, 1 * scale, 19 * scale, 2 * scale);
                gc.strokeLine(18 * scale, 8 * scale, 22 * scale, 4 * scale);
                gc.strokeLine(21 * scale, 4 * scale, 22 * scale, 4 * scale);
                gc.strokeLine(22 * scale, 4 * scale, 22 * scale, 5 * scale);
            }
            case "diode" -> {
                // Diode
                double[] dx = {8 * scale, 8 * scale, 15 * scale};
                double[] dy = {6 * scale, 18 * scale, 12 * scale};
                gc.strokePolygon(dx, dy, 3);
                gc.strokeLine(15 * scale, 6 * scale, 15 * scale, 18 * scale);
                gc.strokeLine(2 * scale, 12 * scale, 8 * scale, 12 * scale);
                gc.strokeLine(15 * scale, 12 * scale, 22 * scale, 12 * scale);
            }
            case "ic", "ic_dip" -> {
                // DIP IC with dual rows of pins
                gc.strokeRoundRect(6 * scale, 4 * scale, 12 * scale, 16 * scale, 2 * scale, 2 * scale);
                gc.strokeArc(10 * scale, 3 * scale, 4 * scale, 3 * scale, 0, -180, ArcType.OPEN);
                gc.strokeLine(2 * scale, 7 * scale, 6 * scale, 7 * scale);
                gc.strokeLine(2 * scale, 12 * scale, 6 * scale, 12 * scale);
                gc.strokeLine(2 * scale, 17 * scale, 6 * scale, 17 * scale);
                gc.strokeLine(18 * scale, 7 * scale, 22 * scale, 7 * scale);
                gc.strokeLine(18 * scale, 12 * scale, 22 * scale, 12 * scale);
                gc.strokeLine(18 * scale, 17 * scale, 22 * scale, 17 * scale);
            }
            case "esp32", "esp32_s3_zero" -> {
                // Microchip / SoC board
                gc.strokeRoundRect(5 * scale, 4 * scale, 14 * scale, 16 * scale, 2 * scale, 2 * scale);
                gc.strokeRoundRect(8 * scale, 7 * scale, 8 * scale, 6 * scale, 1 * scale, 1 * scale);
                gc.strokeLine(2 * scale, 8 * scale, 5 * scale, 8 * scale);
                gc.strokeLine(2 * scale, 12 * scale, 5 * scale, 12 * scale);
                gc.strokeLine(2 * scale, 16 * scale, 5 * scale, 16 * scale);
                gc.strokeLine(19 * scale, 8 * scale, 22 * scale, 8 * scale);
                gc.strokeLine(19 * scale, 12 * scale, 22 * scale, 12 * scale);
                gc.strokeLine(19 * scale, 16 * scale, 22 * scale, 16 * scale);
            }
            case "tp4056", "battery", "battery-charging" -> {
                // Lucide battery-charging
                gc.strokeRoundRect(3 * scale, 6 * scale, 15 * scale, 12 * scale, 2 * scale, 2 * scale);
                gc.strokeLine(20 * scale, 10 * scale, 20 * scale, 14 * scale);
                // Lightning bolt
                double[] bx = {11 * scale, 9 * scale, 12 * scale, 10 * scale};
                double[] by = {8 * scale, 12 * scale, 12 * scale, 16 * scale};
                gc.strokePolyline(bx, by, 4);
            }
            case "pin_header", "pinheader" -> {
                // Row of pin header sockets
                gc.strokeRoundRect(3 * scale, 7 * scale, 18 * scale, 10 * scale, 2 * scale, 2 * scale);
                gc.fillRect(5 * scale, 9.5 * scale, 2.5 * scale, 5 * scale);
                gc.fillRect(9.5 * scale, 9.5 * scale, 2.5 * scale, 5 * scale);
                gc.fillRect(14 * scale, 9.5 * scale, 2.5 * scale, 5 * scale);
                gc.strokeLine(7.5 * scale, 7 * scale, 7.5 * scale, 17 * scale);
                gc.strokeLine(12 * scale, 7 * scale, 12 * scale, 17 * scale);
                gc.strokeLine(16.5 * scale, 7 * scale, 16.5 * scale, 17 * scale);
            }
            case "settings", "gear" -> {
                // Lucide settings / cog
                gc.strokeOval(9 * scale, 9 * scale, 6 * scale, 6 * scale);
                // 8 cogs
                gc.strokeLine(12 * scale, 2 * scale, 12 * scale, 5 * scale);
                gc.strokeLine(12 * scale, 19 * scale, 12 * scale, 22 * scale);
                gc.strokeLine(2 * scale, 12 * scale, 5 * scale, 12 * scale);
                gc.strokeLine(19 * scale, 12 * scale, 22 * scale, 12 * scale);
                gc.strokeLine(5 * scale, 5 * scale, 7 * scale, 7 * scale);
                gc.strokeLine(17 * scale, 17 * scale, 19 * scale, 19 * scale);
                gc.strokeLine(5 * scale, 19 * scale, 7 * scale, 17 * scale);
                gc.strokeLine(17 * scale, 7 * scale, 19 * scale, 5 * scale);
            }
            default -> {
                gc.strokeOval(4 * scale, 4 * scale, 16 * scale, 16 * scale);
            }
        }
    }
}
