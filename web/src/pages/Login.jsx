import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ErrorBox } from '../components/ui';
import AiHrHero from '../components/AiHrHero';
import PasswordInput from '../components/PasswordInput';

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
    <div className="auth-page landing">
      <section className="landing-hero">
        <p className="brand-mark">
          <span className="brand-dot" aria-hidden="true" />
          HR Bots
        </p>
        <h1 className="display">Ask HR anything. Get your answer in seconds.</h1>
        <p className="lede">
          Your leave balance, company policies and first-week questions, answered by an AI
          assistant that knows your workplace.
        </p>
        <AiHrHero size="lg" />
      </section>

      <form className="card auth-card" onSubmit={submit}>
        <h2 className="display">Log in</h2>
        <p className="muted">Use your work email to continue.</p>
        <ErrorBox error={error} />
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <PasswordInput
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
      </form>
    </div>
  );
}