import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ErrorBox } from '../components/ui';
import PasswordInput from '../components/PasswordInput';
import PasswordRules, { passwordOk } from '../components/PasswordRules';

/**
 * Forgot password, in three steps: email -> code + new password -> done.
 * Used for staff (/forgot-password) and job applicants (/careers/forgot-password).
 */
export default function ForgotPassword({ loginPath = '/login', careers = false }) {
  const { resetPassword } = useAuth();
  const [step, setStep] = useState('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [note, setNote] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const sendCode = async (e) => {
    e?.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = await api.forgotPassword(email);
      setNote(res.message);
      setStep('reset');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const reset = async (e) => {
    e.preventDefault();
    setError('');
    if (!passwordOk(password)) {
      setError('Please meet all the password rules.');
      return;
    }
    setBusy(true);
    try {
      await resetPassword(email, code, password);
      setStep('done');
    } catch (err) {
      setError(err.fieldErrors?.newPassword || err.message);
    } finally {
      setBusy(false);
    }
  };

  const card = (
    <div className={`card auth-card ${careers ? 'careers-auth' : ''}`}>
      {step === 'email' && (
        <form onSubmit={sendCode}>
          <h1>Forgot your password?</h1>
          <p className="muted">Enter your email and we'll send you a 6-digit code to reset it.</p>
          <ErrorBox error={error} />
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
          </label>
          <button type="submit" disabled={busy}>
            {busy ? 'Sending…' : 'Send code'}
          </button>
          <p className="muted small">
            Remembered it? <Link to={loginPath}>Back to log in</Link>
          </p>
        </form>
      )}

      {step === 'reset' && (
        <form onSubmit={reset}>
          <h1>Choose a new password</h1>
          <div className="success-box">{note}</div>
          <ErrorBox error={error} />
          <label>
            6-digit code
            <input
              className="code-input"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder="••••••"
              required
              autoFocus
            />
          </label>
          <label>
            New password
            <PasswordInput value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          <PasswordRules password={password} />
          <button type="submit" disabled={busy || code.length !== 6}>
            {busy ? 'Saving…' : 'Save new password'}
          </button>
          <p className="muted small">
            No email? Check spam, or{' '}
            <button type="button" className="link-btn" onClick={() => setStep('email')}>
              try again
            </button>
            .
          </p>
        </form>
      )}

      {step === 'done' && (
        <div>
          <h1>Password changed 🎉</h1>
          <p className="muted">You can now log in with your new password.</p>
          <Link className="button" to={loginPath}>
            Log in
          </Link>
        </div>
      )}
    </div>
  );

  // The careers pages already have their own layout around them
  return careers ? card : <div className="auth-page">{card}</div>;
}