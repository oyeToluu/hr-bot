// Alert tunes, made in the browser with the Web Audio API.
// All tunes are original and generated in code, so there are no sound files
// to download and no copyright to worry about.

export const TONES = [
  { key: 'rose-chime', name: 'Rose chime' },
  { key: 'gold-bell', name: 'Gold bell' },
  { key: 'marimba', name: 'Marimba' },
  { key: 'crystal', name: 'Crystal' },
  { key: 'soft-harp', name: 'Soft harp' },
  { key: 'sunrise', name: 'Sunrise' },
  { key: 'classic-beep', name: 'Classic beep' },
];

let ctx = null;

function audio() {
  if (!ctx) {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    ctx = new AC();
  }
  return ctx;
}

/**
 * Browsers only allow sound after the person has clicked or tapped the page.
 * Call this from any click to "unlock" sound for the rest of the visit.
 */
export function unlockAudio() {
  const c = audio();
  if (c && c.state === 'suspended') c.resume();
}

// One note: a wave with a quick attack and a smooth fade-out
function note(c, out, freq, start, length, type = 'sine', level = 1) {
  const osc = c.createOscillator();
  const gain = c.createGain();
  osc.type = type;
  osc.frequency.setValueAtTime(freq, start);
  gain.gain.setValueAtTime(0.0001, start);
  gain.gain.exponentialRampToValueAtTime(level, start + 0.015);
  gain.gain.exponentialRampToValueAtTime(0.0001, start + length);
  osc.connect(gain).connect(out);
  osc.start(start);
  osc.stop(start + length + 0.05);
}

// A bell: a few slightly "out of tune" overtones that fade at different speeds
function bell(c, out, freq, start, length, level = 1) {
  [
    [1, 1],
    [2.76, 0.45],
    [5.4, 0.25],
    [8.93, 0.12],
  ].forEach(([ratio, l]) => note(c, out, freq * ratio, start, length / ratio ** 0.4, 'sine', level * l));
}

const PATTERNS = {
  'rose-chime': (c, o, t) => {
    bell(c, o, 1318.5, t, 1.4, 0.5);
    bell(c, o, 987.8, t + 0.22, 1.8, 0.5);
  },
  'gold-bell': (c, o, t) => {
    bell(c, o, 523.25, t, 2.6, 0.7);
    bell(c, o, 523.25, t + 1.1, 2.6, 0.45);
  },
  marimba: (c, o, t) => {
    [523.25, 659.25, 783.99, 1046.5].forEach((f, i) => {
      note(c, o, f, t + i * 0.12, 0.45, 'triangle', 0.7);
      note(c, o, f * 4, t + i * 0.12, 0.08, 'sine', 0.15);
    });
  },
  crystal: (c, o, t) => {
    [1567.98, 1975.53, 2349.32, 3135.96].forEach((f, i) => {
      note(c, o, f, t + i * 0.09, 1.1, 'sine', 0.35);
      note(c, o, f * 1.003, t + i * 0.09, 1.1, 'sine', 0.2);
    });
  },
  'soft-harp': (c, o, t) => {
    [523.25, 587.33, 659.25, 783.99, 880, 1046.5].forEach((f, i) => {
      note(c, o, f, t + i * 0.1, 1.2, 'triangle', 0.5);
    });
  },
  sunrise: (c, o, t) => {
    [392, 523.25, 659.25, 783.99].forEach((f, i) => {
      note(c, o, f, t + i * 0.28, 1.4, 'sine', 0.55);
      note(c, o, f / 2, t + i * 0.28, 1.4, 'sine', 0.2);
    });
  },
  'classic-beep': (c, o, t) => {
    note(c, o, 880, t, 0.16, 'square', 0.25);
    note(c, o, 880, t + 0.25, 0.16, 'square', 0.25);
  },
};

/** Plays a tune at a volume from 0 to 100. Does nothing if sound isn't available. */
export function playTone(key = 'rose-chime', volume = 70) {
  const c = audio();
  if (!c || volume <= 0) return;
  if (c.state === 'suspended') c.resume();
  const master = c.createGain();
  master.gain.value = (Math.min(100, volume) / 100) * 0.6;
  master.connect(c.destination);
  const pattern = PATTERNS[key] || PATTERNS['rose-chime'];
  pattern(c, master, c.currentTime + 0.03);
}