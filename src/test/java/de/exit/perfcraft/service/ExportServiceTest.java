package de.exit.perfcraft.service;

import de.exit.perfcraft.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    @Test
    void testBomAggregation() {
        PerfProject project = new PerfProject();
        project.getComponents().add(new PlacedComponent("1", ComponentType.RESISTOR_AXIAL, "R1", "10k", new HoleCoord(0, 0), new HoleCoord(4, 0), 0, BoardLayer.TOP));
        project.getComponents().add(new PlacedComponent("2", ComponentType.RESISTOR_AXIAL, "R2", "10k", new HoleCoord(0, 1), new HoleCoord(4, 1), 0, BoardLayer.TOP));
        project.getComponents().add(new PlacedComponent("3", ComponentType.TACTILE_SWITCH, "SW1", "6x6", new HoleCoord(0, 2), new HoleCoord(2, 3), 0, BoardLayer.TOP));

        var bom = ExportService.generateBom(project);
        assertEquals(2, bom.size());

        var resItem = bom.stream().filter(b -> b.value().equals("10k")).findFirst().orElseThrow();
        assertEquals(2, resItem.quantity());
        assertEquals(List.of("R1", "R2"), resItem.designators());
    }

    @Test
    void testExportBomToCsv(@TempDir Path tempDir) throws IOException {
        PerfProject project = new PerfProject();
        project.getComponents().add(new PlacedComponent("1", ComponentType.TACTILE_SWITCH, "SW1", "6x6", new HoleCoord(0, 0), new HoleCoord(2, 1), 0, BoardLayer.TOP));

        File csvFile = tempDir.resolve("test_bom.csv").toFile();
        ExportService.exportBomToCsv(project, csvFile);

        assertTrue(csvFile.exists());
        String content = Files.readString(csvFile.toPath());
        assertTrue(content.contains("SW1"));
        assertTrue(content.contains("Taster"));
    }
}
