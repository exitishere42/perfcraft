package de.exit.perfcraft.ui.dialogs;

import de.exit.perfcraft.model.BoardConfig;
import de.exit.perfcraft.model.PerfProject;
import de.exit.perfcraft.service.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

/**
 * Modern Material Dark Welcome Screen / Hauptmenü where users create a new board with custom dimensions
 * or choose from standard presets before working on circuits.
 */
public class WelcomeDialog {

    public static void show(Stage owner, Consumer<PerfProject> onProjectReady, Runnable onOpenExisting) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(I18n.get("welcome.title"));

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #2C2C2C; -fx-border-width: 1px;");

        // Header mit Logo
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView logo = new ImageView();
        var iconStream = WelcomeDialog.class.getResourceAsStream("/icons/icon.png");
        if (iconStream != null) {
            logo.setImage(new Image(iconStream));
            logo.setFitWidth(40);
            logo.setFitHeight(40);
            logo.setPreserveRatio(true);
        }

        VBox headerText = new VBox(2);
        Label title = new Label(I18n.get("welcome.title"));
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitle = new Label(I18n.get("welcome.subtitle"));
        subtitle.setStyle("-fx-text-fill: #9E9E9E; -fx-font-size: 12px;");
        subtitle.setWrapText(true);
        headerText.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(logo, headerText);

        Separator sep1 = new Separator();

        // Formular: Projektname, Spalten, Zeilen
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        Label lblName = new Label(I18n.get("dialog.new.name"));
        lblName.setStyle("-fx-text-fill: #CFD8DC; -fx-font-size: 12px;");
        TextField txtName = new TextField("Mein-Perfboard");
        txtName.setStyle("-fx-background-color: #242424; -fx-text-fill: white; -fx-border-color: #2C2C2C; -fx-border-radius: 4px;");

        Label lblCols = new Label(I18n.get("dialog.new.cols"));
        lblCols.setStyle("-fx-text-fill: #CFD8DC; -fx-font-size: 12px;");
        Spinner<Integer> spCols = new Spinner<>(5, 200, 24);
        spCols.setEditable(true);
        spCols.setStyle("-fx-background-color: #242424;");

        Label lblRows = new Label(I18n.get("dialog.new.rows"));
        lblRows.setStyle("-fx-text-fill: #CFD8DC; -fx-font-size: 12px;");
        Spinner<Integer> spRows = new Spinner<>(5, 200, 18);
        spRows.setEditable(true);
        spRows.setStyle("-fx-background-color: #242424;");

        grid.add(lblName, 0, 0);
        grid.add(txtName, 1, 0, 3, 1);
        grid.add(lblCols, 0, 1);
        grid.add(spCols, 1, 1);
        grid.add(lblRows, 2, 1);
        grid.add(spRows, 3, 1);

        // Schnellstart-Vorlagen (Presets)
        Label lblPresets = new Label(I18n.get("welcome.recent"));
        lblPresets.setStyle("-fx-text-fill: #78909C; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox presetsBox = new HBox(8);
        Button btnSmall = createPresetButton(I18n.get("welcome.preset.small"), () -> {
            spCols.getValueFactory().setValue(16);
            spRows.getValueFactory().setValue(12);
        });
        Button btnStd = createPresetButton(I18n.get("welcome.preset.standard"), () -> {
            spCols.getValueFactory().setValue(24);
            spRows.getValueFactory().setValue(18);
        });
        btnStd.setStyle("-fx-background-color: #1B2E22; -fx-text-fill: #00C853; -fx-border-color: #00C853; -fx-border-radius: 4px; -fx-cursor: hand; -fx-font-size: 11px;");

        Button btnLarge = createPresetButton(I18n.get("welcome.preset.large"), () -> {
            spCols.getValueFactory().setValue(36);
            spRows.getValueFactory().setValue(24);
        });
        presetsBox.getChildren().addAll(btnSmall, btnStd, btnLarge);

        Separator sep2 = new Separator();

        // Footer Actions
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnOpen = new Button(I18n.get("welcome.open"));
        btnOpen.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-cursor: hand;");
        btnOpen.setOnAction(e -> {
            dialog.close();
            if (onOpenExisting != null) onOpenExisting.run();
        });

        Button btnCreate = new Button(I18n.get("welcome.new"));
        btnCreate.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> {
            String name = txtName.getText().isBlank() ? "Mein-Perfboard" : txtName.getText().trim();
            BoardConfig config = new BoardConfig(spCols.getValue(), spRows.getValue(), 2.54);
            PerfProject project = new PerfProject(name, config);
            if (onProjectReady != null) onProjectReady.accept(project);
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(btnOpen, spacer, btnCreate);

        root.getChildren().addAll(header, sep1, grid, lblPresets, presetsBox, sep2, actions);

        Scene scene = new Scene(root, 480, 320);
        var cssResource = WelcomeDialog.class.getResource("/css/material-dark.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static Button createPresetButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-border-color: #2C2C2C; -fx-border-radius: 4px; -fx-cursor: hand; -fx-font-size: 11px;");
        btn.setOnAction(e -> action.run());
        return btn;
    }
}
