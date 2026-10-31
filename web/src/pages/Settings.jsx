import { useState } from 'react';
import { ErrorBox } from '../components/ui';
import { sendAlert } from '../components/Notifier';
import { useSettings } from '../context/SettingsContext';
import { CITIES } from '../lib/islamic';
import { TONES, playTone, unlockAudio } from '../lib/sounds';

function Toggle({ checked, onChange, title, hint }) {
  return (
    <label className="toggle-row">
      <span>
        <strong>{title}</strong>
        {hint && <span className="muted small toggle-hint">{hint}</span>}
      </span>
      <input type="checkbox" className="switch" checked={checked} onChange={(e) => onChange(e.target.checked)} />
    </label>
  );
}

export default function Settings() {
  const { settings, save } = useSettings();
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(false);
  const [volume, setVolume] = useState(null); // while the slider is being dragged
  const [permission, setPermission] = useState(
    'Notification' in window ? Notification.permission : 'unsupported',
  );

  // Every change saves straight away
  const change = async (changes) => {
    setError('');
    setSaved(false);
    try {
      await save(changes);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (e) {
      setError(e.message);
    }
  };

  const pickTone = (key) => {
    unlockAudio();
    playTone(key, settings.volume);
    change({ tone: key });
  };

  // Save the volume (and play a preview) when the slider is let go
  const saveVolume = () => {
    if (volume === null) return;
    playTone(settings.tone, volume);
    change({ volume });
    setVolume(null);
  };

  const askPermission = async () => {
    const result = await Notification.requestPermission();
    setPermission(result);
  };

  return (
    <>
      <h1>Alerts &amp; settings</h1>
      <p className="muted">
        Choose what HR Bots reminds you about and how it sounds.{' '}
        {saved && <span className="saved-note">Saved ✓</span>}
      </p>
      <ErrorBox error={error} />

      <div className="settings-grid">
        <section className="card">
          <h2>Reminders</h2>
          <Toggle
            title="Task reminders"
            hint="Rings at 9 AM on the day a task is due."
            checked={settings.taskReminders}
            onChange={(v) => change({ taskReminders: v })}
          />
          <Toggle
            title="Leave decisions"
            hint="Rings when HR approves or rejects your leave."
            checked={settings.leaveAlerts}
            onChange={(v) => change({ leaveAlerts: v })}
          />

          <h3 className="settings-sub">Desktop notifications</h3>
          {permission === 'granted' && <p className="small ok-text">On. Alerts also appear outside the browser.</p>}
          {permission === 'default' && (
            <button className="secondary" onClick={askPermission}>
              Allow notifications
            </button>
          )}
          {permission === 'denied' && (
            <p className="small muted">
              Blocked by your browser. Click the lock icon next to the web address to allow them.
            </p>
          )}
          {permission === 'unsupported' && <p className="small muted">This browser doesn't support notifications.</p>}
          <p className="small muted">Alerts work while HR Bots is open in a browser tab.</p>
        </section>

        <section className="card">
          <h2>Islamic features</h2>
          <Toggle
            title="Show Islamic features"
            hint="Hijri dates, Islamic days, prayer times, Ramadan and Jumu'ah."
            checked={settings.islamicFeatures}
            onChange={(v) => change({ islamicFeatures: v })}
          />
          {settings.islamicFeatures && (
            <>
              <div className="form-grid settings-pair">
                <label>
                  City
                  <select value={settings.city} onChange={(e) => change({ city: e.target.value })}>
                    {Object.entries(CITIES).map(([key, c]) => (
                      <option key={key} value={key}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Asr time
                  <select value={settings.asrMethod} onChange={(e) => change({ asrMethod: e.target.value })}>
                    <option value="STANDARD">Standard (Shafi'i, Maliki, Hanbali)</option>
                    <option value="HANAFI">Hanafi (later)</option>
                  </select>
                </label>
              </div>
              <Toggle
                title="Prayer time reminders"
                hint="Rings at Fajr, Dhuhr, Asr, Maghrib and Isha."
                checked={settings.prayerReminders}
                onChange={(v) => change({ prayerReminders: v })}
              />
              <Toggle
                title="Jumu'ah, suhoor & iftar"
                hint="30 min before Jumu'ah; in Ramadan, 20 min before Fajr and at Maghrib."
                checked={settings.ramadanReminders}
                onChange={(v) => change({ ramadanReminders: v })}
              />
            </>
          )}
        </section>

        <section className="card settings-tones">
          <h2>Alert tune</h2>
          <p className="muted small">Tap a tune to hear it. The one you tap is saved.</p>
          <div className="tone-list">
            {TONES.map((t) => (
              <button
                key={t.key}
                className={`tone-option ${settings.tone === t.key ? 'is-chosen' : ''}`}
                onClick={() => pickTone(t.key)}
                aria-pressed={settings.tone === t.key}
              >
                <span className="tone-play" aria-hidden="true">▶</span>
                {t.name}
                {settings.tone === t.key && <span className="tone-tick">✓</span>}
              </button>
            ))}
          </div>

          <label className="volume-row">
            Volume <span className="muted small">{volume ?? settings.volume}%</span>
            <input
              type="range"
              min="0"
              max="100"
              step="5"
              value={volume ?? settings.volume}
              onChange={(e) => setVolume(Number(e.target.value))}
              onPointerUp={saveVolume}
              onKeyUp={saveVolume}
            />
          </label>

          <button onClick={() => sendAlert('Test alert', 'This is how your reminders will look and sound.')}>
            Send a test alert
          </button>
        </section>
      </div>
    </>
  );
}