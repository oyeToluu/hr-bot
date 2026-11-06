import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';

// Chat UI shared by the Onboarding bot and the Policy assistant.
// `ask(text)` must return a Promise of { answer, detail? }.
// With `handoffSource`, the employee can pass the chat to a person in HR.
export default function ChatBox({ greeting, placeholder, suggestions = [], ask, handoffSource }) {
  const [messages, setMessages] = useState([{ from: 'bot', text: greeting }]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [handoff, setHandoff] = useState(null); // { text, busy, error } while the form is open
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [messages, handoff]);

  const send = async (text) => {
    const question = text.trim();
    if (!question || sending) return;
    setInput('');
    setMessages((m) => [...m, { from: 'user', text: question }]);
    setSending(true);
    try {
      const { answer, detail } = await ask(question);
      setMessages((m) => [...m, { from: 'bot', text: answer, detail }]);
    } catch {
      // Something went wrong on our side (no internet, server busy). Never leave the
      // employee stuck: say so plainly and offer HR straight away.
      setMessages((m) => [
        ...m,
        {
          from: 'bot',
          error: true,
          text: handoffSource
            ? "Sorry, I couldn't get you an answer just now. A person in HR can help you with this."
            : "Sorry, I couldn't get you an answer just now. Please try again in a moment.",
        },
      ]);
    } finally {
      setSending(false);
    }
  };

  const lastQuestion = [...messages].reverse().find((m) => m.from === 'user')?.text || '';
  const asked = Boolean(lastQuestion);

  const openHandoff = () => setHandoff({ text: lastQuestion, busy: false, error: '' });

  const sendToHr = async (e) => {
    e.preventDefault();
    setHandoff((h) => ({ ...h, busy: true, error: '' }));
    // The chat so far (not the greeting, errors or notes), so HR sees what was already asked
    const chat = messages
      .slice(1)
      .filter((m) => !m.error && !m.note)
      .map((m) => ({ from: m.from, text: m.text.slice(0, 2000) }));
    try {
      await api.openSupport(handoffSource, handoff.text, chat);
      setHandoff(null);
      setMessages((m) => [
        ...m,
        {
          from: 'bot',
          note: true,
          text: "I've passed this to your HR team. You'll get an alert when someone replies.",
        },
      ]);
    } catch (err) {
      setHandoff((h) => ({ ...h, busy: false, error: err.message }));
    }
  };

  return (
    <div className="chat">
      <div className="chat-log">
        {messages.map((m, i) => (
          <div key={i} className={`bubble ${m.from} ${m.error ? 'error' : ''} ${m.note ? 'note' : ''}`}>
            <p>{m.text}</p>
            {m.detail && <p className="bubble-detail">{m.detail}</p>}
            {m.error && handoffSource && !handoff && (
              <p className="bubble-detail">
                <button type="button" className="link-btn" onClick={() => openHandoff()}>
                  Talk to a person in HR →
                </button>
              </p>
            )}
            {m.note && (
              <p className="bubble-detail">
                <Link to="/help">See my HR requests →</Link>
              </p>
            )}
          </div>
        ))}
        {sending && <div className="bubble bot typing">Thinking…</div>}
        <div ref={endRef} />
      </div>

      {handoff ? (
        <form className="handoff" onSubmit={sendToHr}>
          <label>
            What do you need help with? Your HR team will see this chat too.
            <textarea
              rows={3}
              maxLength={2000}
              value={handoff.text}
              onChange={(e) => setHandoff({ ...handoff, text: e.target.value })}
              required
            />
          </label>
          {handoff.error && <p className="field-error">{handoff.error}</p>}
          <div className="row">
            <button type="submit" disabled={handoff.busy || !handoff.text.trim()}>
              {handoff.busy ? 'Sending…' : 'Send to HR'}
            </button>
            <button type="button" className="secondary" onClick={() => setHandoff(null)}>
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <>
          {suggestions.length > 0 && (
            <div className="chips">
              {suggestions.map((s) => (
                <button key={s} className="chip" onClick={() => send(s)} disabled={sending}>
                  {s}
                </button>
              ))}
            </div>
          )}

          <form
            className="chat-input"
            onSubmit={(e) => {
              e.preventDefault();
              send(input);
            }}
          >
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={placeholder}
              disabled={sending}
            />
            <button type="submit" disabled={sending || !input.trim()}>
              Send
            </button>
          </form>

          {handoffSource && asked && (
            <p className="handoff-offer small">
              <span className="muted">Didn't get what you need?</span>{' '}
              <button type="button" className="link-btn" onClick={openHandoff}>
                Talk to a person in HR
              </button>
            </p>
          )}
        </>
      )}
    </div>
  );
}