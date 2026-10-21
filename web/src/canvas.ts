import {
  PerfProject,
  PlacedComponent,
  HoleCoord,
  ComponentType,
  WireKind,
  WireTrace,
  BoardLayer,
} from './models.ts';
import { CoordinateTransform } from './transform.ts';
import { parseResistorColorCode } from './resistor.ts';
import { findConnectedNet } from './continuity.ts';
import { HistoryManager } from './history.ts';

export type ActiveTool = 'SELECT' | 'WIRE' | 'COMPONENT';

export class PerfBoardCanvas {
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  private transform: CoordinateTransform;
  private project: PerfProject;

  private activeTool: ActiveTool = 'SELECT';
  private activeWireKind: WireKind = 'CABLE';
  private activeWireColor: string = '#E53935';
  private activeComponentType: ComponentType = 'RESISTOR_AXIAL';
  private activeComponentRotation: number = 0;
  private currentLayer: BoardLayer = 'TOP';

  private selectedComponent: PlacedComponent | null = null;
  private selectedWire: WireTrace | null = null;
  private wireStartHole: HoleCoord | null = null;
  private pinHeaderStartHole: HoleCoord | null = null;
  private currentHoverGrid: HoleCoord | null = null;
  private highlightedNet: Set<string> = new Set();
  private hoveredTerminalText: string | null = null;

  // Touch & Pointer state (Tablet Optimized)
  private activePointers: Map<number, { x: number; y: number }> = new Map();
  private lastPinchDistance: number = 0;
  private lastPinchMidpoint: { x: number; y: number } | null = null;
  private isDraggingObject: boolean = false;
  private dragStartSnapshot: string | null = null;
  private hasObjectMoved: boolean = false;
  private longPressTimeout: number | null = null;
  private longPressStartPos: { x: number; y: number } | null = null;

  // History Manager (Undo / Redo)
  public readonly history: HistoryManager = new HistoryManager();

  // Callbacks
  public onHoverCoordinate?: (gx: number, gy: number) => void;
  public onComponentSelected?: (comp: PlacedComponent | null) => void;
  public onToolChanged?: (tool: ActiveTool) => void;
  public onProjectModified?: () => void;

  constructor(canvas: HTMLCanvasElement, project: PerfProject) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d')!;
    this.project = project;
    this.transform = new CoordinateTransform();

    this.initPointers();
    this.resize();
    window.addEventListener('resize', () => this.resize());
  }

  public resize(): void {
    const rect = this.canvas.parentElement?.getBoundingClientRect() || {
      width: 800,
      height: 600,
    };
    const dpr = window.devicePixelRatio || 1;

    this.canvas.width = rect.width * dpr;
    this.canvas.height = rect.height * dpr;
    this.canvas.style.width = `${rect.width}px`;
    this.canvas.style.height = `${rect.height}px`;

    this.ctx.resetTransform();
    this.ctx.scale(dpr, dpr);

    this.transform.resetView(
      rect.width,
      rect.height,
      this.project.board.columns,
      this.project.board.rows
    );
    this.render();
  }

  private initPointers(): void {
    this.canvas.style.touchAction = 'none'; // Verhindert unerwünschtes Browser-Scrollen auf Tablets
    this.canvas.addEventListener('pointerdown', (e) => this.handlePointerDown(e));
    this.canvas.addEventListener('pointermove', (e) => this.handlePointerMove(e));
    this.canvas.addEventListener('pointerup', (e) => this.handlePointerUp(e));
    this.canvas.addEventListener('pointercancel', (e) => this.handlePointerUp(e));
    this.canvas.addEventListener('contextmenu', (e) => {
      e.preventDefault();
      this.wireStartHole = null;
      this.pinHeaderStartHole = null;
      this.setActiveTool('SELECT');
      if (this.onToolChanged) this.onToolChanged('SELECT');
      this.render();
    });

    this.canvas.addEventListener(
      'wheel',
      (e) => {
        e.preventDefault();
        const factor = e.deltaY < 0 ? 1.15 : 0.87;
        const rect = this.canvas.getBoundingClientRect();
        this.transform.applyZoom(factor, e.clientX - rect.left, e.clientY - rect.top);
        this.render();
      },
      { passive: false }
    );
  }

  private getCanvasPoint(e: PointerEvent): { x: number; y: number } {
    const rect = this.canvas.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    };
  }

  public recordState(): void {
    this.history.pushState(this.project);
  }

  public undo(): void {
    const restored = this.history.undo(this.project);
    if (restored) {
      this.project = restored;
      this.selectedComponent = null;
      this.selectedWire = null;
      this.wireStartHole = null;
      this.pinHeaderStartHole = null;
      this.render();
      if (this.onProjectModified) this.onProjectModified();
    }
  }

  public redo(): void {
    const restored = this.history.redo(this.project);
    if (restored) {
      this.project = restored;
      this.selectedComponent = null;
      this.selectedWire = null;
      this.wireStartHole = null;
      this.pinHeaderStartHole = null;
      this.render();
      if (this.onProjectModified) this.onProjectModified();
    }
  }

  private handlePointerDown(e: PointerEvent): void {
    this.canvas.setPointerCapture(e.pointerId);
    const pt = this.getCanvasPoint(e);
    this.activePointers.set(e.pointerId, pt);

    if (e.button !== 0) {
      // Nur primärer Klick (Linksklick) darf Aktionen ausführen, Rechts-/Mittelklick brechen ab und wechseln zu SELECT
      if (e.button === 2) {
        this.wireStartHole = null;
        this.pinHeaderStartHole = null;
        this.setActiveTool('SELECT');
        if (this.onToolChanged) this.onToolChanged('SELECT');
        this.render();
      }
      return;
    }

    if (this.activePointers.size === 1) {
      // Tablet Long-Press Timer starten (450ms Halten bricht Aktion ab wie Rechtsklick)
      if (e.pointerType === 'touch') {
        this.longPressStartPos = { x: pt.x, y: pt.y };
        if (this.longPressTimeout) clearTimeout(this.longPressTimeout);
        this.longPressTimeout = window.setTimeout(() => {
          if (navigator.vibrate) {
            try { navigator.vibrate(35); } catch (_) {}
          }
          this.wireStartHole = null;
          this.pinHeaderStartHole = null;
          this.setActiveTool('SELECT');
          if (this.onToolChanged) this.onToolChanged('SELECT');
          this.render();
        }, 450);
      }

      // 1-Finger / Linksklick
      const grid = this.transform.screenToGrid(pt.x, pt.y);

      if (e.ctrlKey || e.metaKey) {
        // Net Continuity Highlight
        if (this.highlightedNet.size > 0) {
          this.highlightedNet.clear();
        } else {
          this.highlightedNet = findConnectedNet(this.project, grid);
        }
        this.render();
        return;
      }

      if (this.activeTool === 'SELECT') {
        const hit = this.findComponentAt(grid);
        if (hit) {
          this.selectedComponent = hit;
          this.selectedWire = null;
          this.isDraggingObject = true;
          this.dragStartSnapshot = JSON.stringify(this.project);
          this.hasObjectMoved = false;
          if (this.onComponentSelected) this.onComponentSelected(hit);
        } else {
          const wireHit = this.findWireNear(pt.x, pt.y);
          this.selectedWire = wireHit;
          this.selectedComponent = null;
          this.isDraggingObject = false;
          if (this.onComponentSelected) this.onComponentSelected(null);
        }
        this.render();
      } else if (this.activeTool === 'WIRE') {
        if (!this.wireStartHole) {
          this.wireStartHole = grid;
        } else {
          if (this.wireStartHole.x !== grid.x || this.wireStartHole.y !== grid.y) {
            // Prüfung: TP4056 Lader mit Silberdraht verbunden?
            if (this.activeWireKind === 'SILVER_WIRE' && this.isTp4056Pad(this.wireStartHole, grid)) {
              alert('Warnung: TP4056 Lader darf NICHT mit unisolierter Silber-Drahtbrücke verbunden werden! Bitte isoliertes Kabel verwenden (Kurzschlussgefahr LiPo-Akku).');
              this.wireStartHole = null;
              this.render();
              return;
            }

            this.recordState(); // Undo-Snapshot vor Erstellen des Drahtes
            const wireCol = this.activeWireColor || (this.activeWireKind === 'SILVER_WIRE' ? '#CFD8DC' : '#E53935');
            this.project.wires.push({
              id: 'w_' + Date.now(),
              kind: this.activeWireKind,
              layer: this.activeWireKind === 'SILVER_WIRE' ? 'BOTTOM' : 'TOP',
              color: wireCol,
              points: [this.wireStartHole, grid],
            });
            this.wireStartHole = null;
            if (this.onProjectModified) this.onProjectModified();
          } else {
            this.wireStartHole = null;
          }
        }
        this.render();
      } else if (this.activeTool === 'COMPONENT') {
        if (this.activeComponentType === 'PIN_HEADER') {
          if (!this.pinHeaderStartHole) {
            this.pinHeaderStartHole = grid;
          } else {
            this.recordState(); // Undo-Snapshot vor Pin-Header
            const count = this.project.components.filter((c) => c.type === 'PIN_HEADER').length + 1;
            const pinCount = Math.max(Math.abs(grid.x - this.pinHeaderStartHole.x), Math.abs(grid.y - this.pinHeaderStartHole.y)) + 1;
            const headerComp: PlacedComponent = {
              id: 'c_' + Date.now(),
              type: 'PIN_HEADER',
              designator: 'J' + count,
              value: '1x' + pinCount + ' Header',
              start: this.pinHeaderStartHole,
              end: grid,
              rotation: 0,
              layer: 'TOP',
              visible: true,
            };
            this.project.components.push(headerComp);
            this.selectedComponent = headerComp;
            this.selectedWire = null;
            this.pinHeaderStartHole = null;
            if (this.onComponentSelected) this.onComponentSelected(headerComp);
            if (this.onProjectModified) this.onProjectModified();
          }
        } else {
          this.placeNewComponent(grid);
        }
        this.render();
      }
    } else if (this.activePointers.size === 2) {
      // 2-Finger Tablet Pinch to Zoom & Pan
      if (this.longPressTimeout) {
        clearTimeout(this.longPressTimeout);
        this.longPressTimeout = null;
      }
      this.isDraggingObject = false;
      const pts = Array.from(this.activePointers.values());
      this.lastPinchDistance = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
      this.lastPinchMidpoint = {
        x: (pts[0].x + pts[1].x) / 2,
        y: (pts[0].y + pts[1].y) / 2,
      };
    }
  }

  private handlePointerMove(e: PointerEvent): void {
    if (!this.activePointers.has(e.pointerId)) {
      // Mouse hover (no button pressed)
      const pt = this.getCanvasPoint(e);
      this.currentHoverGrid = this.transform.screenToGrid(pt.x, pt.y);
      if (this.onHoverCoordinate) {
        this.onHoverCoordinate(this.currentHoverGrid.x, this.currentHoverGrid.y);
      }
      this.hoveredTerminalText = this.detectTerminalLabel(this.currentHoverGrid);
      if (this.activeTool === 'COMPONENT' || this.wireStartHole !== null || this.hoveredTerminalText !== null) {
        this.render();
      }
      return;
    }

    const prevPt = this.activePointers.get(e.pointerId)!;
    const pt = this.getCanvasPoint(e);
    this.activePointers.set(e.pointerId, pt);

    if (this.activePointers.size === 1) {
      // 1-Finger drag
      const dx = pt.x - prevPt.x;
      const dy = pt.y - prevPt.y;

      // Wenn der Finger sich bewegt, Long-Press-Timer abbrechen
      if (this.longPressStartPos) {
        const moveDist = Math.hypot(pt.x - this.longPressStartPos.x, pt.y - this.longPressStartPos.y);
        if (moveDist > 8 && this.longPressTimeout) {
          clearTimeout(this.longPressTimeout);
          this.longPressTimeout = null;
        }
      }

      if (e.buttons === 2 || e.buttons === 4 || (e.buttons === 1 && e.altKey)) {
        // Pan
        this.transform.pan(dx, dy);
        this.render();
      } else if (this.activeTool === 'SELECT' && this.isDraggingObject && this.selectedComponent) {
        const grid = this.transform.screenToGrid(pt.x, pt.y);
        const curStart = this.selectedComponent.start;
        if (grid.x !== curStart.x || grid.y !== curStart.y) {
          const shiftX = grid.x - curStart.x;
          const shiftY = grid.y - curStart.y;
          this.selectedComponent.start = grid;
          if (this.selectedComponent.end) {
            this.selectedComponent.end = {
              x: this.selectedComponent.end.x + shiftX,
              y: this.selectedComponent.end.y + shiftY,
            };
          }
          this.hasObjectMoved = true;
          if (this.onProjectModified) this.onProjectModified();
          this.render();
        }
      } else {
        this.currentHoverGrid = this.transform.screenToGrid(pt.x, pt.y);
        this.render();
      }
    } else if (this.activePointers.size === 2) {
      // Stufenloser Tablet Pinch-to-Zoom & Two-Finger-Pan
      const pts = Array.from(this.activePointers.values());
      const currentDist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
      const currentMid = {
        x: (pts[0].x + pts[1].x) / 2,
        y: (pts[0].y + pts[1].y) / 2,
      };

      if (this.lastPinchDistance > 5 && this.lastPinchMidpoint) {
        // 1. Two-Finger Pan Delta
        const panDeltaX = currentMid.x - this.lastPinchMidpoint.x;
        const panDeltaY = currentMid.y - this.lastPinchMidpoint.y;
        this.transform.pan(panDeltaX, panDeltaY);

        // 2. Continuous Pinch Zoom
        const zoomFactor = currentDist / this.lastPinchDistance;
        if (Math.abs(zoomFactor - 1.0) > 0.003) {
          this.transform.applyZoom(zoomFactor, currentMid.x, currentMid.y);
        }
        this.render();
      }

      this.lastPinchDistance = currentDist;
      this.lastPinchMidpoint = currentMid;
    }
  }

  private handlePointerUp(e: PointerEvent): void {
    if (this.longPressTimeout) {
      clearTimeout(this.longPressTimeout);
      this.longPressTimeout = null;
    }
    this.longPressStartPos = null;

    // Wenn ein Objekt verschoben wurde, alten Zustand vor Beginn des Dragging sichern
    if (this.isDraggingObject && this.hasObjectMoved && this.dragStartSnapshot) {
      try {
        const oldState: PerfProject = JSON.parse(this.dragStartSnapshot);
        this.history.pushState(oldState);
      } catch (_) {}
    }
    this.dragStartSnapshot = null;
    this.hasObjectMoved = false;
    this.isDraggingObject = false;

    this.activePointers.delete(e.pointerId);
    if (this.activePointers.size === 0) {
      this.lastPinchDistance = 0;
      this.lastPinchMidpoint = null;
    }
    this.render();
  }

  private findComponentAt(grid: HoleCoord): PlacedComponent | null {
    // Auf der Unterseite: Vorrangig Stiftleisten / Pins prüfen
    for (const c of this.project.components) {
      if (c.type === 'PIN_HEADER') {
        const s = c.start;
        const e = c.end || s;
        const dx = e.x - s.x;
        const dy = e.y - s.y;
        const steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (let i = 0; i <= steps; i++) {
          const hx = steps === 0 ? s.x : s.x + Math.round((i * dx) / steps);
          const hy = steps === 0 ? s.y : s.y + Math.round((i * dy) / steps);
          if (grid.x === hx && grid.y === hy) return c;
        }
      }
    }

    for (const c of this.project.components) {
      if (c.visible === false && this.currentLayer !== 'BOTTOM') continue;
      if (c.type === 'PIN_HEADER') continue; // Bereits oben geprüft

      const minX = Math.min(c.start.x, c.end ? c.end.x : c.start.x);
      const maxX = Math.max(c.start.x, c.end ? c.end.x : c.start.x);
      const minY = Math.min(c.start.y, c.end ? c.end.y : c.start.y);
      const maxY = Math.max(c.start.y, c.end ? c.end.y : c.start.y);

      if (grid.x >= minX - 1 && grid.x <= maxX + 1 && grid.y >= minY - 1 && grid.y <= maxY + 1) {
        return c;
      }
    }
    return null;
  }

  private findWireNear(screenX: number, screenY: number): WireTrace | null {
    const maxDist = Math.max(8.0, this.transform.getHoleSpacingPx() * 0.4);
    for (const wire of this.project.wires) {
      const isSilver = wire.kind === 'SILVER_WIRE';
      const isBottomWire = isSilver || wire.layer === 'BOTTOM';
      if (this.currentLayer === 'TOP' && isBottomWire) continue;
      if (this.currentLayer === 'BOTTOM' && !isBottomWire) continue;

      if (wire.points.length >= 2) {
        const x1 = this.transform.gridToScreenX(wire.points[0].x);
        const y1 = this.transform.gridToScreenY(wire.points[0].y);
        const x2 = this.transform.gridToScreenX(wire.points[1].x);
        const y2 = this.transform.gridToScreenY(wire.points[1].y);

        const dist = this.distancePointToSegment(screenX, screenY, x1, y1, x2, y2);
        if (dist <= maxDist) {
          return wire;
        }
      }
    }
    return null;
  }

  private distancePointToSegment(px: number, py: number, x1: number, y1: number, x2: number, y2: number): number {
    const l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    if (l2 === 0) return Math.hypot(px - x1, py - y1);
    const t = Math.max(0, Math.min(1, ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2));
    const projX = x1 + t * (x2 - x1);
    const projY = y1 + t * (y2 - y1);
    return Math.hypot(px - projX, py - projY);
  }

  private placeNewComponent(start: HoleCoord): void {
    const id = 'c_' + Date.now();
    const count = this.project.components.filter((c) => c.type === this.activeComponentType).length + 1;
    let des = 'C' + count;
    let comp: PlacedComponent;

    switch (this.activeComponentType) {
      case 'TACTILE_SWITCH':
        des = 'SW' + count;
        // 3x4 Pins Raster (3 breit, 4 hoch -> dx=2, dy=3)
        comp = { id, type: this.activeComponentType, designator: des, value: '6x6 mm', start, end: { x: start.x + 2, y: start.y + 3 }, rotation: 0, layer: 'TOP' };
        break;
      case 'TACTILE_SWITCH_SMD':
        des = 'SW' + count;
        // SMD-Taster liegend 6.0 x 3.5 mm mit 5 mm Knebel (3 Raster Spannweite dx=3, dy=0)
        comp = { id, type: this.activeComponentType, designator: des, value: '6x3.5mm', start, end: { x: start.x + 3, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
      case 'RESISTOR_AXIAL':
        des = 'R' + count;
        // 4 Pins verbrauchen -> 4x1 (start bis start.x + 3)
        comp = { id, type: this.activeComponentType, designator: des, value: '10k', start, end: { x: start.x + 3, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
      case 'LED':
        des = 'LED' + count;
        // 2x1 Raster (Breite 2, Höhe 1 -> Anode bei start, Kathode bei start.x + 1)
        comp = { id, type: this.activeComponentType, designator: des, value: 'Red', start, end: { x: start.x + 1, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
      case 'CAPACITOR_CERAMIC':
        des = 'C' + count;
        comp = { id, type: this.activeComponentType, designator: des, value: '100n', start, end: { x: start.x + 1, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
      case 'DIODE':
        des = 'D' + count;
        comp = { id, type: this.activeComponentType, designator: des, value: '1N4148', start, end: { x: start.x + 3, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
      case 'IC_DIP':
        des = 'U' + count;
        comp = { id, type: this.activeComponentType, designator: des, value: 'DIP-8', start, end: { x: start.x + 3, y: start.y + 3 }, rotation: 0, layer: 'TOP' };
        break;
      case 'ESP32_S3_ZERO':
        des = 'U' + count;
        // ESP32-S3-Zero: 7 Spalten x 9 Zeilen (dx=6, dy=8)
        comp = { id, type: this.activeComponentType, designator: des, value: 'ESP32-S3-Zero', start, end: { x: start.x + 6, y: start.y + 8 }, rotation: 0, layer: 'TOP' };
        break;
      case 'TP4056_CHARGER':
        des = 'U' + count;
        // TP4056 Lipo Lader: 7 Spalten x 10 Zeilen (dx=6, dy=9)
        comp = { id, type: this.activeComponentType, designator: des, value: 'TP4056', start, end: { x: start.x + 6, y: start.y + 9 }, rotation: 0, layer: 'TOP' };
        break;
      case 'PIN_HEADER':
        des = 'J' + count;
        comp = { id, type: this.activeComponentType, designator: des, value: '1x4 Header', start, end: { x: start.x + 3, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
      default:
        comp = { id, type: this.activeComponentType, designator: des, value: '10k', start, end: { x: start.x + 2, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
    }

    if (this.activeComponentRotation > 0) {
      comp.rotation = this.activeComponentRotation;
      let baseW = 0;
      let baseH = 0;
      switch (comp.type) {
        case 'ESP32_S3_ZERO': baseW = 6; baseH = 8; break;
        case 'TP4056_CHARGER': baseW = 6; baseH = 9; break;
        case 'TACTILE_SWITCH': baseW = 2; baseH = 3; break;
        case 'IC_DIP': baseW = 3; baseH = 3; break;
        default: break;
      }
      if (baseW > 0 && baseH > 0) {
        const isSwapped = comp.rotation === 90 || comp.rotation === 270;
        const curW = isSwapped ? baseH : baseW;
        const curH = isSwapped ? baseW : baseH;
        comp.end = { x: comp.start.x + curW, y: comp.start.y + curH };
      } else if (comp.end) {
        let dx = comp.end.x - comp.start.x;
        let dy = comp.end.y - comp.start.y;
        const turns = Math.round(this.activeComponentRotation / 90) % 4;
        for (let t = 0; t < turns; t++) {
          const nextDx = -dy;
          const nextDy = dx;
          dx = nextDx;
          dy = nextDy;
        }
        comp.end = { x: comp.start.x + dx, y: comp.start.y + dy };
      }
    }

    this.recordState();
    this.project.components.push(comp);
    this.selectedComponent = comp;
    if (this.onComponentSelected) this.onComponentSelected(comp);
    if (this.onProjectModified) this.onProjectModified();
  }

  public render(): void {
    const rect = this.canvas.parentElement?.getBoundingClientRect() || { width: 800, height: 600 };
    const w = rect.width;
    const h = rect.height;

    this.ctx.fillStyle = '#0E0E0E';
    this.ctx.fillRect(0, 0, w, h);

    const cols = this.project.board.columns;
    const rows = this.project.board.rows;
    const hs = this.transform.getHoleSpacingPx();

    const minGridX = Math.min(this.transform.gridToScreenX(0), this.transform.gridToScreenX(cols - 1));
    const maxGridX = Math.max(this.transform.gridToScreenX(0), this.transform.gridToScreenX(cols - 1));
    const minGridY = this.transform.gridToScreenY(0);
    const maxGridY = this.transform.gridToScreenY(rows - 1);

    // Erweitertes FR4-Platinensubstrat mit viel Platz für Edge-Pads, Silkscreen & Montagelöcher
    const marginX = hs * 1.6;
    const marginY = hs * 2.4;
    const boardX = minGridX - marginX;
    const boardY = minGridY - marginY;
    const boardW = (cols - 1) * hs + marginX * 2;
    const boardH = (rows - 1) * hs + marginY * 2;

    // 1. Platinen-Substrat (Sattes FR4-PCB-Grün)
    this.ctx.fillStyle = '#1B4D2E';
    this.ctx.beginPath();
    this.ctx.roundRect(boardX, boardY, boardW, boardH, 16);
    this.ctx.fill();
    this.ctx.strokeStyle = '#143922';
    this.ctx.lineWidth = 3;
    this.ctx.stroke();

    // 1b. 4 Eck-Montagelöcher (M3 Screw Holes in den Ecken)
    const holeOffsetX = marginX * 0.52;
    const holeOffsetY = marginY * 0.40;
    const mountHoles = [
      { x: boardX + holeOffsetX, y: boardY + holeOffsetY },
      { x: boardX + boardW - holeOffsetX, y: boardY + holeOffsetY },
      { x: boardX + holeOffsetX, y: boardY + boardH - holeOffsetY },
      { x: boardX + boardW - holeOffsetX, y: boardY + boardH - holeOffsetY }
    ];
    const mountRingR = Math.max(5.0, hs * 0.54);
    const mountDrillR = Math.max(2.6, hs * 0.32);

    for (const mh of mountHoles) {
      // Silber-Ring
      this.ctx.fillStyle = '#CFD8DC';
      this.ctx.beginPath();
      this.ctx.arc(mh.x, mh.y, mountRingR, 0, Math.PI * 2);
      this.ctx.fill();
      this.ctx.strokeStyle = '#90A4AE';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      // Bohrung
      this.ctx.fillStyle = '#121212';
      this.ctx.beginPath();
      this.ctx.arc(mh.x, mh.y, mountDrillR, 0, Math.PI * 2);
      this.ctx.fill();
    }

    // 1c. Ovale Rand-Pads (Lötfinger oben und unten – KEINE Überlappung mit Montagelöchern!)
    const padW = Math.max(3.0, hs * 0.48);
    const padH = Math.max(7.0, hs * 0.90);
    const padR = padW / 2;
    const topPadY = minGridY - hs * 2.10;
    const bottomPadY = maxGridY + hs * 1.20;

    for (let c = 0; c < cols; c++) {
      const px = this.transform.gridToScreenX(c);

      // Prüfen, ob oberes Pad mit einem der oberen Montagelöcher kollidieren würde
      const collidesTop = mountHoles.slice(0, 2).some(mh => Math.hypot(px - mh.x, (topPadY + padH / 2) - mh.y) < mountRingR + padW * 0.8);
      if (!collidesTop) {
        this.ctx.fillStyle = '#CFD8DC';
        this.ctx.beginPath();
        this.ctx.roundRect(px - padW / 2, topPadY, padW, padH, padR);
        this.ctx.fill();
        this.ctx.strokeStyle = '#90A4AE';
        this.ctx.lineWidth = 1;
        this.ctx.stroke();
      }

      // Prüfen, ob unteres Pad mit einem der unteren Montagelöcher kollidieren würde
      const collidesBottom = mountHoles.slice(2, 4).some(mh => Math.hypot(px - mh.x, (bottomPadY + padH / 2) - mh.y) < mountRingR + padW * 0.8);
      if (!collidesBottom) {
        this.ctx.fillStyle = '#CFD8DC';
        this.ctx.beginPath();
        this.ctx.roundRect(px - padW / 2, bottomPadY, padW, padH, padR);
        this.ctx.fill();
        this.ctx.strokeStyle = '#90A4AE';
        this.ctx.lineWidth = 1;
        this.ctx.stroke();
      }
    }

    // 1d. Weißer Silkscreen (Trennlinien & Beschriftungen mit perfektem Abstand)
    this.ctx.strokeStyle = 'rgba(255, 255, 255, 0.45)';
    this.ctx.lineWidth = 1;
    this.ctx.beginPath();
    // Obere Silkscreen-Trennlinie zwischen Oval-Pads und Lochmatrix
    this.ctx.moveTo(minGridX - hs * 0.4, minGridY - hs * 0.95);
    this.ctx.lineTo(maxGridX + hs * 0.4, minGridY - hs * 0.95);
    // Untere Silkscreen-Trennlinie
    this.ctx.moveTo(minGridX - hs * 0.4, maxGridY + hs * 0.95);
    this.ctx.lineTo(maxGridX + hs * 0.4, maxGridY + hs * 0.95);
    this.ctx.stroke();

    // Spalten-Buchstaben (oben und unten mittig zwischen Trennlinie und erstem Loch-Pad)
    this.ctx.fillStyle = 'rgba(255, 255, 255, 0.80)';
    this.ctx.font = `600 ${Math.max(8, Math.round(hs * 0.36))}px monospace`;
    this.ctx.textAlign = 'center';
    this.ctx.textBaseline = 'middle';

    for (let c = 0; c < cols; c++) {
      const px = this.transform.gridToScreenX(c);
      let letter = '';
      let n = c;
      while (n >= 0) {
        letter = String.fromCharCode(65 + (n % 26)) + letter;
        n = Math.floor(n / 26) - 1;
      }
      this.ctx.fillText(letter, px, minGridY - hs * 0.62);
      this.ctx.fillText(letter, px, maxGridY + hs * 0.62);
    }

    // Zeilen-Zahlen (links und rechts mit deutlichem Abstand zu den Loch-Pads)
    this.ctx.font = `500 ${Math.max(7, Math.round(hs * 0.32))}px monospace`;
    for (let r = 0; r < rows; r++) {
      if (r === 0 || (r + 1) % 5 === 0 || r === rows - 1) {
        const py = this.transform.gridToScreenY(r);
        const label = String(r + 1);
        this.ctx.textAlign = 'right';
        this.ctx.fillText(label, minGridX - hs * 0.62, py);
        this.ctx.textAlign = 'left';
        this.ctx.fillText(label, maxGridX + hs * 0.62, py);
      }
    }

    // Subtiler FR-4 Typenaufdruck in der freien Ecke (oberhalb oder seitlich der Montagelöcher, niemals über Pads!)
    this.ctx.font = `600 ${Math.max(7, Math.round(hs * 0.32))}px sans-serif`;
    this.ctx.textAlign = 'left';
    this.ctx.fillStyle = 'rgba(255, 255, 255, 0.50)';
    const boardLabel = ((cols === 24 && rows === 18) || (cols === 18 && rows === 24)) ? '5x7CM FR-4' : `${cols}x${rows} FR-4`;
    this.ctx.fillText(boardLabel, boardX + marginX * 1.1, boardY + marginY * 0.22);

    // 2. Silber-Pads (Silber #CFD8DC statt Gold!)
    const holePadR = Math.max(3.0, hs * 0.38);
    const drillR = Math.max(1.5, hs * 0.18);

    for (let c = 0; c < cols; c++) {
      for (let r = 0; r < rows; r++) {
        const px = this.transform.gridToScreenX(c);
        const py = this.transform.gridToScreenY(r);
        const key = `${c},${r}`;

        if (this.highlightedNet.has(key)) {
          this.ctx.fillStyle = '#FFEE58';
          this.ctx.beginPath();
          this.ctx.arc(px, py, holePadR + 3, 0, Math.PI * 2);
          this.ctx.fill();
        }

        // Silber-Pad
        this.ctx.fillStyle = '#CFD8DC';
        this.ctx.beginPath();
        this.ctx.arc(px, py, holePadR, 0, Math.PI * 2);
        this.ctx.fill();
        this.ctx.strokeStyle = '#90A4AE';
        this.ctx.lineWidth = 1;
        this.ctx.stroke();

        // Bohrung
        this.ctx.fillStyle = '#121212';
        this.ctx.beginPath();
        this.ctx.arc(px, py, drillR, 0, Math.PI * 2);
        this.ctx.fill();
      }
    }

    // 3. Leitungen (Kabel und Silberdraht)
    // TOP: Keine Unterseiten-Drähte | BOTTOM: Keine Oberseiten-Drähte | BOTH: Beide transparent
    this.ctx.lineCap = 'round';
    for (const wire of this.project.wires) {
      if (wire.points.length >= 2) {
        const sx1 = this.transform.gridToScreenX(wire.points[0].x);
        const sy1 = this.transform.gridToScreenY(wire.points[0].y);
        const sx2 = this.transform.gridToScreenX(wire.points[1].x);
        const sy2 = this.transform.gridToScreenY(wire.points[1].y);

        const isSilver = wire.kind === 'SILVER_WIRE';
        const isBottomWire = isSilver || wire.layer === 'BOTTOM';

        if (this.currentLayer === 'TOP' && isBottomWire) {
          continue; // Auf TOP absolut nichts von BOTTOM anzeigen
        }
        if (this.currentLayer === 'BOTTOM' && !isBottomWire) {
          continue; // Auf BOTTOM nichts von TOP anzeigen
        }

        if (this.currentLayer === 'BOTH') {
          this.ctx.globalAlpha = 0.60;
        } else {
          this.ctx.globalAlpha = 1.0;
        }

        // Selektions-Aura
        if (wire === this.selectedWire) {
          this.ctx.strokeStyle = '#00E676';
          this.ctx.lineWidth = 7 * this.transform.getScale();
          this.ctx.beginPath();
          this.ctx.moveTo(sx1, sy1);
          this.ctx.lineTo(sx2, sy2);
          this.ctx.stroke();
        }

        if (isSilver) {
          this.ctx.strokeStyle = wire.color || '#CFD8DC';
          this.ctx.lineWidth = 2.2 * this.transform.getScale();
        } else {
          this.ctx.strokeStyle = wire.color || '#E53935';
          this.ctx.lineWidth = 3.5 * this.transform.getScale();
        }
        this.ctx.beginPath();
        this.ctx.moveTo(sx1, sy1);
        this.ctx.lineTo(sx2, sy2);
        this.ctx.stroke();
        this.ctx.globalAlpha = 1.0;
      }
    }

    // Wire live preview rubberband
    if (this.activeTool === 'WIRE' && this.wireStartHole && this.currentHoverGrid) {
      const isSilver = (this.activeWireKind === 'SILVER_WIRE');
      const previewCol = this.activeWireColor || (isSilver ? '#CFD8DC' : '#00C853');
      this.ctx.strokeStyle = previewCol;
      this.ctx.lineWidth = (isSilver ? 1.8 : 3.0) * this.transform.getScale();
      this.ctx.setLineDash([4, 4]);
      this.ctx.beginPath();
      this.ctx.moveTo(this.transform.gridToScreenX(this.wireStartHole.x), this.transform.gridToScreenY(this.wireStartHole.y));
      this.ctx.lineTo(this.transform.gridToScreenX(this.currentHoverGrid.x), this.transform.gridToScreenY(this.currentHoverGrid.y));
      this.ctx.stroke();
      this.ctx.setLineDash([]);
    }

    // 5. Komponenten & Durchsteck-Pins je nach Layer
    if (this.currentLayer === 'BOTTOM') {
      // Unterseite: Ausschließlich Pins und Lötstellen
      for (const comp of this.project.components) {
        this.renderComponentBottomPins(comp, comp === this.selectedComponent);
      }
    } else if (this.currentLayer === 'BOTH') {
      // Beide Seiten: Beide Ebenen semi-transparent überlagert
      this.ctx.save();
      this.ctx.globalAlpha = 0.55;
      for (const comp of this.project.components) {
        this.renderComponentBottomPins(comp, comp === this.selectedComponent);
      }
      this.ctx.restore();

      this.ctx.save();
      this.ctx.globalAlpha = 0.65;
      for (const comp of this.project.components) {
        if (comp.visible === false) continue;
        this.renderComponent(comp, comp === this.selectedComponent);
      }
      this.ctx.restore();
    } else {
      // TOP: Ausschließlich Oberseite, absolut nichts von der Unterseite
      for (const comp of this.project.components) {
        if (comp.visible === false) continue;
        this.renderComponent(comp, comp === this.selectedComponent);
      }
    }

    // 6. Tinkercad-Ghost Preview am Cursor
    if (this.activeTool === 'COMPONENT' && this.currentHoverGrid) {
      this.ctx.save();
      this.ctx.globalAlpha = 0.60;
      if (this.activeComponentType === 'PIN_HEADER') {
        const pStart = this.pinHeaderStartHole || this.currentHoverGrid;
        const dummy: PlacedComponent = {
          id: 'ghost',
          type: 'PIN_HEADER',
          designator: 'J',
          value: 'Header',
          start: pStart,
          end: this.currentHoverGrid,
          rotation: 0,
          layer: 'TOP',
        };
        this.renderPinHeader(dummy, false);
      } else {
        const rot = this.activeComponentRotation;
        const isSwapped = rot === 90 || rot === 270;
        let baseW = 4;
        let baseH = 0;
        if (this.activeComponentType === 'TACTILE_SWITCH') {
          baseW = 2; baseH = 3;
        } else if (this.activeComponentType === 'TACTILE_SWITCH_SMD') {
          baseW = 3; baseH = 0;
        } else if (this.activeComponentType === 'RESISTOR_AXIAL') {
          baseW = 3; baseH = 0;
        } else if (this.activeComponentType === 'LED' || this.activeComponentType === 'CAPACITOR_CERAMIC') {
          baseW = 1; baseH = 0;
        } else if (this.activeComponentType === 'DIODE') {
          baseW = 3; baseH = 0;
        } else if (this.activeComponentType === 'IC_DIP') {
          baseW = 3; baseH = 3;
        } else if (this.activeComponentType === 'ESP32_S3_ZERO') {
          baseW = 6; baseH = 8;
        } else if (this.activeComponentType === 'TP4056_CHARGER') {
          baseW = 6; baseH = 9;
        }

        let dummyEnd: HoleCoord;
        if (baseH > 0) {
          dummyEnd = {
            x: this.currentHoverGrid.x + (isSwapped ? baseH : baseW),
            y: this.currentHoverGrid.y + (isSwapped ? baseW : baseH),
          };
        } else {
          let dx = baseW;
          let dy = 0;
          const turns = Math.round(rot / 90) % 4;
          for (let t = 0; t < turns; t++) {
            const nextDx = -dy;
            const nextDy = dx;
            dx = nextDx;
            dy = nextDy;
          }
          dummyEnd = { x: this.currentHoverGrid.x + dx, y: this.currentHoverGrid.y + dy };
        }

        const dummy: PlacedComponent = {
          id: 'ghost',
          type: this.activeComponentType,
          designator: 'GHOST',
          value: '10k',
          start: this.currentHoverGrid,
          end: dummyEnd,
          rotation: rot,
          layer: 'TOP',
        };
        this.renderComponent(dummy, false);

        // Snap Circles
        this.ctx.strokeStyle = '#00E676';
        this.ctx.lineWidth = 2;
        const sx = this.transform.gridToScreenX(dummy.start.x);
        const sy = this.transform.gridToScreenY(dummy.start.y);
        this.ctx.beginPath();
        this.ctx.arc(sx, sy, hs * 0.45, 0, Math.PI * 2);
        this.ctx.stroke();
      }
      this.ctx.restore();
    }

    // 7. Tinkercad Terminal Badge
    if (this.hoveredTerminalText && this.currentHoverGrid) {
      const hx = this.transform.gridToScreenX(this.currentHoverGrid.x);
      const hy = this.transform.gridToScreenY(this.currentHoverGrid.y);
      this.ctx.fillStyle = '#212121';
      this.ctx.strokeStyle = '#00C853';
      this.ctx.lineWidth = 1;
      const tw = this.hoveredTerminalText.length * 6.5 + 12;
      this.ctx.beginPath();
      this.ctx.roundRect(hx + 10, hy - 18, tw, 20, 4);
      this.ctx.fill();
      this.ctx.stroke();
      this.ctx.fillStyle = '#FFFFFF';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(this.hoveredTerminalText, hx + 16, hy - 4);
    }
  }

  private renderComponent(comp: PlacedComponent, isSelected: boolean): void {
    const hs = this.transform.getHoleSpacingPx();

    if (comp.type === 'TACTILE_SWITCH') {
      const isSwapped = comp.rotation === 90 || comp.rotation === 270;
      const curW = isSwapped ? 3 : 2;
      const curH = isSwapped ? 2 : 3;

      const midGridX = comp.start.x + curW / 2;
      const midGridY = comp.start.y + curH / 2;
      const cx = this.transform.gridToScreenX(midGridX);
      const cy = this.transform.gridToScreenY(midGridY);

      this.ctx.save();
      this.ctx.translate(cx, cy);
      const rotAngle = this.transform.isFlippedX() ? -comp.rotation : comp.rotation;
      this.ctx.rotate((rotAngle * Math.PI) / 180);

      const x0 = -hs * 1.0;
      const x1 = hs * 1.0;
      const y0 = -hs * 1.5;
      const y1 = hs * 1.5;

      // Quadratischer Switch-Körper
      const bodySize = hs * 2.4;
      const bw = bodySize;
      const bh = bodySize;
      const bodyHalf = bodySize / 2;

      // 1. 4 Metall-Pins geschwungen zu den 4 Löchern (3x4 Raster)
      this.ctx.strokeStyle = '#CFD8DC';
      this.ctx.lineWidth = Math.max(2.0, hs * 0.16);
      this.ctx.lineCap = 'round';
      this.ctx.lineJoin = 'round';

      this.ctx.beginPath();
      // Oben links / rechts
      this.ctx.moveTo(x0, y0); this.ctx.lineTo(-bodyHalf + hs * 0.15, -bodyHalf);
      this.ctx.moveTo(x1, y0); this.ctx.lineTo(bodyHalf - hs * 0.15, -bodyHalf);
      // Unten links / rechts
      this.ctx.moveTo(x0, y1); this.ctx.lineTo(-bodyHalf + hs * 0.15, bodyHalf);
      this.ctx.moveTo(x1, y1); this.ctx.lineTo(bodyHalf - hs * 0.15, bodyHalf);
      this.ctx.stroke();

      // Pin Lötkontaktpunkte
      const pinDotR = Math.max(2.5, hs * 0.15);
      this.ctx.fillStyle = '#ECEFF1';
      for (const [px, py] of [[x0, y0], [x1, y0], [x0, y1], [x1, y1]]) {
        this.ctx.beginPath();
        this.ctx.arc(px, py, pinDotR, 0, Math.PI * 2);
        this.ctx.fill();
      }

      // 2. Schwarzes Kunststoffgehäuse (quadratisch)
      this.ctx.fillStyle = '#1E1E1E';
      this.ctx.beginPath();
      this.ctx.roundRect(-bw / 2, -bh / 2, bw, bh, 5);
      this.ctx.fill();
      this.ctx.strokeStyle = '#121212';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      // 3. Silberne quadratische Metallplatte
      const mw = bw * 0.86;
      const mh = bh * 0.86;
      this.ctx.fillStyle = '#B0BEC5';
      this.ctx.beginPath();
      this.ctx.roundRect(-mw / 2, -mh / 2, mw, mh, 4);
      this.ctx.fill();
      this.ctx.strokeStyle = '#78909C';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      // 4 Nieten
      this.ctx.fillStyle = '#37474F';
      const rivetR = Math.max(1.2, hs * 0.08);
      const rxOffset = mw * 0.38;
      const ryOffset = mh * 0.38;
      for (const [ox, oy] of [[-rxOffset, -ryOffset], [rxOffset, -ryOffset], [-rxOffset, ryOffset], [rxOffset, ryOffset]]) {
        this.ctx.beginPath();
        this.ctx.arc(ox, oy, rivetR, 0, Math.PI * 2);
        this.ctx.fill();
      }

      // 4. Zentraler runder schwarzer Betätigungs-Knopf
      const btnR = mw * 0.38;
      this.ctx.fillStyle = '#263238';
      this.ctx.beginPath();
      this.ctx.arc(0, 0, btnR, 0, Math.PI * 2);
      this.ctx.fill();

      this.ctx.fillStyle = '#151515';
      this.ctx.beginPath();
      this.ctx.arc(0, 0, btnR * 0.88, 0, Math.PI * 2);
      this.ctx.fill();

      this.ctx.fillStyle = '#424242';
      this.ctx.beginPath();
      this.ctx.arc(-btnR * 0.3, -btnR * 0.3, btnR * 0.25, 0, Math.PI * 2);
      this.ctx.fill();

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.beginPath();
        this.ctx.roundRect(-bw / 2 - 3, -bh / 2 - 3, bw + 6, bh + 6, 6);
        this.ctx.stroke();
      }

      this.ctx.fillStyle = '#ECEFF1';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(comp.designator, -12, -bh / 2 - 4);
      this.ctx.restore();
    } else if (comp.type === 'TACTILE_SWITCH_SMD') {
      this.renderTactileSwitchSmd(comp, isSelected);
    } else if (comp.type === 'RESISTOR_AXIAL') {
      const x1 = this.transform.gridToScreenX(comp.start.x);
      const y1 = this.transform.gridToScreenY(comp.start.y);
      const x2 = this.transform.gridToScreenX(comp.end ? comp.end.x : comp.start.x + 4);
      const y2 = this.transform.gridToScreenY(comp.end ? comp.end.y : comp.start.y);

      // Leads
      this.ctx.strokeStyle = '#B0BEC5';
      this.ctx.lineWidth = 2.5;
      this.ctx.beginPath();
      this.ctx.moveTo(x1, y1);
      this.ctx.lineTo(x2, y2);
      this.ctx.stroke();

      const midX = (x1 + x2) / 2;
      const midY = (y1 + y2) / 2;
      const bw = hs * 2.5;
      const bh = hs * 0.58;

      const capW = bw * 0.22;
      const capH = bh * 1.18;
      const waistW = bw * 0.56;
      const waistH = bh * 0.82;

      const leftCapX = midX - bw / 2;
      const rightCapX = midX + bw / 2 - capW;
      const waistX = leftCapX + capW;

      // 1. Mittlerer zylindrischer Taillenkörper (warmes Keramik-Beige)
      this.ctx.fillStyle = '#E8D8B8';
      this.ctx.fillRect(waistX, midY - waistH / 2, waistW, waistH);

      // 2. Linke & rechte bauchige Endkappen (Wulst)
      this.ctx.beginPath();
      this.ctx.roundRect(leftCapX, midY - capH / 2, capW + 2, capH, 5);
      this.ctx.fill();

      this.ctx.beginPath();
      this.ctx.roundRect(rightCapX - 2, midY - capH / 2, capW + 2, capH, 5);
      this.ctx.fill();

      // Subtile Konturierung / Schatten für CAD-Plastizität
      this.ctx.strokeStyle = '#BCAAA4';
      this.ctx.lineWidth = 1;
      this.ctx.beginPath();
      this.ctx.roundRect(leftCapX, midY - capH / 2, capW, capH, 5);
      this.ctx.stroke();

      this.ctx.beginPath();
      this.ctx.roundRect(rightCapX, midY - capH / 2, capW, capH, 5);
      this.ctx.stroke();

      this.ctx.beginPath();
      this.ctx.moveTo(waistX, midY - waistH / 2);
      this.ctx.lineTo(waistX + waistW, midY - waistH / 2);
      this.ctx.moveTo(waistX, midY + waistH / 2);
      this.ctx.lineTo(waistX + waistW, midY + waistH / 2);
      this.ctx.stroke();

      // Dezentes oberes Glanzlicht
      this.ctx.strokeStyle = '#FFF8E1';
      this.ctx.lineWidth = 1.2;
      this.ctx.beginPath();
      this.ctx.moveTo(waistX + 2, midY - waistH / 2 + 1.5);
      this.ctx.lineTo(waistX + waistW - 2, midY - waistH / 2 + 1.5);
      this.ctx.stroke();

      // Color bands
      const bands = parseResistorColorCode(comp.value);
      if (bands) {
        const bandW = Math.max(2.2, bw * 0.08);

        const drawBand = (bx: number, by: number, h: number, color: string) => {
          this.ctx.fillStyle = color;
          this.ctx.fillRect(bx, by, bandW, h);
        };

        // Ring 1 (Erste Ziffer auf linker Ausbuchtung)
        drawBand(leftCapX + capW * 0.45, midY - capH / 2, capH, bands.band1);
        // Ring 2 (Zweite Ziffer auf Taillenteil)
        drawBand(waistX + waistW * 0.22, midY - waistH / 2, waistH, bands.band2);
        // Ring 3 (Multiplikator auf Taillenteil)
        drawBand(waistX + waistW * 0.62, midY - waistH / 2, waistH, bands.multiplier);
        // Ring 4 (Toleranz auf rechter Ausbuchtung)
        drawBand(rightCapX + capW * 0.35, midY - capH / 2, capH, bands.tolerance);
      }

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.beginPath();
        this.ctx.roundRect(leftCapX - 3, midY - capH / 2 - 3, bw + 6, capH + 6, 6);
        this.ctx.stroke();
      }
    } else if (comp.type === 'LED') {
      const x1 = this.transform.gridToScreenX(comp.start.x);
      const y1 = this.transform.gridToScreenY(comp.start.y);
      const x2 = this.transform.gridToScreenX(comp.end ? comp.end.x : comp.start.x + 1);
      const y2 = this.transform.gridToScreenY(comp.end ? comp.end.y : comp.start.y);

      // 1. Anschlussdrähte / Beinchen
      this.ctx.strokeStyle = '#B0BEC5';
      this.ctx.lineWidth = 2.2;
      this.ctx.beginPath();
      this.ctx.moveTo(x1, y1);
      this.ctx.lineTo(x2, y2);
      this.ctx.stroke();

      // Pin Löt-Punkte
      const pinR = Math.max(2.2, hs * 0.13);
      this.ctx.fillStyle = '#ECEFF1';
      for (const [px, py] of [[x1, y1], [x2, y2]]) {
        this.ctx.beginPath();
        this.ctx.arc(px, py, pinR, 0, Math.PI * 2);
        this.ctx.fill();
      }

      // 2. LED-Körper zentriert zwischen den 2 Anschlusspads
      const cx = (x1 + x2) / 2;
      const cy = (y1 + y2) / 2;
      const r = hs * 0.44;

      this.ctx.fillStyle = '#E53935';
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, r, 0, Math.PI * 2);
      this.ctx.fill();

      // Kathode
      this.ctx.fillStyle = '#B71C1C';
      this.ctx.fillRect(cx + r * 0.65, cy - r, r * 0.35, r * 2);

      // Glanzpunkt
      this.ctx.fillStyle = '#FF8A80';
      this.ctx.beginPath();
      this.ctx.arc(cx - r * 0.25, cy - r * 0.25, r * 0.25, 0, Math.PI * 2);
      this.ctx.fill();

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(Math.min(x1, x2) - pinR - 3, cy - r - 3, Math.abs(x2 - x1) + (pinR + 3) * 2, (r + 3) * 2);
      }

      this.ctx.fillStyle = '#ECEFF1';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(comp.designator, cx - 12, cy - r - 4);
    } else if (comp.type === 'CAPACITOR_CERAMIC') {
      const px = this.transform.gridToScreenX(comp.start.x);
      const py = this.transform.gridToScreenY(comp.start.y);
      const r = hs * 0.40;
      this.ctx.fillStyle = '#FFA726';
      this.ctx.beginPath();
      this.ctx.roundRect(px - r, py - r, r * 2, r * 2, 4);
      this.ctx.fill();
      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(px - r - 3, py - r - 3, (r + 3) * 2, (r + 3) * 2);
      }
      this.ctx.fillStyle = '#ECEFF1';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(comp.designator, px - 10, py - r - 4);
    } else if (comp.type === 'DIODE') {
      const x1 = this.transform.gridToScreenX(comp.start.x);
      const y1 = this.transform.gridToScreenY(comp.start.y);
      const x2 = this.transform.gridToScreenX(comp.end ? comp.end.x : comp.start.x + 3);
      const y2 = this.transform.gridToScreenY(comp.end ? comp.end.y : comp.start.y);
      this.ctx.strokeStyle = '#B0BEC5';
      this.ctx.lineWidth = 2.0;
      this.ctx.beginPath();
      this.ctx.moveTo(x1, y1);
      this.ctx.lineTo(x2, y2);
      this.ctx.stroke();

      const midX = (x1 + x2) / 2;
      const midY = (y1 + y2) / 2;
      const bw = hs * 1.6;
      const bh = hs * 0.5;
      this.ctx.fillStyle = '#FF7043';
      this.ctx.fillRect(midX - bw / 2, midY - bh / 2, bw, bh);
      this.ctx.fillStyle = '#1A1A1A';
      this.ctx.fillRect(midX + bw / 2 - 4, midY - bh / 2, 4, bh);
      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(midX - bw / 2 - 2, midY - bh / 2 - 2, bw + 4, bh + 4);
      }
      this.ctx.fillStyle = '#ECEFF1';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(comp.designator, midX - 10, midY - bh / 2 - 4);
    } else if (comp.type === 'IC_DIP') {
      const isSwapped = comp.rotation === 90 || comp.rotation === 270;
      const curW = isSwapped ? 3 : 3;
      const curH = isSwapped ? 3 : 3;
      const midGridX = comp.start.x + curW / 2;
      const midGridY = comp.start.y + curH / 2;
      const cx = this.transform.gridToScreenX(midGridX);
      const cy = this.transform.gridToScreenY(midGridY);

      this.ctx.save();
      this.ctx.translate(cx, cy);
      const rotAngle = this.transform.isFlippedX() ? -comp.rotation : comp.rotation;
      this.ctx.rotate((rotAngle * Math.PI) / 180);

      const w = hs * 3.5;
      const h = hs * 3.5;
      this.ctx.fillStyle = '#263238';
      this.ctx.beginPath();
      this.ctx.roundRect(-w / 2, -h / 2, w, h, 6);
      this.ctx.fill();

      // Notch
      this.ctx.fillStyle = '#121212';
      this.ctx.beginPath();
      this.ctx.arc(0, -h / 2, 6, 0, Math.PI * 2);
      this.ctx.fill();

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(-w / 2 - 2, -h / 2 - 2, w + 4, h + 4);
      }
      this.ctx.fillStyle = '#ECEFF1';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(comp.designator + ' (DIP)', -w / 2 + 4, 4);
      this.ctx.restore();
    } else if (comp.type === 'ESP32_S3_ZERO') {
      const isSwapped = comp.rotation === 90 || comp.rotation === 270;
      const curW = isSwapped ? 8 : 6;
      const curH = isSwapped ? 6 : 8;

      const midGridX = comp.start.x + curW / 2;
      const midGridY = comp.start.y + curH / 2;
      const cx = this.transform.gridToScreenX(midGridX);
      const cy = this.transform.gridToScreenY(midGridY);

      this.ctx.save();
      this.ctx.translate(cx, cy);
      const rotAngle = this.transform.isFlippedX() ? -comp.rotation : comp.rotation;
      this.ctx.rotate((rotAngle * Math.PI) / 180);

      const wPx = hs * 6.8;
      const hPx = hs * 8.8;

      // 1. PCB Grundkörper (Waveshare Classic Blue)
      this.ctx.fillStyle = '#0D47A1';
      this.ctx.beginPath();
      this.ctx.roundRect(-wPx / 2, -hPx / 2, wPx, hPx, 6);
      this.ctx.fill();
      this.ctx.strokeStyle = '#1565C0';
      this.ctx.lineWidth = 1.5;
      this.ctx.stroke();

      // 2. USB-C Port oben mittig
      const usbW = hs * 2.6;
      const usbH = hs * 1.5;
      const usbY = -hPx / 2 - hs * 0.2;
      this.ctx.fillStyle = '#B0BEC5';
      this.ctx.beginPath();
      this.ctx.roundRect(-usbW / 2, usbY, usbW, usbH, 4);
      this.ctx.fill();
      this.ctx.strokeStyle = '#78909C';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();
      this.ctx.fillStyle = '#263238';
      this.ctx.beginPath();
      this.ctx.roundRect(-usbW * 0.35, usbY + usbH * 0.25, usbW * 0.7, usbH * 0.45, 2);
      this.ctx.fill();

      // 3. Taster Boot & Reset
      const btnSize = hs * 0.7;
      this.ctx.fillStyle = '#ECEFF1';
      this.ctx.fillRect(-usbW / 2 - btnSize - hs * 0.2, -hPx / 2 + hs * 1.2, btnSize, btnSize);
      this.ctx.fillRect(usbW / 2 + hs * 0.2, -hPx / 2 + hs * 1.2, btnSize, btnSize);
      this.ctx.fillStyle = '#212121';
      this.ctx.beginPath();
      this.ctx.arc(-usbW / 2 - btnSize - hs * 0.2 + btnSize / 2, -hPx / 2 + hs * 1.2 + btnSize / 2, btnSize * 0.3, 0, Math.PI * 2);
      this.ctx.arc(usbW / 2 + hs * 0.2 + btnSize / 2, -hPx / 2 + hs * 1.2 + btnSize / 2, btnSize * 0.3, 0, Math.PI * 2);
      this.ctx.fill();

      // 4. Zentraler ESP32-S3 Chip (45 Grad gedreht)
      const chipMidY = hs * 0.5;
      const chipR = hs * 1.25;

      this.ctx.save();
      this.ctx.translate(0, chipMidY);
      this.ctx.rotate(Math.PI / 4);
      this.ctx.fillStyle = '#212121';
      this.ctx.fillRect(-chipR, -chipR, chipR * 2, chipR * 2);
      this.ctx.strokeStyle = '#424242';
      this.ctx.lineWidth = 1;
      this.ctx.strokeRect(-chipR, -chipR, chipR * 2, chipR * 2);
      this.ctx.fillStyle = '#9E9E9E';
      this.ctx.beginPath();
      this.ctx.arc(0, 0, chipR * 0.4, 0, Math.PI * 2);
      this.ctx.fill();
      this.ctx.restore();

      // 5. Keramik-Antenne unten ("C3")
      const antW = hs * 3.2;
      const antH = hs * 0.9;
      const antX = -antW / 2;
      const antY = hPx / 2 - antH - hs * 0.2;
      this.ctx.fillStyle = '#FAFAFA';
      this.ctx.fillRect(antX, antY, antW, antH);
      this.ctx.strokeStyle = '#BDBDBD';
      this.ctx.lineWidth = 1;
      this.ctx.strokeRect(antX, antY, antW, antH);
      this.ctx.fillStyle = '#D32F2F';
      this.ctx.fillRect(antX + antW * 0.35, antY + 2, antW * 0.3, antH - 4);

      // 6. Goldene Castellated Lötaugen & Pins (je 9 Pins)
      const padR = Math.max(2.8, hs * 0.32);
      const localXLeft = -hs * 3.0;
      const localXRight = hs * 3.0;
      for (let i = 0; i < 9; i++) {
        const localY = -hs * 4.0 + i * hs;
        // Links
        this.ctx.fillStyle = '#FFD54F';
        this.ctx.beginPath();
        this.ctx.arc(localXLeft, localY, padR, 0, Math.PI * 2);
        this.ctx.fill();
        this.ctx.fillStyle = '#121212';
        this.ctx.beginPath();
        this.ctx.arc(localXLeft, localY, padR * 0.5, 0, Math.PI * 2);
        this.ctx.fill();

        // Rechts
        this.ctx.fillStyle = '#FFD54F';
        this.ctx.beginPath();
        this.ctx.arc(localXRight, localY, padR, 0, Math.PI * 2);
        this.ctx.fill();
        this.ctx.fillStyle = '#121212';
        this.ctx.beginPath();
        this.ctx.arc(localXRight, localY, padR * 0.5, 0, Math.PI * 2);
        this.ctx.fill();
      }

      this.ctx.fillStyle = '#FFFFFF';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(comp.designator, -wPx / 2 + 8, -hPx / 2 + 14);
      this.ctx.fillStyle = '#90CAF9';
      this.ctx.fillText('ESP32-S3-Zero', -32, hPx / 2 - 4);

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2.5;
        this.ctx.beginPath();
        this.ctx.roundRect(-wPx / 2 - 4, -hPx / 2 - 4, wPx + 8, hPx + 8, 8);
        this.ctx.stroke();
      }
      this.ctx.restore();
    } else if (comp.type === 'TP4056_CHARGER') {
      const isSwapped = comp.rotation === 90 || comp.rotation === 270;
      const curW = isSwapped ? 9 : 6;
      const curH = isSwapped ? 6 : 9;

      const midGridX = comp.start.x + curW / 2;
      const midGridY = comp.start.y + curH / 2;
      const cx = this.transform.gridToScreenX(midGridX);
      const cy = this.transform.gridToScreenY(midGridY);

      this.ctx.save();
      this.ctx.translate(cx, cy);
      const rotAngle = this.transform.isFlippedX() ? -comp.rotation : comp.rotation;
      this.ctx.rotate((rotAngle * Math.PI) / 180);

      const width = hs * 6.9;
      const height = hs * 9.9;
      const boardLeft = -width / 2;
      const boardTop = -height / 2;

      // 1. PCB Grundkörper (TP4056 Blau)
      this.ctx.fillStyle = '#1565C0';
      this.ctx.beginPath();
      this.ctx.roundRect(boardLeft, boardTop, width, height, 6);
      this.ctx.fill();
      this.ctx.strokeStyle = '#1E88E5';
      this.ctx.lineWidth = 1.5;
      this.ctx.stroke();

      // 2. USB-C Eingang oben mittig
      const usbW = hs * 2.8;
      const usbH = hs * 1.6;
      const usbX = -usbW / 2;
      const usbY = boardTop - hs * 0.1;
      this.ctx.fillStyle = '#CFD8DC';
      this.ctx.beginPath();
      this.ctx.roundRect(usbX, usbY, usbW, usbH, 4);
      this.ctx.fill();
      this.ctx.strokeStyle = '#90A4AE';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();
      this.ctx.fillStyle = '#263238';
      this.ctx.beginPath();
      this.ctx.roundRect(usbX + usbW * 0.15, usbY + usbH * 0.25, usbW * 0.7, usbH * 0.45, 2);
      this.ctx.fill();

      // 3. Haupt-IC TP4056
      const tpY = boardTop + hs * 3.65;
      const tpW = hs * 2.2;
      const tpH = hs * 1.6;
      this.ctx.fillStyle = '#212121';
      this.ctx.beginPath();
      this.ctx.roundRect(-tpW / 2, tpY - tpH / 2, tpW, tpH, 2);
      this.ctx.fill();
      this.ctx.fillStyle = '#B0BEC5';
      this.ctx.font = '10px monospace';
      this.ctx.fillText('4056', -11, tpY + 4);

      // 4. Schutz-IC DW01A
      const protY = boardTop + hs * 6.25;
      this.ctx.fillStyle = '#212121';
      this.ctx.beginPath();
      this.ctx.roundRect(-hs * 0.9, protY, hs * 1.8, hs * 2.0, 2);
      this.ctx.fill();

      // LEDs
      this.ctx.fillStyle = '#E53935';
      this.ctx.beginPath();
      this.ctx.arc(-hs * 1.2, boardTop + hs * 2.45, hs * 0.2, 0, Math.PI * 2);
      this.ctx.fill();
      this.ctx.fillStyle = '#1E88E5';
      this.ctx.beginPath();
      this.ctx.arc(hs * 1.2, boardTop + hs * 2.45, hs * 0.2, 0, Math.PI * 2);
      this.ctx.fill();

      // 5. Die 6 Löt-Pads (IN+, IN-, OUT+, B+, B-, OUT-)
      const padR = Math.max(3.0, hs * 0.35);
      const localXLeft = -hs * 3.0;
      const localXRight = hs * 3.0;
      const localYTop = -hs * 4.5;
      const pads = [
        [localXLeft, localYTop],
        [localXRight, localYTop],
        [localXLeft, localYTop + hs * 8.0],
        [localXLeft, localYTop + hs * 9.0],
        [localXRight, localYTop + hs * 9.0],
        [localXRight, localYTop + hs * 8.0],
      ];
      for (const [px, py] of pads) {
        this.ctx.fillStyle = '#FFD54F';
        this.ctx.beginPath();
        this.ctx.arc(px, py, padR, 0, Math.PI * 2);
        this.ctx.fill();
        this.ctx.fillStyle = '#121212';
        this.ctx.beginPath();
        this.ctx.arc(px, py, padR * 0.5, 0, Math.PI * 2);
        this.ctx.fill();
      }

      // Beschriftung Silkscreen
      this.ctx.fillStyle = '#FFFFFF';
      this.ctx.font = '10px sans-serif';
      this.ctx.fillText('+ IN', localXLeft + 6, localYTop + 4);
      this.ctx.fillText('IN -', localXRight - 24, localYTop + 4);
      this.ctx.fillText('OUT+', localXLeft + 6, localYTop + hs * 8.0 + 4);
      this.ctx.fillText('B+', localXLeft + 6, localYTop + hs * 9.0 + 4);
      this.ctx.fillText('B-', localXRight - 18, localYTop + hs * 9.0 + 4);
      this.ctx.fillText('OUT-', localXRight - 30, localYTop + hs * 8.0 + 4);
      this.ctx.fillText(`${comp.designator} (TP4056)`, boardLeft + 8, boardTop + 14);

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2.5;
        this.ctx.beginPath();
        this.ctx.roundRect(boardLeft - 4, boardTop - 4, width + 8, height + 8, 8);
        this.ctx.stroke();
      }
      this.ctx.restore();
    } else if (comp.type === 'PIN_HEADER') {
      this.renderPinHeader(comp, isSelected);
    }
  }

  private renderPinHeader(comp: PlacedComponent, isSelected: boolean): void {
    const s = comp.start;
    const e = comp.end || s;
    const hs = this.transform.getHoleSpacingPx();

    const dx = e.x - s.x;
    const dy = e.y - s.y;
    const steps = Math.max(Math.abs(dx), Math.abs(dy));

    const boxSize = hs * 0.90;
    const pinSize = hs * 0.30;
    const drillSize = hs * 0.14;

    for (let i = 0; i <= steps; i++) {
      const hx = steps === 0 ? s.x : s.x + Math.round((i * dx) / steps);
      const hy = steps === 0 ? s.y : s.y + Math.round((i * dy) / steps);

      const cx = this.transform.gridToScreenX(hx);
      const cy = this.transform.gridToScreenY(hy);

      // 1. Schwarzer Plastiksockel
      this.ctx.fillStyle = '#1E1E1E';
      this.ctx.beginPath();
      this.ctx.roundRect(cx - boxSize / 2, cy - boxSize / 2, boxSize, boxSize, 3);
      this.ctx.fill();
      this.ctx.strokeStyle = '#121212';
      this.ctx.lineWidth = 1.0;
      this.ctx.stroke();

      // Innerer Fasenfalz
      this.ctx.strokeStyle = '#2E2E2E';
      this.ctx.lineWidth = 0.8;
      this.ctx.beginPath();
      this.ctx.roundRect(cx - boxSize * 0.4, cy - boxSize * 0.4, boxSize * 0.8, boxSize * 0.8, 2);
      this.ctx.stroke();

      // 2. Quadratischer vergoldeter Pin-Kontakt
      this.ctx.fillStyle = '#FFD54F';
      this.ctx.fillRect(cx - pinSize / 2, cy - pinSize / 2, pinSize, pinSize);
      this.ctx.strokeStyle = '#FFA000';
      this.ctx.lineWidth = 0.6;
      this.ctx.strokeRect(cx - pinSize / 2, cy - pinSize / 2, pinSize, pinSize);

      // Mittiges Loch
      this.ctx.fillStyle = '#121212';
      this.ctx.fillRect(cx - drillSize / 2, cy - drillSize / 2, drillSize, drillSize);

      if (isSelected) {
        this.ctx.strokeStyle = '#00E676';
        this.ctx.lineWidth = 2.0;
        this.ctx.beginPath();
        this.ctx.roundRect(cx - boxSize / 2 - 2, cy - boxSize / 2 - 2, boxSize + 4, boxSize + 4, 4);
        this.ctx.stroke();
      }
    }

    // Beschriftung
    const sx0 = this.transform.gridToScreenX(s.x);
    const sy0 = this.transform.gridToScreenY(s.y);
    this.ctx.fillStyle = '#B0BEC5';
    this.ctx.font = '11px sans-serif';
    this.ctx.fillText(comp.designator, sx0 - boxSize * 0.6, sy0 - boxSize * 0.6);
  }

  private findMountedModulePinLabel(grid: HoleCoord): string | null {
    for (const c of this.project.components) {
      if (c.type === 'ESP32_S3_ZERO') {
        const s = c.start;
        if (grid.x === s.x) {
          const dy = grid.y - s.y;
          const leftLabels = ['5V', 'GND', '3V3', 'GP1', 'GP2', 'GP3', 'GP4', 'GP5', 'GP6'];
          if (dy >= 0 && dy < leftLabels.length) return `ESP32: ${leftLabels[dy]}`;
        }
        if (grid.x === s.x + 6) {
          const dy = grid.y - s.y;
          const rightLabels = ['TX (GP43)', 'RX (GP44)', 'GP13', 'GP12', 'GP11', 'GP10', 'GP9', 'GP8', 'GP7'];
          if (dy >= 0 && dy < rightLabels.length) return `ESP32: ${rightLabels[dy]}`;
        }
      } else if (c.type === 'TP4056_CHARGER') {
        const s = c.start;
        if (grid.x === s.x && grid.y === s.y) return 'TP4056: IN+';
        if (grid.x === s.x + 6 && grid.y === s.y) return 'TP4056: IN-';
        if (grid.x === s.x && grid.y === s.y + 8) return 'TP4056: OUT+';
        if (grid.x === s.x && grid.y === s.y + 9) return 'TP4056: B+';
        if (grid.x === s.x + 6 && grid.y === s.y + 9) return 'TP4056: B-';
        if (grid.x === s.x + 6 && grid.y === s.y + 8) return 'TP4056: OUT-';
      }
    }
    return null;
  }

  private detectTerminalLabel(grid: HoleCoord): string | null {
    for (const c of this.project.components) {
      if (c.visible === false && this.currentLayer !== 'BOTTOM') continue;
      if (c.type === 'PIN_HEADER') {
        const s = c.start;
        const e = c.end || s;
        const dx = e.x - s.x;
        const dy = e.y - s.y;
        const steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (let i = 0; i <= steps; i++) {
          const hx = steps === 0 ? s.x : s.x + Math.round((i * dx) / steps);
          const hy = steps === 0 ? s.y : s.y + Math.round((i * dy) / steps);
          if (grid.x === hx && grid.y === hy) {
            const mod = this.findMountedModulePinLabel(grid);
            if (mod) {
              return `${c.designator}: Pin ${i + 1} (${mod})`;
            }
            return `${c.designator}: Pin ${i + 1}`;
          }
        }
      } else if (c.type === 'TACTILE_SWITCH') {
        const s = c.start;
        if (grid.x === s.x && grid.y === s.y) return c.designator + ': Pin 1a';
        if (grid.x === s.x + 2 && grid.y === s.y) return c.designator + ': Pin 1b';
        if (grid.x === s.x && grid.y === s.y + 3) return c.designator + ': Pin 2a';
        if (grid.x === s.x + 2 && grid.y === s.y + 3) return c.designator + ': Pin 2b';
      } else if (c.type === 'RESISTOR_AXIAL') {
        if (grid.x === c.start.x && grid.y === c.start.y) return c.designator + ': Terminal 1';
        if (c.end && grid.x === c.end.x && grid.y === c.end.y) return c.designator + ': Terminal 2';
      } else if (c.type === 'LED') {
        if (grid.x === c.start.x && grid.y === c.start.y) return c.designator + ': Anode (+)';
        if (c.end && grid.x === c.end.x && grid.y === c.end.y) return c.designator + ': Cathode (-)';
      } else if (c.type === 'ESP32_S3_ZERO') {
        const s = c.start;
        // Links
        if (grid.x === s.x) {
          const dy = grid.y - s.y;
          const leftLabels = ['5V', 'GND', '3V3', 'GP1', 'GP2', 'GP3', 'GP4', 'GP5', 'GP6'];
          if (dy >= 0 && dy < leftLabels.length) return `${c.designator} (ESP32): ${leftLabels[dy]}`;
        }
        // Rechts
        if (grid.x === s.x + 6) {
          const dy = grid.y - s.y;
          const rightLabels = ['TX (GP43)', 'RX (GP44)', 'GP13', 'GP12', 'GP11', 'GP10', 'GP9', 'GP8', 'GP7'];
          if (dy >= 0 && dy < rightLabels.length) return `${c.designator} (ESP32): ${rightLabels[dy]}`;
        }
      } else if (c.type === 'TP4056_CHARGER') {
        const s = c.start;
        if (grid.x === s.x && grid.y === s.y) return `${c.designator} (TP4056): IN+ (4.5V-5.5V)`;
        if (grid.x === s.x + 6 && grid.y === s.y) return `${c.designator} (TP4056): IN- (GND)`;
        if (grid.x === s.x && grid.y === s.y + 8) return `${c.designator} (TP4056): OUT+`;
        if (grid.x === s.x && grid.y === s.y + 9) return `${c.designator} (TP4056): B+ (Batterie +)`;
        if (grid.x === s.x + 6 && grid.y === s.y + 9) return `${c.designator} (TP4056): B- (Batterie -)`;
        if (grid.x === s.x + 6 && grid.y === s.y + 8) return `${c.designator} (TP4056): OUT-`;
      }
    }
    return null;
  }

  private drawSolderPin(cx: number, cy: number, hs: number): void {
    const padR = Math.max(3.2, hs * 0.4);
    const pinSize = hs * 0.38;

    // 1. Zinn-Lötkelch
    this.ctx.fillStyle = '#B0BEC5';
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, padR * 0.7, 0, Math.PI * 2);
    this.ctx.fill();

    // 2. Quadratischer Metall-Pin
    this.ctx.fillStyle = '#ECEFF1';
    this.ctx.fillRect(cx - pinSize / 2, cy - pinSize / 2, pinSize, pinSize);
    this.ctx.strokeStyle = '#78909C';
    this.ctx.lineWidth = 0.8;
    this.ctx.strokeRect(cx - pinSize / 2, cy - pinSize / 2, pinSize, pinSize);

    // 3. Glanzpunkt
    this.ctx.fillStyle = '#FFFFFF';
    this.ctx.fillRect(cx - pinSize * 0.25, cy - pinSize * 0.25, pinSize * 0.5, pinSize * 0.5);
  }

  private renderComponentBottomPins(comp: PlacedComponent, isSelected: boolean): void {
    if (comp.type === 'PIN_HEADER') {
      this.renderPinHeaderBottom(comp, isSelected);
    } else if (comp.type === 'TACTILE_SWITCH') {
      this.renderTactileSwitchBottom(comp, isSelected);
    } else if (comp.type === 'TACTILE_SWITCH_SMD') {
      this.renderTwoPinBottom(comp, 'SW', isSelected);
    } else if (comp.type === 'RESISTOR_AXIAL') {
      this.renderTwoPinBottom(comp, 'R', isSelected);
    } else if (comp.type === 'LED') {
      this.renderTwoPinBottom(comp, 'LED', isSelected);
    } else if (comp.type === 'CAPACITOR_CERAMIC') {
      this.renderTwoPinBottom(comp, 'C', isSelected);
    } else if (comp.type === 'DIODE') {
      this.renderTwoPinBottom(comp, 'D', isSelected);
    } else if (comp.type === 'IC_DIP') {
      this.renderICBottom(comp, isSelected);
    } else if (comp.type === 'ESP32_S3_ZERO') {
      this.renderESP32Bottom(comp, isSelected);
    } else if (comp.type === 'TP4056_CHARGER') {
      this.renderTP4056Bottom(comp, isSelected);
    }
  }

  private renderTactileSwitchSmd(comp: PlacedComponent, isSelected: boolean): void {
    const s = comp.start;
    const e = comp.end || { x: s.x + 3, y: s.y };
    const hs = this.transform.getHoleSpacingPx();

    const sx0 = this.transform.gridToScreenX(s.x);
    const sy0 = this.transform.gridToScreenY(s.y);
    const sx1 = this.transform.gridToScreenX(e.x);
    const sy1 = this.transform.gridToScreenY(e.y);

    const cx = (sx0 + sx1) / 2;
    const cy = (sy0 + sy1) / 2;

    const dx = e.x - s.x;
    const dy = e.y - s.y;
    const isHorizontal = Math.abs(dx) >= Math.abs(dy);

    // Gehäusemaße (6.0 x 3.5 mm)
    const bw = isHorizontal ? hs * 2.36 : hs * 1.38;
    const bh = isHorizontal ? hs * 1.38 : hs * 2.36;

    // 1. SMD-Lötlaschen
    this.ctx.strokeStyle = '#CFD8DC';
    this.ctx.lineWidth = Math.max(2.5, hs * 0.22);
    this.ctx.lineCap = 'round';
    this.ctx.beginPath();
    if (isHorizontal) {
      this.ctx.moveTo(sx0, sy0);
      this.ctx.lineTo(cx - bw * 0.45, cy);
      this.ctx.moveTo(sx1, sy1);
      this.ctx.lineTo(cx + bw * 0.45, cy);
    } else {
      this.ctx.moveTo(sx0, sy0);
      this.ctx.lineTo(cx, cy - bh * 0.45);
      this.ctx.moveTo(sx1, sy1);
      this.ctx.lineTo(cx, cy + bh * 0.45);
    }
    this.ctx.stroke();

    // Lötstellen-Punkte an den Pads
    const padDotR = Math.max(2.5, hs * 0.16);
    this.ctx.fillStyle = '#ECEFF1';
    this.ctx.beginPath();
    this.ctx.arc(sx0, sy0, padDotR, 0, Math.PI * 2);
    this.ctx.arc(sx1, sy1, padDotR, 0, Math.PI * 2);
    this.ctx.fill();

    // 2. Herausragender Knebel / Drücker (Stößel 3.0 x 1.5 mm, Höhe 5 mm)
    this.ctx.fillStyle = '#ECEFF1';
    this.ctx.strokeStyle = '#78909C';
    this.ctx.lineWidth = 1;
    this.ctx.beginPath();
    if (isHorizontal) {
      const btnW = hs * 1.18;
      const btnH = hs * 0.55;
      const btnY = (dx >= 0) ? cy - bh / 2 - btnH : cy + bh / 2;
      this.ctx.roundRect(cx - btnW / 2, btnY, btnW, btnH, 2);
    } else {
      const btnW = hs * 0.55;
      const btnH = hs * 1.18;
      const btnX = (dy >= 0) ? cx + bw / 2 : cx - bw / 2 - btnW;
      this.ctx.roundRect(btnX, cy - btnH / 2, btnW, btnH, 2);
    }
    this.ctx.fill();
    this.ctx.stroke();

    // 3. Hauptkörper (Dunkler Thermoplast)
    this.ctx.fillStyle = '#1E1E1E';
    this.ctx.strokeStyle = '#37474F';
    this.ctx.lineWidth = 1.2;
    this.ctx.beginPath();
    this.ctx.roundRect(cx - bw / 2, cy - bh / 2, bw, bh, 3);
    this.ctx.fill();
    this.ctx.stroke();

    // 4. Metallische Abdeckung / Silberner Deckel
    const mw = bw * 0.72;
    const mh = bh * 0.72;
    this.ctx.fillStyle = '#78909C';
    this.ctx.strokeStyle = '#90A4AE';
    this.ctx.lineWidth = 0.8;
    this.ctx.beginPath();
    this.ctx.roundRect(cx - mw / 2, cy - mh / 2, mw, mh, 2);
    this.ctx.fill();
    this.ctx.stroke();

    // Zentrierter kleiner runder Schaltpunkt
    this.ctx.fillStyle = '#263238';
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, hs * 0.18, 0, Math.PI * 2);
    this.ctx.fill();

    // Selektions-Aura
    if (isSelected) {
      this.ctx.strokeStyle = '#00E676';
      this.ctx.lineWidth = 2;
      this.ctx.beginPath();
      this.ctx.roundRect(cx - bw / 2 - 2, cy - bh / 2 - 2, bw + 4, bh + 4, 4);
      this.ctx.stroke();
    }

    // Beschriftung
    this.ctx.fillStyle = '#E0E0E0';
    this.ctx.font = '10px sans-serif';
    this.ctx.fillText(comp.designator, cx - 10, cy - bh / 2 - hs * 0.65);
  }

  private renderPinHeaderBottom(comp: PlacedComponent, isSelected: boolean): void {
    const s = comp.start;
    const e = comp.end || s;
    const hs = this.transform.getHoleSpacingPx();

    const dx = e.x - s.x;
    const dy = e.y - s.y;
    const steps = Math.max(Math.abs(dx), Math.abs(dy));

    const sx0 = this.transform.gridToScreenX(s.x);
    const sy0 = this.transform.gridToScreenY(s.y);
    const sx1 = this.transform.gridToScreenX(e.x);
    const sy1 = this.transform.gridToScreenY(e.y);

    // Gestrichelter dezenter Umriss der Stiftleiste auf der Unterseite (Backside Silkscreen)
    this.ctx.strokeStyle = 'rgba(144, 164, 174, 0.45)';
    this.ctx.lineWidth = 1;
    this.ctx.setLineDash([3, 3]);
    const minSx = Math.min(sx0, sx1) - hs * 0.5;
    const maxSx = Math.max(sx0, sx1) + hs * 0.5;
    const minSy = Math.min(sy0, sy1) - hs * 0.5;
    const maxSy = Math.max(sy0, sy1) + hs * 0.5;
    this.ctx.beginPath();
    this.ctx.roundRect(minSx, minSy, maxSx - minSx, maxSy - minSy, 4);
    this.ctx.stroke();
    this.ctx.setLineDash([]);

    // Beschriftung: "J1 (Pins)"
    this.ctx.fillStyle = 'rgba(144, 164, 174, 0.85)';
    this.ctx.font = '11px sans-serif';
    this.ctx.fillText(`${comp.designator} (Pins)`, minSx, minSy - 4);

    // Durchgesteckte Metallpins an jedem Loch
    for (let i = 0; i <= steps; i++) {
      const hx = steps === 0 ? s.x : s.x + Math.round((i * dx) / steps);
      const hy = steps === 0 ? s.y : s.y + Math.round((i * dy) / steps);

      const cx = this.transform.gridToScreenX(hx);
      const cy = this.transform.gridToScreenY(hy);

      this.drawSolderPin(cx, cy, hs);

      // Pin-Nummer dezent neben dem Pin
      this.ctx.fillStyle = 'rgba(120, 144, 156, 0.85)';
      this.ctx.font = '10px sans-serif';
      this.ctx.fillText(String(i + 1), cx + hs * 0.45, cy + 3);

      if (isSelected) {
        this.ctx.strokeStyle = '#00E676';
        this.ctx.lineWidth = 2;
        this.ctx.beginPath();
        this.ctx.arc(cx, cy, hs * 0.45, 0, Math.PI * 2);
        this.ctx.stroke();
      }
    }
  }

  private renderTactileSwitchBottom(comp: PlacedComponent, isSelected: boolean): void {
    const isSwapped = comp.rotation === 90 || comp.rotation === 270;
    const curW = isSwapped ? 3 : 2;
    const curH = isSwapped ? 2 : 3;
    const hs = this.transform.getHoleSpacingPx();

    const midGridX = comp.start.x + curW / 2;
    const midGridY = comp.start.y + curH / 2;
    const cx = this.transform.gridToScreenX(midGridX);
    const cy = this.transform.gridToScreenY(midGridY);

    this.ctx.save();
    this.ctx.translate(cx, cy);
    const rotAngle = this.transform.isFlippedX() ? -comp.rotation : comp.rotation;
    this.ctx.rotate((rotAngle * Math.PI) / 180);

    const x0 = -hs * 1.0;
    const x1 = hs * 1.0;
    const y0 = -hs * 1.5;
    const y1 = hs * 1.5;
    const pinCoords = [
      [x0, y0],
      [x1, y0],
      [x0, y1],
      [x1, y1],
    ];

    const minX = -hs * 1.0;
    const maxX = hs * 1.0;
    const minY = -hs * 1.5;
    const maxY = hs * 1.5;

    this.ctx.strokeStyle = 'rgba(144, 164, 174, 0.35)';
    this.ctx.lineWidth = 1;
    this.ctx.setLineDash([3, 3]);
    this.ctx.beginPath();
    this.ctx.roundRect(minX - hs * 0.5, minY - hs * 0.5, maxX - minX + hs, maxY - minY + hs, 4);
    this.ctx.stroke();
    this.ctx.setLineDash([]);

    this.ctx.fillStyle = 'rgba(144, 164, 174, 0.75)';
    this.ctx.font = '10px sans-serif';
    this.ctx.fillText(comp.designator, minX - hs * 0.5, minY - hs * 0.6);

    for (const [px, py] of pinCoords) {
      this.drawSolderPin(px, py, hs);
      if (isSelected) {
        this.ctx.strokeStyle = '#00E676';
        this.ctx.lineWidth = 2;
        this.ctx.beginPath();
        this.ctx.arc(px, py, hs * 0.45, 0, Math.PI * 2);
        this.ctx.stroke();
      }
    }
    this.ctx.restore();
  }

  private renderTwoPinBottom(comp: PlacedComponent, _typeLabel: string, isSelected: boolean): void {
    const s = comp.start;
    const e = comp.end || s;
    const hs = this.transform.getHoleSpacingPx();

    const x0 = this.transform.gridToScreenX(s.x);
    const y0 = this.transform.gridToScreenY(s.y);
    const x1 = this.transform.gridToScreenX(e.x);
    const y1 = this.transform.gridToScreenY(e.y);

    this.ctx.strokeStyle = 'rgba(144, 164, 174, 0.35)';
    this.ctx.lineWidth = 1;
    this.ctx.setLineDash([3, 3]);
    this.ctx.beginPath();
    this.ctx.moveTo(x0, y0);
    this.ctx.lineTo(x1, y1);
    this.ctx.stroke();
    this.ctx.setLineDash([]);

    this.ctx.fillStyle = 'rgba(144, 164, 174, 0.75)';
    this.ctx.font = '10px sans-serif';
    this.ctx.fillText(comp.designator, (x0 + x1) / 2 - 10, (y0 + y1) / 2 - 4);

    this.drawSolderPin(x0, y0, hs);
    this.drawSolderPin(x1, y1, hs);

    if (isSelected) {
      this.ctx.strokeStyle = '#00E676';
      this.ctx.lineWidth = 2;
      this.ctx.beginPath();
      this.ctx.arc(x0, y0, hs * 0.45, 0, Math.PI * 2);
      this.ctx.stroke();
      this.ctx.beginPath();
      this.ctx.arc(x1, y1, hs * 0.45, 0, Math.PI * 2);
      this.ctx.stroke();
    }
  }

  private renderICBottom(comp: PlacedComponent, isSelected: boolean): void {
    const s = comp.start;
    const e = comp.end || s;
    const hs = this.transform.getHoleSpacingPx();

    const x0 = this.transform.gridToScreenX(s.x);
    const y0 = this.transform.gridToScreenY(s.y);
    const x1 = this.transform.gridToScreenX(e.x);
    const y1 = this.transform.gridToScreenY(e.y);

    const minX = Math.min(x0, x1);
    const maxX = Math.max(x0, x1);
    const minY = Math.min(y0, y1);
    const maxY = Math.max(y0, y1);

    this.ctx.strokeStyle = 'rgba(144, 164, 174, 0.35)';
    this.ctx.lineWidth = 1;
    this.ctx.setLineDash([3, 3]);
    this.ctx.beginPath();
    this.ctx.roundRect(minX - hs * 0.5, minY - hs * 0.5, maxX - minX + hs, maxY - minY + hs, 4);
    this.ctx.stroke();
    this.ctx.setLineDash([]);

    this.ctx.fillStyle = 'rgba(144, 164, 174, 0.75)';
    this.ctx.font = '10px sans-serif';
    this.ctx.fillText(comp.designator, minX - hs * 0.5, minY - hs * 0.6);

    const minGy = Math.min(s.y, e.y);
    const maxGy = Math.max(s.y, e.y);
    for (let y = minGy; y <= maxGy; y++) {
      this.drawSolderPin(x0, this.transform.gridToScreenY(y), hs);
      this.drawSolderPin(x1, this.transform.gridToScreenY(y), hs);
    }

    if (isSelected) {
      this.ctx.strokeStyle = '#00E676';
      this.ctx.lineWidth = 2;
      this.ctx.beginPath();
      this.ctx.roundRect(minX - hs * 0.6, minY - hs * 0.6, maxX - minX + hs * 1.2, maxY - minY + hs * 1.2, 6);
      this.ctx.stroke();
    }
  }

  private renderESP32Bottom(comp: PlacedComponent, isSelected: boolean): void {
    const isSwapped = comp.rotation === 90 || comp.rotation === 270;
    const curW = isSwapped ? 8 : 6;
    const curH = isSwapped ? 6 : 8;
    const hs = this.transform.getHoleSpacingPx();

    const midGridX = comp.start.x + curW / 2;
    const midGridY = comp.start.y + curH / 2;
    const cx = this.transform.gridToScreenX(midGridX);
    const cy = this.transform.gridToScreenY(midGridY);

    this.ctx.save();
    this.ctx.translate(cx, cy);
    const rotAngle = this.transform.isFlippedX() ? -comp.rotation : comp.rotation;
    this.ctx.rotate((rotAngle * Math.PI) / 180);

    const wPx = hs * 6.8;
    const hPx = hs * 8.8;

    this.ctx.strokeStyle = 'rgba(144, 164, 174, 0.35)';
    this.ctx.lineWidth = 1;
    this.ctx.setLineDash([3, 3]);
    this.ctx.beginPath();
    this.ctx.roundRect(-wPx / 2, -hPx / 2, wPx, hPx, 6);
    this.ctx.stroke();
    this.ctx.setLineDash([]);

    this.ctx.fillStyle = 'rgba(144, 164, 174, 0.75)';
    this.ctx.font = '10px sans-serif';
    this.ctx.fillText(`${comp.designator} (ESP32 Pins)`, -wPx / 2, -hPx / 2 - 6);

    const localXLeft = -hs * 3.0;
    const localXRight = hs * 3.0;
    for (let i = 0; i < 9; i++) {
      const localY = -hs * 4.0 + i * hs;
      this.drawSolderPin(localXLeft, localY, hs);
      this.drawSolderPin(localXRight, localY, hs);
    }

    if (isSelected) {
      this.ctx.strokeStyle = '#00E676';
      this.ctx.lineWidth = 2;
      this.ctx.beginPath();
      this.ctx.roundRect(-wPx / 2 - 3, -hPx / 2 - 3, wPx + 6, hPx + 6, 8);
      this.ctx.stroke();
    }
    this.ctx.restore();
  }

  private renderTP4056Bottom(comp: PlacedComponent, isSelected: boolean): void {
    const isSwapped = comp.rotation === 90 || comp.rotation === 270;
    const curW = isSwapped ? 9 : 6;
    const curH = isSwapped ? 6 : 9;
    const hs = this.transform.getHoleSpacingPx();

    const midGridX = comp.start.x + curW / 2;
    const midGridY = comp.start.y + curH / 2;
    const cx = this.transform.gridToScreenX(midGridX);
    const cy = this.transform.gridToScreenY(midGridY);

    this.ctx.save();
    this.ctx.translate(cx, cy);
    const rotAngle = this.transform.isFlippedX() ? -comp.rotation : comp.rotation;
    this.ctx.rotate((rotAngle * Math.PI) / 180);

    const width = hs * 6.9;
    const height = hs * 9.9;
    const boardLeft = -width / 2;
    const boardTop = -height / 2;

    this.ctx.strokeStyle = 'rgba(144, 164, 174, 0.35)';
    this.ctx.lineWidth = 1;
    this.ctx.setLineDash([3, 3]);
    this.ctx.beginPath();
    this.ctx.roundRect(boardLeft, boardTop, width, height, 6);
    this.ctx.stroke();
    this.ctx.setLineDash([]);

    this.ctx.fillStyle = 'rgba(144, 164, 174, 0.75)';
    this.ctx.font = '10px sans-serif';
    this.ctx.fillText(`${comp.designator} (TP4056 Pins)`, boardLeft + 8, boardTop - 6);

    const localXLeft = -hs * 3.0;
    const localXRight = hs * 3.0;
    const localYTop = -hs * 4.5;
    const pads = [
      [localXLeft, localYTop],
      [localXRight, localYTop],
      [localXLeft, localYTop + hs * 8.0],
      [localXLeft, localYTop + hs * 9.0],
      [localXRight, localYTop + hs * 9.0],
      [localXRight, localYTop + hs * 8.0],
    ];

    for (const [px, py] of pads) {
      this.drawSolderPin(px, py, hs);
    }

    if (isSelected) {
      this.ctx.strokeStyle = '#00E676';
      this.ctx.lineWidth = 2;
      this.ctx.beginPath();
      this.ctx.roundRect(boardLeft - 4, boardTop - 4, width + 8, height + 8, 8);
      this.ctx.stroke();
    }
    this.ctx.restore();
  }

  public applyZoom(factor: number): void {
    const rect = this.canvas.getBoundingClientRect();
    this.transform.applyZoom(factor, rect.width / 2, rect.height / 2);
    this.render();
  }

  public setActiveTool(tool: ActiveTool): void {
    this.activeTool = tool;
    this.wireStartHole = null;
    this.pinHeaderStartHole = null;
    this.render();
  }

  public setActiveComponentType(type: ComponentType): void {
    this.activeComponentType = type;
    this.activeTool = 'COMPONENT';
    this.render();
  }

  public setActiveWireKind(kind: WireKind): void {
    this.activeWireKind = kind;
  }

  public setActiveWireColor(col: string): void {
    this.activeWireColor = col;
  }

  public setLayer(layer: BoardLayer): void {
    this.currentLayer = layer;
    const cols = this.project?.board?.columns || 24;
    this.transform.setFlippedX(layer === 'BOTTOM', cols);
    this.render();
  }

  public rotateSelected(): void {
    if (this.selectedComponent) {
      this.recordState();
      const comp = this.selectedComponent;
      comp.rotation = ((comp.rotation || 0) + 90) % 360;

      let baseW = 0;
      let baseH = 0;
      switch (comp.type) {
        case 'ESP32_S3_ZERO': baseW = 6; baseH = 8; break;
        case 'TP4056_CHARGER': baseW = 6; baseH = 9; break;
        case 'TACTILE_SWITCH': baseW = 2; baseH = 3; break;
        case 'IC_DIP': baseW = 3; baseH = 3; break;
        default: break;
      }

      if (baseW > 0 && baseH > 0) {
        const isSwapped = comp.rotation === 90 || comp.rotation === 270;
        const curW = isSwapped ? baseH : baseW;
        const curH = isSwapped ? baseW : baseH;
        comp.end = { x: comp.start.x + curW, y: comp.start.y + curH };
      } else if (comp.end) {
        const dx = comp.end.x - comp.start.x;
        const dy = comp.end.y - comp.start.y;
        comp.end = {
          x: comp.start.x - dy,
          y: comp.start.y + dx,
        };
      }

      this.activeComponentRotation = comp.rotation;
      if (this.onProjectModified) this.onProjectModified();
      this.render();
    } else if (this.activeTool === 'COMPONENT') {
      this.activeComponentRotation = ((this.activeComponentRotation || 0) + 90) % 360;
      this.render();
    }
  }

  public deleteSelected(): void {
    if (this.selectedComponent) {
      this.recordState();
      this.project.components = this.project.components.filter((c) => c !== this.selectedComponent);
      this.selectedComponent = null;
      if (this.onComponentSelected) this.onComponentSelected(null);
      if (this.onProjectModified) this.onProjectModified();
      this.render();
    } else if (this.selectedWire) {
      this.recordState();
      this.project.wires = this.project.wires.filter((w) => w !== this.selectedWire);
      this.selectedWire = null;
      if (this.onComponentSelected) this.onComponentSelected(null);
      if (this.onProjectModified) this.onProjectModified();
      this.render();
    }
  }

  public getSelectedWire(): WireTrace | null {
    return this.selectedWire;
  }

  public setProject(project: PerfProject): void {
    this.project = project;
    this.history.clear();
    this.selectedComponent = project.components[0] || null;
    const cols = this.project?.board?.columns || 24;
    this.transform.setFlippedX(this.currentLayer === 'BOTTOM', cols);
    this.resize();
  }

  public getProject(): PerfProject {
    return this.project;
  }

  private isTp4056Pad(h1: HoleCoord, h2: HoleCoord): boolean {
    for (const c of this.project.components) {
      if (c.type === 'TP4056_CHARGER') {
        const s = c.start;
        const pads = [
          { x: s.x, y: s.y },
          { x: s.x + 6, y: s.y },
          { x: s.x, y: s.y + 8 },
          { x: s.x, y: s.y + 9 },
          { x: s.x + 6, y: s.y + 9 },
          { x: s.x + 6, y: s.y + 8 },
        ];
        for (const p of pads) {
          if ((p.x === h1.x && p.y === h1.y) || (p.x === h2.x && p.y === h2.y)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
