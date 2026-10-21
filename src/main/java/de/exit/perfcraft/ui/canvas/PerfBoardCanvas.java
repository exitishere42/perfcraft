package de.exit.perfcraft.ui.canvas;

import de.exit.perfcraft.model.*;
import de.exit.perfcraft.service.ResistorColorCode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PerfBoardCanvas extends Canvas {

    public enum ActiveTool {
        SELECT,
        WIRE,
        COMPONENT
    }

    private final CoordinateTransform transform = new CoordinateTransform();
    private PerfProject project = new PerfProject();
    private BoardLayer currentLayer = BoardLayer.TOP;
    private ActiveTool activeTool = ActiveTool.SELECT;

    private WireKind activeWireKind = WireKind.CABLE;
    private String activeWireColor = "#E53935";
    private ComponentType activeComponentType = ComponentType.TACTILE_SWITCH;

    private PlacedComponent selectedComponent = null;

    // Interaktions-Status
    private HoleCoord wireStartHole = null;
    private HoleCoord currentHoverGrid = null;
    private boolean isDraggingObject = false;
    private double lastDragX;
    private double lastDragY;

    // KiCad & Tinkercad Inspirations
    private java.util.Set<HoleCoord> highlightedNet = java.util.Collections.emptySet();
    private String hoveredTerminalText = null;

    private BiConsumer<Integer, Integer> onHoverCoordinate;
    private Consumer<PlacedComponent> onComponentSelected;
    private Runnable onProjectModified;

    private boolean initialFitDone = false;

    public PerfBoardCanvas(double width, double height) {
        super(width, height);
        initInteraction();
        initDemoData();
        render();
    }

    private void initDemoData() {
        project.getComponents().add(new PlacedComponent(
                "c1", ComponentType.TACTILE_SWITCH, "SW1", "6x6 mm",
                new HoleCoord(5, 4), new HoleCoord(7, 5), 0, BoardLayer.TOP
        ));

        project.getComponents().add(new PlacedComponent(
                "c2", ComponentType.RESISTOR_AXIAL, "R1", "10k",
                new HoleCoord(10, 4), new HoleCoord(14, 4), 0, BoardLayer.TOP
        ));

        project.getComponents().add(new PlacedComponent(
                "c3", ComponentType.LED, "LED1", "Red",
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
            lastDragX = e.getX();
            lastDragY = e.getY();
            HoleCoord grid = transform.screenToGrid(e.getX(), e.getY());

            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.isControlDown() || e.isShortcutDown()) {
                    // KiCad Style: Ctrl+Click toggles Net Highlighting
                    if (!highlightedNet.isEmpty()) {
                        highlightedNet = java.util.Collections.emptySet();
                    } else {
                        highlightedNet = de.exit.perfcraft.service.ContinuityService.findConnectedNet(project, grid);
                    }
                    render();
                    return;
                }

                if (activeTool == ActiveTool.SELECT) {
                    // Selektion / Hit-Test
                    PlacedComponent hit = findComponentAt(grid);
                    selectedComponent = hit;
                    isDraggingObject = (hit != null);
                    if (onComponentSelected != null && hit != null) {
                        onComponentSelected.accept(hit);
                    }
                    render();
                } else if (activeTool == ActiveTool.WIRE) {
                    // Draht ziehen: Start oder Ziel
                    if (wireStartHole == null) {
                        wireStartHole = grid;
                    } else {
                        if (!wireStartHole.equals(grid)) {
                            WireTrace trace = new WireTrace(
                                    UUID.randomUUID().toString(),
                                    activeWireKind,
                                    activeWireKind.getDefaultLayer(),
                                    activeWireKind == WireKind.SILVER_WIRE ? "#CFD8DC" : activeWireColor,
                                    java.util.List.of(wireStartHole, grid)
                            );
                            project.getWires().add(trace);
                            wireStartHole = null;
                            if (onProjectModified != null) onProjectModified.run();
                        } else {
                            wireStartHole = null;
                        }
                    }
                    render();
                } else if (activeTool == ActiveTool.COMPONENT) {
                    // Bauteil platzieren
                    placeNewComponent(grid);
                    render();
                }
            } else if (e.getButton() == MouseButton.SECONDARY) {
                // Rechtsklick: Vorgang abbrechen
                wireStartHole = null;
                render();
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.MIDDLE || e.isSecondaryButtonDown() || (e.isPrimaryButtonDown() && e.isAltDown())) {
                // Pan
                double dx = e.getX() - lastDragX;
                double dy = e.getY() - lastDragY;
                transform.pan(dx, dy);
                lastDragX = e.getX();
                lastDragY = e.getY();
                render();
            } else if (e.getButton() == MouseButton.PRIMARY && activeTool == ActiveTool.SELECT && isDraggingObject && selectedComponent != null) {
                // Bauteil bewegen
                HoleCoord grid = transform.screenToGrid(e.getX(), e.getY());
                if (!grid.equals(selectedComponent.getStart())) {
                    int dx = grid.x() - selectedComponent.getStart().x();
                    int dy = grid.y() - selectedComponent.getStart().y();
                    selectedComponent.setStart(grid);
                    if (selectedComponent.getEnd() != null) {
                        selectedComponent.setEnd(new HoleCoord(selectedComponent.getEnd().x() + dx, selectedComponent.getEnd().y() + dy));
                    }
                    if (onProjectModified != null) onProjectModified.run();
                    render();
                }
            }
        });

        setOnMouseReleased(e -> {
            isDraggingObject = false;
        });

        setOnMouseMoved(e -> {
            currentHoverGrid = transform.screenToGrid(e.getX(), e.getY());
            if (onHoverCoordinate != null) {
                onHoverCoordinate.accept(currentHoverGrid.x(), currentHoverGrid.y());
            }

            // Tinkercad Style: Detect terminal hover
            hoveredTerminalText = detectTerminalLabel(currentHoverGrid);

            if ((activeTool == ActiveTool.WIRE && wireStartHole != null) || activeTool == ActiveTool.COMPONENT || hoveredTerminalText != null) {
                render();
            }
        });

        setOnScroll(e -> {
            double zoomFactor = e.getDeltaY() > 0 ? 1.15 : 0.87;
            transform.applyZoom(zoomFactor, e.getX(), e.getY());
            render();
        });
    }

    private PlacedComponent findComponentAt(HoleCoord grid) {
        for (PlacedComponent c : project.getComponents()) {
            int minX = Math.min(c.getStart().x(), c.getEnd() != null ? c.getEnd().x() : c.getStart().x());
            int maxX = Math.max(c.getStart().x(), c.getEnd() != null ? c.getEnd().x() : c.getStart().x());
            int minY = Math.min(c.getStart().y(), c.getEnd() != null ? c.getEnd().y() : c.getStart().y());
            int maxY = Math.max(c.getStart().y(), c.getEnd() != null ? c.getEnd().y() : c.getStart().y());

            if (grid.x() >= minX - 1 && grid.x() <= maxX + 1 &&
                grid.y() >= minY - 1 && grid.y() <= maxY + 1) {
                return c;
            }
        }
        return null;
    }

    private void placeNewComponent(HoleCoord start) {
        String id = UUID.randomUUID().toString();
        int count = (int) project.getComponents().stream().filter(c -> c.getType() == activeComponentType).count() + 1;
        String des = activeComponentType.getDefaultPrefix() + count;

        PlacedComponent comp;
        if (activeComponentType == ComponentType.TACTILE_SWITCH) {
            comp = new PlacedComponent(id, activeComponentType, des, "6x6 mm", start, new HoleCoord(start.x() + 2, start.y() + 1), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.RESISTOR_AXIAL) {
            comp = new PlacedComponent(id, activeComponentType, des, "10k", start, new HoleCoord(start.x() + 4, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.LED) {
            comp = new PlacedComponent(id, activeComponentType, des, "Red", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.CAPACITOR_CERAMIC) {
            comp = new PlacedComponent(id, activeComponentType, des, "100n", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.DIODE) {
            comp = new PlacedComponent(id, activeComponentType, des, "1N4148", start, new HoleCoord(start.x() + 3, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.IC_DIP) {
            comp = new PlacedComponent(id, activeComponentType, des, "DIP-8", start, new HoleCoord(start.x() + 3, start.y() + 3), 0, BoardLayer.TOP);
        } else {
            comp = new PlacedComponent(id, activeComponentType, des, "10k", start, new HoleCoord(start.x() + 2, start.y()), 0, BoardLayer.TOP);
        }

        project.getComponents().add(comp);
        selectedComponent = comp;
        if (onComponentSelected != null) onComponentSelected.accept(comp);
        if (onProjectModified != null) onProjectModified.run();
    }

    public void render() {
        if (getWidth() <= 10 || getHeight() <= 10) return;

        // Auto zoom-to-fit on initial startup so the perfboard fills the view completely
        if (!initialFitDone && getWidth() > 50 && getHeight() > 50) {
            zoomToFit();
            initialFitDone = true;
        }

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

        // 3. SILBER-PADS (Nutzer-Vorgabe: SILBER statt Gold!)
        double padR = Math.max(3.0, hs * 0.38);
        double drillR = Math.max(1.5, hs * 0.18);

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                double px = transform.gridToScreenX(c);
                double py = transform.gridToScreenY(r);
                HoleCoord currentHole = new HoleCoord(c, r);

                // KiCad Style: Highlighted Net Glow
                if (highlightedNet.contains(currentHole)) {
                    gc.setFill(Color.web("#FFEE58")); // Neongelb
                    gc.fillOval(px - padR - 3, py - padR - 3, (padR + 3) * 2, (padR + 3) * 2);
                }

                // Reines Silber-Pad (#CFD8DC) mit Rand (#90A4AE)
                gc.setFill(Color.web("#CFD8DC"));
                gc.fillOval(px - padR, py - padR, padR * 2, padR * 2);

                gc.setStroke(Color.web("#90A4AE"));
                gc.setLineWidth(1.0);
                gc.strokeOval(px - padR, py - padR, padR * 2, padR * 2);

                // Schwarze zentrierte Bohrung (#121212)
                gc.setFill(Color.web("#121212"));
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

        // Elastische Vorschau-Linie beim Drahtziehen
        if (activeTool == ActiveTool.WIRE && wireStartHole != null && currentHoverGrid != null) {
            gc.setStroke(activeWireKind == WireKind.SILVER_WIRE ? Color.web("#CFD8DC") : Color.web(activeWireColor));
            gc.setLineWidth(3.0 * transform.getScale());
            gc.setLineDashes(4, 4);
            gc.strokeLine(
                    transform.gridToScreenX(wireStartHole.x()), transform.gridToScreenY(wireStartHole.y()),
                    transform.gridToScreenX(currentHoverGrid.x()), transform.gridToScreenY(currentHoverGrid.y())
            );
            gc.setLineDashes(null);
        }

        // 5. Komponenten
        if (currentLayer == BoardLayer.TOP || currentLayer == BoardLayer.BOTH) {
            double alpha = (currentLayer == BoardLayer.BOTH) ? 0.65 : 1.0;
            gc.setGlobalAlpha(alpha);

            for (PlacedComponent comp : project.getComponents()) {
                boolean isSel = (comp == selectedComponent);
                renderComponentInstance(gc, comp, isSel);
            }
            gc.setGlobalAlpha(1.0);
        }

        // 5b. Tinkercad Style: Semi-transparent Ghost Preview floating at mouse cursor
        if (activeTool == ActiveTool.COMPONENT && currentHoverGrid != null) {
            gc.save();
            gc.setGlobalAlpha(0.60);
            PlacedComponent ghost = createDummyComponent(activeComponentType, currentHoverGrid);
            renderComponentInstance(gc, ghost, false);

            // Highlight snap target holes
            gc.setStroke(Color.web("#00E676"));
            gc.setLineWidth(2.0);
            double holeSpacing = transform.getHoleSpacingPx();
            double sx = transform.gridToScreenX(ghost.getStart().x());
            double sy = transform.gridToScreenY(ghost.getStart().y());
            gc.strokeOval(sx - holeSpacing * 0.45, sy - holeSpacing * 0.45, holeSpacing * 0.9, holeSpacing * 0.9);
            if (ghost.getEnd() != null) {
                double ex = transform.gridToScreenX(ghost.getEnd().x());
                double ey = transform.gridToScreenY(ghost.getEnd().y());
                gc.strokeOval(ex - holeSpacing * 0.45, ey - holeSpacing * 0.45, holeSpacing * 0.9, holeSpacing * 0.9);
            }
            gc.restore();
        }

        // 6. Tinkercad Style: Terminal Tooltip Badge
        if (hoveredTerminalText != null && currentHoverGrid != null) {
            double hx = transform.gridToScreenX(currentHoverGrid.x());
            double hy = transform.gridToScreenY(currentHoverGrid.y());

            gc.setFill(Color.web("#212121"));
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(1.0);
            double tw = hoveredTerminalText.length() * 6.5 + 12;
            gc.fillRoundRect(hx + 10, hy - 18, tw, 20, 4, 4);
            gc.strokeRoundRect(hx + 10, hy - 18, tw, 20, 4, 4);

            gc.setFill(Color.web("#FFFFFF"));
            gc.fillText(hoveredTerminalText, hx + 16, hy - 4);
        }
    }

    private void renderComponentInstance(GraphicsContext gc, PlacedComponent comp, boolean isSel) {
        if (comp.getType() == ComponentType.TACTILE_SWITCH) {
            renderTactileSwitch(gc, comp, isSel);
        } else if (comp.getType() == ComponentType.RESISTOR_AXIAL) {
            renderResistor(gc, comp, isSel);
        } else if (comp.getType() == ComponentType.LED) {
            renderLED(gc, comp, isSel);
        } else if (comp.getType() == ComponentType.CAPACITOR_CERAMIC) {
            renderCapacitor(gc, comp, isSel);
        } else if (comp.getType() == ComponentType.DIODE) {
            renderDiode(gc, comp, isSel);
        } else if (comp.getType() == ComponentType.IC_DIP) {
            renderIC(gc, comp, isSel);
        }
    }

    private PlacedComponent createDummyComponent(ComponentType type, HoleCoord start) {
        if (type == ComponentType.TACTILE_SWITCH) {
            return new PlacedComponent("ghost", type, "SW", "6x6 mm", start, new HoleCoord(start.x() + 2, start.y() + 1), 0, BoardLayer.TOP);
        } else if (type == ComponentType.RESISTOR_AXIAL) {
            return new PlacedComponent("ghost", type, "R", "10k", start, new HoleCoord(start.x() + 4, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.LED) {
            return new PlacedComponent("ghost", type, "D", "Red", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.CAPACITOR_CERAMIC) {
            return new PlacedComponent("ghost", type, "C", "100n", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.DIODE) {
            return new PlacedComponent("ghost", type, "D", "1N4148", start, new HoleCoord(start.x() + 3, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.IC_DIP) {
            return new PlacedComponent("ghost", type, "U", "DIP-8", start, new HoleCoord(start.x() + 3, start.y() + 3), 0, BoardLayer.TOP);
        }
        return new PlacedComponent("ghost", type, "COMP", "10k", start, new HoleCoord(start.x() + 2, start.y()), 0, BoardLayer.TOP);
    }

    private String detectTerminalLabel(HoleCoord grid) {
        if (grid == null) return null;
        for (PlacedComponent c : project.getComponents()) {
            HoleCoord s = c.getStart();
            HoleCoord e = c.getEnd() != null ? c.getEnd() : s;

            if (c.getType() == ComponentType.TACTILE_SWITCH) {
                if (grid.equals(s)) return c.getDesignator() + ": Terminal 1a";
                if (grid.equals(new HoleCoord(s.x() + 2, s.y()))) return c.getDesignator() + ": Terminal 1b";
                if (grid.equals(new HoleCoord(s.x(), s.y() + 1))) return c.getDesignator() + ": Terminal 2a";
                if (grid.equals(new HoleCoord(s.x() + 2, s.y() + 1))) return c.getDesignator() + ": Terminal 2b";
            } else if (c.getType() == ComponentType.RESISTOR_AXIAL) {
                if (grid.equals(s)) return c.getDesignator() + ": Terminal 1";
                if (grid.equals(e)) return c.getDesignator() + ": Terminal 2";
            } else if (c.getType() == ComponentType.LED) {
                if (grid.equals(s)) return c.getDesignator() + ": Anode (+)";
                if (grid.equals(e)) return c.getDesignator() + ": Cathode (-)";
            } else if (c.getType() == ComponentType.DIODE) {
                if (grid.equals(s)) return c.getDesignator() + ": Anode";
                if (grid.equals(e)) return c.getDesignator() + ": Cathode";
            }
        }
        return null;
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

    private void renderCapacitor(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        double px = transform.gridToScreenX(comp.getStart().x());
        double py = transform.gridToScreenY(comp.getStart().y());
        double r = transform.getHoleSpacingPx() * 0.40;

        gc.setFill(Color.web("#FFA726")); // Gelb-Orange Keramikkondensator
        gc.fillRoundRect(px - r, py - r, r * 2, r * 2, 4, 4);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRect(px - r - 3, py - r - 3, (r + 3) * 2, (r + 3) * 2);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator(), px - 10, py - r - 4);
    }

    private void renderDiode(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        double x1 = transform.gridToScreenX(comp.getStart().x());
        double y1 = transform.gridToScreenY(comp.getStart().y());
        double x2 = transform.gridToScreenX(comp.getEnd().x());
        double y2 = transform.gridToScreenY(comp.getEnd().y());

        gc.setStroke(Color.web("#B0BEC5"));
        gc.setLineWidth(2.0);
        gc.strokeLine(x1, y1, x2, y2);

        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;
        double bw = transform.getHoleSpacingPx() * 1.6;
        double bh = transform.getHoleSpacingPx() * 0.5;

        gc.setFill(Color.web("#FF7043")); // Glas-Körper 1N4148
        gc.fillRect(midX - bw/2, midY - bh/2, bw, bh);

        // Kathoden-Band schwarz
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillRect(midX + bw/2 - 4, midY - bh/2, 4, bh);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRect(midX - bw/2 - 2, midY - bh/2 - 2, bw + 4, bh + 4);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator(), midX - 10, midY - bh/2 - 4);
    }

    private void renderIC(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        double x1 = transform.gridToScreenX(comp.getStart().x());
        double y1 = transform.gridToScreenY(comp.getStart().y());
        double w = transform.getHoleSpacingPx() * 3.5;
        double h = transform.getHoleSpacingPx() * 3.5;

        gc.setFill(Color.web("#263238"));
        gc.fillRoundRect(x1 - 10, y1 - 10, w, h, 6, 6);

        // Pin 1 Kerbe (Notch)
        gc.setFill(Color.web("#121212"));
        gc.fillOval(x1 + w/2 - 16, y1 - 16, 12, 12);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(x1 - 12, y1 - 12, w + 4, h + 4, 8, 8);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator() + " (DIP)", x1 + 4, y1 + h/2);
    }

    private void drawBand(GraphicsContext gc, double x, double y, double w, double h, String hexColor) {
        gc.setFill(Color.web(hexColor));
        gc.fillRect(x, y, w, h);
    }

    public void setLayer(BoardLayer layer) {
        this.currentLayer = layer;
        render();
    }

    public void setActiveTool(ActiveTool tool) {
        this.activeTool = tool;
        this.wireStartHole = null;
        render();
    }

    public void setActiveWireKind(WireKind kind) {
        this.activeWireKind = kind;
    }

    public void setActiveWireColor(String col) {
        this.activeWireColor = col;
    }

    public void setActiveComponentType(ComponentType type) {
        this.activeComponentType = type;
        this.activeTool = ActiveTool.COMPONENT;
    }

    public void deleteSelected() {
        if (selectedComponent != null) {
            project.getComponents().remove(selectedComponent);
            selectedComponent = null;
            if (onProjectModified != null) onProjectModified.run();
            render();
        }
    }

    public void rotateSelected() {
        if (selectedComponent != null) {
            selectedComponent.rotate90();
            if (onProjectModified != null) onProjectModified.run();
            render();
        }
    }

    public void zoomToFit() {
        if (getWidth() > 50 && getHeight() > 50) {
            transform.resetView(getWidth(), getHeight(), project.getBoard().getColumns(), project.getBoard().getRows());
        }
    }

    public void setProject(PerfProject project) {
        this.project = project;
        this.selectedComponent = project.getComponents().isEmpty() ? null : project.getComponents().getFirst();
        zoomToFit();
        render();
    }

    public CoordinateTransform getTransform() { return transform; }
    public boolean isInitialFitDone() { return initialFitDone; }
    public PerfProject getProject() { return project; }
    public PlacedComponent getSelectedComponent() { return selectedComponent; }

    public void setOnHoverCoordinate(BiConsumer<Integer, Integer> callback) {
        this.onHoverCoordinate = callback;
    }

    public void setOnComponentSelected(Consumer<PlacedComponent> callback) {
        this.onComponentSelected = callback;
    }

    public void setOnProjectModified(Runnable callback) {
        this.onProjectModified = callback;
    }
}
