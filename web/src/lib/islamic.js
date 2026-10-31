// Hijri dates, Islamic dates and prayer times, worked out on the device.
// Hijri dates use the Umm al-Qura calendar built into the browser. Actual
// dates of Ramadan and the Eids depend on moon sighting and official
// announcements, so they are shown as estimates.

import { CalculationMethod, Coordinates, Madhab, PrayerTimes } from 'adhan';

export const HIJRI_MONTHS = [
  'Muharram',
  'Safar',
  "Rabi' al-Awwal",
  "Rabi' al-Thani",
  'Jumada al-Ula',
  'Jumada al-Akhirah',
  'Rajab',
  "Sha'ban",
  'Ramadan',
  'Shawwal',
  "Dhu al-Qa'dah",
  'Dhu al-Hijjah',
];

// Cities for prayer times (latitude, longitude). All on West Africa Time.
export const CITIES = {
  LAGOS: { name: 'Lagos', lat: 6.5244, lng: 3.3792 },
  ABUJA: { name: 'Abuja', lat: 9.0765, lng: 7.3986 },
  KANO: { name: 'Kano', lat: 12.0022, lng: 8.592 },
  IBADAN: { name: 'Ibadan', lat: 7.3775, lng: 3.947 },
  KADUNA: { name: 'Kaduna', lat: 10.5105, lng: 7.4165 },
  PORT_HARCOURT: { name: 'Port Harcourt', lat: 4.8156, lng: 7.0498 },
  BENIN_CITY: { name: 'Benin City', lat: 6.335, lng: 5.6037 },
  ILORIN: { name: 'Ilorin', lat: 8.4966, lng: 4.5421 },
  ABEOKUTA: { name: 'Abeokuta', lat: 7.1475, lng: 3.3619 },
  OSOGBO: { name: 'Osogbo', lat: 7.7827, lng: 4.5418 },
  SOKOTO: { name: 'Sokoto', lat: 13.0059, lng: 5.2476 },
  ZARIA: { name: 'Zaria', lat: 11.0855, lng: 7.7199 },
  JOS: { name: 'Jos', lat: 9.8965, lng: 8.8583 },
  MAIDUGURI: { name: 'Maiduguri', lat: 11.8311, lng: 13.151 },
  ENUGU: { name: 'Enugu', lat: 6.4584, lng: 7.5464 },
};

const hijriFormatter = new Intl.DateTimeFormat('en-u-ca-islamic-umalqura', {
  day: 'numeric',
  month: 'numeric',
  year: 'numeric',
});

/** { day, month (1-12), year } of a date in the Hijri calendar. */
export function hijri(date) {
  // Noon avoids any edge-of-day surprises
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 12);
  const parts = Object.fromEntries(hijriFormatter.formatToParts(d).map((p) => [p.type, p.value]));
  return { day: Number(parts.day), month: Number(parts.month), year: Number(parts.year) };
}

/** e.g. "19 Jumada al-Ula 1448 AH" */
export function formatHijri(date) {
  const h = hijri(date);
  return `${h.day} ${HIJRI_MONTHS[h.month - 1]} ${h.year} AH`;
}

// Islamic dates by Hijri month/day. publicHoliday = a Nigerian public holiday.
const ISLAMIC_DATES = [
  { month: 1, day: 1, name: 'Islamic New Year' },
  { month: 1, day: 10, name: 'Ashura' },
  { month: 3, day: 12, name: 'Eid al-Mawlid', publicHoliday: true },
  { month: 9, day: 1, name: 'Ramadan begins' },
  { month: 10, day: 1, name: 'Eid al-Fitr', publicHoliday: true },
  { month: 10, day: 2, name: 'Eid al-Fitr holiday', publicHoliday: true },
  { month: 12, day: 9, name: 'Day of Arafah' },
  { month: 12, day: 10, name: 'Eid al-Adha', publicHoliday: true },
  { month: 12, day: 11, name: 'Eid al-Adha holiday', publicHoliday: true },
];

/**
 * Islamic dates between two dates (inclusive).
 * Returns [{ date: 'YYYY-MM-DD', name, publicHoliday }]
 */
export function islamicDatesBetween(start, end) {
  const out = [];
  const d = new Date(start.getFullYear(), start.getMonth(), start.getDate());
  while (d <= end) {
    const h = hijri(d);
    for (const e of ISLAMIC_DATES) {
      if (e.month === h.month && e.day === h.day) {
        out.push({ date: isoDate(d), name: e.name, publicHoliday: !!e.publicHoliday });
      }
    }
    d.setDate(d.getDate() + 1);
  }
  return out;
}

/** Today's five prayers (plus sunrise) for the employee's city and Asr method. */
export function prayerTimes(date, cityKey = 'LAGOS', asrMethod = 'STANDARD') {
  const city = CITIES[cityKey] || CITIES.LAGOS;
  const params = CalculationMethod.MuslimWorldLeague();
  params.madhab = asrMethod === 'HANAFI' ? Madhab.Hanafi : Madhab.Shafi;
  const t = new PrayerTimes(new Coordinates(city.lat, city.lng), date, params);
  return [
    { key: 'fajr', name: 'Fajr', time: t.fajr },
    { key: 'sunrise', name: 'Sunrise', time: t.sunrise, notAPrayer: true },
    { key: 'dhuhr', name: date.getDay() === 5 ? "Dhuhr (Jumu'ah)" : 'Dhuhr', time: t.dhuhr },
    { key: 'asr', name: 'Asr', time: t.asr },
    { key: 'maghrib', name: 'Maghrib', time: t.maghrib },
    { key: 'isha', name: 'Isha', time: t.isha },
  ];
}

/** The next prayer from now (looks into tomorrow after Isha). */
export function nextPrayer(now, cityKey, asrMethod) {
  const today = prayerTimes(now, cityKey, asrMethod).filter((p) => !p.notAPrayer);
  const next = today.find((p) => p.time > now);
  if (next) return next;
  const tomorrow = new Date(now);
  tomorrow.setDate(tomorrow.getDate() + 1);
  return prayerTimes(tomorrow, cityKey, asrMethod).find((p) => p.key === 'fajr');
}

/**
 * Ramadan status for a date:
 *  { inRamadan: true, day } during Ramadan, otherwise
 *  { inRamadan: false, startsOn: Date, daysUntil } for the next Ramadan.
 */
export function ramadanInfo(date) {
  const h = hijri(date);
  if (h.month === 9) return { inRamadan: true, day: h.day };
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  for (let i = 1; i <= 400; i++) {
    d.setDate(d.getDate() + 1);
    const x = hijri(d);
    if (x.month === 9 && x.day === 1) {
      return { inRamadan: false, startsOn: new Date(d), daysUntil: i };
    }
  }
  return { inRamadan: false };
}

/** The next Friday on or after a date. */
export function nextFriday(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  while (d.getDay() !== 5) d.setDate(d.getDate() + 1);
  return d;
}

export function isoDate(d) {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

export function formatTime(date) {
  return date.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
}