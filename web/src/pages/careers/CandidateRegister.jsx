import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ErrorBox } from '../../components/ui';

export default function CandidateRegister() {
  const { candidate, candidateRegister } = useAuth();
  const location = useLocation();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', phone: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  // Once logged in (or already logged in), go back to the job they came
  // from, if any.
  if (candidate) {
    return <Navigate to={location.state?.from?.pathname || '/careers'} replace />;
  }

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});
    setBusy(true);
    try {
      await candidateRegister(form.fullName, form.email, form.password, form.phone || null);
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors || {});
      setBusy(false);
    }
  };

  return (
    <form className="card auth-card careers-auth" onSubmit={submit}>
      <h1>Create a candidate account</h1>
      <p className="muted">One account lets you apply for any of our open jobs.</p>
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
        <input type="password" value={form.password} onChange={update('password')} minLength={6} required />
        {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
      </label>
      <label>
        Phone <span className="muted">(optional)</span>
        <input type="tel" value={form.phone} onChange={update('phone')} />
      </label>
      <button type="submit" disabled={busy}>
        {busy ? 'Creating account…' : 'Create account'}
      </button>
      <p className="muted small">
        Already have an account?{' '}
        <Link to="/careers/login" state={location.state}>
          Log in
        </Link>
      </p>
    </form>
  );
}
