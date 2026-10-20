package de.exit.perfcraft.ui.dialogs;

import de.exit.perfcraft.model.BoardConfig;
import de.exit.perfcraft.model.PerfProject;
import de.exit.perfcraft.service.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class NewBoardDialog {

    public static void show(Stage owner, Consumer<PerfProject> onProjectCreated) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle(I18n.get("dialog.new.title"));

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1E1E1E;");

        Label title = new Label(I18n.get("dialog.new.title"));
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label lblName = new Label(I18n.get("dialog.new.name"));
        lblName.setStyle("-fx-text-fill: #E0E0E0;");
        TextField txtName = new TextField("MyBoard");
        txtName.setStyle("-fx-background-color: #242424; -fx-text-fill: white; -fx-border-color: #2C2C2C;");

        Label lblCols = new Label(I18n.get("dialog.new.cols"));
        lblCols.setStyle("-fx-text-fill: #E0E0E0;");
        Spinner<Integer> spCols = new Spinner<>(5, 200, 24);
        spCols.setEditable(true);

        Label lblRows = new Label(I18n.get("dialog.new.rows"));
        lblRows.setStyle("-fx-text-fill: #E0E0E0;");
        Spinner<Integer> spRows = new Spinner<>(5, 200, 18);
        spRows.setEditable(true);

        grid.add(lblName, 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(lblCols, 0, 1);
        grid.add(spCols, 1, 1);
        grid.add(lblRows, 0, 2);
        grid.add(spRows, 1, 2);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnCreate = new Button(I18n.get("dialog.create"));
        btnCreate.setStyle("-fx-background-color: #00C853; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> {
            String name = txtName.getText().isBlank() ? "Untitled" : txtName.getText().trim();
            BoardConfig config = new BoardConfig(spCols.getValue(), spRows.getValue(), 2.54);
            PerfProject project = new PerfProject(name, config);
            if (onProjectCreated != null) {
                onProjectCreated.accept(project);
            }
            dialog.close();
        });

        Button btnCancel = new Button(I18n.get("dialog.cancel"));
        btnCancel.setStyle("-fx-background-color: #242424; -fx-text-fill: #B0BEC5; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(btnCancel, btnCreate);
        root.getChildren().addAll(title, grid, buttonBox);

        Scene scene = new Scene(root, 380, 240);
        var cssResource = NewBoardDialog.class.getResource("/css/material-dark.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
