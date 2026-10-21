package de.exit.perfcraft.service;

import de.exit.perfcraft.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContinuityServiceTest {

    @Test
    void testWireContinuity() {
        PerfProject project = new PerfProject();
        // Wire 1: (0,0) -> (4,0)
        project.getWires().add(new WireTrace("w1", WireKind.CABLE, BoardLayer.TOP, "#E53935", List.of(new HoleCoord(0, 0), new HoleCoord(4, 0))));
        // Wire 2: (4,0) -> (4,6)
        project.getWires().add(new WireTrace("w2", WireKind.SILVER_WIRE, BoardLayer.BOTTOM, "#CFD8DC", List.of(new HoleCoord(4, 0), new HoleCoord(4, 6))));

        Set<HoleCoord> net = ContinuityService.findConnectedNet(project, new HoleCoord(0, 0));
        assertEquals(3, net.size());
        assertTrue(net.contains(new HoleCoord(0, 0)));
        assertTrue(net.contains(new HoleCoord(4, 0)));
        assertTrue(net.contains(new HoleCoord(4, 6)));
    }

    @Test
    void testTactileSwitchInternalBridge() {
        PerfProject project = new PerfProject();
        // Add switch at (5, 5) -> 1a=(5,5), 1b=(7,5), 2a=(5,8), 2b=(7,8) (3x4 pins grid)
        project.getComponents().add(new PlacedComponent(
                "sw1", ComponentType.TACTILE_SWITCH, "SW1", "6x6",
                new HoleCoord(5, 5), new HoleCoord(7, 8), 0, BoardLayer.TOP
        ));

        // Wire connected to pin 1a
        project.getWires().add(new WireTrace("w1", WireKind.CABLE, BoardLayer.TOP, "#E53935", List.of(new HoleCoord(1, 1), new HoleCoord(5, 5))));

        // Discover net starting at (1, 1) -> must reach bridged pin 1b at (7, 5)
        Set<HoleCoord> net = ContinuityService.findConnectedNet(project, new HoleCoord(1, 1));
        assertTrue(net.contains(new HoleCoord(1, 1)));
        assertTrue(net.contains(new HoleCoord(5, 5)));
        assertTrue(net.contains(new HoleCoord(7, 5))); // Traversed internal switch bridge!
        assertFalse(net.contains(new HoleCoord(5, 8))); // Separate terminal pair
    }
}
