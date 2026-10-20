package de.exit.perfcraft.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root project model containing the board configuration, components, and wires.
 */
public class PerfProject {
    private String name = "Unbenanntes-Projekt";
    private BoardConfig board = new BoardConfig();
    private List<PlacedComponent> components = new ArrayList<>();
    private List<WireTrace> wires = new ArrayList<>();

    public PerfProject() {}

    public PerfProject(String name, BoardConfig board) {
        this.name = name;
        this.board = board;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BoardConfig getBoard() { return board; }
    public void setBoard(BoardConfig board) { this.board = board; }
    public List<PlacedComponent> getComponents() { return components; }
    public void setComponents(List<PlacedComponent> components) { this.components = components; }
    public List<WireTrace> getWires() { return wires; }
    public void setWires(List<WireTrace> wires) { this.wires = wires; }
}
