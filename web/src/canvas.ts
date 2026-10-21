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

    if (this.activePointers.size === 1) {
      // 1-Finger / Primary Mouse
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
        comp = { id, type: this.activeComponentType, designator: des, value: '6x6 mm', start, end: { x: start.x + 2, y: start.y + 1 }, rotation: 0, layer: 'TOP' };
        break;
      case 'RESISTOR_AXIAL':
        des = 'R' + count;
        comp = { id, type: this.activeComponentType, designator: des, value: '10k', start, end: { x: start.x + 4, y: start.y }, rotation: 0, layer: 'TOP' };
        break;
      case 'LED':
        des = 'LED' + count;
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

    // 2. Bottom Wires (Silberdraht)
    if (this.currentLayer === 'BOTTOM' || this.currentLayer === 'BOTH') {
      this.ctx.strokeStyle = '#CFD8DC';
      this.ctx.lineWidth = 4.0 * this.transform.getScale();
      this.ctx.lineCap = 'round';
      for (const wire of this.project.wires) {
        if (wire.kind === 'SILVER_WIRE' && wire.points.length >= 2) {
          this.ctx.beginPath();
          this.ctx.moveTo(this.transform.gridToScreenX(wire.points[0].x), this.transform.gridToScreenY(wire.points[0].y));
          this.ctx.lineTo(this.transform.gridToScreenX(wire.points[1].x), this.transform.gridToScreenY(wire.points[1].y));
          this.ctx.stroke();
        }
      }
    }

    // 3. Silber-Pads (Silber #CFD8DC statt Gold!)
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

    // 4. Top Wires (Isolierte Kabel)
    if (this.currentLayer === 'TOP' || this.currentLayer === 'BOTH') {
      this.ctx.lineWidth = 4.5 * this.transform.getScale();
      this.ctx.lineCap = 'round';
      for (const wire of this.project.wires) {
        if (wire.kind === 'CABLE' && wire.points.length >= 2) {
          this.ctx.strokeStyle = wire.color || '#E53935';
          this.ctx.beginPath();
          this.ctx.moveTo(this.transform.gridToScreenX(wire.points[0].x), this.transform.gridToScreenY(wire.points[0].y));
          this.ctx.lineTo(this.transform.gridToScreenX(wire.points[1].x), this.transform.gridToScreenY(wire.points[1].y));
          this.ctx.stroke();
        }
      }
    }

    // Wire live preview rubberband
    if (this.activeTool === 'WIRE' && this.wireStartHole && this.currentHoverGrid) {
      this.ctx.strokeStyle = '#00C853';
      this.ctx.lineWidth = 3.0 * this.transform.getScale();
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
      const dummy: PlacedComponent = {
        id: 'ghost',
        type: this.activeComponentType,
        designator: 'GHOST',
        value: '10k',
        start: this.currentHoverGrid,
        end: { x: this.currentHoverGrid.x + 4, y: this.currentHoverGrid.y },
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
      const cx = this.transform.gridToScreenX(comp.start.x + 1);
      const cy = (this.transform.gridToScreenY(comp.start.y) + this.transform.gridToScreenY(comp.start.y + 1)) / 2;
      const w = hs * 2.3;
      const h = hs * 1.5;

      this.ctx.fillStyle = '#37474F';
      this.ctx.beginPath();
      this.ctx.roundRect(cx - w / 2, cy - h / 2, w, h, 6);
      this.ctx.fill();

      this.ctx.fillStyle = '#607D8B';
      this.ctx.beginPath();
      this.ctx.roundRect(cx - w * 0.35, cy - h * 0.35, w * 0.7, h * 0.7, 4);
      this.ctx.fill();

      // Button
      this.ctx.fillStyle = '#FBC02D';
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, Math.min(w, h) * 0.28, 0, Math.PI * 2);
      this.ctx.fill();

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(cx - w / 2 - 3, cy - h / 2 - 3, w + 6, h + 6);
      }
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
      const bw = hs * 2.2;
      const bh = hs * 0.55;

      this.ctx.fillStyle = '#D7CCC8';
      this.ctx.beginPath();
      this.ctx.roundRect(midX - bw / 2, midY - bh / 2, bw, bh, 5);
      this.ctx.fill();
      this.ctx.strokeStyle = '#A1887F';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();

      // Color bands
      const bands = parseResistorColorCode(comp.value);
      if (bands) {
        const bandW = Math.max(2, bw * 0.08);
        const spacing = bw * 0.18;
        const startOffset = midX - bw / 2 + bw * 0.18;

        const drawBand = (bx: number, color: string) => {
          this.ctx.fillStyle = color;
          this.ctx.fillRect(bx, midY - bh / 2, bandW, bh);
        };

        drawBand(startOffset + 0 * spacing, bands.band1);
        drawBand(startOffset + 1 * spacing, bands.band2);
        drawBand(startOffset + 2 * spacing, bands.multiplier);
        drawBand(startOffset + 3.2 * spacing, bands.tolerance);
      }

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(midX - bw / 2 - 2, midY - bh / 2 - 2, bw + 4, bh + 4);
      }
    } else if (comp.type === 'LED') {
      const px = this.transform.gridToScreenX(comp.start.x);
      const py = this.transform.gridToScreenY(comp.start.y);
      const r = hs * 0.42;

      this.ctx.fillStyle = '#E53935';
      this.ctx.beginPath();
      this.ctx.arc(px, py, r, 0, Math.PI * 2);
      this.ctx.fill();

      // Kathode
      this.ctx.fillStyle = '#B71C1C';
      this.ctx.fillRect(px + r * 0.7, py - r, r * 0.3, r * 2);

      if (isSelected) {
        this.ctx.strokeStyle = '#00C853';
        this.ctx.lineWidth = 2;
        this.ctx.strokeRect(px - r - 3, py - r - 3, (r + 3) * 2, (r + 3) * 2);
      }
    }
  }

  private detectTerminalLabel(grid: HoleCoord): string | null {
    for (const c of this.project.components) {
      if (c.type === 'TACTILE_SWITCH') {
        const s = c.start;
        if (grid.x === s.x && grid.y === s.y) return c.designator + ': Pin 1a';
        if (grid.x === s.x + 2 && grid.y === s.y) return c.designator + ': Pin 1b';
        if (grid.x === s.x && grid.y === s.y + 1) return c.designator + ': Pin 2a';
        if (grid.x === s.x + 2 && grid.y === s.y + 1) return c.designator + ': Pin 2b';
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
