// The password rule, shown as a live checklist while someone types.
// The server checks the same rule, so nobody can get round it.

const RULES = [
  { label: 'At least 8 characters', test: (p) => p.length >= 8 },
  { label: 'A letter', test: (p) => /[A-Za-z]/.test(p) },
  { label: 'A number', test: (p) => /\d/.test(p) },
  { label: 'A special character (like @ # ! ?)', test: (p) => /[^A-Za-z0-9]/.test(p) },
];

export function passwordOk(password) {
  return RULES.every((r) => r.test(password || ''));
}

export default function PasswordRules({ password }) {
  const p = password || '';
  return (
    <ul className="pw-rules" aria-label="Password rules">
      {RULES.map((r) => {
        const ok = r.test(p);
        return (
          <li key={r.label} className={ok ? 'ok' : ''}>
            <span aria-hidden="true">{ok ? '✓' : '○'}</span> {r.label}
          </li>
        );
      })}
    </ul>
  );
}