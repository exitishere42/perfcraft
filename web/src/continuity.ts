import { HoleCoord, PerfProject } from './models.ts';

export function findConnectedNet(project: PerfProject, startHole: HoleCoord): Set<string> {
  const visited = new Set<string>();
  const queue: HoleCoord[] = [startHole];
  const startKey = `${startHole.x},${startHole.y}`;
  visited.add(startKey);

  while (queue.length > 0) {
    const current = queue.shift()!;

    // 1. Check all wire traces
    for (const wire of project.wires) {
      if (wire.points.length >= 2) {
        const p1 = wire.points[0];
        const p2 = wire.points[1];

        if (p1.x === current.x && p1.y === current.y) {
          const key2 = `${p2.x},${p2.y}`;
          if (!visited.has(key2)) {
            visited.add(key2);
            queue.push(p2);
          }
        } else if (p2.x === current.x && p2.y === current.y) {
          const key1 = `${p1.x},${p1.y}`;
          if (!visited.has(key1)) {
            visited.add(key1);
            queue.push(p1);
          }
        }
      }
    }

    // 2. Check components with internal continuity (e.g. Tactile Switch pin pairs)
    for (const comp of project.components) {
      if (comp.type === 'TACTILE_SWITCH') {
        const s = comp.start;
        // Pin 1a: (s.x, s.y) connects internally to 1b: (s.x + 2, s.y)
        // Pin 2a: (s.x, s.y + 3) connects internally to 2b: (s.x + 2, s.y + 3)
        const checkPair = (h1: HoleCoord, h2: HoleCoord) => {
          if (current.x === h1.x && current.y === h1.y) {
            const k = `${h2.x},${h2.y}`;
            if (!visited.has(k)) {
              visited.add(k);
              queue.push(h2);
            }
          } else if (current.x === h2.x && current.y === h2.y) {
            const k = `${h1.x},${h1.y}`;
            if (!visited.has(k)) {
              visited.add(k);
              queue.push(h1);
            }
          }
        };

        checkPair({ x: s.x, y: s.y }, { x: s.x + 2, y: s.y });
        checkPair({ x: s.x, y: s.y + 3 }, { x: s.x + 2, y: s.y + 3 });
      }
    }
  }

  return visited;
}
