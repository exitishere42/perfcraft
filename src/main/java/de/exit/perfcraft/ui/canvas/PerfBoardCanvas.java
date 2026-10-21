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
    private Consumer<String> onWarningNotification;

    private boolean initialFitDone = false;

    public PerfBoardCanvas(double width, double height) {
        super(width, height);
        initInteraction();
        render();
    }

    private void initDemoData() {
        project.getComponents().add(new PlacedComponent(
                "c1", ComponentType.TACTILE_SWITCH, "SW1", "6x6 mm",
                new HoleCoord(5, 4), new HoleCoord(7, 7), 0, BoardLayer.TOP
        ));

        project.getComponents().add(new PlacedComponent(
                "c2", ComponentType.RESISTOR_AXIAL, "R1", "10k",
                new HoleCoord(10, 4), new HoleCoord(13, 4), 0, BoardLayer.TOP
        ));

        project.getComponents().add(new PlacedComponent(
                "c3", ComponentType.LED, "LED1", "Red",
                new HoleCoord(15, 4), new HoleCoord(16, 4), 0, BoardLayer.TOP
        ));

        project.getWires().add(new WireTrace(
                "w1", WireKind.CABLE, BoardLayer.TOP, "#E53935",
                java.util.List.of(new HoleCoord(5, 4), new HoleCoord(10, 4))
        ));

        project.getWires().add(new WireTrace(
                "w2", WireKind.SILVER_WIRE, BoardLayer.BOTTOM, "#CFD8DC",
                java.util.List.of(new HoleCoord(13, 4), new HoleCoord(15, 4))
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
                            // Prüfung: Ist einer der Endpunkte ein TP4056 Pad und wird Silberdraht verwendet?
                            if (activeWireKind == WireKind.SILVER_WIRE && isTp4056Pad(wireStartHole, grid)) {
                                if (onWarningNotification != null) {
                                    onWarningNotification.accept("Warnung: TP4056 Lader darf NICHT mit unisolierter Silber-Drahtbrücke verbunden werden! Bitte isoliertes Kabel verwenden (Kurzschlussgefahr LiPo-Akku).");
                                }
                                wireStartHole = null;
                                render();
                                return;
                            }

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
            // 3x4 Pins Raster (3 breit, 4 hoch -> dx=2, dy=3)
            comp = new PlacedComponent(id, activeComponentType, des, "6x6 mm", start, new HoleCoord(start.x() + 2, start.y() + 3), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.RESISTOR_AXIAL) {
            // 4 Pins verbrauchen -> 4x1 (start bis start.x + 3 -> 4 Pins total: x, x+1, x+2, x+3)
            comp = new PlacedComponent(id, activeComponentType, des, "10k", start, new HoleCoord(start.x() + 3, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.LED) {
            // LED 2x1 (Breite 2, Höhe 1 -> Anode bei start, Kathode bei start.x + 1)
            comp = new PlacedComponent(id, activeComponentType, des, "Red", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.CAPACITOR_CERAMIC) {
            comp = new PlacedComponent(id, activeComponentType, des, "100n", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.DIODE) {
            comp = new PlacedComponent(id, activeComponentType, des, "1N4148", start, new HoleCoord(start.x() + 3, start.y()), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.IC_DIP) {
            comp = new PlacedComponent(id, activeComponentType, des, "DIP-8", start, new HoleCoord(start.x() + 3, start.y() + 3), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.ESP32_S3_ZERO) {
            // ESP32-S3-Zero: 7 Spalten x 9 Zeilen (dx=6, dy=8)
            comp = new PlacedComponent(id, activeComponentType, des, "ESP32-S3-Zero", start, new HoleCoord(start.x() + 6, start.y() + 8), 0, BoardLayer.TOP);
        } else if (activeComponentType == ComponentType.TP4056_CHARGER) {
            // TP4056 Lipo Charger: 7 Spalten x 10 Zeilen (dx=6, dy=9)
            comp = new PlacedComponent(id, activeComponentType, des, "TP4056", start, new HoleCoord(start.x() + 6, start.y() + 9), 0, BoardLayer.TOP);
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

        // 2. SILBER-PADS (Nutzer-Vorgabe: SILBER statt Gold!)
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

        // 3. Leitungen (Kabel und Silberdraht - beide liegen oben auf den Pads)
        // Silberdraht ist dünner (1.8px) als normales Kabel (3.5px) und in glänzendem Silber (#E0E0E0)
        for (WireTrace w : project.getWires()) {
            if (w.getPoints().size() >= 2) {
                var p1 = w.getPoints().get(0);
                var p2 = w.getPoints().get(1);

                if (w.getKind() == WireKind.SILVER_WIRE) {
                    gc.setStroke(Color.web("#E0E0E0"));
                    gc.setLineWidth(1.8 * transform.getScale());
                } else {
                    gc.setStroke(Color.web(w.getColor()));
                    gc.setLineWidth(3.5 * transform.getScale());
                }
                gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                gc.strokeLine(
                        transform.gridToScreenX(p1.x()), transform.gridToScreenY(p1.y()),
                        transform.gridToScreenX(p2.x()), transform.gridToScreenY(p2.y())
                );
            }
        }

        // Elastische Vorschau-Linie beim Drahtziehen
        if (activeTool == ActiveTool.WIRE && wireStartHole != null && currentHoverGrid != null) {
            boolean isSilver = (activeWireKind == WireKind.SILVER_WIRE);
            gc.setStroke(isSilver ? Color.web("#E0E0E0") : Color.web(activeWireColor));
            gc.setLineWidth((isSilver ? 1.8 : 3.0) * transform.getScale());
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
        } else if (comp.getType() == ComponentType.ESP32_S3_ZERO) {
            renderESP32S3Zero(gc, comp, isSel);
        } else if (comp.getType() == ComponentType.TP4056_CHARGER) {
            renderTP4056(gc, comp, isSel);
        }
    }

    private PlacedComponent createDummyComponent(ComponentType type, HoleCoord start) {
        if (type == ComponentType.TACTILE_SWITCH) {
            return new PlacedComponent("ghost", type, "SW", "6x6 mm", start, new HoleCoord(start.x() + 2, start.y() + 3), 0, BoardLayer.TOP);
        } else if (type == ComponentType.RESISTOR_AXIAL) {
            return new PlacedComponent("ghost", type, "R", "10k", start, new HoleCoord(start.x() + 3, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.LED) {
            return new PlacedComponent("ghost", type, "D", "Red", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.CAPACITOR_CERAMIC) {
            return new PlacedComponent("ghost", type, "C", "100n", start, new HoleCoord(start.x() + 1, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.DIODE) {
            return new PlacedComponent("ghost", type, "D", "1N4148", start, new HoleCoord(start.x() + 3, start.y()), 0, BoardLayer.TOP);
        } else if (type == ComponentType.IC_DIP) {
            return new PlacedComponent("ghost", type, "U", "DIP-8", start, new HoleCoord(start.x() + 3, start.y() + 3), 0, BoardLayer.TOP);
        } else if (type == ComponentType.ESP32_S3_ZERO) {
            return new PlacedComponent("ghost", type, "U", "ESP32-S3-Zero", start, new HoleCoord(start.x() + 6, start.y() + 8), 0, BoardLayer.TOP);
        } else if (type == ComponentType.TP4056_CHARGER) {
            return new PlacedComponent("ghost", type, "U", "TP4056", start, new HoleCoord(start.x() + 6, start.y() + 9), 0, BoardLayer.TOP);
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
                if (grid.equals(new HoleCoord(s.x(), s.y() + 3))) return c.getDesignator() + ": Terminal 2a";
                if (grid.equals(new HoleCoord(s.x() + 2, s.y() + 3))) return c.getDesignator() + ": Terminal 2b";
            } else if (c.getType() == ComponentType.RESISTOR_AXIAL) {
                if (grid.equals(s)) return c.getDesignator() + ": Terminal 1";
                if (grid.equals(e)) return c.getDesignator() + ": Terminal 2";
            } else if (c.getType() == ComponentType.LED) {
                if (grid.equals(s)) return c.getDesignator() + ": Anode (+)";
                if (grid.equals(e)) return c.getDesignator() + ": Cathode (-)";
            } else if (c.getType() == ComponentType.DIODE) {
                if (grid.equals(s)) return c.getDesignator() + ": Anode";
                if (grid.equals(e)) return c.getDesignator() + ": Cathode";
            } else if (c.getType() == ComponentType.ESP32_S3_ZERO) {
                // Linke Pinleiste (x = s.x)
                if (grid.x() == s.x()) {
                    int dy = grid.y() - s.y();
                    switch (dy) {
                        case 0 -> { return c.getDesignator() + " (ESP32): 5V"; }
                        case 1 -> { return c.getDesignator() + " (ESP32): GND"; }
                        case 2 -> { return c.getDesignator() + " (ESP32): 3V3"; }
                        case 3 -> { return c.getDesignator() + " (ESP32): GP1"; }
                        case 4 -> { return c.getDesignator() + " (ESP32): GP2"; }
                        case 5 -> { return c.getDesignator() + " (ESP32): GP3"; }
                        case 6 -> { return c.getDesignator() + " (ESP32): GP4"; }
                        case 7 -> { return c.getDesignator() + " (ESP32): GP5"; }
                        case 8 -> { return c.getDesignator() + " (ESP32): GP6"; }
                    }
                }
                // Rechte Pinleiste (x = s.x + 6)
                if (grid.x() == s.x() + 6) {
                    int dy = grid.y() - s.y();
                    switch (dy) {
                        case 0 -> { return c.getDesignator() + " (ESP32): TX (GP43)"; }
                        case 1 -> { return c.getDesignator() + " (ESP32): RX (GP44)"; }
                        case 2 -> { return c.getDesignator() + " (ESP32): GP13"; }
                        case 3 -> { return c.getDesignator() + " (ESP32): GP12"; }
                        case 4 -> { return c.getDesignator() + " (ESP32): GP11"; }
                        case 5 -> { return c.getDesignator() + " (ESP32): GP10"; }
                        case 6 -> { return c.getDesignator() + " (ESP32): GP9"; }
                        case 7 -> { return c.getDesignator() + " (ESP32): GP8"; }
                        case 8 -> { return c.getDesignator() + " (ESP32): GP7"; }
                    }
                }
            } else if (c.getType() == ComponentType.TP4056_CHARGER) {
                // TP4056 Lader (7 Spalten breit: x = s.x oder s.x + 6; y = s.y, s.y+1, s.y+8, s.y+9)
                if (grid.x() == s.x() && grid.y() == s.y()) return c.getDesignator() + " (TP4056): IN+ (4.5V-5.5V)";
                if (grid.x() == s.x() + 6 && grid.y() == s.y()) return c.getDesignator() + " (TP4056): IN- (GND)";
                if (grid.x() == s.x() && grid.y() == s.y() + 8) return c.getDesignator() + " (TP4056): OUT+";
                if (grid.x() == s.x() && grid.y() == s.y() + 9) return c.getDesignator() + " (TP4056): B+ (Batterie +)";
                if (grid.x() == s.x() + 6 && grid.y() == s.y() + 9) return c.getDesignator() + " (TP4056): B- (Batterie -)";
                if (grid.x() == s.x() + 6 && grid.y() == s.y() + 8) return c.getDesignator() + " (TP4056): OUT-";
            }
        }
        return null;
    }

    private void renderTactileSwitch(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        int gx = comp.getStart().x();
        int gy = comp.getStart().y();
        double hs = transform.getHoleSpacingPx();

        // 3x4 Pins Raster: Pin 1a=(gx, gy), 1b=(gx+2, gy), Pin 2a=(gx, gy+3), 2b=(gx+2, gy+3)
        double x0 = transform.gridToScreenX(gx);
        double y0 = transform.gridToScreenY(gy);
        double x1 = transform.gridToScreenX(gx + 2);
        double y1 = transform.gridToScreenY(gy + 3);

        double cx = (x0 + x1) / 2.0;
        double cy = (y0 + y1) / 2.0;

        // Quadratischer Switch-Körper (ca. 2.4 * hs breit und hoch)
        double bodySize = hs * 2.4;
        double bw = bodySize;
        double bh = bodySize;

        // 1. Die 4 geschwungenen Metall-Pins (Gull-Wing Leads zu den 4 Pads)
        gc.setStroke(Color.web("#CFD8DC"));
        gc.setLineWidth(Math.max(2.0, hs * 0.16));
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        double bodyHalf = bodySize / 2.0;

        // Oben-links (Pin 1a)
        gc.strokeLine(x0, y0, cx - bodyHalf + hs * 0.15, cy - bodyHalf);
        // Oben-rechts (Pin 1b)
        gc.strokeLine(x1, y0, cx + bodyHalf - hs * 0.15, cy - bodyHalf);
        // Unten-links (Pin 2a)
        gc.strokeLine(x0, y1, cx - bodyHalf + hs * 0.15, cy + bodyHalf);
        // Unten-rechts (Pin 2b)
        gc.strokeLine(x1, y1, cx + bodyHalf - hs * 0.15, cy + bodyHalf);

        // Kleine glänzende Löt-Pads an den Pins
        gc.setFill(Color.web("#ECEFF1"));
        double pinDotR = Math.max(2.5, hs * 0.15);
        gc.fillOval(x0 - pinDotR, y0 - pinDotR, pinDotR * 2, pinDotR * 2);
        gc.fillOval(x1 - pinDotR, y0 - pinDotR, pinDotR * 2, pinDotR * 2);
        gc.fillOval(x0 - pinDotR, y1 - pinDotR, pinDotR * 2, pinDotR * 2);
        gc.fillOval(x1 - pinDotR, y1 - pinDotR, pinDotR * 2, pinDotR * 2);

        // 2. Schwarzer Kunststoff-Körper (quadratisch)
        gc.setFill(Color.web("#1E1E1E")); // Tiefschwarzer Thermoplast
        gc.fillRoundRect(cx - bw/2, cy - bh/2, bw, bh, 5, 5);
        gc.setStroke(Color.web("#121212"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(cx - bw/2, cy - bh/2, bw, bh, 5, 5);

        // 3. Silberne quadratische Metall-Deckplatte mit 4 Eck-Nieten
        double mw = bw * 0.86;
        double mh = bh * 0.86;
        gc.setFill(Color.web("#B0BEC5")); // Mattes Silber-Blech
        gc.fillRoundRect(cx - mw/2, cy - mh/2, mw, mh, 4, 4);
        gc.setStroke(Color.web("#78909C"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(cx - mw/2, cy - mh/2, mw, mh, 4, 4);

        // 4 Eck-Nieten / Befestigungspunkte
        gc.setFill(Color.web("#37474F"));
        double rivetR = Math.max(1.2, hs * 0.08);
        double rxOffset = mw * 0.38;
        double ryOffset = mh * 0.38;
        gc.fillOval(cx - rxOffset - rivetR, cy - ryOffset - rivetR, rivetR * 2, rivetR * 2);
        gc.fillOval(cx + rxOffset - rivetR, cy - ryOffset - rivetR, rivetR * 2, rivetR * 2);
        gc.fillOval(cx - rxOffset - rivetR, cy + ryOffset - rivetR, rivetR * 2, rivetR * 2);
        gc.fillOval(cx + rxOffset - rivetR, cy + ryOffset - rivetR, rivetR * 2, rivetR * 2);

        // 4. Zentraler runder schwarzer Betätigungs-Knopf
        double btnR = mw * 0.38;
        // Äußerer Schatten/Sockel
        gc.setFill(Color.web("#263238"));
        gc.fillOval(cx - btnR, cy - btnR, btnR * 2, btnR * 2);
        // Knopf-Oberseite mit dezenter Schattierung
        gc.setFill(Color.web("#151515"));
        gc.fillOval(cx - btnR * 0.88, cy - btnR * 0.88, btnR * 1.76, btnR * 1.76);
        // Kleines Glanzlicht oben links
        gc.setFill(Color.web("#424242"));
        gc.fillOval(cx - btnR * 0.5, cy - btnR * 0.5, btnR * 0.35, btnR * 0.35);

        // Selektions-Rahmen
        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(cx - bw/2 - 3, cy - bh/2 - 3, bw + 6, bh + 6, 6, 6);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator(), cx - 12, cy - bh/2 - 4);
    }

    private void renderResistor(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        double x1 = transform.gridToScreenX(comp.getStart().x());
        double y1 = transform.gridToScreenY(comp.getStart().y());
        double x2 = transform.gridToScreenX(comp.getEnd().x());
        double y2 = transform.gridToScreenY(comp.getEnd().y());
        double hs = transform.getHoleSpacingPx();

        // Anschlussdrähte (Axial Leads)
        gc.setStroke(Color.web("#B0BEC5"));
        gc.setLineWidth(2.5 * transform.getScale());
        gc.strokeLine(x1, y1, x2, y2);

        // Geometrie des Widerstandskörpers (Knochen-/Hantel-Form)
        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;
        double bw = hs * 2.5;
        double bh = hs * 0.58;

        double capW = bw * 0.22;
        double capH = bh * 1.18;
        double waistW = bw * 0.56;
        double waistH = bh * 0.82;

        double leftCapX = midX - bw / 2.0;
        double rightCapX = midX + bw / 2.0 - capW;
        double waistX = leftCapX + capW;

        // 1. Mittlerer zylindrischer Taillenkörper (warmes Keramik-Beige)
        gc.setFill(Color.web("#E8D8B8"));
        gc.fillRect(waistX, midY - waistH / 2.0, waistW, waistH);

        // 2. Linke & rechte bauchige Endkappen (Wulst)
        gc.fillRoundRect(leftCapX, midY - capH / 2.0, capW + 2, capH, 5, 5);
        gc.fillRoundRect(rightCapX - 2, midY - capH / 2.0, capW + 2, capH, 5, 5);

        // Subtile Konturierung / Schatten für CAD-Plastizität
        gc.setStroke(Color.web("#BCAAA4"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(leftCapX, midY - capH / 2.0, capW, capH, 5, 5);
        gc.strokeRoundRect(rightCapX, midY - capH / 2.0, capW, capH, 5, 5);
        gc.strokeLine(waistX, midY - waistH / 2.0, waistX + waistW, midY - waistH / 2.0);
        gc.strokeLine(waistX, midY + waistH / 2.0, waistX + waistW, midY + waistH / 2.0);

        // Dezentes oberes Glanzlicht
        gc.setStroke(Color.web("#FFF8E1"));
        gc.setLineWidth(1.2);
        gc.strokeLine(waistX + 2, midY - waistH / 2.0 + 1.5, waistX + waistW - 2, midY - waistH / 2.0 + 1.5);

        // Echte Farbringe (Band 1 auf linker Kappe, 2 & Multiplikator in der Taille, Toleranz auf rechter Kappe)
        var bands = ResistorColorCode.parseAndCalculate(comp.getValue());
        if (bands != null) {
            double bandW = Math.max(2.2, bw * 0.08);

            // Ring 1 (Erste Ziffer auf linker Ausbuchtung)
            drawBand(gc, leftCapX + capW * 0.45, midY - capH / 2.0, bandW, capH, bands.band1().getHexColor());

            // Ring 2 (Zweite Ziffer auf Taillenteil)
            drawBand(gc, waistX + waistW * 0.22, midY - waistH / 2.0, bandW, waistH, bands.band2().getHexColor());

            // Ring 3 (Multiplikator auf Taillenteil)
            drawBand(gc, waistX + waistW * 0.62, midY - waistH / 2.0, bandW, waistH, bands.multiplier().getHexColor());

            // Ring 4 (Toleranz z. B. Gold/Silber auf rechter Ausbuchtung)
            drawBand(gc, rightCapX + capW * 0.35, midY - capH / 2.0, bandW, capH, bands.tolerance().getHexColor());
        }

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRoundRect(leftCapX - 3, midY - capH / 2.0 - 3, bw + 6, capH + 6, 6, 6);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator() + " (" + comp.getValue() + "Ω)", midX - 20, midY - capH / 2.0 - 4);
    }

    private void renderLED(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        double x1 = transform.gridToScreenX(comp.getStart().x());
        double y1 = transform.gridToScreenY(comp.getStart().y());
        double x2 = transform.gridToScreenX(comp.getEnd() != null ? comp.getEnd().x() : comp.getStart().x() + 1);
        double y2 = transform.gridToScreenY(comp.getEnd() != null ? comp.getEnd().y() : comp.getStart().y());
        double hs = transform.getHoleSpacingPx();

        // 1. Anschlussdrähte / Beinchen von den 2 Löt-Pads (Anode & Kathode)
        gc.setStroke(Color.web("#B0BEC5"));
        gc.setLineWidth(2.2 * transform.getScale());
        gc.strokeLine(x1, y1, x2, y2);

        // Kleine glänzende Löt-Punkte an den Anschlüssen
        gc.setFill(Color.web("#ECEFF1"));
        double pinR = Math.max(2.2, hs * 0.13);
        gc.fillOval(x1 - pinR, y1 - pinR, pinR * 2, pinR * 2);
        gc.fillOval(x2 - pinR, y2 - pinR, pinR * 2, pinR * 2);

        // 2. LED-Körper zentriert zwischen den 2 Anschlusspads (2x1 Spanne)
        double cx = (x1 + x2) / 2.0;
        double cy = (y1 + y2) / 2.0;
        double r = hs * 0.44;

        // Farbige Linse / Kuppel
        gc.setFill(Color.web("#E53935"));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Kathoden-Abflachung (flache Kante auf Kathodenseite / rechts)
        gc.setFill(Color.web("#B71C1C"));
        gc.fillRect(cx + r * 0.65, cy - r, r * 0.35, r * 2);

        // Innenreflektor / Glanzpunkt (plastische Linse)
        gc.setFill(Color.web("#FF8A80"));
        gc.fillOval(cx - r * 0.45, cy - r * 0.45, r * 0.4, r * 0.4);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.0);
            gc.strokeRect(Math.min(x1, x2) - pinR - 3, cy - r - 3, Math.abs(x2 - x1) + (pinR + 3) * 2, (r + 3) * 2);
        }

        gc.setFill(Color.web("#ECEFF1"));
        gc.fillText(comp.getDesignator(), cx - 12, cy - r - 4);
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

    private void renderESP32S3Zero(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        int gx = comp.getStart().x();
        int gy = comp.getStart().y();
        double hs = transform.getHoleSpacingPx();

        double xLeft = transform.gridToScreenX(gx);
        double xRight = transform.gridToScreenX(gx + 6);
        double yTop = transform.gridToScreenY(gy);
        double yBottom = transform.gridToScreenY(gy + 8);

        double width = xRight - xLeft + hs * 0.8;
        double height = yBottom - yTop + hs * 0.8;
        double boardLeft = xLeft - hs * 0.4;
        double boardTop = yTop - hs * 0.4;

        // 1. PCB Grundkörper (Waveshare Classic Blue #0D47A1)
        gc.setFill(Color.web("#0D47A1"));
        gc.fillRoundRect(boardLeft, boardTop, width, height, 6, 6);
        gc.setStroke(Color.web("#1565C0"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(boardLeft, boardTop, width, height, 6, 6);

        // 2. USB-C Port oben mittig (Metall-Gehäuse überstehend)
        double usbW = hs * 2.6;
        double usbH = hs * 1.5;
        double usbX = (xLeft + xRight) / 2.0 - usbW / 2.0;
        double usbY = boardTop - hs * 0.6;
        gc.setFill(Color.web("#B0BEC5"));
        gc.fillRoundRect(usbX, usbY, usbW, usbH, 4, 4);
        gc.setStroke(Color.web("#78909C"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(usbX, usbY, usbW, usbH, 4, 4);
        // USB-C Schlitz innen
        gc.setFill(Color.web("#263238"));
        gc.fillRoundRect(usbX + usbW * 0.15, usbY + usbH * 0.25, usbW * 0.7, usbH * 0.45, 2, 2);

        // 3. Taster Boot (B) und Reset (R)
        double btnSize = hs * 0.7;
        // Boot Button (links neben USB)
        gc.setFill(Color.web("#ECEFF1"));
        gc.fillRect(usbX - btnSize - hs * 0.2, yTop + hs * 0.8, btnSize, btnSize);
        gc.setFill(Color.web("#212121"));
        gc.fillOval(usbX - btnSize - hs * 0.2 + btnSize * 0.2, yTop + hs * 0.8 + btnSize * 0.2, btnSize * 0.6, btnSize * 0.6);

        // Reset Button (rechts neben USB)
        gc.setFill(Color.web("#ECEFF1"));
        gc.fillRect(usbX + usbW + hs * 0.2, yTop + hs * 0.8, btnSize, btnSize);
        gc.setFill(Color.web("#212121"));
        gc.fillOval(usbX + usbW + hs * 0.2 + btnSize * 0.2, yTop + hs * 0.8 + btnSize * 0.2, btnSize * 0.6, btnSize * 0.6);

        // 4. Zentraler ESP32-S3 QFN-Chip (schwarz, diagonal rotiert wie im Original!)
        double chipMidX = (xLeft + xRight) / 2.0;
        double chipMidY = (yTop + yBottom) / 2.0 + hs * 0.5;
        double chipR = hs * 1.25;

        gc.save();
        gc.translate(chipMidX, chipMidY);
        gc.rotate(45);
        gc.setFill(Color.web("#212121"));
        gc.fillRect(-chipR, -chipR, chipR * 2, chipR * 2);
        gc.setStroke(Color.web("#424242"));
        gc.setLineWidth(1.0);
        gc.strokeRect(-chipR, -chipR, chipR * 2, chipR * 2);
        // Chip Silkscreen Logo (Espressif Symbol)
        gc.setFill(Color.web("#9E9E9E"));
        gc.fillOval(-chipR * 0.4, -chipR * 0.4, chipR * 0.8, chipR * 0.8);
        gc.restore();

        // 5. Keramik-Antenne unten (weißes Rechteck mit roten Streifen "C3")
        double antW = hs * 3.2;
        double antH = hs * 0.9;
        double antX = chipMidX - antW / 2.0;
        double antY = yBottom - antH + hs * 0.2;
        gc.setFill(Color.web("#FAFAFA"));
        gc.fillRect(antX, antY, antW, antH);
        gc.setStroke(Color.web("#BDBDBD"));
        gc.setLineWidth(1.0);
        gc.strokeRect(antX, antY, antW, antH);
        gc.setFill(Color.web("#D32F2F"));
        gc.fillRect(antX + antW * 0.35, antY + 2, antW * 0.3, antH - 4);

        // 6. Goldene Castellated Pads & Pin-Lötkontakte links & rechts (je 9 Pins)
        double padR = Math.max(2.8, hs * 0.32);
        for (int i = 0; i < 9; i++) {
            double py = transform.gridToScreenY(gy + i);

            // Links (Pin 1 bis 9)
            gc.setFill(Color.web("#FFD54F")); // Goldene Lötaugen
            gc.fillOval(xLeft - padR, py - padR, padR * 2, padR * 2);
            gc.setFill(Color.web("#121212")); // Lochbohrung
            gc.fillOval(xLeft - padR * 0.5, py - padR * 0.5, padR, padR);

            // Rechts (Pin 10 bis 18)
            gc.setFill(Color.web("#FFD54F"));
            gc.fillOval(xRight - padR, py - padR, padR * 2, padR * 2);
            gc.setFill(Color.web("#121212"));
            gc.fillOval(xRight - padR * 0.5, py - padR * 0.5, padR, padR);
        }

        // Beschriftung & Silkscreen
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillText(comp.getDesignator(), boardLeft + 8, boardTop + 14);
        gc.setFill(Color.web("#90CAF9"));
        gc.fillText("ESP32-S3-Zero", chipMidX - 32, boardTop + height - 4);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.5);
            gc.strokeRoundRect(boardLeft - 4, boardTop - 4, width + 8, height + 8, 8, 8);
        }
    }

    private void renderTP4056(GraphicsContext gc, PlacedComponent comp, boolean isSelected) {
        int gx = comp.getStart().x();
        int gy = comp.getStart().y();
        double hs = transform.getHoleSpacingPx();

        double xLeft = transform.gridToScreenX(gx);
        double xRight = transform.gridToScreenX(gx + 6);
        double yTop = transform.gridToScreenY(gy);
        double yBottom = transform.gridToScreenY(gy + 9);

        double width = xRight - xLeft + hs * 0.9;
        double height = yBottom - yTop + hs * 0.9;
        double boardLeft = xLeft - hs * 0.45;
        double boardTop = yTop - hs * 0.45;

        // 1. PCB Grundkörper (Klassisches TP4056 Dunkelblau #1565C0)
        gc.setFill(Color.web("#1565C0"));
        gc.fillRoundRect(boardLeft, boardTop, width, height, 6, 6);
        gc.setStroke(Color.web("#1E88E5"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(boardLeft, boardTop, width, height, 6, 6);

        // 2. USB-C Eingang oben mittig (überstehend)
        double usbW = hs * 2.8;
        double usbH = hs * 1.6;
        double usbX = (xLeft + xRight) / 2.0 - usbW / 2.0;
        double usbY = boardTop - hs * 0.55;
        gc.setFill(Color.web("#CFD8DC"));
        gc.fillRoundRect(usbX, usbY, usbW, usbH, 4, 4);
        gc.setStroke(Color.web("#90A4AE"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(usbX, usbY, usbW, usbH, 4, 4);
        gc.setFill(Color.web("#263238"));
        gc.fillRoundRect(usbX + usbW * 0.15, usbY + usbH * 0.25, usbW * 0.7, usbH * 0.45, 2, 2);

        // 3. Haupt-Lade-IC (TP4056 SOP-8) in der Mitte
        double midX = (xLeft + xRight) / 2.0;
        double tpY = yTop + hs * 3.2;
        double tpW = hs * 2.2;
        double tpH = hs * 1.6;
        gc.setFill(Color.web("#212121"));
        gc.fillRoundRect(midX - tpW / 2.0, tpY - tpH / 2.0, tpW, tpH, 2, 2);
        gc.setStroke(Color.web("#424242"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(midX - tpW / 2.0, tpY - tpH / 2.0, tpW, tpH, 2, 2);
        gc.setFill(Color.web("#B0BEC5"));
        gc.fillText("4056", midX - 11, tpY + 4);

        // 4. Schutz-IC DW01A + 8205A Dual-MOSFET unten
        double protY = yTop + hs * 5.8;
        gc.setFill(Color.web("#212121"));
        gc.fillRoundRect(midX - hs * 0.9, protY, hs * 1.8, hs * 2.0, 2, 2);
        gc.setStroke(Color.web("#37474F"));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(midX - hs * 0.9, protY, hs * 1.8, hs * 2.0, 2, 2);

        // Zwei Status-LEDs (Rot = Laden, Blau = Voll)
        gc.setFill(Color.web("#E53935")); // Rot
        gc.fillOval(midX - hs * 1.4, yTop + hs * 1.8, hs * 0.4, hs * 0.4);
        gc.setFill(Color.web("#1E88E5")); // Blau
        gc.fillOval(midX + hs * 1.0, yTop + hs * 1.8, hs * 0.4, hs * 0.4);

        // 5. Die 6 Löt-Pads (IN+, IN-, OUT+, B+, B-, OUT-)
        double padR = Math.max(3.0, hs * 0.35);
        double[][] pads = {
                {xLeft, yTop},             // IN+
                {xRight, yTop},            // IN-
                {xLeft, yTop + hs * 8.0},  // OUT+
                {xLeft, yTop + hs * 9.0},  // B+
                {xRight, yTop + hs * 9.0}, // B-
                {xRight, yTop + hs * 8.0}  // OUT-
        };
        for (double[] pt : pads) {
            gc.setFill(Color.web("#FFD54F")); // Goldener Lötkontakt
            gc.fillOval(pt[0] - padR, pt[1] - padR, padR * 2, padR * 2);
            gc.setFill(Color.web("#121212")); // Lochbohrung
            gc.fillOval(pt[0] - padR * 0.5, pt[1] - padR * 0.5, padR, padR);
        }

        // Beschriftung Silkscreen
        gc.setFill(Color.web("#FFFFFF"));
        gc.fillText("+ IN", xLeft + 6, yTop + 4);
        gc.fillText("IN -", xRight - 24, yTop + 4);
        gc.fillText("OUT+", xLeft + 6, yTop + hs * 8.0 + 4);
        gc.fillText("B+", xLeft + 6, yTop + hs * 9.0 + 4);
        gc.fillText("B-", xRight - 18, yTop + hs * 9.0 + 4);
        gc.fillText("OUT-", xRight - 30, yTop + hs * 8.0 + 4);
        gc.fillText(comp.getDesignator() + " (TP4056)", boardLeft + 8, boardTop + 14);

        if (isSelected) {
            gc.setStroke(Color.web("#00C853"));
            gc.setLineWidth(2.5);
            gc.strokeRoundRect(boardLeft - 4, boardTop - 4, width + 8, height + 8, 8, 8);
        }
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

    private boolean isTp4056Pad(HoleCoord h1, HoleCoord h2) {
        for (PlacedComponent c : project.getComponents()) {
            if (c.getType() == ComponentType.TP4056_CHARGER) {
                HoleCoord s = c.getStart();
                HoleCoord[] pads = {
                        new HoleCoord(s.x(), s.y()),
                        new HoleCoord(s.x() + 6, s.y()),
                        new HoleCoord(s.x(), s.y() + 8),
                        new HoleCoord(s.x(), s.y() + 9),
                        new HoleCoord(s.x() + 6, s.y() + 9),
                        new HoleCoord(s.x() + 6, s.y() + 8)
                };
                for (HoleCoord p : pads) {
                    if (p.equals(h1) || p.equals(h2)) return true;
                }
            }
        }
        return false;
    }

    public void setOnProjectModified(Runnable callback) {
        this.onProjectModified = callback;
    }

    public void setOnWarningNotification(Consumer<String> callback) {
        this.onWarningNotification = callback;
    }
}
