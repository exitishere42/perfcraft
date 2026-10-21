import { PerfProject } from './models.ts';

export class HistoryManager {
  private undoStack: string[] = [];
  private redoStack: string[] = [];
  private readonly maxHistory: number;
  private isApplyingHistory: boolean = false;

  public onStateChanged?: (canUndo: boolean, canRedo: boolean) => void;

  constructor(maxHistory: number = 50) {
    this.maxHistory = maxHistory;
  }

  public pushState(project: PerfProject): void {
    if (this.isApplyingHistory) return;

    try {
      const snapshot = JSON.stringify(project);
      // Vermeide Duplikate des identischen Zustands
      if (this.undoStack.length > 0 && this.undoStack[this.undoStack.length - 1] === snapshot) {
        return;
      }

      this.undoStack.push(snapshot);
      if (this.undoStack.length > this.maxHistory) {
        this.undoStack.shift();
      }
      this.redoStack = []; // Nach einer neuen Aktion Redo-Historie verwerfen
      this.notify();
    } catch (e) {
      console.warn('Failed to snapshot history state:', e);
    }
  }

  public undo(currentProject: PerfProject): PerfProject | null {
    if (this.undoStack.length === 0) return null;

    try {
      this.isApplyingHistory = true;
      // Aktuellen Zustand auf den Redo-Stack legen
      this.redoStack.push(JSON.stringify(currentProject));

      // Vorherigen Zustand vom Undo-Stack holen
      const snapshot = this.undoStack.pop()!;
      const restoredProject: PerfProject = JSON.parse(snapshot);
      return restoredProject;
    } finally {
      this.isApplyingHistory = false;
      this.notify();
    }
  }

  public redo(currentProject: PerfProject): PerfProject | null {
    if (this.redoStack.length === 0) return null;

    try {
      this.isApplyingHistory = true;
      // Aktuellen Zustand auf den Undo-Stack legen
      this.undoStack.push(JSON.stringify(currentProject));

      // Nächsten Zustand vom Redo-Stack holen
      const snapshot = this.redoStack.pop()!;
      const restoredProject: PerfProject = JSON.parse(snapshot);
      return restoredProject;
    } finally {
      this.isApplyingHistory = false;
      this.notify();
    }
  }

  public clear(): void {
    this.undoStack = [];
    this.redoStack = [];
    this.notify();
  }

  public canUndo(): boolean {
    return this.undoStack.length > 0;
  }

  public canRedo(): boolean {
    return this.redoStack.length > 0;
  }

  private notify(): void {
    if (this.onStateChanged) {
      this.onStateChanged(this.canUndo(), this.canRedo());
    }
  }
}
