package de.exit.perfcraft.service;

import de.exit.perfcraft.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DrcServiceTest {

    @Test
    void testBoundaryViolationDetected() {
        PerfProject project = new PerfProject("Test", new BoardConfig(10, 10, 2.54));
        // Component placed outside board (x=12)
        project.getComponents().add(new PlacedComponent(
                "r1", ComponentType.RESISTOR_AXIAL, "R1", "10k",
                new HoleCoord(8, 2), new HoleCoord(12, 2), 0, BoardLayer.TOP
        ));

        List<DrcService.DrcIssue> issues = DrcService.checkProject(project);
        assertEquals(1, issues.size());
        assertEquals(DrcService.Severity.ERROR, issues.getFirst().severity());
        assertTrue(issues.getFirst().message().contains("beyond the board"));
    }

    @Test
    void testOverlapDetection() {
        PerfProject project = new PerfProject("Test", new BoardConfig(20, 20, 2.54));
        // Two overlapping resistors at same coordinates
        project.getComponents().add(new PlacedComponent(
                "r1", ComponentType.RESISTOR_AXIAL, "R1", "10k",
                new HoleCoord(5, 5), new HoleCoord(9, 5), 0, BoardLayer.TOP
        ));
        project.getComponents().add(new PlacedComponent(
                "r2", ComponentType.RESISTOR_AXIAL, "R2", "470",
                new HoleCoord(6, 5), new HoleCoord(10, 5), 0, BoardLayer.TOP
        ));

        List<DrcService.DrcIssue> issues = DrcService.checkProject(project);
        assertEquals(1, issues.size());
        assertEquals(DrcService.Severity.WARNING, issues.getFirst().severity());
        assertTrue(issues.getFirst().message().contains("overlap"));
    }
}
