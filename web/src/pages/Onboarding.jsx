import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import ChatBox from '../components/ChatBox';
import { ErrorBox, label } from '../components/ui';

export default function Onboarding() {
  const { employee, setEmployee } = useAuth();
  const [faqs, setFaqs] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.listFaqs().then(setFaqs).catch((e) => setError(e.message));
  }, []);

  const ask = async (message) => {
    const res = await api.askOnboarding(message); // { answer, matchedQuestion, confidence }
    return {
      answer: res.answer,
      detail: res.matchedQuestion ? `Matched FAQ: "${res.matchedQuestion}"` : null,
    };
  };

  const markComplete = async () => {
    try {
      setEmployee(await api.completeOnboarding());
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <>
      <h1>Onboarding bot</h1>
      <p className="muted">Ask anything about your first weeks: pay, equipment, benefits, hours.</p>
      <ErrorBox error={error} />

      <ChatBox
        greeting={`Hi ${employee.fullName.split(' ')[0]}! I'm the onboarding bot. What would you like to know?`}
        placeholder="e.g. When do I get paid?"
        suggestions={faqs.slice(0, 4).map((f) => f.question)}
        ask={ask}
      />

      <div className="card">
        <div className="card-head">
          <h2>Onboarding status</h2>
          {employee.onboardingComplete ? (
            <span className="badge badge-approved">Complete</span>
          ) : (
            <button onClick={markComplete}>Mark onboarding complete</button>
          )}
        </div>
      </div>

      {faqs.length > 0 && (
        <div className="card">
          <h2>All FAQs</h2>
          {faqs.map((f) => (
            <details key={f.id} className="faq">
              <summary>
                {f.question} <span className="muted small">· {label(f.category)}</span>
              </summary>
              <p>{f.answer}</p>
            </details>
          ))}
        </div>
      )}
    </>
  );
}
