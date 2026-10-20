package de.exit.perfcraft;

import javafx.application.Application;

/**
 * Standard launcher class to bypass JavaFX module path restrictions in fat jars.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(PerfCraftApp.class, args);
    }
}
