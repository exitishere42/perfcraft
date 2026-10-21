export interface ResistorColorBands {
  band1: string;
  band2: string;
  multiplier: string;
  tolerance: string;
}

const DIGIT_COLORS: Record<number, string> = {
  0: '#1A1A1A', // Schwarz
  1: '#8D6E63', // Braun
  2: '#E53935', // Rot
  3: '#FB8C00', // Orange
  4: '#FDD835', // Gelb
  5: '#43A047', // Grün
  6: '#1E88E5', // Blau
  7: '#8E24AA', // Violett
  8: '#757575', // Grau
  9: '#FAFAFA', // Weiß
};

const MULT_COLORS: Record<number, string> = {
  0: '#1A1A1A',
  1: '#8D6E63',
  2: '#E53935',
  3: '#FB8C00',
  4: '#FDD835',
  5: '#43A047',
  6: '#1E88E5',
  7: '#8E24AA',
  8: '#757575',
  9: '#FAFAFA',
  '-1': '#D4AF37', // Gold
  '-2': '#C0C0C0', // Silber
};

export function parseResistorColorCode(rawVal: string): ResistorColorBands | null {
  if (!rawVal) return null;
  const s = rawVal.trim().toLowerCase().replace('ω', '').replace('ohm', '');

  let numeric = 0;
  try {
    if (s.endsWith('k')) {
      numeric = parseFloat(s.slice(0, -1)) * 1000.0;
    } else if (s.endsWith('m')) {
      numeric = parseFloat(s.slice(0, -1)) * 1000000.0;
    } else if (s.includes('k')) {
      numeric = parseFloat(s.replace('k', '.')) * 1000.0;
    } else {
      numeric = parseFloat(s);
    }
  } catch {
    return null;
  }

  if (isNaN(numeric) || numeric <= 0) return null;

  let val = numeric;
  let exp = 0;
  while (val >= 100.0) {
    val /= 10.0;
    exp++;
  }
  while (val < 10.0 && exp > -2) {
    val *= 10.0;
    exp--;
  }

  const rounded = Math.round(val);
  const d1 = Math.floor(rounded / 10);
  const d2 = rounded % 10;

  return {
    band1: DIGIT_COLORS[d1] || '#8D6E63',
    band2: DIGIT_COLORS[d2] || '#1A1A1A',
    multiplier: MULT_COLORS[exp] || '#D4AF37',
    tolerance: '#D4AF37', // Standard Gold 5%
  };
}
