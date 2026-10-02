import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ErrorBox } from '../components/ui';

export default function Register() {
  const { register } = useAuth();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', department: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setBusy(true);
    try {
      await register(form.fullName, form.email, form.password, form.department || null);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors || {});
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={submit}>
        <h1>Create your account</h1>
        <p className="muted">New accounts are employee accounts. HR admin access is set by HR.</p>
        <ErrorBox error={error} />
        <label>
          Full name
          <input value={form.fullName} onChange={update('fullName')} required />
          {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
        </label>
        <label>
          Email
          <input type="email" value={form.email} onChange={update('email')} required />
          {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
        </label>
        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={update('password')}
            minLength={6}
            required
          />
          {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
        </label>
        <label>
          Department <span className="muted">(optional)</span>
          <input value={form.department} onChange={update('department')} />
        </label>
        <button type="submit" disabled={busy}>
          {busy ? 'Creating account…' : 'Create account'}
        </button>
        <p className="muted small">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
