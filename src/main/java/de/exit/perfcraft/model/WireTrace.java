package de.exit.perfcraft.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a wire or solder connection across hole coordinates.
 */
public class WireTrace {
    private String id;
    private WireKind kind;
    private BoardLayer layer;
    private String color;
    private List<HoleCoord> points = new ArrayList<>();

    public WireTrace() {}

    public WireTrace(String id, WireKind kind, BoardLayer layer, String color, List<HoleCoord> points) {
        this.id = id;
        this.kind = kind;
        this.layer = layer;
        this.color = color;
        if (points != null) this.points.addAll(points);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public WireKind getKind() { return kind; }
    public void setKind(WireKind kind) { this.kind = kind; }
    public BoardLayer getLayer() { return layer; }
    public void setLayer(BoardLayer layer) { this.layer = layer; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<HoleCoord> getPoints() { return points; }
    public void setPoints(List<HoleCoord> points) { this.points = points; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WireTrace wireTrace = (WireTrace) o;
        return Objects.equals(id, wireTrace.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
