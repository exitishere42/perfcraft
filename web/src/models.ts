export type BoardLayer = 'TOP' | 'BOTTOM' | 'BOTH';

export type WireKind = 'CABLE' | 'SILVER_WIRE';

export type ComponentType =
  | 'RESISTOR_AXIAL'
  | 'TACTILE_SWITCH'
  | 'LED'
  | 'CAPACITOR_CERAMIC'
  | 'DIODE'
  | 'IC_DIP';

export interface HoleCoord {
  x: number;
  y: number;
}

export interface BoardConfig {
  columns: number;
  rows: number;
  pitchMm: number;
  padDiameterMm: number;
  drillDiameterMm: number;
}

export interface PlacedComponent {
  id: string;
  type: ComponentType;
  designator: string;
  value: string;
  start: HoleCoord;
  end: HoleCoord | null;
  rotation: number; // 0, 90, 180, 270
  layer: BoardLayer;
}

export interface WireTrace {
  id: string;
  kind: WireKind;
  layer: BoardLayer;
  color: string;
  points: HoleCoord[];
}

export interface PerfProject {
  name: string;
  board: BoardConfig;
  components: PlacedComponent[];
  wires: WireTrace[];
}

export function createDefaultProject(): PerfProject {
  return {
    name: 'Unbenanntes-Projekt',
    board: {
      columns: 24,
      rows: 18,
      pitchMm: 2.54,
      padDiameterMm: 2.1,
      drillDiameterMm: 1.0,
    },
    components: [],
    wires: [],
  };
}
