package de.exit.perfcraft.service;

import de.exit.perfcraft.model.PerfProject;
import de.exit.perfcraft.model.PlacedComponent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ExportService {

    public record BomItem(String componentType, String value, List<String> designators, int quantity) {}

    public static List<BomItem> generateBom(PerfProject project) {
        if (project == null || project.getComponents().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<PlacedComponent>> grouped = new LinkedHashMap<>();
        for (PlacedComponent c : project.getComponents()) {
            String key = c.getType().getDisplayName() + "|" + (c.getValue() != null ? c.getValue() : "");
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }

        List<BomItem> items = new ArrayList<>();
        for (List<PlacedComponent> list : grouped.values()) {
            PlacedComponent first = list.getFirst();
            List<String> designators = list.stream().map(PlacedComponent::getDesignator).sorted().toList();
            items.add(new BomItem(first.getType().getDisplayName(), first.getValue(), designators, list.size()));
        }
        return items;
    }

    public static void exportBomToCsv(PerfProject project, File targetFile) throws IOException {
        List<BomItem> items = generateBom(project);
        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
            writer.println("Item;Component;Value;Designators;Quantity");
            int index = 1;
            for (BomItem item : items) {
                String designatorsJoined = String.join(", ", item.designators());
                writer.printf(Locale.ROOT, "%d;\"%s\";\"%s\";\"%s\";%d%n",
                        index++,
                        item.componentType(),
                        item.value() != null ? item.value() : "",
                        designatorsJoined,
                        item.quantity());
            }
        }
    }
}
