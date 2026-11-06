import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import TicketThread from '../../components/TicketThread';
import { ErrorBox, Loading, formatDate } from '../../components/ui';

const STATUS = {
  OPEN: { text: 'Needs a reply', cls: 'badge-pending' },
  ANSWERED: { text: 'Replied', cls: 'badge-approved' },
  CLOSED: { text: 'Closed', cls: 'badge-cancelled' },
};
const SOURCE = { HR_ASSISTANT: 'from HR assistant', ONBOARDING_BOT: 'from onboarding bot', DIRECT: 'asked directly' };
const FILTERS = [
  ['OPEN', 'Needs a reply'],
  ['ANSWERED', 'Replied'],
  ['CLOSED', 'Closed'],
  ['', 'All'],
];

// HR admin: employees who asked the bot for a person.
export default function AdminSupport() {
  const [filter, setFilter] = useState('OPEN');
  const [list, setList] = useState(null);
  const [open, setOpen] = useState(null);
  const [error, setError] = useState('');

  const load = () => api.supportInbox(filter || null).then(setList).catch((e) => setError(e.message));
  useEffect(() => {
    setList(null);
    load();
  }, [filter]);

  const view = async (id) => {
    setError('');
    try {
      setOpen(await api.supportInboxOne(id));
    } catch (e) {
      setError(e.message);
    }
  };

  if (open) {
    return (
      <>
        <p className="small">
          <button
            className="link-btn"
            onClick={() => {
              setOpen(null);
              load();
            }}
          >
            ← All help requests
          </button>
        </p>
        <h1>{open.subject}</h1>
        <p className="muted">
          <strong>{open.employeeName}</strong>
          {open.employeeDepartment ? ` · ${open.employeeDepartment}` : ''} · {SOURCE[open.source]} ·{' '}
          <span className={`badge ${STATUS[open.status].cls}`}>{STATUS[open.status].text}</span>
        </p>
        <ErrorBox error={error} />
        <div className="card">
          <TicketThread
            ticket={open}
            me="HR"
            onReply={async (text) => setOpen(await api.replySupportInbox(open.id, text))}
            onClose={async () => setOpen(await api.closeSupportInbox(open.id))}
          />
        </div>
      </>
    );
  }

  return (
    <>
      <h1>Help requests</h1>
      <p className="muted">
        When the HR bot can't help, employees can ask for a person. Their chat with the bot comes
        with the request, so you can see what they already asked.
      </p>
      <div className="tabs">
        {FILTERS.map(([value, text]) => (
          <button key={text} className={`tab ${filter === value ? 'is-active' : ''}`} onClick={() => setFilter(value)}>
            {text}
          </button>
        ))}
      </div>
      <ErrorBox error={error} />

      {!list ? (
        <Loading />
      ) : list.length === 0 ? (
        <div className="card">
          <p className="muted">Nothing here.</p>
        </div>
      ) : (
        <div className="ticket-list">
          {list.map((t) => (
            <button key={t.id} className={`card ticket-row ${t.unreadByHr ? 'is-unread' : ''}`} onClick={() => view(t.id)}>
              <span className="ticket-subject">
                {t.unreadByHr && <span className="dot-new" aria-label="New" />}
                {t.subject}
                <span className="muted small ticket-who">
                  {t.employeeName} · {SOURCE[t.source]}
                </span>
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