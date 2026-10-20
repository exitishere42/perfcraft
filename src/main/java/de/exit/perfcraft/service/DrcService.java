package de.exit.perfcraft.service;

import de.exit.perfcraft.model.HoleCoord;
import de.exit.perfcraft.model.PerfProject;
import de.exit.perfcraft.model.PlacedComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Design Rule Check (DRC) service inspired by KiCad.
 * Validates board boundaries, component clearances, and collision overlaps.
 */
public class DrcService {

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }

    public record DrcIssue(Severity severity, String componentId, String message, HoleCoord location) {}

    public static List<DrcIssue> checkProject(PerfProject project) {
        List<DrcIssue> issues = new ArrayList<>();
        if (project == null || project.getBoard() == null) {
            return issues;
        }

        int cols = project.getBoard().getColumns();
        int rows = project.getBoard().getRows();

        // 1. Check boundary violations
        for (PlacedComponent comp : project.getComponents()) {
            HoleCoord s = comp.getStart();
            HoleCoord e = comp.getEnd() != null ? comp.getEnd() : s;

            if (s.x() < 0 || s.x() >= cols || s.y() < 0 || s.y() >= rows ||
                e.x() < 0 || e.x() >= cols || e.y() < 0 || e.y() >= rows) {
                issues.add(new DrcIssue(
                        Severity.ERROR,
                        comp.getId(),
                        comp.getDesignator() + " extends beyond the board boundaries",
                        s
                ));
            }
        }

        // 2. Check component footprint collisions
        List<PlacedComponent> comps = project.getComponents();
        for (int i = 0; i < comps.size(); i++) {
            for (int j = i + 1; j < comps.size(); j++) {
                PlacedComponent c1 = comps.get(i);
                PlacedComponent c2 = comps.get(j);

                if (isOverlapping(c1, c2)) {
                    issues.add(new DrcIssue(
                            Severity.WARNING,
                            c1.getId(),
                            "Physical overlap between " + c1.getDesignator() + " and " + c2.getDesignator(),
                            c1.getStart()
                    ));
                }
            }
        }

        return issues;
    }

    private static boolean isOverlapping(PlacedComponent c1, PlacedComponent c2) {
        int minX1 = Math.min(c1.getStart().x(), c1.getEnd() != null ? c1.getEnd().x() : c1.getStart().x());
        int maxX1 = Math.max(c1.getStart().x(), c1.getEnd() != null ? c1.getEnd().x() : c1.getStart().x());
        int minY1 = Math.min(c1.getStart().y(), c1.getEnd() != null ? c1.getEnd().y() : c1.getStart().y());
        int maxY1 = Math.max(c1.getStart().y(), c1.getEnd() != null ? c1.getEnd().y() : c1.getStart().y());

        int minX2 = Math.min(c2.getStart().x(), c2.getEnd() != null ? c2.getEnd().x() : c2.getStart().x());
        int maxX2 = Math.max(c2.getStart().x(), c2.getEnd() != null ? c2.getEnd().x() : c2.getStart().x());
        int minY2 = Math.min(c2.getStart().y(), c2.getEnd() != null ? c2.getEnd().y() : c2.getStart().y());
        int maxY2 = Math.max(c2.getStart().y(), c2.getEnd() != null ? c2.getEnd().y() : c2.getStart().y());

        return (minX1 <= maxX2 && maxX1 >= minX2 && minY1 <= maxY2 && maxY1 >= minY2);
    }
}
