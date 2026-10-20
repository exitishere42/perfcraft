package de.exit.perfcraft.ui.canvas;

import de.exit.perfcraft.model.*;
import de.exit.perfcraft.service.ResistorColorCode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PerfBoardCanvas extends Canvas {

    private final CoordinateTransform transform = new CoordinateTransform();
    private PerfProject project = new PerfProject();
    private BoardLayer currentLayer = BoardLayer.TOP;
    private PlacedComponent selectedComponent = null;

    private double lastDragX;
    private double lastDragY;
    private BiConsumer<Integer, Integer> onHoverCoordinate;
    private Consumer<PlacedComponent> onComponentSelected;

    public PerfBoardCanvas(double width, double height) {
        super(width, height);
        initInteraction();
        initDemoData();
        render();
    }

    private void initDemoData() {
        project.getComponents().add(new PlacedComponent(
                "c1", ComponentType.TACTILE_SWITCH, "SW1", "Taster 6x6",
                new HoleCoord(5, 4), new HoleCoord(7, 5), 0, BoardLayer.TOP
        ));

        project.getComponents().add(new PlacedComponent(
                "c2", ComponentType.RESISTOR_AXIAL, "R1", "10k",
                new HoleCoord(10, 4), new HoleCoord(14, 4), 0, BoardLayer.TOP
        ));

        project.getComponents().add(new PlacedComponent(
                "c3", ComponentType.LED, "LED1", "Rot",
                new HoleCoord(16, 4), new HoleCoord(17, 4), 0, BoardLayer.TOP
        ));

        project.getWires().add(new WireTrace(
                "w1", WireKind.CABLE, BoardLayer.TOP, "#E53935",
                java.util.List.of(new HoleCoord(5, 4), new HoleCoord(10, 4))
        ));

        project.getWires().add(new WireTrace(
                "w2", WireKind.SILVER_WIRE, BoardLayer.BOTTOM, "#CFD8DC",
                java.util.List.of(new HoleCoord(14, 4), new HoleCoord(16, 4))
        ));

        selectedComponent = project.getComponents().getFirst();
    }

    private void initInteraction() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE || e.isSecondaryButtonDown() || e.isPrimaryButtonDown()) {
                lastDragX = e.getX();
                lastDragY = e.getY();
            }

            if (e.getButton() == MouseButton.PRIMARY) {
                HoleCoord grid = transform.screenToGrid(e.getX(), e.getY());
                // Hit testing
                PlacedComponent hit = null;
                for (PlacedComponent c : project.getComponents()) {
                    int minX = Math.min(c.getStart().x(), c.getEnd() != null ? c.getEnd().x() : c.getStart().x());
                    int maxX = Math.max(c.getStart().x(), c.getEnd() != null ? c.getEnd().x() : c.getStart().x());
                    int minY = Math.min(c.getStart().y(), c.getEnd() != null ? c.getEnd().y() : c.getStart().y());
                    int maxY = Math.max(c.getStart().y(), c.getEnd() != null ? c.getEnd().y() : c.getStart().y());

                    if (grid.x() >= minX - 1 && grid.x() <= maxX + 1 &&
                        grid.y() >= minY - 1 && grid.y() <= maxY + 1) {
                        hit = c;
                        break;
                    }
                }
                selectedComponent = hit;
                if (onComponentSelected != null && hit != null) {
                    onComponentSelected.accept(hit);
                }
                render();
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.MIDDLE || e.isSecondaryButtonDown() || (e.isPrimaryButtonDown() && e.isAltDown())) {
                double dx = e.getX() - lastDragX;
                double dy = e.getY() - lastDragY;
                transform.pan(dx, dy);
                lastDragX = e.getX();
                lastDragY = e.getY();
                render();
            }
        });

        setOnMouseMoved(e -> {
            HoleCoord grid = transform.screenToGrid(e.getX(), e.getY());
            if (onHoverCoordinate != null) {
                onHoverCoordinate.accept(grid.x(), grid.y());
            }
        });

        setOnScroll(e -> {
            double zoomFactor = e.getDeltaY() > 0 ? 1.15 : 0.87;
            transform.applyZoom(zoomFactor, e.getX(), e.getY());
            render();
        });
    }

    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.web("#0E0E0E"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        int cols = project.getBoard().getColumns();
        int rows = project.getBoard().getRows();
        double hs = transform.getHoleSpacingPx();

        double boardX = transform.gridToScreenX(0) - hs * 0.8;
        double boardY = transform.gridToScreenY(0) - hs * 0.8;
        double boardW = (cols - 1) * hs + hs * 1.6;
        double boardH = (rows - 1) * hs + hs * 1.6;

        // 1. Platinen-Substrat (Clean sattes PCB-Grün)
        gc.setFill(Color.web("#1B4D2E"));
        gc.fillRoundRect(boardX, boardY, boardW, boardH, 12, 12);
        gc.setStroke(Color.web("#143922"));
        gc.setLineWidth(3);
        gc.strokeRoundRect(boardX, boardY, boardW, boardH, 12, 12);

        // 2. Bottom-Layer Leitungen (Silberdraht)
        if (currentLayer == BoardLayer.BOTTOM || currentLayer == BoardLayer.BOTH) {
            gc.setStroke(Color.web("#CFD8DC"));
            gc.setLineWidth(4.0 * transform.getScale());
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            for (WireTrace w : project.getWires()) {
                if (w.getKind() == WireKind.SILVER_WIRE && w.getPoints().size() >= 2) {
                    var p1 = w.getPoints().get(0);
                    var p2 = w.getPoints().get(1);
                    gc.strokeLine(
                            transform.gridToScreenX(p1.x()), transform.gridToScreenY(p1.y()),
                            transform.gridToScreenX(p2.x()), transform.gridToScreenY(p2.y())
                    );
                }
            }
        }

        // 3. Kupfer-Pads & Bohrlöcher
        double padR = Math.max(3.0, hs * 0.38);
        double drillR = Math.max(1.5, hs * 0.18);

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                double px = transform.gridToScreenX(c);
                double py = transform.gridToScreenY(r);

                gc.setFill(Color.web("#D4AF37")); // Gold / Kupfer
                gc.fillOval(px - padR, py - padR, padR * 2, padR * 2);

                gc.setFill(Color.web("#121212")); // Bohrung
                gc.fillOval(px - drillR, py - drillR, drillR * 2, drillR * 2);
            }
        }

        // 4. Top-Layer Leitungen (Kabel)
        if (currentLayer == BoardLayer.TOP || currentLayer == BoardLayer.BOTH) {
            for (WireTrace w : project.getWires()) {
                if (w.getKind() == WireKind.CABLE && w.getPoints().size() >= 2) {
                    var p1 = w.getPoints().get(0);
                    var p2 = w.getPoints().get(1);
                    gc.setStroke(Color.web(w.getColor()));
                    gc.setLineWidth(3.5 * transform.getScale());
                    gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                    gc.strokeLine(
                            transform.gridToScreenX(p1.x()), transform.gridToScreenY(p1.y()),
                            transform.gridToScreenX(p2.x()), transform.gridToScreenY(p2.y())
                    );
                }
            }
        }

        // 5. Komponenten
        if (currentLayer == BoardLayer.TOP || currentLayer == BoardLayer.BOTH) {
            double alpha = (currentLayer == BoardLayer.BOTH) ? 0.65 : 1.0;
            gc.setGlobalAlpha(alpha);

            for (PlacedComponent comp : project.getComponents()) {
                boolean isSel = (comp == selectedComponent);
                if (comp.getType() == ComponentType.TACTILE_SWITCH) {
                    renderTactileSwitch(gc, comp, isSel);
                } else if (comp.getType() == ComponentType.RESISTOR_AXIAL) {
                    renderResistor(gc, comp, isSel);
                } else if (comp.getType() == ComponentType.LED) {
                    renderLED(gc, comp, isSel);
                }
            }
            gc.setGlobalAlpha(1.0);
        }
    }

    private void renderTactileSwitch(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        int gx = comp.getStart().x();
        int gy = comp.getStart().y();
        double hs = transform.getHoleSpacingPx();

        double x1 = transform.gridToScreenX(gx);
        double y1 = transform.gridToScreenY(gy);
        double cx = transform.gridToScreenX(gx + 1);
        double cy = (transform.gridToScreenY(gy) + transform.gridToScreenY(gy + 1)) / 2.0;

        double w = hs * 2.3;
        double h = hs * 1.5;

        // 4 Pins an den Ecken
        gc.setFill(Color.web("#B0BEC5"));
        double pinS = hs * 0.3;
        gc.fillRect(x1 - pinS/2, y1 - pinS/2, pinS, pinS);
        gc.fillRect(transform.gridToScreenX(gx + 2) - pinS/2, y1 - pinS/2, pinS, pinS);
        gc.fillRect(x1 - pinS/2, transform.gridToScreenY(gy + 1) - pinS/2, pinS, pinS);
        gc.fillRect(transform.gridToScreenX(gx + 2) - pinS/2, transform.gridToScreenY(gy + 1) - pinS/2, pinS, pinS);

        // Gehäusekörper dunkelgrau
        gc.setFill(Color.web("#37474F"));
        gc.fillRoundRect(cx - w/2, cy - h/2, w, h, 6, 6);

        // Metall-Deckplatte
        gc.setFill(Color.web("#607D8B"));
        gc.fillRoundRect(cx - w*0.35, cy - h*0.35, w*0.7, h*0.7, 4, 4);

        // Runder Taster-Knopf
        double btnR = Math.min(w, h) * 0.28;
        gc.setFill(Color.web("#212121"));
        gc.fillOval(cx - btnR, cy - btnR, btnR * 2, btnR * 2);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(cx - w/2 - 2, cy - h/2 - 2, w + 4, h + 4, 8, 8);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator(), cx - 12, cy - h/2 - 4);
    }

    private void renderResistor(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        double x1 = transform.gridToScreenX(comp.getStart().x());
        double y1 = transform.gridToScreenY(comp.getStart().y());
        double x2 = transform.gridToScreenX(comp.getEnd().x());
        double y2 = transform.gridToScreenY(comp.getEnd().y());
        double hs = transform.getHoleSpacingPx();

        // Anschlussdrähte
        gc.setStroke(Color.web("#B0BEC5"));
        gc.setLineWidth(2.5 * transform.getScale());
        gc.strokeLine(x1, y1, x2, y2);

        // Zylinderkörper
        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;
        double bw = hs * 2.4;
        double bh = hs * 0.55;

        gc.setFill(Color.web("#D7CCC8")); // Beige
        gc.fillRoundRect(midX - bw/2, midY - bh/2, bw, bh, 5, 5);
        gc.setStroke(Color.web("#A1887F"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(midX - bw/2, midY - bh/2, bw, bh, 5, 5);

        // Echte Farbringe
        var bands = ResistorColorCode.parseAndCalculate(comp.getValue());
        if (bands != null) {
            double bandW = Math.max(2.0, bw * 0.08);
            double spacing = bw * 0.18;
            double startOffset = midX - bw/2 + bw * 0.18;

            drawBand(gc, startOffset + 0 * spacing, midY - bh/2, bandW, bh, bands.band1().getHexColor());
            drawBand(gc, startOffset + 1 * spacing, midY - bh/2, bandW, bh, bands.band2().getHexColor());
            drawBand(gc, startOffset + 2 * spacing, midY - bh/2, bandW, bh, bands.multiplier().getHexColor());
            drawBand(gc, startOffset + 3.2 * spacing, midY - bh/2, bandW, bh, bands.tolerance().getHexColor());
        }

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(midX - bw/2 - 2, midY - bh/2 - 2, bw + 4, bh + 4, 7, 7);
        }

        gc.setFill(Color.web("#E0E0E0"));
        gc.fillText(comp.getDesignator() + " (" + comp.getValue() + "Ω)", midX - 20, midY - bh/2 - 4);
    }

    private void renderLED(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        double px = transform.gridToScreenX(comp.getStart().x());
        double py = transform.gridToScreenY(comp.getStart().y());
        double r = transform.getHoleSpacingPx() * 0.42;

        gc.setFill(Color.web("#E53935"));
        gc.fillOval(px - r, py - r, r * 2, r * 2);

        // Kathoden-Abflachung
        gc.setFill(Color.web("#B71C1C"));
        gc.fillRect(px + r * 0.7, py - r, r * 0.3, r * 2);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRect(px - r - 3, py - r - 3, (r + 3) * 2, (r + 3) * 2);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator(), px - 12, py - r - 4);
    }

    private void drawBand(GraphicsContext gc, double x, double y, double w, double h, String hexColor) {
        gc.setFill(Color.web(hexColor));
        gc.fillRect(x, y, w, h);
    }

    public void setLayer(BoardLayer layer) {
        this.currentLayer = layer;
        render();
    }

    public CoordinateTransform getTransform() { return transform; }
    public PerfProject getProject() { return project; }
    public PlacedComponent getSelectedComponent() { return selectedComponent; }

    public void setOnHoverCoordinate(BiConsumer<Integer, Integer> callback) {
        this.onHoverCoordinate = callback;
    }

    public void setOnComponentSelected(Consumer<PlacedComponent> callback) {
        this.onComponentSelected = callback;
    }
}
