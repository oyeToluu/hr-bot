import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { label } from '../components/ui';
import { useSettings } from '../context/SettingsContext';
import {
  CITIES,
  HIJRI_MONTHS,
  formatHijri,
  formatTime,
  hijri,
  islamicDatesBetween,
  isoDate,
  nextFriday,
  nextPrayer,
  prayerTimes,
  ramadanInfo,
} from '../lib/islamic';
import { publicHolidaysBetween } from '../lib/holidays';

const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

// "in 2 h 15 min" / "in 8 min"
function countdown(to, now) {
  const mins = Math.max(0, Math.round((to - now) / 60000));
  if (mins < 60) return `in ${mins} min`;
  return `in ${Math.floor(mins / 60)} h ${mins % 60} min`;
}

// Every day from start to end (inclusive) as 'YYYY-MM-DD'
function daysBetween(startIso, endIso) {
  const out = [];
  const d = new Date(`${startIso}T00:00:00`);
  const end = new Date(`${endIso}T00:00:00`);
  while (d <= end) {
    out.push(isoDate(d));
    d.setDate(d.getDate() + 1);
  }
  return out;
}

export default function Calendar() {
  const { settings } = useSettings();
  const islamic = settings.islamicFeatures;
  const [now, setNow] = useState(new Date());
  const [month, setMonth] = useState(() => new Date(now.getFullYear(), now.getMonth(), 1));
  const [selected, setSelected] = useState(isoDate(now));
  const [todos, setTodos] = useState([]);
  const [leave, setLeave] = useState([]);

  useEffect(() => {
    api.myTodos().then(setTodos).catch(() => {});
    api.myLeave().then(setLeave).catch(() => {});
    const timer = setInterval(() => setNow(new Date()), 30 * 1000);
    return () => clearInterval(timer);
  }, []);

  // The 6-week grid, starting on a Monday
  const cells = useMemo(() => {
    const first = new Date(month);
    const offset = (first.getDay() + 6) % 7;
    const start = new Date(first);
    start.setDate(first.getDate() - offset);
    return Array.from({ length: 42 }, (_, i) => {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      return d;
    });
  }, [month]);

  // Everything happening on each day: { 'YYYY-MM-DD': [{ kind, text }] }
  const events = useMemo(() => {
    const map = {};
    const add = (date, kind, text) => (map[date] ||= []).push({ kind, text });
    const start = cells[0];
    const end = cells[cells.length - 1];

    for (const h of publicHolidaysBetween(start, end)) {
      add(h.date, 'holiday', h.estimated ? `${h.name} (expected)` : h.name);
    }
    if (islamic) {
      for (const e of islamicDatesBetween(start, end)) {
        if (!e.publicHoliday) add(e.date, 'islamic', e.name);
      }
    }
    for (const t of todos) {
      if (t.dueDate) add(t.dueDate, t.done ? 'task-done' : 'task', t.title);
    }
    for (const r of leave) {
      if (r.status !== 'APPROVED' && r.status !== 'PENDING') continue;
      const text = `${label(r.leaveType)} leave${r.status === 'PENDING' ? ' (pending)' : ''}`;
      for (const day of daysBetween(r.startDate, r.endDate)) add(day, 'leave', text);
    }
    return map;
  }, [cells, todos, leave, islamic]);

  const move = (n) => setMonth((m) => new Date(m.getFullYear(), m.getMonth() + n, 1));
  const goToday = () => {
    setMonth(new Date(now.getFullYear(), now.getMonth(), 1));
    setSelected(isoDate(now));
  };

  // e.g. "Jumada al-Ula – Jumada al-Akhirah 1448 AH"
  const hijriRange = useMemo(() => {
    const a = hijri(month);
    const b = hijri(new Date(month.getFullYear(), month.getMonth() + 1, 0));
    const first = HIJRI_MONTHS[a.month - 1];
    const last = HIJRI_MONTHS[b.month - 1];
    if (a.month === b.month) return `${first} ${a.year} AH`;
    return a.year === b.year ? `${first} – ${last} ${b.year} AH` : `${first} ${a.year} – ${last} ${b.year} AH`;
  }, [month]);

  const today = isoDate(now);
  const selectedDate = new Date(`${selected}T00:00:00`);
  const selectedEvents = events[selected] || [];

  return (
    <>
      <h1>Calendar</h1>
      <p className="muted">
        Your tasks, leave and public holidays
        {islamic ? ', with Hijri dates, Islamic days and prayer times.' : '.'}
      </p>

      <div className="cal-layout">
        <section className="card cal-card">
          <div className="cal-head">
            <button className="secondary cal-nav" onClick={() => move(-1)} aria-label="Previous month">
              ‹
            </button>
            <div className="cal-title">
              <h2>{month.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })}</h2>
              {islamic && <span className="cal-hijri-range">{hijriRange}</span>}
            </div>
            <button className="secondary cal-nav" onClick={() => move(1)} aria-label="Next month">
              ›
            </button>
            <button className="link-btn cal-today-btn" onClick={goToday}>
              Today
            </button>
          </div>

          <div className="cal-grid">
            {WEEKDAYS.map((w) => (
              <div key={w} className={`cal-weekday ${islamic && w === 'Fri' ? 'is-friday' : ''}`}>
                {w}
              </div>
            ))}
            {cells.map((d) => {
              const key = isoDate(d);
              const list = events[key] || [];
              const classes = [
                'cal-day',
                d.getMonth() !== month.getMonth() && 'other-month',
                key === today && 'is-today',
                key === selected && 'is-selected',
                islamic && d.getDay() === 5 && 'is-friday',
                list.some((e) => e.kind === 'holiday') && 'is-holiday',
              ]
                .filter(Boolean)
                .join(' ');
              return (
                <button key={key} className={classes} onClick={() => setSelected(key)}>
                  <span className="cal-num">{d.getDate()}</span>
                  {islamic && <span className="cal-hijri">{hijri(d).day}</span>}
                  <span className="cal-dots">
                    {list.slice(0, 4).map((e, i) => (
                      <span key={i} className={`cal-dot dot-${e.kind}`} />
                    ))}
                  </span>
                  {list[0] && <span className="cal-first">{list[0].text}</span>}
                </button>
              );
            })}
          </div>

          <div className="cal-legend">
            <span><i className="cal-dot dot-task" /> Task</span>
            <span><i className="cal-dot dot-leave" /> Leave</span>
            <span><i className="cal-dot dot-holiday" /> Public holiday</span>
            {islamic && <span><i className="cal-dot dot-islamic" /> Islamic day</span>}
          </div>
        </section>

        <aside className="cal-side">
          <section className="card">
            <h2 className="cal-side-title">
              {selectedDate.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long' })}
            </h2>
            {islamic && <p className="cal-hijri-line">{formatHijri(selectedDate)}</p>}
            {selectedEvents.length === 0 ? (
              <p className="muted small">Nothing on this day.</p>
            ) : (
              <ul className="cal-events">
                {selectedEvents.map((e, i) => (
                  <li key={i}>
                    <span className={`cal-dot dot-${e.kind}`} /> {e.text}
                  </li>
                ))}
              </ul>
            )}
            {islamic && <PrayerList date={selectedDate} now={now} settings={settings} />}
          </section>

          {islamic ? (
            <IslamicCards now={now} settings={settings} />
          ) : (
            <section className="card cal-optin">
              <h2 className="cal-side-title">Muslim employee?</h2>
              <p className="muted small">
                Turn on Islamic features to see Hijri dates, prayer times, Ramadan and Jumu'ah reminders.
              </p>
              <Link className="button secondary" to="/settings">
                Open settings
              </Link>
            </section>
          )}
        </aside>
      </div>

      <p className="muted small cal-note">
        Islamic dates follow the Umm al-Qura calendar and may differ by a day from local moon sighting.
        Public holiday dates are confirmed each year by the Federal Government.
      </p>
    </>
  );
}

function PrayerList({ date, now, settings }) {
  const prayers = prayerTimes(date, settings.city, settings.asrMethod);
  const next = nextPrayer(now, settings.city, settings.asrMethod);
  return (
    <ul className="prayer-list">
      {prayers.map((p) => {
        const isNext = next && p.time.getTime() === next.time.getTime();
        return (
          <li key={p.key} className={`${p.notAPrayer ? 'is-sunrise' : ''} ${isNext ? 'is-next' : ''}`}>
            <span>{p.name}</span>
            <span>{formatTime(p.time)}</span>
          </li>
        );
      })}
    </ul>
  );
}

function IslamicCards({ now, settings }) {
  const next = nextPrayer(now, settings.city, settings.asrMethod);
  const ramadan = ramadanInfo(now);
  const today = prayerTimes(now, settings.city, settings.asrMethod);
  const at = (key) => today.find((p) => p.key === key).time;
  const friday = nextFriday(now);
  const fridayDhuhr = prayerTimes(friday, settings.city, settings.asrMethod).find((p) => p.key === 'dhuhr').time;
  const isFriday = now.getDay() === 5;

  return (
    <>
      <section className="card islamic-card">
        <p className="eyebrow">Today · {CITIES[settings.city]?.name || 'Lagos'}</p>
        <h2 className="islamic-date">{formatHijri(now)}</h2>
        {next && (
          <p className="next-prayer">
            Next: <strong>{next.name}</strong> at {formatTime(next.time)}{' '}
            <span className="muted">({countdown(next.time, now)})</span>
          </p>
        )}
      </section>

      <section className="card ramadan-card">
        <p className="eyebrow">☾ Ramadan</p>
        {ramadan.inRamadan ? (
          <>
            <h2 className="islamic-date">Day {ramadan.day} of Ramadan</h2>
            <p className="small">
              Suhoor ends <strong>{formatTime(at('fajr'))}</strong> · Iftar{' '}
              <strong>{formatTime(at('maghrib'))}</strong>
            </p>
          </>
        ) : ramadan.startsOn ? (
          <>
            <h2 className="islamic-date">{ramadan.daysUntil} days to go</h2>
            <p className="small muted">
              Expected to begin around{' '}
              {ramadan.startsOn.toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' })}.
            </p>
          </>
        ) : null}
      </section>

      <section className="card jumuah-card">
        <p className="eyebrow">Jumu'ah</p>
        <h2 className="islamic-date">{isFriday ? 'Jumu\u2019ah Mubarak' : friday.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'short' })}</h2>
        <p className="small muted">Dhuhr / Jumu'ah at {formatTime(fridayDhuhr)}</p>
      </section>
    </>
  );
}