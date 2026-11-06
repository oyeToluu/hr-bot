import { useEffect, useRef, useState } from 'react';
import { ErrorBox, formatDate } from './ui';

const WHO = { EMPLOYEE: 'employee', BOT: 'bot', HR: 'hr' };

// "5 Nov, 9:41 PM"
function when(at) {
  if (!at) return '';
  const d = new Date(at);
  return `${formatDate(at)}, ${d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}`;
}

/**
 * One help request: the bot chat that came before, then the messages between
 * the employee and HR, with a reply box. `me` is "EMPLOYEE" or "HR".
 */
export default function TicketThread({ ticket, me, onReply, onClose }) {
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [ticket.messages.length]);

  const send = async (e) => {
    e.preventDefault();
    if (!text.trim()) return;
    setBusy(true);
    setError('');
    try {
      await onReply(text);
      setText('');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const closed = ticket.status === 'CLOSED';
  // Everything up to the bot's last answer is the chat the employee had before asking for a person
  const lastBot = ticket.messages.map((m) => m.author).lastIndexOf('BOT');

  return (
    <div className="thread">
      <div className="thread-log">
        {ticket.messages.map((m, i) => (
          <div key={i}>
            <div
              className={`thread-msg from-${WHO[m.author]} ${m.author === me ? 'mine' : ''} ${
                i <= lastBot ? 'earlier' : ''
              }`}
            >
              <p className="thread-meta">
                <strong>{m.author === 'BOT' ? 'HR bot' : m.authorName}</strong>
                {m.author === 'HR' && <span className="role-tag">HR</span>}
                <span className="muted"> · {when(m.at)}</span>
              </p>
              <p className="thread-text">{m.text}</p>
            </div>
            {i === lastBot && <p className="thread-divider">Asked for a person in HR</p>}
          </div>
        ))}
        <div ref={endRef} />
      </div>

      <ErrorBox error={error} />
      {closed ? (
        <p className="muted small thread-closed">This request is closed.</p>
      ) : (
        <form className="thread-reply" onSubmit={send}>
          <textarea
            rows={3}
            maxLength={2000}
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={me === 'HR' ? 'Write your reply to the employee…' : 'Write a message to HR…'}
          />
          <div className="row">
            <button type="submit" disabled={busy || !text.trim()}>
              {busy ? 'Sending…' : 'Send'}
            </button>
            {onClose && (
              <button type="button" className="secondary" onClick={onClose}>
                {me === 'HR' ? 'Close request' : 'My question is sorted'}
              </button>
            )}
          </div>
        </form>
      )}
    </div>
  );
}