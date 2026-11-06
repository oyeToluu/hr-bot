import { useEffect, useState } from 'react';
import { api } from '../api/client';
import TicketThread from '../components/TicketThread';
import { ErrorBox, Loading, formatDate } from '../components/ui';

const STATUS = {
  OPEN: { text: 'Waiting for HR', cls: 'badge-pending' },
  ANSWERED: { text: 'HR replied', cls: 'badge-approved' },
  CLOSED: { text: 'Closed', cls: 'badge-cancelled' },
};

// Employee: my requests to a person in HR, and HR's replies.
export default function HelpRequests() {
  const [list, setList] = useState(null);
  const [open, setOpen] = useState(null); // the request being read
  const [writing, setWriting] = useState(null); // new request text
  const [error, setError] = useState('');

  const load = () => api.mySupport().then(setList).catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);

  const view = async (id) => {
    setError('');
    try {
      setOpen(await api.mySupportOne(id));
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const startNew = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const t = await api.openSupport('DIRECT', writing, []);
      setWriting(null);
      setOpen(t);
      load();
    } catch (err) {
      setError(err.message);
    }
  };

  if (open) {
    return (
      <>
        <p className="small">
          <button className="link-btn" onClick={() => setOpen(null)}>
            ← All my requests
          </button>
        </p>
        <h1>{open.subject}</h1>
        <p className="muted">
          <span className={`badge ${STATUS[open.status].cls}`}>{STATUS[open.status].text}</span> · started{' '}
          {formatDate(open.createdAt)}
        </p>
        <ErrorBox error={error} />
        <div className="card">
          <TicketThread
            ticket={open}
            me="EMPLOYEE"
            onReply={async (text) => setOpen(await api.replySupport(open.id, text))}
            onClose={async () => {
              setOpen(await api.closeSupport(open.id));
              load();
            }}
          />
        </div>
      </>
    );
  }

  return (
    <>
      <div className="page-head">
        <h1>My HR requests</h1>
        {!writing && <button onClick={() => setWriting('')}>Ask HR something</button>}
      </div>
      <p className="muted">
        When the HR bot can't help, a person in your HR team can. Their replies show here, and you
        get an alert.
      </p>
      <ErrorBox error={error} />

      {writing !== null && (
        <form className="card" onSubmit={startNew}>
          <label>
            What do you need help with?
            <textarea rows={3} maxLength={2000} value={writing} onChange={(e) => setWriting(e.target.value)} required />
          </label>
          <div className="row">
            <button type="submit" disabled={!writing.trim()}>
              Send to HR
            </button>
            <button type="button" className="secondary" onClick={() => setWriting(null)}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {!list ? (
        <Loading />
      ) : list.length === 0 ? (
        <div className="card">
          <p className="muted">
            No requests yet. In the HR assistant or onboarding bot, press <strong>Talk to a person in HR</strong>,
            or use the button above.
          </p>
        </div>
      ) : (
        <div className="ticket-list">
          {list.map((t) => (
            <button key={t.id} className={`card ticket-row ${t.unreadByEmployee ? 'is-unread' : ''}`} onClick={() => view(t.id)}>
              <span className="ticket-subject">
                {t.unreadByEmployee && <span className="dot-new" aria-label="New reply" />}
                {t.subject}
              </span>
              <span className={`badge ${STATUS[t.status].cls}`}>{STATUS[t.status].text}</span>
              <span className="muted small">{formatDate(t.updatedAt)}</span>
            </button>
          ))}
        </div>
      )}
    </>
  );
}