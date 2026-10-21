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
    components: [
      {
        id: 'c1',
        type: 'TACTILE_SWITCH',
        designator: 'SW1',
        value: '6x6 mm',
        start: { x: 5, y: 4 },
        end: { x: 7, y: 5 },
        rotation: 0,
        layer: 'TOP',
      },
      {
        id: 'c2',
        type: 'RESISTOR_AXIAL',
        designator: 'R1',
        value: '10k',
        start: { x: 10, y: 4 },
        end: { x: 14, y: 4 },
        rotation: 0,
        layer: 'TOP',
      },
      {
        id: 'c3',
        type: 'LED',
        designator: 'LED1',
        value: 'Red',
        start: { x: 16, y: 4 },
        end: { x: 17, y: 4 },
        rotation: 0,
        layer: 'TOP',
      },
    ],
    wires: [
      {
        id: 'w1',
        kind: 'CABLE',
        layer: 'TOP',
        color: '#E53935',
        points: [{ x: 5, y: 4 }, { x: 10, y: 4 }],
      },
      {
        id: 'w2',
        kind: 'SILVER_WIRE',
        layer: 'BOTTOM',
        color: '#CFD8DC',
        points: [{ x: 14, y: 4 }, { x: 16, y: 4 }],
      },
    ],
  };
}
