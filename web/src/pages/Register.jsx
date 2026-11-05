import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ErrorBox, Loading } from '../components/ui';
import PasswordInput from '../components/PasswordInput';
import PasswordRules, { passwordOk } from '../components/PasswordRules';
import CodeStep from '../components/CodeStep';

// Staff join their company with the invite link HR shares: /join/<code>.
// /register (no code) asks them to paste the link or code first.
export default function Register() {
  const { code } = useParams();
  const navigate = useNavigate();
  const { register, verifyCode } = useAuth();
  const [company, setCompany] = useState(null);
  const [linkError, setLinkError] = useState('');
  const [typed, setTyped] = useState('');
  const [form, setForm] = useState({ fullName: '', email: '', password: '', department: '' });
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState(null); // { email, purpose } while waiting for the code

  // Check the invite link and find out which company it is for
  useEffect(() => {
    setCompany(null);
    setLinkError('');
    if (!code) return;
    api.companyByJoinCode(code).then(setCompany).catch((e) => setLinkError(e.message));
  }, [code]);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  // Accepts the whole link (https://.../join/abc123) or just the code (abc123)
  const openTyped = (e) => {
    e.preventDefault();
    const value = typed.trim().split('/join/').pop().split(/[?#/\s]/)[0];
    if (value) navigate(`/join/${value}`);
  };

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
      const result = await register(form.fullName, form.email, form.password, form.department || null, code);
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

  // Step 1: no link yet, or the link didn't work
  if (!code || linkError) {
    return (
      <div className="auth-page">
        <div className="card auth-card">
          <form onSubmit={openTyped}>
            <h1>Join your company</h1>
            <p className="muted">
              Your HR team sends you an invite link. Open it, or paste the link or code here.
            </p>
            <ErrorBox error={linkError} />
            <label>
              Invite link or code
              <input
                value={typed}
                onChange={(e) => setTyped(e.target.value)}
                placeholder="https://…/join/…"
                required
              />
            </label>
            <button type="submit">Continue</button>
            <p className="muted small">
              Setting up HR Bots for your business? <Link to="/register-company">Register your company</Link>
            </p>
            <p className="muted small">
              Already have an account? <Link to="/login">Log in</Link>
            </p>
          </form>
        </div>
      </div>
    );
  }

  if (!company) {
    return (
      <div className="auth-page">
        <div className="card auth-card">
          <Loading />
        </div>
      </div>
    );
  }

  // Step 2: the sign-up form for that company
  return (
    <div className="auth-page">
      <div className="card auth-card">
        {pending ? (
          <CodeStep
            email={pending.email}
            purpose={pending.purpose}
            onVerify={(c) => verifyCode(pending.email, pending.purpose, c)}
            onBack={() => setPending(null)}
          />
        ) : (
          <form onSubmit={submit}>
            <p className="eyebrow">You're invited</p>
            <h1>Join {company.name}</h1>
            <p className="muted">Create your employee account. HR admin access is set by your HR team.</p>
            <ErrorBox error={error} />
            <label>
              Full name
              <input value={form.fullName} onChange={update('fullName')} required />
              {fieldErrors.fullName && <span className="field-error">{fieldErrors.fullName}</span>}
            </label>
            <label>
              Work email
              <input type="email" value={form.email} onChange={update('email')} required />
              {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
            </label>
            <label>
              Password
              <PasswordInput value={form.password} onChange={update('password')} required />
              {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
            </label>
            <PasswordRules password={form.password} />
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
        )}
      </div>
    </div>
  );
}