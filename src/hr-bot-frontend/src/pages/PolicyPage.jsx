import { useState } from 'react';
import { policyApi } from '../api/client';
import { ErrorAlert } from '../components/ui';

const EXAMPLES = ['How many sick days do I get?', 'Can I work from home?', 'What happens if someone harasses me?'];

// POST /api/policy/ask { question } -> { answer, matches: [{ score, docTitle, heading, content }] }
export default function PolicyPage() {
  const [question, setQuestion] = useState('');
  const [result, setResult] = useState(null);
  const [asked, setAsked] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const ask = async (text) => {
    const q = text.trim();
    if (!q) return;
    setLoading(true);
    setError('');
    setQuestion(q);
    try {
      setResult(await policyApi.ask(q));
      setAsked(q);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Policy Q&amp;A</h1>
          <p className="muted">Ask a question and get the relevant part of the company handbook.</p>
        </div>
      </header>

      <form
        className="chat-input standalone"
        onSubmit={(e) => {
          e.preventDefault();
          ask(question);
        }}
      >
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask about leave, remote work, conduct…"
          aria-label="Your policy question"
        />
        <button type="submit" className="btn-primary" disabled={loading || !question.trim()}>
          {loading ? 'Searching…' : 'Ask'}
        </button>
      </form>

      <div className="chips">
        {EXAMPLES.map((q) => (
          <button key={q} type="button" className="chip" onClick={() => ask(q)}>{q}</button>
        ))}
      </div>

      <ErrorAlert message={error} />

      {result && (
        <section className="panel">
          <p className="muted small">You asked: “{asked}”</p>
          <p className="answer">{result.answer}</p>

          {result.matches?.length > 0 && (
            <>
              <h3 className="sources-title">From the handbook</h3>
              <ul className="sources">
                {result.matches.map((m, i) => (
                  <li key={i} className="source">
                    <div className="source-head">
                      <strong>{m.heading}</strong>
                      <span className="muted small">{m.docTitle}</span>
                    </div>
                    <p>{m.content}</p>
                  </li>
                ))}
              </ul>
            </>
          )}
        </section>
      )}
    </div>
  );
}
