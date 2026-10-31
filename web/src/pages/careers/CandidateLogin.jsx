import { useState } from 'react';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ErrorBox } from '../../components/ui';
  import PasswordInput from '../../components/PasswordInput';

export default function CandidateLogin() {
  const { candidate, candidateLogin } = useAuth();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  // Once logged in (or already logged in), go back to the job they came
  // from, if any.
  if (candidate) {
    return <Navigate to={location.state?.from?.pathname || '/careers/applications'} replace />;
  }

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await candidateLogin(email, password);
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <form className="card auth-card careers-auth" onSubmit={submit}>
      <h1>Candidate log in</h1>
      <p className="muted">Log in to apply for jobs and track your applications.</p>
      <ErrorBox error={error} />
      <label>
        Email
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      </label>
      <label>
        Password
          <PasswordInput 
          value={password} 
          onChange={(e) => setPassword(e.target.value)} required />  
              </label>
      <button type="submit" disabled={busy}>
        {busy ? 'Logging in…' : 'Log in'}
      </button>
      <p className="muted small">
        New here?{' '}
        <Link to="/careers/register" state={location.state}>
          Create a candidate account
        </Link>
      </p>
      <p className="muted small">
        Work here? <Link to="/login">Staff login</Link>
      </p>
    </form>
  );
}
