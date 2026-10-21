import { createDefaultProject, PerfProject, PlacedComponent } from './models.ts';
import { PerfBoardCanvas } from './canvas.ts';
import { parseResistorColorCode } from './resistor.ts';

let currentProject: PerfProject = createDefaultProject();
const canvasEl = document.getElementById('perfCanvas') as HTMLCanvasElement;
const canvas = new PerfBoardCanvas(canvasEl, currentProject);

// Header buttons
document.getElementById('btnNew')?.addEventListener('click', () => {
  if (confirm('Neues Projekt anlegen? Nicht gespeicherte Änderungen gehen verloren.')) {
    currentProject = createDefaultProject();
    canvas.setProject(currentProject);
  }
});

// File Open (.perf)
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
          document.title = `PerfCraft Web - ${currentProject.name}`;
        } else {
          alert('Ungültige .perf-Datei: Erforderliche Felder fehlen.');
        }
      } catch (err) {
        alert('Fehler beim Parsen der Datei: ' + err);
      }
    };
    reader.readAsText(file);
  }
});

// Drag & Drop .perf file directly onto window/canvas
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
        document.title = `PerfCraft Web - ${currentProject.name}`;
      } catch (err) {
        alert('Fehler beim Laden: ' + err);
      }
    };
    reader.readAsText(file);
  }
});

// Save Project (.perf)
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

// Tool selection
const btnSelect = document.getElementById('toolSelect')!;
const btnWire = document.getElementById('toolWire')!;
const btnComp = document.getElementById('toolComp')!;

function setToolActive(activeBtn: HTMLElement, tool: 'SELECT' | 'WIRE' | 'COMPONENT') {
  [btnSelect, btnWire, btnComp].forEach((b) => b.classList.remove('active'));
  activeBtn.classList.add('active');
  canvas.setActiveTool(tool);
}

btnSelect.addEventListener('click', () => setToolActive(btnSelect, 'SELECT'));
btnWire.addEventListener('click', () => setToolActive(btnWire, 'WIRE'));
btnComp.addEventListener('click', () => setToolActive(btnComp, 'COMPONENT'));

// Component chips
document.querySelectorAll('.comp-chip').forEach((el) => {
  el.addEventListener('click', () => {
    document.querySelectorAll('.comp-chip').forEach((c) => c.classList.remove('selected'));
    el.classList.add('selected');
    const type = el.getAttribute('data-type') as any;
    setToolActive(btnComp, 'COMPONENT');
    canvas.setActiveComponentType(type);
  });
});

// Layers
document.getElementById('btnLayerTop')?.addEventListener('click', () => canvas.setLayer('TOP'));
document.getElementById('btnLayerBottom')?.addEventListener('click', () => canvas.setLayer('BOTTOM'));
document.getElementById('btnLayerBoth')?.addEventListener('click', () => canvas.setLayer('BOTH'));

// Inspector elements
const propDes = document.getElementById('propDesignator') as HTMLInputElement;
const propVal = document.getElementById('propValue') as HTMLInputElement;
const bandsPreview = document.getElementById('bandsPreview')!;

function updateInspector(comp: PlacedComponent | null) {
  if (!comp) {
    propDes.value = '';
    propVal.value = '';
    bandsPreview.innerHTML = '<div style="color: #666; font-size: 11px; text-align: center;">Keine Auswahl</div>';
    return;
  }
  propDes.value = comp.designator;
  propVal.value = comp.value;

  if (comp.type === 'RESISTOR_AXIAL') {
    const bands = parseResistorColorCode(comp.value);
    if (bands) {
      bandsPreview.innerHTML = `
        <div style="display: flex; gap: 6px; align-items: center; justify-content: center; height: 100%;">
          <div style="width: 8px; height: 26px; background: ${bands.band1}; border-radius: 2px;"></div>
          <div style="width: 8px; height: 26px; background: ${bands.band2}; border-radius: 2px;"></div>
          <div style="width: 8px; height: 26px; background: ${bands.multiplier}; border-radius: 2px;"></div>
          <div style="width: 8px; height: 26px; background: ${bands.tolerance}; border-radius: 2px;"></div>
        </div>
      `;
    } else {
      bandsPreview.innerHTML = '<div style="color: #666; font-size: 11px;">Ungültiger Wert</div>';
    }
  } else {
    bandsPreview.innerHTML = `<div style="color: #888; font-size: 11px; text-align: center;">${comp.type}</div>`;
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

// Floating touch action bar
document.getElementById('actionRotate')?.addEventListener('click', () => canvas.rotateSelected());
document.getElementById('actionDelete')?.addEventListener('click', () => canvas.deleteSelected());

// Keyboard shortcuts
window.addEventListener('keydown', (e) => {
  if (document.activeElement?.tagName === 'INPUT') return;
  switch (e.key.toLowerCase()) {
    case 'v': setToolActive(btnSelect, 'SELECT'); break;
    case 'w': setToolActive(btnWire, 'WIRE'); break;
    case 'c': setToolActive(btnComp, 'COMPONENT'); break;
    case 'r': canvas.rotateSelected(); break;
    case 'delete':
    case 'backspace': canvas.deleteSelected(); break;
  }
});
