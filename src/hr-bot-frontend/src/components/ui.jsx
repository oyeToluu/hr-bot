// Small shared helpers used across pages.

export function formatDate(value) {
  if (!value) return '—';
  // LocalDate comes as "2026-10-09"; parse as local time so it doesn't shift a day.
  const date = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value);
  return date.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function titleCase(value) {
  if (!value) return '';
  return value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, ' ');
}

export function StatusBadge({ status }) {
  return <span className={`badge badge-${status?.toLowerCase()}`}>{titleCase(status)}</span>;
}

export function Spinner({ label = 'Loading' }) {
  return (
    <div className="inline-loading">
      <div className="spinner spinner-sm" aria-hidden="true" />
      <span>{label}…</span>
    </div>
  );
}

export function ErrorAlert({ message }) {
  if (!message) return null;
  return <div className="alert alert-error" role="alert">{message}</div>;
}
