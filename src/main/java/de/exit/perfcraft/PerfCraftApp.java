package de.exit.perfcraft;

import de.exit.perfcraft.model.BoardLayer;
import de.exit.perfcraft.model.ComponentType;
import de.exit.perfcraft.model.PlacedComponent;
import de.exit.perfcraft.model.WireKind;
import de.exit.perfcraft.service.ExportService;
import de.exit.perfcraft.service.I18n;
import de.exit.perfcraft.service.ProjectStorageService;
import de.exit.perfcraft.service.ResistorColorCode;
import de.exit.perfcraft.ui.canvas.PerfBoardCanvas;
import de.exit.perfcraft.ui.dialogs.NewBoardDialog;
import de.exit.perfcraft.ui.dialogs.SettingsDialog;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class PerfCraftApp extends Application {

    private Stage primaryStage;
    private PerfBoardCanvas canvas;
    private ProjectStorageService storageService;

    // UI Nodes for Dynamic Localization
    private Label logoLabel;
    private Button btnNew, btnOpen, btnSave, btnExport, btnSettings;
    private Button btnLayerTop, btnLayerBottom, btnLayerBoth;
    private Button btnToolSelect, btnToolWire, btnToolComp;
    private Button btnWireCable, btnWireSilver;
    private Label lblSection1, lblSection2;
    private Label lblDesignator, lblValue, lblBandsTitle;
    private Label statHoleLabel, statMmLabel, statLayerTitle, statLayerValue, statGridLabel;

    private TextField propDesignatorField;
    private TextField propValueField;
    private VBox resistorPreviewBox;
    private HBox resistorColorStrip;
    private Label resistorOhmLabel;
    private HBox cableColorsBox;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.storageService = new ProjectStorageService();

        primaryStage.setTitle(I18n.get("app.title"));

        var iconStream = getClass().getResourceAsStream("/icons/icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
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
        canvasHolder.widthProperty().addListener((obs, oldV, newV) -> {
            if (!canvas.isInitialFitDone()) {
                canvas.zoomToFit();
            }
            canvas.render();
        });
        canvasHolder.heightProperty().addListener((obs, oldV, newV) -> {
            if (!canvas.isInitialFitDone()) {
                canvas.zoomToFit();
            }
            canvas.render();
        });
        root.setCenter(canvasHolder);

        // 3. LEFT TOOLBAR
        root.setLeft(createLeftToolbar());

        // 4. RIGHT SIDEBAR
        root.setRight(createRightSidebar());

        // 5. BOTTOM STATUSBAR
        root.setBottom(createStatusBar());

        // Callbacks
        canvas.setOnHoverCoordinate((gx, gy) -> {
            statHoleLabel.setText(I18n.get("stat.hole") + " X: " + (gx + 1) + ", Y: " + (gy + 1));
            statMmLabel.setText(I18n.get("stat.pos") + " " + String.format("%.2f", gx * 2.54) + " mm x " + String.format("%.2f", gy * 2.54) + " mm");
        });

        canvas.setOnComponentSelected(this::updateInspector);
        canvas.setOnProjectModified(() -> {
            if (canvas.getSelectedComponent() != null) {
                updateInspector(canvas.getSelectedComponent());
            }
        });

        if (canvas.getSelectedComponent() != null) {
            updateInspector(canvas.getSelectedComponent());
        }

        Scene scene = new Scene(root, 1200, 800);
        var cssResource = getClass().getResource("/css/material-dark.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        // Tastaturkürzel
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case V -> activateTool(PerfBoardCanvas.ActiveTool.SELECT);
                case W -> activateTool(PerfBoardCanvas.ActiveTool.WIRE);
                case C -> activateTool(PerfBoardCanvas.ActiveTool.COMPONENT);
                case R -> canvas.rotateSelected();
                case DELETE -> canvas.deleteSelected();
                default -> {}
            }
        });

        I18n.addListener(lang -> applyLocalization());

        boolean openedFromArgs = false;
        // Check if a project file was passed as argument (e.g. from file association double-click)
        var parameters = getParameters();
        if (parameters != null && !parameters.getRaw().isEmpty()) {
            String initialFile = parameters.getRaw().get(0);
            File f = new File(initialFile);
            if (f.exists() && f.getName().toLowerCase().endsWith(".perf")) {
                try {
                    var proj = storageService.load(f);
                    canvas.setProject(proj);
                    primaryStage.setTitle("PerfCraft - " + proj.getName());
                    openedFromArgs = true;
                } catch (Exception ex) {
                    System.err.println("Failed to load initial file: " + ex.getMessage());
                }
            }
        }

        primaryStage.setScene(scene);
        primaryStage.show();

        // Nutzer-Vorgabe: Am Anfang Hauptmenü / Dialog zur Board-Erstellung anzeigen
        if (!openedFromArgs) {
            javafx.application.Platform.runLater(() -> {
                de.exit.perfcraft.ui.dialogs.WelcomeDialog.show(primaryStage, project -> {
                    canvas.setProject(project);
                    primaryStage.setTitle("PerfCraft - " + project.getName());
                }, this::openProject);
            });
        }
    }

    private HBox createHeaderBar() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 14, 8, 14));
        header.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #2C2C2C; -fx-border-width: 0 0 1 0;");

        // SVG Circuit Board Logo (28x28)
        ImageView logoView = new ImageView();
        var iconStream = getClass().getResourceAsStream("/icons/icon.png");
        if (iconStream != null) {
            logoView.setImage(new Image(iconStream));
            logoView.setFitWidth(28);
            logoView.setFitHeight(28);
            logoView.setPreserveRatio(true);
        }

        logoLabel = new Label("PerfCraft");
        logoLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px;");

        Separator s1 = new Separator(javafx.geometry.Orientation.VERTICAL);

        btnNew = createIconButton(I18n.get("menu.new"));
        btnNew.setOnAction(e -> NewBoardDialog.show(primaryStage, project -> {
            canvas.setProject(project);
            primaryStage.setTitle("PerfCraft - " + project.getName());
        }));

        btnOpen = createIconButton(I18n.get("menu.open"));
        btnOpen.setOnAction(e -> openProject());

        btnSave = createIconButton(I18n.get("menu.save"));
        btnSave.setStyle("-fx-background-color: #242424; -fx-text-fill: #00C853; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold;");
        btnSave.setOnAction(e -> saveProject());

        btnExport = createIconButton(I18n.get("menu.export"));
        btnExport.setOnAction(e -> exportProject());

        Separator s2 = new Separator(javafx.geometry.Orientation.VERTICAL);

        HBox layerBox = new HBox(4);
        layerBox.setPadding(new Insets(2));
        layerBox.setStyle("-fx-background-color: #121212; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        btnLayerTop = createLayerButton(I18n.get("layer.top"), true);
        btnLayerBottom = createLayerButton(I18n.get("layer.bottom"), false);
        btnLayerBoth = createLayerButton(I18n.get("layer.both"), false);

        btnLayerTop.setOnAction(e -> {
            updateLayerButtons(btnLayerTop, btnLayerBottom, btnLayerBoth);
            canvas.setLayer(BoardLayer.TOP);
            statLayerValue.setText(I18n.get("layer.top"));
        });

        btnLayerBottom.setOnAction(e -> {
            updateLayerButtons(btnLayerBottom, btnLayerTop, btnLayerBoth);
            canvas.setLayer(BoardLayer.BOTTOM);
            statLayerValue.setText(I18n.get("layer.bottom"));
        });

        btnLayerBoth.setOnAction(e -> {
            updateLayerButtons(btnLayerBoth, btnLayerTop, btnLayerBottom);
            canvas.setLayer(BoardLayer.BOTH);
            statLayerValue.setText(I18n.get("layer.both"));
        });

        layerBox.getChildren().addAll(btnLayerTop, btnLayerBottom, btnLayerBoth);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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

        btnSettings = createIconButton("⚙ " + I18n.get("menu.settings"));
        btnSettings.setOnAction(e -> SettingsDialog.show(primaryStage, this::applyLocalization));

        header.getChildren().addAll(logoView, logoLabel, s1, btnNew, btnOpen, btnSave, btnExport, s2, layerBox, spacer, btnZoomOut, btnZoomIn, btnSettings);
        return header;
    }

    private VBox createLeftToolbar() {
        VBox bar = new VBox(10);
        bar.setAlignment(Pos.TOP_CENTER);
        bar.setPadding(new Insets(12, 6, 12, 6));
        bar.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #2C2C2C; -fx-border-width: 0 1 0 0;");
        bar.setPrefWidth(68);

        btnToolSelect = createBigToolButton("↖", I18n.get("tool.select"), true);
        btnToolWire = createBigToolButton("〰", I18n.get("tool.wire"), false);
        btnToolComp = createBigToolButton("⌖", I18n.get("tool.component"), false);

        btnToolSelect.setOnAction(e -> activateTool(PerfBoardCanvas.ActiveTool.SELECT));
        btnToolWire.setOnAction(e -> activateTool(PerfBoardCanvas.ActiveTool.WIRE));
        btnToolComp.setOnAction(e -> activateTool(PerfBoardCanvas.ActiveTool.COMPONENT));

        Separator s = new Separator(javafx.geometry.Orientation.HORIZONTAL);
        s.setPadding(new Insets(6, 0, 6, 0));

        Button btnRotate = new Button("↻");
        btnRotate.setTooltip(new Tooltip("Rotate 90° (R)"));
        btnRotate.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-font-size: 14px; -fx-cursor: hand;");
        btnRotate.setOnAction(e -> canvas.rotateSelected());

        Button btnDelete = new Button("✕");
        btnDelete.setTooltip(new Tooltip("Delete (Del)"));
        btnDelete.setStyle("-fx-background-color: #242424; -fx-text-fill: #E53935; -fx-font-size: 12px; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> canvas.deleteSelected());

        bar.getChildren().addAll(btnToolSelect, btnToolWire, btnToolComp, s, btnRotate, btnDelete);
        return bar;
    }

    private void activateTool(PerfBoardCanvas.ActiveTool tool) {
        canvas.setActiveTool(tool);
        btnToolSelect.setStyle(tool == PerfBoardCanvas.ActiveTool.SELECT ? getActiveToolStyle() : getInactiveToolStyle());
        btnToolWire.setStyle(tool == PerfBoardCanvas.ActiveTool.WIRE ? getActiveToolStyle() : getInactiveToolStyle());
        btnToolComp.setStyle(tool == PerfBoardCanvas.ActiveTool.COMPONENT ? getActiveToolStyle() : getInactiveToolStyle());
    }

    private VBox createRightSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(14));
        sidebar.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #2C2C2C; -fx-border-width: 0 0 0 1;");
        sidebar.setPrefWidth(270);

        lblSection1 = new Label(I18n.get("sidebar.components"));
        lblSection1.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px; -fx-font-weight: bold;");

        GridPane compGrid = new GridPane();
        compGrid.setHgap(6);
        compGrid.setVgap(6);

        Button btnSwitch = createCompSelectButton(I18n.get("comp.switch"), "⊡");
        Button btnResistor = createCompSelectButton(I18n.get("comp.resistor"), "〰");
        Button btnCap = createCompSelectButton(I18n.get("comp.capacitor"), "||");
        Button btnLed = createCompSelectButton(I18n.get("comp.led"), "▷|↗");
        Button btnDiode = createCompSelectButton(I18n.get("comp.diode"), "▷|");
        Button btnIc = createCompSelectButton(I18n.get("comp.ic"), "⚏");
        Button btnEsp32 = createCompSelectButton(I18n.get("comp.esp32"), "🖳");

        btnSwitch.setOnAction(e -> { canvas.setActiveComponentType(ComponentType.TACTILE_SWITCH); activateTool(PerfBoardCanvas.ActiveTool.COMPONENT); });
        btnResistor.setOnAction(e -> { canvas.setActiveComponentType(ComponentType.RESISTOR_AXIAL); activateTool(PerfBoardCanvas.ActiveTool.COMPONENT); });
        btnCap.setOnAction(e -> { canvas.setActiveComponentType(ComponentType.CAPACITOR_CERAMIC); activateTool(PerfBoardCanvas.ActiveTool.COMPONENT); });
        btnLed.setOnAction(e -> { canvas.setActiveComponentType(ComponentType.LED); activateTool(PerfBoardCanvas.ActiveTool.COMPONENT); });
        btnDiode.setOnAction(e -> { canvas.setActiveComponentType(ComponentType.DIODE); activateTool(PerfBoardCanvas.ActiveTool.COMPONENT); });
        btnIc.setOnAction(e -> { canvas.setActiveComponentType(ComponentType.IC_DIP); activateTool(PerfBoardCanvas.ActiveTool.COMPONENT); });
        btnEsp32.setOnAction(e -> { canvas.setActiveComponentType(ComponentType.ESP32_S3_ZERO); activateTool(PerfBoardCanvas.ActiveTool.COMPONENT); });

        compGrid.add(btnSwitch, 0, 0);
        compGrid.add(btnResistor, 1, 0);
        compGrid.add(btnCap, 0, 1);
        compGrid.add(btnLed, 1, 1);
        compGrid.add(btnDiode, 0, 2);
        compGrid.add(btnIc, 1, 2);
        compGrid.add(btnEsp32, 0, 3, 2, 1);

        // Leitungs-Auswahl
        VBox wireTypeBox = new VBox(6);
        wireTypeBox.setStyle("-fx-padding: 8 0 0 0; -fx-border-color: #2C2C2C; -fx-border-width: 1 0 0 0;");
        Label lblWireType = new Label("Wire Mode:");
        lblWireType.setStyle("-fx-text-fill: #B0BEC5; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox wireBtnBox = new HBox(6);
        btnWireCable = new Button(I18n.get("wire.cable"));
        btnWireSilver = new Button(I18n.get("wire.silver"));
        btnWireCable.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnWireSilver.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-font-size: 11px; -fx-cursor: hand;");

        btnWireCable.setOnAction(e -> {
            canvas.setActiveWireKind(WireKind.CABLE);
            btnWireCable.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-size: 11px; -fx-font-weight: bold;");
            btnWireSilver.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-font-size: 11px;");
            cableColorsBox.setVisible(true);
            activateTool(PerfBoardCanvas.ActiveTool.WIRE);
        });

        btnWireSilver.setOnAction(e -> {
            canvas.setActiveWireKind(WireKind.SILVER_WIRE);
            btnWireSilver.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-size: 11px; -fx-font-weight: bold;");
            btnWireCable.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-font-size: 11px;");
            cableColorsBox.setVisible(false);
            activateTool(PerfBoardCanvas.ActiveTool.WIRE);
        });

        wireBtnBox.getChildren().addAll(btnWireCable, btnWireSilver);

        // Farb-Palette für Kabel
        cableColorsBox = new HBox(6);
        cableColorsBox.setAlignment(Pos.CENTER_LEFT);
        cableColorsBox.getChildren().addAll(
                createColorDot("#E53935"),
                createColorDot("#212121"),
                createColorDot("#1E88E5"),
                createColorDot("#FDD835"),
                createColorDot("#43A047")
        );

        wireTypeBox.getChildren().addAll(lblWireType, wireBtnBox, cableColorsBox);

        Separator s = new Separator(javafx.geometry.Orientation.HORIZONTAL);

        // 2. EIGENSCHAFTEN
        lblSection2 = new Label(I18n.get("sidebar.properties"));
        lblSection2.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px; -fx-font-weight: bold;");

        lblDesignator = new Label(I18n.get("prop.designator"));
        lblDesignator.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");
        propDesignatorField = new TextField("SW1");
        propDesignatorField.setStyle("-fx-background-color: #242424; -fx-text-fill: white; -fx-border-color: #2C2C2C;");

        lblValue = new Label(I18n.get("prop.value"));
        lblValue.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");
        propValueField = new TextField("6x6 mm");
        propValueField.setStyle("-fx-background-color: #242424; -fx-text-fill: white; -fx-border-color: #2C2C2C;");

        propValueField.textProperty().addListener((obs, oldV, newV) -> {
            if (canvas.getSelectedComponent() != null) {
                canvas.getSelectedComponent().setValue(newV);
                updateResistorColorPreview(newV);
                canvas.render();
            }
        });

        resistorPreviewBox = new VBox(4);
        resistorPreviewBox.setPadding(new Insets(8));
        resistorPreviewBox.setStyle("-fx-background-color: #121212; -fx-border-color: #2C2C2C; -fx-border-radius: 4px;");

        lblBandsTitle = new Label(I18n.get("prop.bands"));
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

        sidebar.getChildren().addAll(lblSection1, compGrid, wireTypeBox, s, lblSection2, lblDesignator, propDesignatorField, lblValue, propValueField, resistorPreviewBox);
        return sidebar;
    }

    private Region createColorDot(String hex) {
        Region r = new Region();
        r.setPrefSize(16, 16);
        r.setStyle("-fx-background-color: " + hex + "; -fx-background-radius: 8px; -fx-cursor: hand; -fx-border-color: #555; -fx-border-radius: 8px;");
        r.setOnMouseClicked(e -> canvas.setActiveWireColor(hex));
        return r;
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

        statHoleLabel = new Label(I18n.get("stat.hole") + " X: 1, Y: 1");
        statHoleLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-family: monospace; -fx-font-size: 11px;");

        statMmLabel = new Label(I18n.get("stat.pos") + " 0.00 mm x 0.00 mm");
        statMmLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-family: monospace; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statLayerTitle = new Label(I18n.get("stat.layer") + " ");
        statLayerTitle.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

        statLayerValue = new Label(I18n.get("layer.top"));
        statLayerValue.setStyle("-fx-text-fill: #00C853; -fx-font-size: 11px; -fx-font-weight: bold;");

        statGridLabel = new Label(I18n.get("stat.grid"));
        statGridLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 11px;");

        bar.getChildren().addAll(statHoleLabel, statMmLabel, spacer, statLayerTitle, statLayerValue, new Label("|") {{ setStyle("-fx-text-fill: #424242;"); }}, statGridLabel);
        return bar;
    }

    private void applyLocalization() {
        primaryStage.setTitle(I18n.get("app.title"));
        btnNew.setText(I18n.get("menu.new"));
        btnOpen.setText(I18n.get("menu.open"));
        btnSave.setText(I18n.get("menu.save"));
        btnExport.setText(I18n.get("menu.export"));
        btnSettings.setText("⚙ " + I18n.get("menu.settings"));

        btnLayerTop.setText(I18n.get("layer.top"));
        btnLayerBottom.setText(I18n.get("layer.bottom"));
        btnLayerBoth.setText(I18n.get("layer.both"));

        btnToolSelect.setText("↖\n" + I18n.get("tool.select"));
        btnToolWire.setText("〰\n" + I18n.get("tool.wire"));
        btnToolComp.setText("⌖\n" + I18n.get("tool.component"));

        btnWireCable.setText(I18n.get("wire.cable"));
        btnWireSilver.setText(I18n.get("wire.silver"));

        lblSection1.setText(I18n.get("sidebar.components"));
        lblSection2.setText(I18n.get("sidebar.properties"));
        lblDesignator.setText(I18n.get("prop.designator"));
        lblValue.setText(I18n.get("prop.value"));
        lblBandsTitle.setText(I18n.get("prop.bands"));

        statGridLabel.setText(I18n.get("stat.grid"));
        statLayerTitle.setText(I18n.get("stat.layer") + " ");
    }

    private void openProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Project");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PerfCraft Project (*.perf)", "*.perf"));
        File file = fc.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                var loaded = storageService.load(file);
                canvas.setProject(loaded);
                primaryStage.setTitle("PerfCraft - " + loaded.getName());
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Could not load file: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void saveProject() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Project");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PerfCraft Project (*.perf)", "*.perf"));
        fc.setInitialFileName(canvas.getProject().getName() + ".perf");
        File file = fc.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                storageService.save(canvas.getProject(), file);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Could not save file: " + e.getMessage()).showAndWait();
            }
        }
    }

    private void exportProject() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("PNG Image", "PNG Image", "CSV Bill of Materials (BOM)");
        dialog.setTitle("Export");
        dialog.setHeaderText("Choose export format");
        var result = dialog.showAndWait();

        if (result.isPresent()) {
            if (result.get().equals("PNG Image")) {
                FileChooser fc = new FileChooser();
                fc.setTitle("Export PNG");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"));
                fc.setInitialFileName(canvas.getProject().getName() + ".png");
                File f = fc.showSaveDialog(primaryStage);
                if (f != null) {
                    try {
                        SnapshotParameters params = new SnapshotParameters();
                        params.setFill(Color.TRANSPARENT);
                        WritableImage image = canvas.snapshot(params, null);
                        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", f);
                    } catch (IOException e) {
                        new Alert(Alert.AlertType.ERROR, "Export error: " + e.getMessage()).showAndWait();
                    }
                }
            } else {
                FileChooser fc = new FileChooser();
                fc.setTitle("Export CSV BOM");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
                fc.setInitialFileName(canvas.getProject().getName() + "_BOM.csv");
                File f = fc.showSaveDialog(primaryStage);
                if (f != null) {
                    try {
                        ExportService.exportBomToCsv(canvas.getProject(), f);
                    } catch (IOException e) {
                        new Alert(Alert.AlertType.ERROR, "Export error: " + e.getMessage()).showAndWait();
                    }
                }
            }
        }
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
        b.setPrefSize(56, 48);
        b.setStyle(active ? getActiveToolStyle() : getInactiveToolStyle());
        return b;
    }

    private String getActiveToolStyle() {
        return "-fx-background-color: #1B4D2E; -fx-text-fill: #00C853; -fx-border-color: #00C853; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-alignment: center; -fx-cursor: hand;";
    }

    private String getInactiveToolStyle() {
        return "-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-border-color: #2C2C2C; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 10px; -fx-alignment: center; -fx-cursor: hand;";
    }

    private Button createCompSelectButton(String name, String icon) {
        Button b = new Button(icon + " " + name);
        b.setPrefWidth(114);
        b.setStyle("-fx-background-color: #242424; -fx-text-fill: #E0E0E0; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-font-size: 11px; -fx-alignment: center-left; -fx-cursor: hand;");
        return b;
    }
}
