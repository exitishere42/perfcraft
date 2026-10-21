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
    private boolean visible = true; // Visibility toggle (z. B. für aufgestecktes Modul-Overlay)

    public PlacedComponent() {}

    public PlacedComponent(String id, ComponentType type, String designator, String value,
                           HoleCoord start, HoleCoord end, int rotation, BoardLayer layer) {
        this(id, type, designator, value, start, end, rotation, layer, true);
    }

    public PlacedComponent(String id, ComponentType type, String designator, String value,
                           HoleCoord start, HoleCoord end, int rotation, BoardLayer layer, boolean visible) {
        this.id = id;
        this.type = type;
        this.designator = designator;
        this.value = value;
        this.start = start;
        this.end = end;
        this.rotation = rotation;
        this.layer = layer;
        this.visible = visible;
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
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public void rotate90() {
        this.rotation = (this.rotation + 1) % 4;

        int baseW = 0;
        int baseH = 0;
        if (type != null) {
            switch (type) {
                case ESP32_S3_ZERO -> { baseW = 6; baseH = 8; }
                case TP4056_CHARGER -> { baseW = 6; baseH = 9; }
                case TACTILE_SWITCH -> { baseW = 2; baseH = 3; }
                case IC_DIP -> { baseW = 3; baseH = 3; }
                default -> {}
            }
        }

        if (baseW > 0 && baseH > 0) {
            boolean isSwapped = (rotation == 1 || rotation == 3);
            int curW = isSwapped ? baseH : baseW;
            int curH = isSwapped ? baseW : baseH;
            this.end = new HoleCoord(start.x() + curW, start.y() + curH);
        } else if (end != null) {
            int dx = end.x() - start.x();
            int dy = end.y() - start.y();
            this.end = new HoleCoord(start.x() - dy, start.y() + dx);
        }
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
