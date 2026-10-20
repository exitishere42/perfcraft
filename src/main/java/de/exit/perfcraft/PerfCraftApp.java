package de.exit.perfcraft;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Main JavaFX application class for PerfCraft.
 */
public class PerfCraftApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("PerfCraft");

        var iconStream = getClass().getResourceAsStream("/icons/icon.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
        }

        StackPane root = new StackPane();
        Label label = new Label("PerfCraft - Perfboard Circuit Designer");
        root.getChildren().add(label);

        Scene scene = new Scene(root, 1024, 768);
        var cssResource = getClass().getResource("/css/material-dark.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
