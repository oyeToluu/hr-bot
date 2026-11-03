import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { ErrorBox } from './ui';

const WORDING = {
  SIGNUP: {
    title: 'Confirm your email',
    text: 'We sent a 6-digit code to',
  },
  LOGIN: {
    title: 'Check your email',
    text: "You're signing in on a new device. We sent a 6-digit code to",
  },
};

/**
 * The "enter the 6-digit code" step, shown after sign-up or a login on a new device.
 * onVerify(code) should log the person in (it throws a friendly error if the code is wrong).
 */
export default function CodeStep({ email, purpose, onVerify, onBack }) {
  const words = WORDING[purpose] || WORDING.LOGIN;
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [wait, setWait] = useState(60); // seconds until "Send a new code" is allowed

  useEffect(() => {
    if (wait <= 0) return undefined;
    const t = setTimeout(() => setWait((w) => w - 1), 1000);
    return () => clearTimeout(t);
  }, [wait]);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setNote('');
    setBusy(true);
    try {
      await onVerify(code);
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  const resend = async () => {
    setError('');
    setNote('');
    try {
      const res = await api.resendCode(email, purpose);
      setNote(res.message);
      setCode('');
      setWait(60);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form className="code-step" onSubmit={submit}>
      <h2 className="display">{words.title}</h2>
      <p className="muted">
        {words.text} <strong>{email}</strong>. It expires in 10 minutes.
      </p>
      <ErrorBox error={error} />
      {note && <div className="success-box">{note}</div>}
      <label>
        6-digit code
        <input
          className="code-input"
          value={code}
          onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
          inputMode="numeric"
          autoComplete="one-time-code"
          placeholder="••••••"
          autoFocus
          required
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
          <button type="button" className="link-btn" onClick={resend}>
            Send a new code
          </button>
        )}
      </p>
      {onBack && (
        <p className="muted small">
          <button type="button" className="link-btn" onClick={onBack}>
            ← Use a different email
          </button>
        </p>
      )}
    </form>
  );
}