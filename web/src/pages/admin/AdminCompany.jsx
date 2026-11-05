import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { ErrorBox, Loading, formatDate } from '../../components/ui';

// HR admin: the company's details, and how staff join it.
export default function AdminCompany() {
  const [company, setCompany] = useState(null);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [email, setEmail] = useState('');
  const [inviteMsg, setInviteMsg] = useState('');
  const [inviteError, setInviteError] = useState('');
  const [busy, setBusy] = useState(false);
  const linkBox = useRef(null);

  useEffect(() => {
    api.myCompany().then(setCompany).catch((e) => setError(e.message));
  }, []);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(company.joinLink);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Copying blocked by the browser: select the link so it can be copied by hand
      linkBox.current?.select();
    }
  };

  const invite = async (e) => {
    e.preventDefault();
    setInviteMsg('');
    setInviteError('');
    setBusy(true);
    try {
      const result = await api.inviteStaff(email);
      setInviteMsg(result.message);
      setEmail('');
    } catch (err) {
      setInviteError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const newLink = async () => {
    const ok = window.confirm(
      'Make a new invite link? The old link stops working straight away. People who already joined are not affected.',
    );
    if (!ok) return;
    setError('');
    try {
      setCompany(await api.resetJoinLink());
    } catch (err) {
      setError(err.message);
    }
  };

  if (!company) return error ? <ErrorBox error={error} /> : <Loading />;

  return (
    <>
      <h1>{company.name}</h1>
      <p className="muted">
        {[company.industry, `${company.staffCount} ${company.staffCount === 1 ? 'person' : 'people'}`]
          .filter(Boolean)
          .join(' · ')}
      </p>
      <ErrorBox error={error} />

      <div className="company-grid">
        <section className="card">
          <h2>Invite your staff</h2>
          <p className="muted small">
            Anyone with this link can create an employee account in {company.name}. Share it on
            WhatsApp, email or your staff group.
          </p>
          <div className="join-link-row">
            <input
              ref={linkBox}
              value={company.joinLink || ''}
              readOnly
              onFocus={(e) => e.target.select()}
              aria-label="Invite link"
            />
            <button type="button" onClick={copy}>
              {copied ? 'Copied ✓' : 'Copy link'}
            </button>
          </div>

          <form className="invite-form" onSubmit={invite}>
            <label>
              Or email the link to someone
              <input
                type="email"
                placeholder="new.staff@yourcompany.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </label>
            <button type="submit" disabled={busy}>
              {busy ? 'Sending…' : 'Send invite'}
            </button>
          </form>
          {inviteMsg && <p className="small ok-text">{inviteMsg}</p>}
          <ErrorBox error={inviteError} />

          <p className="small muted">
            Link shared with the wrong person?{' '}
            <button type="button" className="link-btn" onClick={newLink}>
              Make a new link
            </button>
          </p>
        </section>

        <section className="card">
          <h2>Your plan</h2>
          {company.plan === 'FREE_TRIAL' ? (
            <p>
              <span className="badge badge-pending">Free trial</span>{' '}
              <span className="muted small">until {formatDate(company.trialEndsAt)}</span>
            </p>
          ) : (
            <p>
              <span className="badge badge-approved">Paid</span>
            </p>
          )}
          <h3 className="settings-sub">Your careers page</h3>
          <p className="muted small">Your open jobs, on a page of their own for job seekers.</p>
          <p>
            <Link to={`/careers?company=${company.slug}`}>See your careers page →</Link>
          </p>
          <h3 className="settings-sub">Next steps</h3>
          <ul className="small next-steps">
            <li>
              <Link to="/admin/content">Add your company policies and FAQs</Link> so the HR assistant
              answers from them.
            </li>
            <li>
              <Link to="/admin/recruitment">Post a job</Link> for your careers page.
            </li>
          </ul>
        </section>
      </div>
    </>
  );
}