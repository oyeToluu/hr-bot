import { createContext, useContext, useEffect, useState, useRef } from 'react';
import { api } from '../api/client';

// Used until the saved settings arrive (and if the server can't be reached)
export const DEFAULT_SETTINGS = {
  islamicFeatures: false,
  city: 'LAGOS',
  asrMethod: 'STANDARD',
  taskReminders: true,
  prayerReminders: true,
  ramadanReminders: true,
  leaveAlerts: true,
  tone: 'rose-chime',
  volume: 70,
};

const SettingsContext = createContext(null);

export function SettingsProvider({ children }) {
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [loaded, setLoaded] = useState(false);
   const queue = useRef(Promise.resolve());

  useEffect(() => {
    api
      .mySettings()
      .then((s) => setSettings({ ...DEFAULT_SETTINGS, ...s }))
      .catch(() => {})
      .finally(() => setLoaded(true));
  }, []);

  const save = (changes) => {
    let before;
    setSettings((s) => {
      before = s;
      return { ...s, ...changes };
    });
    const run = queue.current.then(async () => {
      try {
        // The change is already showing, so nothing more to do on success
        return await api.saveSettings(changes);
      } catch (err) {
        // Put back only the fields this change touched
        setSettings((s) => {
          const undo = { ...s };
          for (const key of Object.keys(changes)) undo[key] = before[key];
          return undo;
        });
        throw err;
      }
    });
    queue.current = run.catch(() => {});
    return run;
};

  return (
    <SettingsContext.Provider value={{ settings, loaded, save }}>
      {children}
    </SettingsContext.Provider>
  );
}

export function useSettings() {
  return useContext(SettingsContext);
}