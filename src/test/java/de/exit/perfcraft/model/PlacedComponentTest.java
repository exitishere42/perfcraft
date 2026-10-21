package de.exit.perfcraft.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlacedComponentTest {

    @Test
    void testRotationCycle() {
        PlacedComponent comp = new PlacedComponent(
                "c1", ComponentType.TACTILE_SWITCH, "SW1", "6x6",
                new HoleCoord(2, 2), new HoleCoord(4, 3), 0, BoardLayer.TOP
        );

        assertEquals(0, comp.getRotation());
        comp.rotate90();
        assertEquals(1, comp.getRotation());
        comp.rotate90();
        assertEquals(2, comp.getRotation());
        comp.rotate90();
        assertEquals(3, comp.getRotation());
        comp.rotate90();
        assertEquals(0, comp.getRotation());
    }

    @Test
    void testComponentDefaults() {
        assertEquals("SW", ComponentType.TACTILE_SWITCH.getDefaultPrefix());
        assertEquals("R", ComponentType.RESISTOR_AXIAL.getDefaultPrefix());
        assertEquals("LED", ComponentType.LED.getDefaultPrefix());
        assertEquals("IC", ComponentType.IC_DIP.getDefaultPrefix());
        assertEquals("J", ComponentType.PIN_HEADER.getDefaultPrefix());
    }

    @Test
    void testVisibilityToggle() {
        PlacedComponent comp = new PlacedComponent(
                "c1", ComponentType.ESP32_S3_ZERO, "U1", "ESP32",
                new HoleCoord(0, 0), new HoleCoord(6, 8), 0, BoardLayer.TOP
        );
        assertTrue(comp.isVisible());
        comp.setVisible(false);
        assertFalse(comp.isVisible());
    }
}
