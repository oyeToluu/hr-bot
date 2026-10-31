import { createContext, useContext, useEffect, useState } from 'react';
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

  useEffect(() => {
    api
      .mySettings()
      .then((s) => setSettings({ ...DEFAULT_SETTINGS, ...s }))
      .catch(() => {})
      .finally(() => setLoaded(true));
  }, []);

  // Changes show straight away, then get saved. Returns the saved settings.
  const save = async (changes) => {
    setSettings((s) => ({ ...s, ...changes }));
    const saved = await api.saveSettings(changes);
    setSettings({ ...DEFAULT_SETTINGS, ...saved });
    return saved;
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