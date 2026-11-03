import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ErrorBox } from '../components/ui';
import PasswordInput from '../components/PasswordInput';
import PasswordRules, { passwordOk } from '../components/PasswordRules';

/**
 * Forgot password, step by step:
 *   1. email  -> we email a 6-digit code
 *   2. code   -> the code is checked first
 *   3. new password (only shown once the code is confirmed)
 *   4. done
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
  const [wait, setWait] = useState(0); // seconds until "Send a new code" is allowed

  useEffect(() => {
    if (wait <= 0) return undefined;
    const t = setTimeout(() => setWait((w) => w - 1), 1000);
    return () => clearTimeout(t);
  }, [wait]);

  // Step 1: send the code
  const sendCode = async (e) => {
    e?.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = await api.forgotPassword(email);
      setNote(res.message);
      setCode('');
      setWait(60);
      setStep('code');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  // Step 2: check the code before asking for a new password
  const checkCode = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await api.checkResetCode(email, code);
      setStep('password');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  // Step 3: save the new password
  const savePassword = async (e) => {
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
      // If the code expired in the meantime, go back to ask for a new one
      if (/expired|Too many/i.test(err.message)) setStep('code');
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

      {step === 'code' && (
        <form onSubmit={checkCode}>
          <h1>Enter your code</h1>
          <div className="success-box">{note}</div>
          <p className="muted small">
            We sent it to <strong>{email}</strong>. It expires in 10 minutes.
          </p>
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
          <button type="submit" disabled={busy || code.length !== 6}>
            {busy ? 'Checking…' : 'Continue'}
          </button>
          <p className="muted small">
            Didn't get it? Check your spam folder.{' '}
            {wait > 0 ? (
              <span>You can ask for a new code in {wait}s.</span>
            ) : (
              <button type="button" className="link-btn" onClick={() => sendCode()}>
                Send a new code
              </button>
            )}
          </p>
          <p className="muted small">
            <button type="button" className="link-btn" onClick={() => setStep('email')}>
              ← Use a different email
            </button>
          </p>
        </form>
      )}

      {step === 'password' && (
        <form onSubmit={savePassword}>
          <h1>Choose a new password</h1>
          <div className="success-box">Code confirmed ✓</div>
          <ErrorBox error={error} />
          <label>
            New password
            <PasswordInput value={password} onChange={(e) => setPassword(e.target.value)} required autoFocus />
          </label>
          <PasswordRules password={password} />
          <button type="submit" disabled={busy}>
            {busy ? 'Saving…' : 'Save new password'}
          </button>
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