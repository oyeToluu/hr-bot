import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ErrorBox } from '../components/ui';
import PasswordInput from '../components/PasswordInput';
import PasswordRules, { passwordOk } from '../components/PasswordRules';
import CodeStep from '../components/CodeStep';

const INDUSTRIES = [
  'Agriculture',
  'Construction & Real Estate',
  'Education',
  'Energy, Oil & Gas',
  'Finance & Banking',
  'Food & Beverage',
  'Health Care',
  'Hospitality & Travel',
  'Logistics & Transport',
  'Manufacturing',
  'Media & Entertainment',
  'NGO / Non-profit',
  'Professional Services',
  'Retail & E-commerce',
  'Technology',
  'Telecoms',
  'Other',
];

// A business signing up for HR Bots. The person signing up becomes the
// company's first HR admin and can then invite their staff.
export default function RegisterCompany() {
  const { registerCompany, verifyCode } = useAuth();
  const [form, setForm] = useState({ companyName: '', industry: '', fullName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState(null); // { email, purpose } while waiting for the code

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    if (!passwordOk(form.password)) {
      setFieldErrors({ password: 'Please meet all the password rules below.' });
      return;
    }
    setBusy(true);
    try {
      const result = await registerCompany(
        form.companyName,
        form.industry || null,
        form.fullName,
        form.email,
        form.password,
      );
      if (result.status === 'CODE_SENT') {
        setPending({ email: result.email, purpose: result.purpose });
        setBusy(false);
      }
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors || {});
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="card auth-card">
        {pending ? (
          <CodeStep
            email={pending.email}
            purpose={pending.purpose}
            onVerify={(code) => verifyCode(pending.email, pending.purpose, code)}
            onBack={() => setPending(null)}
          />
        ) : (
          <form onSubmit={submit}>
            <p className="eyebrow">Free 30-day trial</p>
            <h1>Register your company</h1>
            <p className="muted">
              You'll be your company's HR admin. Then add your policies and invite your staff.
            </p>
            <ErrorBox error={error} />
            <label>
              Company name
              <input value={form.companyName} onChange={update('companyName')} maxLength={100} required />
              {fieldErrors.companyName && <span className="field-error">{fieldErrors.companyName}</span>}
            </label>
            <label>
              Industry <span className="muted">(optional)</span>
              <select value={form.industry} onChange={update('industry')}>
                <option value="">Choose…</option>
                {INDUSTRIES.map((i) => (
                  <option key={i} value={i}>
                    {i}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Your full name
              <input value={form.fullName} onChange={update('fullName')} required />
              {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
            </label>
            <label>
              Your work email
              <input type="email" value={form.email} onChange={update('email')} required />
              {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
            </label>
            <label>
              Password
              <PasswordInput value={form.password} onChange={update('password')} required />
              {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
            </label>
            <PasswordRules password={form.password} />
            <button type="submit" disabled={busy}>
              {busy ? 'Setting up your company…' : 'Create company account'}
            </button>
            <p className="muted small">
              Joining a company that already uses HR Bots? <Link to="/register">Use your invite</Link>
            </p>
            <p className="muted small">
              Already have an account? <Link to="/login">Log in</Link>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}