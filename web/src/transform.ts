import { HoleCoord } from './models.ts';

export class CoordinateTransform {
  private static readonly PITCH_MM = 2.54;
  private static readonly DEFAULT_PPM = 3.78; // 96 DPI basis

  private scale: number = 1.0;
  private translateX: number = 40.0;
  private translateY: number = 40.0;
  private readonly pixelsPerMm: number;
  private flippedX: boolean = false;
  private boardCols: number = 24;

  constructor(pixelsPerMm: number = CoordinateTransform.DEFAULT_PPM) {
    this.pixelsPerMm = pixelsPerMm;
  }

  public setFlippedX(flippedX: boolean, boardCols: number): void {
    this.flippedX = flippedX;
    this.boardCols = Math.max(1, boardCols);
  }

  public isFlippedX(): boolean {
    return this.flippedX;
  }

  public getHoleSpacingPx(): number {
    return CoordinateTransform.PITCH_MM * this.pixelsPerMm * this.scale;
  }

  public gridToScreenX(gridX: number): number {
    const effX = this.flippedX ? this.boardCols - 1 - gridX : gridX;
    return effX * CoordinateTransform.PITCH_MM * this.pixelsPerMm * this.scale + this.translateX;
  }

  public gridToScreenY(gridY: number): number {
    return gridY * CoordinateTransform.PITCH_MM * this.pixelsPerMm * this.scale + this.translateY;
  }

  public screenToGrid(screenX: number, screenY: number): HoleCoord {
    const spacing = this.getHoleSpacingPx();
    let gx = Math.round((screenX - this.translateX) / spacing);
    const gy = Math.round((screenY - this.translateY) / spacing);
    if (this.flippedX) {
      gx = this.boardCols - 1 - gx;
    }
    return { x: gx, y: gy };
  }

  public applyZoom(factor: number, pivotX: number, pivotY: number): void {
    const newScale = Math.min(Math.max(this.scale * factor, 0.2), 8.0);
    const actual = newScale / this.scale;
    this.translateX = pivotX - (pivotX - this.translateX) * actual;
    this.translateY = pivotY - (pivotY - this.translateY) * actual;
    this.scale = newScale;
  }

  public pan(dx: number, dy: number): void {
    this.translateX += dx;
    this.translateY += dy;
  }

  public resetView(canvasWidth: number, canvasHeight: number, cols: number, rows: number): void {
    this.boardCols = Math.max(1, cols);
    const pitchPx = CoordinateTransform.PITCH_MM * this.pixelsPerMm;
    const boardWidthPx = (cols - 1) * pitchPx;
    const boardHeightPx = (rows - 1) * pitchPx;
    const totalW = boardWidthPx + pitchPx * 3.4;
    const totalH = boardHeightPx + pitchPx * 5.0;
    const scaleX = (canvasWidth - 40.0) / Math.max(1, totalW);
    const scaleY = (canvasHeight - 40.0) / Math.max(1, totalH);
    this.scale = Math.min(Math.max(Math.min(scaleX, scaleY), 0.2), 8.0);
    this.translateX = (canvasWidth - boardWidthPx * this.scale) / 2.0;
    this.translateY = (canvasHeight - boardHeightPx * this.scale) / 2.0;
  }

  public getScale(): number {
    return this.scale;
  }
}
