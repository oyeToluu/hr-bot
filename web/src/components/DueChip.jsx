import { formatDate } from './ui';

// "Overdue", "Due today", "Due tomorrow" or "Due 5 Nov 2026" for a task's due date.
export function DueChip({ date }) {
  if (!date) return null;
  const days = daysFromToday(date);
  if (days < 0) return <span className="due-chip due-overdue">Overdue, {formatDate(date)}</span>;
  if (days === 0) return <span className="due-chip due-today">Due today</span>;
  if (days === 1) return <span className="due-chip due-soon">Due tomorrow</span>;
  return <span className="due-chip">Due {formatDate(date)}</span>;
}

// Whole days between today and a "YYYY-MM-DD" date (negative = in the past)
export function daysFromToday(date) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const d = new Date(`${date}T00:00:00`);
  return Math.round((d - today) / 86400000);
}