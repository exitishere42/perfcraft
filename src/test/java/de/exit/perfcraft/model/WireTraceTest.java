package de.exit.perfcraft.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WireTraceTest {

    @Test
    void testWireDefaults() {
        WireTrace cable = new WireTrace("w1", WireKind.CABLE, BoardLayer.TOP, "#E53935", List.of(new HoleCoord(0, 0), new HoleCoord(5, 5)));
        assertEquals(BoardLayer.TOP, cable.getLayer());
        assertEquals("#E53935", cable.getColor());
        assertEquals(2, cable.getPoints().size());

        WireTrace silver = new WireTrace("w2", WireKind.SILVER_WIRE, BoardLayer.BOTTOM, "#CFD8DC", List.of(new HoleCoord(2, 2), new HoleCoord(2, 3)));
        assertEquals(BoardLayer.BOTTOM, silver.getLayer());
        assertEquals("#CFD8DC", silver.getColor());
    }

    @Test
    void testHoleCoordAdjacency() {
        HoleCoord c1 = new HoleCoord(3, 3);
        assertTrue(c1.isAdjacentTo(new HoleCoord(3, 4)));
        assertTrue(c1.isAdjacentTo(new HoleCoord(4, 3)));
        assertTrue(c1.isAdjacentTo(new HoleCoord(4, 4))); // diagonal
        assertFalse(c1.isAdjacentTo(new HoleCoord(3, 3))); // self
        assertFalse(c1.isAdjacentTo(new HoleCoord(3, 5))); // 2 steps away
    }
}
