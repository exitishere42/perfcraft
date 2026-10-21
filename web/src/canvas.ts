import {
  PerfProject,
  PlacedComponent,
  HoleCoord,
  ComponentType,
  WireKind,
  BoardLayer,
} from './models.ts';
import { CoordinateTransform } from './transform.ts';
import { parseResistorColorCode } from './resistor.ts';
import { findConnectedNet } from './continuity.ts';

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
  private currentLayer: BoardLayer = 'TOP';

  private selectedComponent: PlacedComponent | null = null;
  private wireStartHole: HoleCoord | null = null;
  private currentHoverGrid: HoleCoord | null = null;
  private highlightedNet: Set<string> = new Set();
  private hoveredTerminalText: string | null = null;

  // Touch & Pointer state
  private activePointers: Map<number, { x: number; y: number }> = new Map();
  private initialPinchDistance: number = 0;
  private isDraggingObject: boolean = false;
  private longPressTimeout: number | null = null;

  // Callbacks
  public onHoverCoordinate?: (gx: number, gy: number) => void;
  public onComponentSelected?: (comp: PlacedComponent | null) => void;
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
    this.canvas.addEventListener('pointerdown', (e) => this.handlePointerDown(e));
    this.canvas.addEventListener('pointermove', (e) => this.handlePointerMove(e));
    this.canvas.addEventListener('pointerup', (e) => this.handlePointerUp(e));
    this.canvas.addEventListener('pointercancel', (e) => this.handlePointerUp(e));

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

  private handlePointerDown(e: PointerEvent): void {
    this.canvas.setPointerCapture(e.pointerId);
    const pt = this.getCanvasPoint(e);
    this.activePointers.set(e.pointerId, pt);

    if (e.button !== 0) {
      // Nur primärer Klick (Linksklick) darf Aktionen ausführen, Rechts-/Mittelklick brechen nur ab
      if (e.button === 2) {
        this.wireStartHole = null;
        this.render();
      }
      return;
    }

    if (this.activePointers.size === 1) {
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
        this.selectedComponent = hit;
        this.isDraggingObject = hit !== null;
        if (this.onComponentSelected) this.onComponentSelected(hit);
        this.render();
      } else if (this.activeTool === 'WIRE') {
        if (!this.wireStartHole) {
          this.wireStartHole = grid;
        } else {
          if (this.wireStartHole.x !== grid.x || this.wireStartHole.y !== grid.y) {
            this.project.wires.push({
              id: 'w_' + Date.now(),
              kind: this.activeWireKind,
              layer: this.activeWireKind === 'SILVER_WIRE' ? 'BOTTOM' : 'TOP',
              color: this.activeWireKind === 'SILVER_WIRE' ? '#CFD8DC' : this.activeWireColor,
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
        this.placeNewComponent(grid);
        this.render();
      }
    } else if (this.activePointers.size === 2) {
      // 2-Finger Pinch to Zoom / Pan
      const pts = Array.from(this.activePointers.values());
      const dx = pts[0].x - pts[1].x;
      const dy = pts[0].y - pts[1].y;
      this.initialPinchDistance = Math.hypot(dx, dy);
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
          if (this.onProjectModified) this.onProjectModified();
          this.render();
        }
      } else {
        this.currentHoverGrid = this.transform.screenToGrid(pt.x, pt.y);
        this.render();
      }
    } else if (this.activePointers.size === 2) {
      // Pinch zoom
      const pts = Array.from(this.activePointers.values());
      const dist = Math.hypot(pts[0].x - pts[1].x, pts[0].y - pts[1].y);
      if (this.initialPinchDistance > 10) {
        const factor = dist / this.initialPinchDistance;
        const midX = (pts[0].x + pts[1].x) / 2;
        const midY = (pts[0].y + pts[1].y) / 2;
        this.transform.applyZoom(factor > 1 ? 1.04 : 0.96, midX, midY);
        this.render();
      }
    }
  }

  private handlePointerUp(e: PointerEvent): void {
    this.activePointers.delete(e.pointerId);
    this.isDraggingObject = false;
    if (this.longPressTimeout) {
      clearTimeout(this.longPressTimeout);
      this.longPressTimeout = null;
    }
    this.render();
  }

  private findComponentAt(grid: HoleCoord): PlacedComponent | null {
    for (const c of this.project.components) {
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
    }

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

    const boardX = this.transform.gridToScreenX(0) - hs * 0.8;
    const boardY = this.transform.gridToScreenY(0) - hs * 0.8;
    const boardW = (cols - 1) * hs + hs * 1.6;
    const boardH = (rows - 1) * hs + hs * 1.6;

    // 1. Platinen-Substrat (Clean sattes PCB-Grün)
    this.ctx.fillStyle = '#1B4D2E';
    this.ctx.beginPath();
    this.ctx.roundRect(boardX, boardY, boardW, boardH, 12);
    this.ctx.fill();
    this.ctx.strokeStyle = '#143922';
    this.ctx.lineWidth = 3;
    this.ctx.stroke();

    // 2. Silber-Pads (Silber #CFD8DC statt Gold!)
    const padR = Math.max(3.0, hs * 0.38);
    const drillR = Math.max(1.5, hs * 0.18);

    for (let c = 0; c < cols; c++) {
      for (let r = 0; r < rows; r++) {
        const px = this.transform.gridToScreenX(c);
        const py = this.transform.gridToScreenY(r);
        const key = `${c},${r}`;

        if (this.highlightedNet.has(key)) {
          this.ctx.fillStyle = '#FFEE58';
          this.ctx.beginPath();
          this.ctx.arc(px, py, padR + 3, 0, Math.PI * 2);
          this.ctx.fill();
        }

        // Silber-Pad
        this.ctx.fillStyle = '#CFD8DC';
        this.ctx.beginPath();
        this.ctx.arc(px, py, padR, 0, Math.PI * 2);
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

    // 3. Leitungen (Kabel und Silberdraht - jetzt über den Pads gerendert!)
    // Silberdraht ist dünner (1.8px) als normales Kabel (3.5px)
    this.ctx.lineCap = 'round';
    for (const wire of this.project.wires) {
      if (wire.points.length >= 2) {
        if (wire.kind === 'SILVER_WIRE') {
          this.ctx.strokeStyle = '#E0E0E0';
          this.ctx.lineWidth = 1.8 * this.transform.getScale();
        } else {
          this.ctx.strokeStyle = wire.color || '#E53935';
          this.ctx.lineWidth = 3.5 * this.transform.getScale();
        }
        this.ctx.beginPath();
        this.ctx.moveTo(this.transform.gridToScreenX(wire.points[0].x), this.transform.gridToScreenY(wire.points[0].y));
        this.ctx.lineTo(this.transform.gridToScreenX(wire.points[1].x), this.transform.gridToScreenY(wire.points[1].y));
        this.ctx.stroke();
      }
    }

    // Wire live preview rubberband
    if (this.activeTool === 'WIRE' && this.wireStartHole && this.currentHoverGrid) {
      const isSilver = (this.activeWireKind === 'SILVER_WIRE');
      this.ctx.strokeStyle = isSilver ? '#E0E0E0' : '#00C853';
      this.ctx.lineWidth = (isSilver ? 1.8 : 3.0) * this.transform.getScale();
      this.ctx.setLineDash([4, 4]);
      this.ctx.beginPath();
      this.ctx.moveTo(this.transform.gridToScreenX(this.wireStartHole.x), this.transform.gridToScreenY(this.wireStartHole.y));
      this.ctx.lineTo(this.transform.gridToScreenX(this.currentHoverGrid.x), this.transform.gridToScreenY(this.currentHoverGrid.y));
      this.ctx.stroke();
      this.ctx.setLineDash([]);
    }

    // 5. Komponenten
    if (this.currentLayer === 'TOP' || this.currentLayer === 'BOTH') {
      for (const comp of this.project.components) {
        this.renderComponent(comp, comp === this.selectedComponent);
      }
    }

    // 6. Tinkercad-Ghost Preview am Cursor
    if (this.activeTool === 'COMPONENT' && this.currentHoverGrid) {
      this.ctx.save();
      this.ctx.globalAlpha = 0.60;
      let dummyEnd: HoleCoord = { x: this.currentHoverGrid.x + 4, y: this.currentHoverGrid.y };
      if (this.activeComponentType === 'TACTILE_SWITCH') {
        dummyEnd = { x: this.currentHoverGrid.x + 2, y: this.currentHoverGrid.y + 3 };
      } else if (this.activeComponentType === 'RESISTOR_AXIAL') {
        dummyEnd = { x: this.currentHoverGrid.x + 3, y: this.currentHoverGrid.y };
      } else if (this.activeComponentType === 'LED' || this.activeComponentType === 'CAPACITOR_CERAMIC') {
        dummyEnd = { x: this.currentHoverGrid.x + 1, y: this.currentHoverGrid.y };
      } else if (this.activeComponentType === 'DIODE') {
        dummyEnd = { x: this.currentHoverGrid.x + 3, y: this.currentHoverGrid.y };
      } else if (this.activeComponentType === 'IC_DIP') {
        dummyEnd = { x: this.currentHoverGrid.x + 3, y: this.currentHoverGrid.y + 3 };
      }

      const dummy: PlacedComponent = {
        id: 'ghost',
        type: this.activeComponentType,
        designator: 'GHOST',
        value: '10k',
        start: this.currentHoverGrid,
        end: dummyEnd,
        rotation: 0,
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
      const x0 = this.transform.gridToScreenX(comp.start.x);
      const y0 = this.transform.gridToScreenY(comp.start.y);
      const x1 = this.transform.gridToScreenX(comp.start.x + 2);
      const y1 = this.transform.gridToScreenY(comp.start.y + 3);

      const cx = (x0 + x1) / 2;
      const cy = (y0 + y1) / 2;

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
      this.ctx.moveTo(x0, y0); this.ctx.lineTo(cx - bodyHalf + hs * 0.15, cy - bodyHalf);
      this.ctx.moveTo(x1, y0); this.ctx.lineTo(cx + bodyHalf - hs * 0.15, cy - bodyHalf);
      // Unten links / rechts
      this.ctx.moveTo(x0, y1); this.ctx.lineTo(cx - bodyHalf + hs * 0.15, cy + bodyHalf);
      this.ctx.moveTo(x1, y1); this.ctx.lineTo(cx + bodyHalf - hs * 0.15, cy + bodyHalf);
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
      this.ctx.roundRect(cx - bw / 2, cy - bh / 2, bw, bh, 5);
      this.ctx.fill();
      this.ctx.strokeStyle = '#121212';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      // 3. Silberne quadratische Metallplatte
      const mw = bw * 0.86;
      const mh = bh * 0.86;
      this.ctx.fillStyle = '#B0BEC5';
      this.ctx.beginPath();
      this.ctx.roundRect(cx - mw / 2, cy - mh / 2, mw, mh, 4);
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
        this.ctx.arc(cx + ox, cy + oy, rivetR, 0, Math.PI * 2);
        this.ctx.fill();
      }

      // 4. Zentraler runder schwarzer Betätigungs-Knopf
      const btnR = mw * 0.38;
      this.ctx.fillStyle = '#263238';
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, btnR, 0, Math.PI * 2);
      this.ctx.fill();

      this.ctx.fillStyle = '#151515';
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, btnR * 0.88, 0, Math.PI * 2);
      this.ctx.fill();

      this.ctx.fillStyle = '#424242';
      this.ctx.beginPath();
      this.ctx.arc(cx - btnR * 0.3, cy - btnR * 0.3, btnR * 0.25, 0, Math.PI * 2);
      this.ctx.fill();

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.beginPath();
        this.ctx.roundRect(cx - bw / 2 - 3, cy - bh / 2 - 3, bw + 6, bh + 6, 6);
        this.ctx.stroke();
      }

      this.ctx.fillStyle = '#ECEFF1';
      this.ctx.font = '11px sans-serif';
      this.ctx.fillText(comp.designator, cx - 12, cy - bh / 2 - 4);
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
    }
  }

  private detectTerminalLabel(grid: HoleCoord): string | null {
    for (const c of this.project.components) {
      if (c.type === 'TACTILE_SWITCH') {
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
      }
    }
    return null;
  }

  public applyZoom(factor: number): void {
    const rect = this.canvas.getBoundingClientRect();
    this.transform.applyZoom(factor, rect.width / 2, rect.height / 2);
    this.render();
  }

  public setActiveTool(tool: ActiveTool): void {
    this.activeTool = tool;
    this.wireStartHole = null;
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
    this.render();
  }

  public rotateSelected(): void {
    if (this.selectedComponent) {
      this.selectedComponent.rotation = (this.selectedComponent.rotation + 90) % 360;
      if (this.selectedComponent.end) {
        const dx = this.selectedComponent.end.x - this.selectedComponent.start.x;
        const dy = this.selectedComponent.end.y - this.selectedComponent.start.y;
        this.selectedComponent.end = {
          x: this.selectedComponent.start.x - dy,
          y: this.selectedComponent.start.y + dx,
        };
      }
      if (this.onProjectModified) this.onProjectModified();
      this.render();
    }
  }

  public deleteSelected(): void {
    if (this.selectedComponent) {
      this.project.components = this.project.components.filter((c) => c !== this.selectedComponent);
      this.selectedComponent = null;
      if (this.onComponentSelected) this.onComponentSelected(null);
      if (this.onProjectModified) this.onProjectModified();
      this.render();
    }
  }

  public setProject(project: PerfProject): void {
    this.project = project;
    this.selectedComponent = project.components[0] || null;
    this.resize();
  }

  public getProject(): PerfProject {
    return this.project;
  }
}
