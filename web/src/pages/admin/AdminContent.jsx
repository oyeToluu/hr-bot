import { useState } from 'react';
import { api, FAQ_CATEGORIES } from '../../api/client';
import { ErrorBox, label } from '../../components/ui';

// "pay, salary , payroll" -> ["pay", "salary", "payroll"]
const toKeywords = (text) =>
  text
    .split(',')
    .map((k) => k.trim().toLowerCase())
    .filter(Boolean);

const EMPTY_SECTION = { heading: '', content: '', keywords: '' };

export default function AdminContent() {
  // --- Add onboarding FAQ ---
  const [faq, setFaq] = useState({ question: '', answer: '', keywords: '', category: 'GENERAL' });
  const [faqMsg, setFaqMsg] = useState('');
  const [faqError, setFaqError] = useState('');

  const saveFaq = async (e) => {
    e.preventDefault();
    setFaqMsg('');
    setFaqError('');
    try {
      await api.createFaq(faq.question, faq.answer, toKeywords(faq.keywords), faq.category);
      setFaqMsg('FAQ added. The onboarding bot can answer it now.');
      setFaq({ question: '', answer: '', keywords: '', category: 'GENERAL' });
    } catch (err) {
      setFaqError(err.message);
    }
  };

  // --- Add policy document ---
  const [title, setTitle] = useState('');
  const [sections, setSections] = useState([{ ...EMPTY_SECTION }]);
  const [policyMsg, setPolicyMsg] = useState('');
  const [policyError, setPolicyError] = useState('');

  const updateSection = (i, field, value) =>
    setSections(sections.map((s, idx) => (idx === i ? { ...s, [field]: value } : s)));

  const savePolicy = async (e) => {
    e.preventDefault();
    setPolicyMsg('');
    setPolicyError('');
    try {
      await api.createPolicy(
        title,
        sections.map((s) => ({ heading: s.heading, content: s.content, keywords: toKeywords(s.keywords) }))
      );
      setPolicyMsg('Policy document added. The policy assistant can answer from it now.');
      setTitle('');
      setSections([{ ...EMPTY_SECTION }]);
    } catch (err) {
      setPolicyError(err.message);
    }
  };

  return (
    <>
      <h1>FAQs &amp; policies</h1>

      <form className="card form-grid" onSubmit={saveFaq}>
        <h2>Add onboarding FAQ</h2>
        <ErrorBox error={faqError} />
        {faqMsg && <div className="success-box">{faqMsg}</div>}
        <label className="full">
          Question
          <input
            value={faq.question}
            onChange={(e) => setFaq({ ...faq, question: e.target.value })}
            required
          />
        </label>
        <label className="full">
          Answer
          <textarea
            rows={3}
            value={faq.answer}
            onChange={(e) => setFaq({ ...faq, answer: e.target.value })}
            required
          />
        </label>
        <label>
          Keywords <span className="muted">(comma separated)</span>
          <input
            value={faq.keywords}
            placeholder="pay, salary, payroll"
            onChange={(e) => setFaq({ ...faq, keywords: e.target.value })}
          />
        </label>
        <label>
          Category
          <select value={faq.category} onChange={(e) => setFaq({ ...faq, category: e.target.value })}>
            {FAQ_CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {label(c)}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Add FAQ</button>
      </form>

      <form className="card form-grid" onSubmit={savePolicy}>
        <h2>Add policy document</h2>
        <ErrorBox error={policyError} />
        {policyMsg && <div className="success-box">{policyMsg}</div>}
        <label className="full">
          Document title
          <input value={title} onChange={(e) => setTitle(e.target.value)} required />
        </label>

        {sections.map((s, i) => (
          <fieldset key={i} className="full section-box">
            <legend>Section {i + 1}</legend>
            <label>
              Heading
              <input value={s.heading} onChange={(e) => updateSection(i, 'heading', e.target.value)} required />
            </label>
            <label>
              Content
              <textarea
                rows={3}
                value={s.content}
                onChange={(e) => updateSection(i, 'content', e.target.value)}
                required
              />
            </label>
            <label>
              Keywords <span className="muted">(comma separated)</span>
              <input value={s.keywords} onChange={(e) => updateSection(i, 'keywords', e.target.value)} />
            </label>
            {sections.length > 1 && (
              <button
                type="button"
                className="link-btn"
                onClick={() => setSections(sections.filter((_, idx) => idx !== i))}
              >
                Remove section
              </button>
            )}
          </fieldset>
        ))}

        <div className="full row">
          <button type="button" className="secondary" onClick={() => setSections([...sections, { ...EMPTY_SECTION }])}>
            + Add section
          </button>
          <button type="submit">Save document</button>
        </div>
      </form>
    </>
  );
}
