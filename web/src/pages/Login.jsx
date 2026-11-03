import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ErrorBox } from '../components/ui';
import AiHrHero from '../components/AiHrHero';
import PasswordInput from '../components/PasswordInput';
import CodeStep from '../components/CodeStep';

export default function Login() {
  const { login, verifyCode } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState(null); // { email, purpose } while waiting for a code

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const result = await login(email, password); // App.jsx redirects once logged in
      if (result.status === 'CODE_SENT') {
        setPending({ email: result.email, purpose: result.purpose });
        setBusy(false);
      }
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
          HR Bots for Business And Productivity
        </p>
        <h1 className="display">Ask HR anything. Get your answer in seconds.</h1>
        <p className="lede">
          Your leave balance, company policies and first-week questions, answered by an AI
          assistant that knows your workplace.
        </p>
        <AiHrHero size="lg" />
      </section>

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
            <h2 className="display">Log in</h2>
            <p className="muted">Use your work email to continue.</p>
            <ErrorBox error={error} />
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            </label>
            <label>
              Password
              <PasswordInput value={password} onChange={(e) => setPassword(e.target.value)} required />
            </label>
            <p className="forgot-link small">
              <Link to="/forgot-password">Forgot password?</Link>
            </p>
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
        )}
      </div>
    </div>
  );
}