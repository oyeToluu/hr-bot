// Watches the clock while HR Bots is open and rings for:
//  - tasks due today (from 9 AM)
//  - prayer times, Jumu'ah, suhoor and iftar (Islamic features only)
//  - leave requests that HR has approved or rejected
// Each alert shows a pop-up in the page, plays the chosen tune and, if the
// browser allows it, a system notification. Alerts only work while the site
// is open in a browser tab.

import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import { formatDate } from './ui';
import { useAuth } from '../context/AuthContext';
import { useSettings } from '../context/SettingsContext';
import { isoDate, prayerTimes, ramadanInfo, formatTime } from '../lib/islamic';
import { playTone, unlockAudio } from '../lib/sounds';

const MINUTE = 60 * 1000;
const TASK_HOUR = 9; // tasks ring at 9 AM on the day they are due

/** Lets any page pop up an alert, e.g. the "Test alert" button in Settings. */
export function sendAlert(title, body) {
  window.dispatchEvent(new CustomEvent('hrbots:alert', { detail: { title, body } }));
}

// Remembers which alerts already rang, so each one rings only once
function readStore(key, fallback) {
  try {
    return JSON.parse(localStorage.getItem(key)) ?? fallback;
  } catch {
    return fallback;
  }
}
function writeStore(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // storage full or blocked: alerts may repeat, nothing worse
  }
}

export default function Notifier() {
  const { employee } = useAuth();
  const { settings } = useSettings();
  const [toasts, setToasts] = useState([]);
  const todosRef = useRef([]);
  const tickRef = useRef(() => {});
  const settingsRef = useRef(settings);
  settingsRef.current = settings;

  const ringKey = `hrbots_rang_${employee.id}`;
  const leaveKey = `hrbots_leave_${employee.id}`;

  // Show + sound + system notification
  const ring = (title, body) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t.slice(-3), { id, title, body }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 15000);
    const s = settingsRef.current;
    playTone(s.tone, s.volume);
    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification(title, { body, tag: title });
      } catch {
        // some phones only allow notifications from an installed app
      }
    }
  };

  // Ring once for a given key (e.g. "task:abc:2026-10-30")
  const ringOnce = (key, title, body) => {
    const rang = readStore(ringKey, {});
    if (rang[key]) return;
    rang[key] = Date.now();
    // forget anything older than 3 days so the list stays small
    for (const k of Object.keys(rang)) if (Date.now() - rang[k] > 3 * 24 * 60 * MINUTE) delete rang[k];
    writeStore(ringKey, rang);
    ring(title, body);
  };

  // Browsers block sound until the first click or tap on the page
  useEffect(() => {
    const unlock = () => unlockAudio();
    window.addEventListener('pointerdown', unlock, { once: true });
    window.addEventListener('keydown', unlock, { once: true });
    return () => {
      window.removeEventListener('pointerdown', unlock);
      window.removeEventListener('keydown', unlock);
    };
  }, []);

  // Alerts sent by other pages
  useEffect(() => {
    const onAlert = (e) => ring(e.detail.title, e.detail.body);
    window.addEventListener('hrbots:alert', onAlert);
    return () => window.removeEventListener('hrbots:alert', onAlert);
  });

  // Keep a fresh copy of my tasks (every 5 minutes)
  useEffect(() => {
    const load = () =>
      api
        .myTodos()
        .then((t) => {
          todosRef.current = t;
          tickRef.current(); // check straight away, don't wait for the clock
        })
        .catch(() => {});
    load();
    const timer = setInterval(load, 5 * MINUTE);
    return () => clearInterval(timer);
  }, []);

  // Leave decisions: compare each request's status with what we saw last time
  useEffect(() => {
    if (!settings.leaveAlerts) return undefined;
    const check = async () => {
      let requests;
      try {
        requests = await api.myLeave();
      } catch {
        return;
      }
      const seen = readStore(leaveKey, null);
      const now = Object.fromEntries(requests.map((r) => [r.id, r.status]));
      if (seen) {
        for (const r of requests) {
          if (seen[r.id] === 'PENDING' && (r.status === 'APPROVED' || r.status === 'REJECTED')) {
            const word = r.status === 'APPROVED' ? 'approved 🎉' : 'not approved';
            ring(`Leave ${word}`, `Your leave from ${formatDate(r.startDate)} to ${formatDate(r.endDate)} was ${r.status.toLowerCase()}.`);
          }
        }
      }
      writeStore(leaveKey, now);
    };
    check();
    const timer = setInterval(check, 2 * MINUTE);
    return () => clearInterval(timer);
  }, [settings.leaveAlerts]);

  // The clock: check everything every 30 seconds
  useEffect(() => {
    const tick = () => {
      const s = settingsRef.current;
      const now = new Date();
      const today = isoDate(now);

      if (s.taskReminders && now.getHours() >= TASK_HOUR) {
        for (const t of todosRef.current) {
          if (!t.done && t.dueDate === today) {
            ringOnce(`task:${t.id}:${today}`, 'Task due today', t.title);
          }
        }
      }

      if (!s.islamicFeatures) return;
      const prayers = prayerTimes(now, s.city, s.asrMethod);
      const at = (key) => prayers.find((p) => p.key === key).time;
      const ramadan = ramadanInfo(now);
      const iftarAlert = s.ramadanReminders && ramadan.inRamadan;
      const within = (time, beforeMin, afterMin) =>
        now >= time.getTime() - beforeMin * MINUTE && now < time.getTime() + afterMin * MINUTE;

      if (s.prayerReminders) {
        for (const p of prayers) {
          if (p.notAPrayer) continue;
          if (p.key === 'maghrib' && iftarAlert) continue; // the iftar alert covers it
          if (within(p.time, 0, 10)) {
            ringOnce(`prayer:${p.key}:${today}`, `Time for ${p.name}`, `${p.name} began at ${formatTime(p.time)}.`);
          }
        }
      }

      if (s.ramadanReminders) {
        if (now.getDay() === 5 && within(at('dhuhr'), 30, 0)) {
          ringOnce(`jumuah:${today}`, "Jumu'ah Mubarak", `Jumu'ah prayer is at ${formatTime(at('dhuhr'))}.`);
        }
        if (ramadan.inRamadan) {
          if (within(at('fajr'), 20, 0)) {
            ringOnce(`suhoor:${today}`, 'Suhoor reminder', `Fajr is at ${formatTime(at('fajr'))}. Time to finish suhoor.`);
          }
          if (within(at('maghrib'), 0, 10)) {
            ringOnce(`iftar:${today}`, 'Iftar time', `Maghrib is at ${formatTime(at('maghrib'))}. Ramadan day ${ramadan.day}.`);
          }
        }
      }
    };
    tickRef.current = tick;
    tick();
    const timer = setInterval(tick, 30 * 1000);
    return () => clearInterval(timer);
  }, []);

  // Check again as soon as settings arrive or change
  useEffect(() => tickRef.current(), [settings]);

  if (toasts.length === 0) return null;
  return (
    <div className="toasts" role="status" aria-live="polite">
      {toasts.map((t) => (
        <div key={t.id} className="toast">
          <span className="toast-bell" aria-hidden="true">🔔</span>
          <div>
            <strong>{t.title}</strong>
            <p>{t.body}</p>
          </div>
          <button
            className="toast-close"
            aria-label="Dismiss"
            onClick={() => setToasts((list) => list.filter((x) => x.id !== t.id))}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}