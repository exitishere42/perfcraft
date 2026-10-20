package de.exit.perfcraft.service;

import de.exit.perfcraft.model.*;

import java.util.*;

/**
 * Continuity and electrical net analysis (inspired by KiCad net highlighting and Tinkercad wire tracing).
 */
public class ContinuityService {

    /**
     * Traverses all connected pins, wires and pads starting from an origin hole coordinate.
     * Uses Breadth-First Search (BFS) to discover the entire contiguous electrical net.
     */
    public static Set<HoleCoord> findConnectedNet(PerfProject project, HoleCoord start) {
        if (project == null || start == null) {
            return Collections.emptySet();
        }

        Set<HoleCoord> visited = new HashSet<>();
        Queue<HoleCoord> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            HoleCoord current = queue.poll();

            // 1. Check wires connecting to current coordinate
            for (WireTrace wire : project.getWires()) {
                if (wire.getPoints().contains(current)) {
                    for (HoleCoord pt : wire.getPoints()) {
                        if (visited.add(pt)) {
                            queue.add(pt);
                        }
                    }
                }
            }

            // 2. Check components with internal connections (e.g. Tactile Switch paired terminals)
            for (PlacedComponent comp : project.getComponents()) {
                if (comp.getType() == ComponentType.TACTILE_SWITCH) {
                    // Pins: (start.x, start.y) is 1a, (start.x + 2, start.y) is 1b -> internally bridged!
                    // (start.x, start.y + 1) is 2a, (start.x + 2, start.y + 1) is 2b -> internally bridged!
                    HoleCoord p1a = comp.getStart();
                    HoleCoord p1b = new HoleCoord(comp.getStart().x() + 2, comp.getStart().y());
                    HoleCoord p2a = new HoleCoord(comp.getStart().x(), comp.getStart().y() + 1);
                    HoleCoord p2b = new HoleCoord(comp.getStart().x() + 2, comp.getStart().y() + 1);

                    if (current.equals(p1a) && visited.add(p1b)) queue.add(p1b);
                    if (current.equals(p1b) && visited.add(p1a)) queue.add(p1a);
                    if (current.equals(p2a) && visited.add(p2b)) queue.add(p2b);
                    if (current.equals(p2b) && visited.add(p2a)) queue.add(p2a);
                }
            }
        }

        return visited;
    }
}
