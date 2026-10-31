// Nigerian public holidays. The Federal Government announces the official
// dates each year (and may move a holiday that falls on a weekend), so these
// are the usual dates. The Eids and Mawlid come from the Hijri calendar.

import { islamicDatesBetween, isoDate } from './islamic';

const FIXED = [
  { month: 1, day: 1, name: "New Year's Day" },
  { month: 5, day: 1, name: "Workers' Day" },
  { month: 6, day: 12, name: 'Democracy Day' },
  { month: 10, day: 1, name: 'Independence Day' },
  { month: 12, day: 25, name: 'Christmas Day' },
  { month: 12, day: 26, name: 'Boxing Day' },
];

// Easter Sunday (Gregorian), using the Anonymous Gregorian algorithm
function easterSunday(year) {
  const a = year % 19;
  const b = Math.floor(year / 100);
  const c = year % 100;
  const d = Math.floor(b / 4);
  const e = b % 4;
  const f = Math.floor((b + 8) / 25);
  const g = Math.floor((b - f + 1) / 3);
  const h = (19 * a + b - d - g + 15) % 30;
  const i = Math.floor(c / 4);
  const k = c % 4;
  const l = (32 + 2 * e + 2 * i - h - k) % 7;
  const m = Math.floor((a + 11 * h + 22 * l) / 451);
  const month = Math.floor((h + l - 7 * m + 114) / 31);
  const day = ((h + l - 7 * m + 114) % 31) + 1;
  return new Date(year, month - 1, day);
}

/**
 * Public holidays between two dates (inclusive):
 * [{ date: 'YYYY-MM-DD', name, estimated }]
 */
export function publicHolidaysBetween(start, end) {
  const out = [];
  for (let year = start.getFullYear(); year <= end.getFullYear(); year++) {
    for (const h of FIXED) {
      out.push({ date: isoDate(new Date(year, h.month - 1, h.day)), name: h.name, estimated: false });
    }
    const easter = easterSunday(year);
    const goodFriday = new Date(easter);
    goodFriday.setDate(easter.getDate() - 2);
    const easterMonday = new Date(easter);
    easterMonday.setDate(easter.getDate() + 1);
    out.push({ date: isoDate(goodFriday), name: 'Good Friday', estimated: false });
    out.push({ date: isoDate(easterMonday), name: 'Easter Monday', estimated: false });
  }

  for (const e of islamicDatesBetween(start, end)) {
    if (e.publicHoliday) out.push({ date: e.date, name: e.name, estimated: true });
  }

  const from = isoDate(start);
  const to = isoDate(end);
  return out.filter((h) => h.date >= from && h.date <= to);
}