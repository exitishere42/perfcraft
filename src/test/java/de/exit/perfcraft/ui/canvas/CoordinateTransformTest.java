package de.exit.perfcraft.ui.canvas;

import de.exit.perfcraft.model.HoleCoord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateTransformTest {

    @Test
    void testGridToScreenAndBack() {
        CoordinateTransform transform = new CoordinateTransform();
        // default: translateX=40, translateY=40, scale=1.0, pitch=2.54, px/mm=3.7795
        double sx = transform.gridToScreenX(5);
        double sy = transform.gridToScreenY(10);

        HoleCoord grid = transform.screenToGrid(sx, sy);
        assertEquals(5, grid.x());
        assertEquals(10, grid.y());
    }

    @Test
    void testZoomClamping() {
        CoordinateTransform transform = new CoordinateTransform();
        // Zooming out excessively should clamp to min 0.2
        transform.applyZoom(0.01, 100, 100);
        assertTrue(transform.getScale() >= 0.2);

        // Zooming in excessively should clamp to max 8.0
        transform.applyZoom(100.0, 100, 100);
        assertTrue(transform.getScale() <= 8.0);
    }

    @Test
    void testPan() {
        CoordinateTransform transform = new CoordinateTransform();
        double initialX = transform.getTranslateX();
        double initialY = transform.getTranslateY();

        transform.pan(25.5, -15.0);
        assertEquals(initialX + 25.5, transform.getTranslateX(), 0.001);
        assertEquals(initialY - 15.0, transform.getTranslateY(), 0.001);
    }
}
