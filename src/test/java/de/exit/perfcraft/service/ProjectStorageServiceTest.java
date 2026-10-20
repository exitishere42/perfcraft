package de.exit.perfcraft.service;

import de.exit.perfcraft.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectStorageServiceTest {

    @Test
    void testSaveAndLoad(@TempDir Path tempDir) throws IOException {
        ProjectStorageService service = new ProjectStorageService();

        PerfProject project = new PerfProject("TestProject", new BoardConfig(20, 15, 2.54));
        project.getComponents().add(new PlacedComponent(
                "c1", ComponentType.TACTILE_SWITCH, "SW1", "6x6 mm",
                new HoleCoord(2, 2), new HoleCoord(4, 3), 0, BoardLayer.TOP
        ));
        project.getComponents().add(new PlacedComponent(
                "c2", ComponentType.RESISTOR_AXIAL, "R1", "10k",
                new HoleCoord(5, 5), new HoleCoord(9, 5), 0, BoardLayer.TOP
        ));
        project.getWires().add(new WireTrace(
                "w1", WireKind.CABLE, BoardLayer.TOP, "#E53935",
                List.of(new HoleCoord(2, 2), new HoleCoord(5, 5))
        ));

        File file = tempDir.resolve("test.perf").toFile();
        service.save(project, file);

        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        PerfProject loaded = service.load(file);
        assertNotNull(loaded);
        assertEquals("TestProject", loaded.getName());
        assertEquals(20, loaded.getBoard().getColumns());
        assertEquals(15, loaded.getBoard().getRows());
        assertEquals(2, loaded.getComponents().size());
        assertEquals(1, loaded.getWires().size());
        assertEquals("SW1", loaded.getComponents().get(0).getDesignator());
        assertEquals(ComponentType.TACTILE_SWITCH, loaded.getComponents().get(0).getType());
    }
}
