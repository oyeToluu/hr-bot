import { useEffect, useRef, useState } from 'react';

// Chat UI shared by the Onboarding bot and the Policy assistant.
// `ask(text)` must return a Promise of { answer, detail? }.
export default function ChatBox({ greeting, placeholder, suggestions = [], ask }) {
  const [messages, setMessages] = useState([{ from: 'bot', text: greeting }]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const endRef = useRef(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [messages]);

  const send = async (text) => {
    const question = text.trim();
    if (!question || sending) return;
    setInput('');
    setMessages((m) => [...m, { from: 'user', text: question }]);
    setSending(true);
    try {
      const { answer, detail } = await ask(question);
      setMessages((m) => [...m, { from: 'bot', text: answer, detail }]);
    } catch (err) {
      setMessages((m) => [...m, { from: 'bot', text: err.message, error: true }]);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="chat">
      <div className="chat-log">
        {messages.map((m, i) => (
          <div key={i} className={`bubble ${m.from} ${m.error ? 'error' : ''}`}>
            <p>{m.text}</p>
            {m.detail && <p className="bubble-detail">{m.detail}</p>}
          </div>
        ))}
        {sending && <div className="bubble bot typing">Thinking…</div>}
        <div ref={endRef} />
      </div>

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
    </div>
  );
}
