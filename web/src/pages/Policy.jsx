import { useEffect, useState } from 'react';
import { api } from '../api/client';
import ChatBox from '../components/ChatBox';
import { ErrorBox } from '../components/ui';

export default function Policy() {
  const [docs, setDocs] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.listPolicies().then(setDocs).catch((e) => setError(e.message));
  }, []);

  const ask = async (question) => {
    const res = await api.askPolicy(question); // { answer, matches: [{ docTitle, heading, ... }] }
    const sources = (res.matches || []).map((m) => `${m.docTitle} → ${m.heading}`);
    return {
      answer: res.answer,
      detail: sources.length ? `Source: ${sources.join('; ')}` : null,
    };
  };

  return (
    <>
      <h1>Policy assistant</h1>
      <p className="muted">Ask about company policies. Answers come from the policy documents HR has added.</p>
      <ErrorBox error={error} />

      <ChatBox
        greeting="Hi! Ask me about leave, remote work, conduct, or any other company policy."
        placeholder="e.g. How many sick days do I get?"
        suggestions={[
          'How many annual leave days do I get?',
          'Can I work from home?',
          'What is the code of conduct?',
        ]}
        ask={ask}
      />

      {docs.map((doc) => (
        <div className="card" key={doc.id}>
          <h2>{doc.title}</h2>
          {doc.sections.map((s) => (
            <details key={s.heading} className="faq">
              <summary>{s.heading}</summary>
              <p>{s.content}</p>
            </details>
          ))}
        </div>
      ))}
    </>
  );
}
