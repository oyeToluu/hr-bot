// Small shared pieces used across pages.

export function StatusBadge({ status }) {
  return <span className={`badge badge-${(status || '').toLowerCase()}`}>{status}</span>;
}

export function ErrorBox({ error }) {
  if (!error) return null;
  return <div className="error-box">{error}</div>;
}

export function Loading() {
  return <p className="muted">Loading…</p>;
}

// "ANNUAL" -> "Annual", "HR_ADMIN" -> "Hr admin"
export function label(value) {
  if (!value) return '';
  const s = value.replace(/_/g, ' ').toLowerCase();
  return s.charAt(0).toUpperCase() + s.slice(1);
}

// "2026-10-01" -> "1 Oct 2026"
export function formatDate(value) {
  if (!value) return '';
  const d = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

// 0-100 fit score as a coloured bar: green >= 70, amber >= 40, red below.
export function ScoreBar({ score }) {
  const value = Math.max(0, Math.min(100, score ?? 0));
  const tone = value >= 70 ? 'good' : value >= 40 ? 'mid' : 'low';
  return (
    <div className="score" title={`Fit score ${value}/100`}>
      <div className="score-track">
        <div className={`score-fill score-${tone}`} style={{ width: `${value}%` }} />
      </div>
      <span className="score-num">{value}</span>
    </div>
  );
}

export function SkillChips({ skills, tone = 'neutral' }) {
  if (!skills || skills.length === 0) return null;
  return (
    <span className="chips">
      {skills.map((s) => (
        <span key={s} className={`chip chip-${tone}`}>
          {s}
        </span>
      ))}
    </span>
  );
}

// What a candidate sees. Internal labels like "auto rejected" stay HR-side.
const CANDIDATE_STATUS = {
  SHORTLISTED: 'Shortlisted',
  UNDER_REVIEW: 'Under review',
  AUTO_REJECTED: 'Not progressed',
  INTERVIEW: 'Invited to interview',
  HIRED: 'Offer made',
  REJECTED: 'Not progressed',
};

export function CandidateStatus({ status }) {
  const cls = status === 'AUTO_REJECTED' ? 'rejected' : (status || '').toLowerCase();
  return <span className={`badge badge-${cls}`}>{CANDIDATE_STATUS[status] || status}</span>;
}
