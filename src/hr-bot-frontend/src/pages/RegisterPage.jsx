import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import AuthCard from './AuthCard';

const DEPARTMENTS = ['Engineering', 'Finance', 'Human Resources', 'Marketing', 'Operations', 'Sales'];

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', password: '', department: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Same rule as RegisterRequest (@Size(min = 6)) so users see it instantly.
    if (form.password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    setSubmitting(true);
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        department: form.department || null, // backend defaults to "Unassigned"
      });
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard title="Create your account" subtitle="Staff accounts start with the Employee role.">
      <form onSubmit={handleSubmit} className="form" noValidate>
        {error && <div className="alert alert-error" role="alert">{error}</div>}

        <label className="field">
          <span>Full name</span>
          <input name="fullName" autoComplete="name" value={form.fullName} onChange={update} required autoFocus />
        </label>

        <label className="field">
          <span>Work email</span>
          <input type="email" name="email" autoComplete="email" value={form.email} onChange={update} required />
        </label>

        <label className="field">
          <span>Password</span>
          <input
            type="password"
            name="password"
            autoComplete="new-password"
            value={form.password}
            onChange={update}
            required
          />
          <small className="hint">At least 6 characters.</small>
        </label>

        <label className="field">
          <span>Department</span>
          <select name="department" value={form.department} onChange={update}>
            <option value="">Select later</option>
            {DEPARTMENTS.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
        </label>

        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="auth-switch">
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </AuthCard>
  );
}
