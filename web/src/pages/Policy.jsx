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
    // { answer, matches: [{ docTitle, heading, ... }], aiAnswer }
    const res = await api.askPolicy(question);
    const sources = (res.matches || []).map((m) => `${m.docTitle} → ${m.heading}`);
    return {
      answer: res.answer,
      detail: sources.length
        ? `${res.aiAnswer ? 'Based on' : 'Source'}: ${sources.join('; ')}`
        : null,
    };
  };

  return (
    <>
      <h1>HR Assistant</h1>
      <p className="muted">
        Ask about company policies or your own HR details, like your leave balance or leave
        requests. Answers come from HR's policy documents and your own record, and show where
        they came from.
      </p>
      <ErrorBox error={error} />

      <ChatBox
        greeting="Hi! Ask me about company policy or your own HR details, like how many leave days you have left."
        placeholder="e.g. Has my leave request been approved?"
        suggestions={[
          'How many leave days do I have left?',
          'Has my leave been approved?',
          'Can I work from home?',
          'How many sick days do I get?',
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