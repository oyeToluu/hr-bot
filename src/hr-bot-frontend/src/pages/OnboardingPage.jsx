import { useEffect, useRef, useState } from 'react';
import { onboardingApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorAlert } from '../components/ui';

// Chat with the onboarding FAQ bot: POST /api/onboarding/ask { message }
// -> { answer, matchedQuestion, confidence }
export default function OnboardingPage() {
  const { user, setUser } = useAuth();
  const [faqs, setFaqs] = useState([]);
  const [messages, setMessages] = useState([
    { from: 'bot', text: `Hi ${user.fullName.split(' ')[0]}! Ask me anything about starting here — pay, equipment, benefits, office hours.` },
  ]);
  const [input, setInput] = useState('');
  const [asking, setAsking] = useState(false);
  const [error, setError] = useState('');
  const [completing, setCompleting] = useState(false);
  const endRef = useRef(null);

  useEffect(() => {
    onboardingApi.faqs().then(setFaqs).catch(() => {});
  }, []);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }, [messages]);

  const ask = async (text) => {
    const question = text.trim();
    if (!question || asking) return;
    setError('');
    setInput('');
    setMessages((m) => [...m, { from: 'user', text: question }]);
    setAsking(true);
    try {
      const res = await onboardingApi.ask(question);
      setMessages((m) => [...m, { from: 'bot', text: res.answer, matched: res.matchedQuestion }]);
    } catch (err) {
      setError(err.message);
    } finally {
      setAsking(false);
    }
  };

  const markComplete = async () => {
    setCompleting(true);
    setError('');
    try {
      setUser(await onboardingApi.complete());
    } catch (err) {
      setError(err.message);
    } finally {
      setCompleting(false);
    }
  };

  return (
    <div className="page">
      <header className="page-header row">
        <div>
          <h1>Onboarding</h1>
          <p className="muted">Your onboarding assistant, available any time.</p>
        </div>
        {user.onboardingComplete ? (
          <span className="badge badge-approved">Onboarding complete</span>
        ) : (
          <button type="button" className="btn-secondary" onClick={markComplete} disabled={completing}>
            {completing ? 'Saving…' : 'Mark onboarding complete'}
          </button>
        )}
      </header>

      <ErrorAlert message={error} />

      <section className="chat">
        <div className="chat-log" aria-live="polite">
          {messages.map((msg, i) => (
            <div key={i} className={`bubble bubble-${msg.from}`}>
              <p>{msg.text}</p>
              {msg.matched && <span className="bubble-meta">Matched: {msg.matched}</span>}
            </div>
          ))}
          {asking && <div className="bubble bubble-bot typing">Thinking…</div>}
          <div ref={endRef} />
        </div>

        {faqs.length > 0 && (
          <div className="chips">
            {faqs.slice(0, 6).map((faq) => (
              <button key={faq.id} type="button" className="chip" onClick={() => ask(faq.question)}>
                {faq.question}
              </button>
            ))}
          </div>
        )}

        <form
          className="chat-input"
          onSubmit={(e) => {
            e.preventDefault();
            ask(input);
          }}
        >
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="e.g. When do I get paid?"
            aria-label="Your question"
          />
          <button type="submit" className="btn-primary" disabled={asking || !input.trim()}>Send</button>
        </form>
      </section>
    </div>
  );
}
