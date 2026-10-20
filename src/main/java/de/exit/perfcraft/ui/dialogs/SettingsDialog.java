package de.exit.perfcraft.ui.dialogs;

import de.exit.perfcraft.service.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsDialog {

    public static void show(Stage owner, Runnable onLanguageChanged) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(I18n.get("dialog.settings.title"));

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1E1E1E;");

        Label title = new Label(I18n.get("dialog.settings.title"));
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        HBox langBox = new HBox(12);
        langBox.setAlignment(Pos.CENTER_LEFT);
        Label langLabel = new Label(I18n.get("dialog.settings.lang"));
        langLabel.setStyle("-fx-text-fill: #E0E0E0;");

        ComboBox<I18n.Language> combo = new ComboBox<>();
        combo.getItems().addAll(I18n.Language.EN, I18n.Language.DE);
        combo.setValue(I18n.getLanguage());
        combo.setStyle("-fx-background-color: #242424; -fx-border-color: #2C2C2C;");

        langBox.getChildren().addAll(langLabel, combo);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnSave = new Button(I18n.get("dialog.save"));
        btnSave.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            I18n.setLanguage(combo.getValue());
            if (onLanguageChanged != null) {
                onLanguageChanged.run();
            }
            dialog.close();
        });

        Button btnCancel = new Button(I18n.get("dialog.cancel"));
        btnCancel.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(btnCancel, btnSave);
        root.getChildren().addAll(title, langBox, buttonBox);

        Scene scene = new Scene(root, 360, 180);
        var cssResource = SettingsDialog.class.getResource("/css/material-dark.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
