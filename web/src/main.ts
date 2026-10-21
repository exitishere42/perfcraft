import { createDefaultProject, PerfProject, PlacedComponent, ComponentType } from './models.ts';
import { PerfBoardCanvas } from './canvas.ts';
import { parseResistorColorCode } from './resistor.ts';

let currentProject: PerfProject = createDefaultProject();
const canvasEl = document.getElementById('perfCanvas') as HTMLCanvasElement;
const canvas = new PerfBoardCanvas(canvasEl, currentProject);

// 1. HAUPTMENÜ & NEUES PROJEKT MODAL
const welcomeOverlay = document.getElementById('welcomeOverlay') as HTMLDivElement;
const dlgProjName = document.getElementById('dlgProjName') as HTMLInputElement;
const dlgCols = document.getElementById('dlgCols') as HTMLInputElement;
const dlgRows = document.getElementById('dlgRows') as HTMLInputElement;
const btnWelcomeCreate = document.getElementById('btnWelcomeCreate') as HTMLButtonElement;
const btnWelcomeOpen = document.getElementById('btnWelcomeOpen') as HTMLButtonElement;

// Preset buttons
document.querySelectorAll('.preset-btn').forEach((btn) => {
  btn.addEventListener('click', (e) => {
    document.querySelectorAll('.preset-btn').forEach((b) => b.classList.remove('active'));
    const target = e.currentTarget as HTMLButtonElement;
    target.classList.add('active');
    const cols = target.getAttribute('data-cols');
    const rows = target.getAttribute('data-rows');
    if (cols) dlgCols.value = cols;
    if (rows) dlgRows.value = rows;
  });
});

const startNewProject = (name: string, cols: number, rows: number) => {
  currentProject = {
    name: name || 'Unbenanntes-Projekt',
    board: {
      columns: Math.max(5, Math.min(200, cols)),
      rows: Math.max(5, Math.min(200, rows)),
      pitchMm: 2.54,
      padDiameterMm: 2.1,
      drillDiameterMm: 1.0,
    },
    components: [],
    wires: [],
  };
  canvas.setProject(currentProject);
  document.title = `PerfCraft - ${currentProject.name}`;
  welcomeOverlay.classList.remove('active');
};

btnWelcomeCreate?.addEventListener('click', () => {
  const name = dlgProjName.value.trim() || 'Mein-Perfboard';
  const cols = parseInt(dlgCols.value, 10) || 24;
  const rows = parseInt(dlgRows.value, 10) || 18;
  startNewProject(name, cols, rows);
});

btnWelcomeOpen?.addEventListener('click', () => {
  fileInput.click();
});

// 2. HEADER CONTROLS
document.getElementById('btnNew')?.addEventListener('click', () => {
  welcomeOverlay.classList.add('active');
});

const fileInput = document.getElementById('fileInput') as HTMLInputElement;
document.getElementById('btnOpen')?.addEventListener('click', () => {
  fileInput.click();
});

fileInput.addEventListener('change', (e) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const json = JSON.parse(event.target?.result as string);
        if (json.board && json.components && json.wires) {
          currentProject = json as PerfProject;
          canvas.setProject(currentProject);
          document.title = `PerfCraft - ${currentProject.name}`;
          welcomeOverlay.classList.remove('active');
        } else {
          alert('Invalid .perf project format.');
        }
      } catch (err) {
        alert('Failed to parse file: ' + err);
      }
    };
    reader.readAsText(file);
  }
});

// Drag & drop .perf files
window.addEventListener('dragover', (e) => e.preventDefault());
window.addEventListener('drop', (e) => {
  e.preventDefault();
  const file = e.dataTransfer?.files[0];
  if (file && file.name.endsWith('.perf')) {
    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        currentProject = JSON.parse(ev.target?.result as string);
        canvas.setProject(currentProject);
        document.title = `PerfCraft - ${currentProject.name}`;
      } catch (err) {
        alert('Failed to load: ' + err);
      }
    };
    reader.readAsText(file);
  }
});

document.getElementById('btnSave')?.addEventListener('click', () => {
  const jsonStr = JSON.stringify(currentProject, null, 2);
  const blob = new Blob([jsonStr], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${currentProject.name || 'project'}.perf`;
  a.click();
  URL.revokeObjectURL(url);
});

// Export high-res PNG
document.getElementById('btnExport')?.addEventListener('click', () => {
  const dataUrl = canvasEl.toDataURL('image/png');
  const a = document.createElement('a');
  a.href = dataUrl;
  a.download = `${currentProject.name || 'perfcraft'}-render.png`;
  a.click();
});

// Layer buttons
const btnLayerTop = document.getElementById('btnLayerTop')!;
const btnLayerBottom = document.getElementById('btnLayerBottom')!;
const btnLayerBoth = document.getElementById('btnLayerBoth')!;
const statLayerVal = document.getElementById('statLayerVal')!;

function setLayer(btn: HTMLElement, layer: 'TOP' | 'BOTTOM' | 'BOTH', label: string) {
  [btnLayerTop, btnLayerBottom, btnLayerBoth].forEach((b) => b.classList.remove('active'));
  btn.classList.add('active');
  canvas.setLayer(layer);
  statLayerVal.textContent = label;
}

btnLayerTop.addEventListener('click', () => setLayer(btnLayerTop, 'TOP', 'Top (F)'));
btnLayerBottom.addEventListener('click', () => setLayer(btnLayerBottom, 'BOTTOM', 'Bottom (B)'));
btnLayerBoth.addEventListener('click', () => setLayer(btnLayerBoth, 'BOTH', 'Both'));

// Zoom in / out
document.getElementById('btnZoomIn')?.addEventListener('click', () => {
  canvas.applyZoom(1.15);
});
document.getElementById('btnZoomOut')?.addEventListener('click', () => {
  canvas.applyZoom(0.85);
});

// 2. LEFT TOOLBAR
const toolSelect = document.getElementById('toolSelect')!;
const toolWire = document.getElementById('toolWire')!;
const toolComp = document.getElementById('toolComp')!;

function setToolActive(btn: HTMLElement, tool: 'SELECT' | 'WIRE' | 'COMPONENT') {
  [toolSelect, toolWire, toolComp].forEach((b) => b.classList.remove('active'));
  btn.classList.add('active');
  canvas.setActiveTool(tool);
}

toolSelect.addEventListener('click', () => setToolActive(toolSelect, 'SELECT'));
toolWire.addEventListener('click', () => setToolActive(toolWire, 'WIRE'));
toolComp.addEventListener('click', () => setToolActive(toolComp, 'COMPONENT'));

document.getElementById('actionRotate')?.addEventListener('click', () => canvas.rotateSelected());
document.getElementById('actionDelete')?.addEventListener('click', () => canvas.deleteSelected());

// 3. RIGHT SIDEBAR COMPONENTS
const compButtons = document.querySelectorAll('.comp-grid-btn');
compButtons.forEach((btn) => {
  btn.addEventListener('click', () => {
    compButtons.forEach((b) => b.classList.remove('selected'));
    btn.classList.add('selected');
    const type = btn.getAttribute('data-type') as ComponentType;
    setToolActive(toolComp, 'COMPONENT');
    canvas.setActiveComponentType(type);
  });
});

// Wire Mode (Cable / Silver Wire)
const btnWireCable = document.getElementById('btnWireCable')!;
const btnWireSilver = document.getElementById('btnWireSilver')!;
const cableColorsBox = document.getElementById('cableColorsBox')!;

btnWireCable.addEventListener('click', () => {
  btnWireCable.classList.add('active');
  btnWireSilver.classList.remove('active');
  cableColorsBox.style.display = 'flex';
  canvas.setActiveWireKind('CABLE');
  setToolActive(toolWire, 'WIRE');
});

btnWireSilver.addEventListener('click', () => {
  btnWireSilver.classList.add('active');
  btnWireCable.classList.remove('active');
  cableColorsBox.style.display = 'none';
  canvas.setActiveWireKind('SILVER_WIRE');
  setToolActive(toolWire, 'WIRE');
});

document.querySelectorAll('.color-dot').forEach((dot) => {
  dot.addEventListener('click', () => {
    document.querySelectorAll('.color-dot').forEach((d) => d.classList.remove('active'));
    dot.classList.add('active');
    const col = dot.getAttribute('data-color') || '#E53935';
    canvas.setActiveWireColor(col);
  });
});

// 4. INSPECTOR PROPERTIES
const propDes = document.getElementById('propDesignator') as HTMLInputElement;
const propVal = document.getElementById('propValue') as HTMLInputElement;
const propVisible = document.getElementById('propVisible') as HTMLInputElement;
const resistorPreviewBox = document.getElementById('resistorPreviewBox')!;
const resistorColorStrip = document.getElementById('resistorColorStrip')!;
const resistorOhmLabel = document.getElementById('resistorOhmLabel')!;

function updateInspector(comp: PlacedComponent | null) {
  if (!comp) {
    const wire = canvas.getSelectedWire();
    if (wire) {
      propDes.value = wire.kind === 'SILVER_WIRE' ? 'SILVER' : 'WIRE';
      propVal.value = wire.kind === 'SILVER_WIRE' ? 'Silberdraht' : `Kabel (${wire.color})`;
    } else {
      propDes.value = '';
      propVal.value = '';
    }
    resistorPreviewBox.style.display = 'none';
    return;
  }

  propDes.value = comp.designator;
  propVal.value = comp.value;
  propVisible.checked = comp.visible !== false;

  if (comp.type === 'RESISTOR_AXIAL') {
    resistorPreviewBox.style.display = 'flex';
    const bands = parseResistorColorCode(comp.value);
    if (bands) {
      resistorColorStrip.innerHTML = `
        <div class="resistor-band" style="background: ${bands.band1};"></div>
        <div class="resistor-band" style="background: ${bands.band2};"></div>
        <div class="resistor-band" style="background: ${bands.multiplier};"></div>
        <div style="width: 8px;"></div>
        <div class="resistor-band" style="background: ${bands.tolerance};"></div>
      `;
      resistorOhmLabel.textContent = `${comp.value}Ω ±5%`;
    } else {
      resistorColorStrip.innerHTML = '';
      resistorOhmLabel.textContent = 'Invalid value';
    }
  } else {
    resistorPreviewBox.style.display = 'none';
  }
}

canvas.onComponentSelected = updateInspector;

propDes.addEventListener('input', () => {
  const sel = (canvas as any).selectedComponent;
  if (sel) {
    sel.designator = propDes.value;
    canvas.render();
  }
});

propVal.addEventListener('input', () => {
  const sel = (canvas as any).selectedComponent;
  if (sel) {
    sel.value = propVal.value;
    updateInspector(sel);
    canvas.render();
  }
});

propVisible.addEventListener('change', () => {
  const sel = (canvas as any).selectedComponent;
  if (sel) {
    sel.visible = propVisible.checked;
    canvas.render();
  }
});

// 5. STATUS BAR
const statHole = document.getElementById('statHoleLabel')!;
const statMm = document.getElementById('statMmLabel')!;

canvas.onHoverCoordinate = (gx, gy) => {
  statHole.textContent = `Hole: X: ${gx + 1}, Y: ${gy + 1}`;
  statMm.textContent = `Pos: ${(gx * 2.54).toFixed(2)} mm x ${(gy * 2.54).toFixed(2)} mm`;
};

// Keyboard Shortcuts identical to desktop
window.addEventListener('keydown', (e) => {
  if (document.activeElement?.tagName === 'INPUT') return;
  switch (e.key.toLowerCase()) {
    case 'v': setToolActive(toolSelect, 'SELECT'); break;
    case 'w': setToolActive(toolWire, 'WIRE'); break;
    case 'c': setToolActive(toolComp, 'COMPONENT'); break;
    case 'r': canvas.rotateSelected(); break;
    case 'delete':
    case 'backspace': canvas.deleteSelected(); break;
  }
});
