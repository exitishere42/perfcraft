package de.exit.perfcraft.model;

import java.util.Objects;

/**
 * Represents an electronic component placed onto the board.
 */
public class PlacedComponent {
    private String id;
    private ComponentType type;
    private String designator;
    private String value;
    private HoleCoord start;
    private HoleCoord end;
    private int rotation; // 0, 1, 2, 3 (x90 deg)
    private BoardLayer layer = BoardLayer.TOP;

    public PlacedComponent() {}

    public PlacedComponent(String id, ComponentType type, String designator, String value,
                           HoleCoord start, HoleCoord end, int rotation, BoardLayer layer) {
        this.id = id;
        this.type = type;
        this.designator = designator;
        this.value = value;
        this.start = start;
        this.end = end;
        this.rotation = rotation;
        this.layer = layer;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ComponentType getType() { return type; }
    public void setType(ComponentType type) { this.type = type; }
    public String getDesignator() { return designator; }
    public void setDesignator(String designator) { this.designator = designator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public HoleCoord getStart() { return start; }
    public void setStart(HoleCoord start) { this.start = start; }
    public HoleCoord getEnd() { return end; }
    public void setEnd(HoleCoord end) { this.end = end; }
    public int getRotation() { return rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }
    public BoardLayer getLayer() { return layer; }
    public void setLayer(BoardLayer layer) { this.layer = layer; }

    public void rotate90() {
        this.rotation = (this.rotation + 1) % 4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlacedComponent that = (PlacedComponent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
