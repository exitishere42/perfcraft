package de.exit.perfcraft;

import de.exit.perfcraft.model.BoardLayer;
import de.exit.perfcraft.model.ComponentType;
import de.exit.perfcraft.model.PlacedComponent;
import de.exit.perfcraft.service.ResistorColorCode;
import de.exit.perfcraft.ui.canvas.PerfBoardCanvas;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class PerfCraftApp extends Application {

    private PerfBoardCanvas canvas;
    private Label statCoordLabel;
    private Label statMmLabel;
    private Label statLayerLabel;
    private TextField propDesignatorField;
    private TextField propValueField;
    private VBox resistorPreviewBox;
    private HBox resistorColorStrip;
    private Label resistorOhmLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("PerfCraft - Perfboard Designer");

        var iconStream = getClass().getResourceAsStream("/icons/icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #121212;");

        // 1. TOP HEADER BAR
        root.setTop(createHeaderBar());

        // 2. CENTER CANVAS
        canvas = new PerfBoardCanvas(850, 650);
        Pane canvasHolder = new Pane(canvas);
        canvasHolder.setStyle("-fx-background-color: #0E0E0E;");
        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvasHolder.widthProperty().addListener((obs, oldV, newV) -> canvas.render());
        canvasHolder.heightProperty().addListener((obs, oldV, newV) -> canvas.render());

        root.setCenter(canvasHolder);

        // 3. LEFT TOOLBAR (Die 3 Kern-Tools)
        root.setLeft(createLeftToolbar());

        // 4. RIGHT SIDEBAR (Bauteil-Wahl & Inspektor)
        root.setRight(createRightSidebar());

        // 5. BOTTOM STATUSBAR
        root.setBottom(createStatusBar());

        // Callbacks verbinden
        canvas.setOnHoverCoordinate((gx, gy) -> {
            statCoordLabel.setText("Loch: X: " + (gx + 1) + ", Y: " + (gy + 1));
            statMmLabel.setText("Pos: " + String.format("%.2f", gx * 2.54) + " mm x " + String.format("%.2f", gy * 2.54) + " mm");
        });

        canvas.setOnComponentSelected(this::updateInspector);

        // Initiales Bauteil im Inspektor anzeigen
        if (canvas.getSelectedComponent() != null) {
            updateInspector(canvas.getSelectedComponent());
        }

        Scene scene = new Scene(root, 1200, 800);
        var cssResource = getClass().getResource("/css/material-dark.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createHeaderBar() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 14, 8, 14));
        header.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #2C2C2C; -fx-border-width: 0 0 1 0;");

        // Logo
        Label logo = new Label("PerfCraft");
        logo.setStyle("-fx-text-fill: #00C853; -fx-font-weight: bold; -fx-font-size: 16px;");

        Separator s1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        // Dateibefehle
        Button btnNew = createIconButton("Neu");
        Button btnOpen = createIconButton("Öffnen");
        Button btnSave = createIconButton("Speichern");

        Separator s2 = new Separator(javafx.geometry.Orientation.VERTICAL);

        // Undo / Redo
        Button btnUndo = createIconButton("↺");
        Button btnRedo = createIconButton("↻");

        // Ebenen-Schalter
        HBox layerBox = new HBox(4);
        layerBox.setPadding(new Insets(2));
        layerBox.setStyle("-fx-background-color: #121212; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        Button btnTop = createLayerButton("Bauteilseite", true);
        Button btnBottom = createLayerButton("Lötseite", false);
        Button btnBoth = createLayerButton("Beide", false);

        btnTop.setOnAction(e -> {
            updateLayerButtons(btnTop, btnBottom, btnBoth);
            canvas.setLayer(BoardLayer.TOP);
            statLayerLabel.setText("Bauteilseite");
        });

        btnBottom.setOnAction(e -> {
            updateLayerButtons(btnBottom, btnTop, btnBoth);
            canvas.setLayer(BoardLayer.BOTTOM);
            statLayerLabel.setText("Lötseite");
        });

        btnBoth.setOnAction(e -> {
            updateLayerButtons(btnBoth, btnTop, btnBottom);
            canvas.setLayer(BoardLayer.BOTH);
            statLayerLabel.setText("Beide (Röntgen)");
        });

        layerBox.getChildren().addAll(btnTop, btnBottom, btnBoth);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Zoom
        Button btnZoomOut = createIconButton("-");
        Button btnZoomIn = createIconButton("+");
        btnZoomOut.setOnAction(e -> {
            canvas.getTransform().applyZoom(0.85, canvas.getWidth()/2, canvas.getHeight()/2);
            canvas.render();
        });
        btnZoomIn.setOnAction(e -> {
            canvas.getTransform().applyZoom(1.15, canvas.getWidth()/2, canvas.getHeight()/2);
            canvas.render();
        });

        header.getChildren().addAll(logo, s1, btnNew, btnOpen, btnSave, s2, btnUndo, btnRedo, layerBox, spacer, btnZoomOut, btnZoomIn);
        return header;
    }

    private VBox createLeftToolbar() {
        VBox bar = new VBox(10);
        bar.setAlignment(Pos.TOP_CENTER);
        bar.setPadding(new Insets(12, 6, 12, 6));
        bar.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #2C2C2C; -fx-border-width: 0 1 0 0;");
        bar.setPrefWidth(64);

        Button btnSelect = createBigToolButton("↖", "Wählen", true);
        Button btnWire = createBigToolButton("〰", "Leitung", false);
        Button btnComp = createBigToolButton("⌖", "Bauteil", false);

        btnSelect.setOnAction(e -> updateToolButtons(btnSelect, btnWire, btnComp));
        btnWire.setOnAction(e -> updateToolButtons(btnWire, btnSelect, btnComp));
        btnComp.setOnAction(e -> updateToolButtons(btnComp, btnSelect, btnWire));

        Separator s = new Separator(javafx.geometry.Orientation.HORIZONTAL);
        s.setPadding(new Insets(6, 0, 6, 0));

        Button btnRotate = new Button("↻");
        btnRotate.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-font-size: 14px; -fx-cursor: hand;");
        btnRotate.setOnAction(e -> {
            if (canvas.getSelectedComponent() != null) {
                canvas.getSelectedComponent().rotate90();
                canvas.render();
            }
        });

        Button btnDelete = new Button("✕");
        btnDelete.setStyle("-fx-background-color: #242424; -fx-text-fill: #E53935; -fx-font-size: 12px; -fx-cursor: hand;");

        bar.getChildren().addAll(btnSelect, btnWire, btnComp, s, btnRotate, btnDelete);
        return bar;
    }

    private VBox createRightSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(14));
        sidebar.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #2C2C2C; -fx-border-width: 0 0 0 1;");
        sidebar.setPrefWidth(260);

        // 1. BAUTEIL-AUSWAHL
        Label lblSection1 = new Label("1. BAUTEIL-AUSWAHL");
        lblSection1.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px; -fx-font-weight: bold;");

        GridPane compGrid = new GridPane();
        compGrid.setHgap(6);
        compGrid.setVgap(6);

        Button btnResistor = createCompSelectButton("Widerstand", "〰");
        Button btnSwitch = createCompSelectButton("Taster", "⊡");
        Button btnCap = createCompSelectButton("Kondensator", "||");
        Button btnLed = createCompSelectButton("LED", "💡");

        btnSwitch.setStyle("-fx-background-color: #242424; -fx-text-fill: #00C853; -fx-border-color: #00C853; -fx-border-radius: 4px;");

        compGrid.add(btnResistor, 0, 0);
        compGrid.add(btnSwitch, 1, 0);
        compGrid.add(btnCap, 0, 1);
        compGrid.add(btnLed, 1, 1);

        Separator s = new Separator(javafx.geometry.Orientation.HORIZONTAL);

        // 2. EIGENSCHAFTEN
        Label lblSection2 = new Label("2. EIGENSCHAFTEN");
        lblSection2.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label lblDesignator = new Label("Bezeichner");
        lblDesignator.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");
        propDesignatorField = new TextField("SW1");
        propDesignatorField.setStyle("-fx-background-color: #242424; -fx-text-fill: white; -fx-border-color: #2C2C2C; -fx-border-radius: 4px;");

        Label lblValue = new Label("Wert / Typ");
        lblValue.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");
        propValueField = new TextField("Taster 6x6");
        propValueField.setStyle("-fx-background-color: #242424; -fx-text-fill: white; -fx-border-color: #2C2C2C; -fx-border-radius: 4px;");

        propValueField.textProperty().addListener((obs, oldV, newV) -> {
            if (canvas.getSelectedComponent() != null) {
                canvas.getSelectedComponent().setValue(newV);
                updateResistorColorPreview(newV);
                canvas.render();
            }
        });

        // Farbbänder Vorschau-Kasten
        resistorPreviewBox = new VBox(4);
        resistorPreviewBox.setPadding(new Insets(8));
        resistorPreviewBox.setStyle("-fx-background-color: #121212; -fx-border-color: #2C2C2C; -fx-border-radius: 4px;");

        Label lblBandsTitle = new Label("Farbringe (IEC 60062):");
        lblBandsTitle.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 10px;");

        resistorColorStrip = new HBox(3);
        resistorColorStrip.setAlignment(Pos.CENTER);
        resistorColorStrip.setPrefHeight(20);
        resistorColorStrip.setStyle("-fx-background-color: #D7CCC8; -fx-border-color: #A1887F; -fx-border-radius: 3px; -fx-background-radius: 3px;");

        resistorOhmLabel = new Label("10 kΩ ±5%");
        resistorOhmLabel.setStyle("-fx-text-fill: #B0BEC5; -fx-font-size: 10px; -fx-font-family: monospace;");

        resistorPreviewBox.getChildren().addAll(lblBandsTitle, resistorColorStrip, resistorOhmLabel);
        resistorPreviewBox.setVisible(false);
        resistorPreviewBox.setManaged(false);

        sidebar.getChildren().addAll(lblSection1, compGrid, s, lblSection2, lblDesignator, propDesignatorField, lblValue, propValueField, resistorPreviewBox);
        return sidebar;
    }

    private void updateInspector(PlacedComponent comp) {
        propDesignatorField.setText(comp.getDesignator());
        propValueField.setText(comp.getValue());

        if (comp.getType() == ComponentType.RESISTOR_AXIAL) {
            resistorPreviewBox.setVisible(true);
            resistorPreviewBox.setManaged(true);
            updateResistorColorPreview(comp.getValue());
        } else {
            resistorPreviewBox.setVisible(false);
            resistorPreviewBox.setManaged(false);
        }
    }

    private void updateResistorColorPreview(String value) {
        resistorColorStrip.getChildren().clear();
        var bands = ResistorColorCode.parseAndCalculate(value);
        if (bands != null) {
            resistorColorStrip.getChildren().addAll(
                    createBandView(bands.band1().getHexColor()),
                    createBandView(bands.band2().getHexColor()),
                    createBandView(bands.multiplier().getHexColor()),
                    new Region() {{ setPrefWidth(8); }},
                    createBandView(bands.tolerance().getHexColor())
            );
            resistorOhmLabel.setText(value + "Ω ±5%");
        }
    }

    private Region createBandView(String hex) {
        Region r = new Region();
        r.setPrefWidth(6);
        r.setPrefHeight(18);
        r.setStyle("-fx-background-color: " + hex + ";");
        return r;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 14, 4, 14));
        bar.setStyle("-fx-background-color: #181818; -fx-border-color: #2C2C2C; -fx-border-width: 1 0 0 0;");

        statCoordLabel = new Label("Loch: X: 1, Y: 1");
        statCoordLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-family: monospace; -fx-font-size: 11px;");

        statMmLabel = new Label("Pos: 0.00 mm x 0.00 mm");
        statMmLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-family: monospace; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statLayerLabel = new Label("Bauteilseite");
        statLayerLabel.setStyle("-fx-text-fill: #00C853; -fx-font-size: 11px; -fx-font-weight: bold;");

        bar.getChildren().addAll(statCoordLabel, statMmLabel, spacer, new Label("Ebene: ") {{ setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;"); }}, statLayerLabel);
        return bar;
    }

    private Button createIconButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-cursor: hand; -fx-font-size: 12px;");
        return b;
    }

    private Button createLayerButton(String text, boolean active) {
        Button b = new Button(text);
        if (active) {
            b.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 3px; -fx-cursor: hand;");
        } else {
            b.setStyle("-fx-background-color: transparent; -fx-text-fill: #B0BEC5; -fx-font-size: 11px; -fx-cursor: hand;");
        }
        return b;
    }

    private void updateLayerButtons(Button active, Button b2, Button b3) {
        active.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 3px; -fx-cursor: hand;");
        b2.setStyle("-fx-background-color: transparent; -fx-text-fill: #B0BEC5; -fx-font-size: 11px; -fx-cursor: hand;");
        b3.setStyle("-fx-background-color: transparent; -fx-text-fill: #B0BEC5; -fx-font-size: 11px; -fx-cursor: hand;");
    }

    private Button createBigToolButton(String icon, String label, boolean active) {
        Button b = new Button(icon + "\n" + label);
        b.setPrefSize(50, 48);
        b.setStyle(active ?
                "-fx-background-color: #1B4D2E; -fx-text-fill: #00C853; -fx-border-color: #00C853; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-alignment: center; -fx-cursor: hand;" :
                "-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-border-color: #2C2C2C; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-alignment: center; -fx-cursor: hand;");
        return b;
    }

    private void updateToolButtons(Button active, Button b2, Button b3) {
        active.setStyle("-fx-background-color: #1B4D2E; -fx-text-fill: #00C853; -fx-border-color: #00C853; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-alignment: center; -fx-cursor: hand;");
        b2.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-border-color: #2C2C2C; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-alignment: center; -fx-cursor: hand;");
        b3.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-border-color: #2C2C2C; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-alignment: center; -fx-cursor: hand;");
    }

    private Button createCompSelectButton(String name, String icon) {
        Button b = new Button(icon + " " + name);
        b.setPrefWidth(110);
        b.setStyle("-fx-background-color: #242424; -fx-text-fill: #E0E0E0; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-font-size: 11px; -fx-alignment: center-left; -fx-cursor: hand;");
        return b;
    }
}
