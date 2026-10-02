import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ErrorBox } from '../components/ui';

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(email, password); // App.jsx redirects once logged in
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={submit}>
        <h1>HR Bots</h1>
        <p className="muted">AI for Business &amp; Productivity. Log in to continue.</p>
        <ErrorBox error={error} />
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={busy}>
          {busy ? 'Logging in…' : 'Log in'}
        </button>
        <p className="muted small">
          New employee? <Link to="/register">Create an account</Link>
        </p>
        <p className="muted small">
          Looking for a job? <Link to="/careers">See open positions</Link>
        </p>
        <p className="muted small demo-hint">
          Demo accounts (from DemoDataSeeder): admin@hrbots.test and ada@hrbots.test, password
          password123
        </p>
      </form>
    </div>
  );
}
