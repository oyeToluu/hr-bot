import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, APPLICATION_STATUSES } from '../../api/client';
import { ErrorBox, Loading, ScoreBar, SkillChips, formatDate, label } from '../../components/ui';

// Which buttons HR gets for an application in each status.
const ACTIONS = {
  SHORTLISTED: [
    ['INTERVIEW', 'Invite to interview', ''],
    ['REJECTED', 'Reject', 'danger'],
  ],
  UNDER_REVIEW: [
    ['INTERVIEW', 'Invite to interview', ''],
    ['REJECTED', 'Reject', 'danger'],
  ],
  AUTO_REJECTED: [['REINSTATE', 'Reinstate for review', 'secondary']],
  INTERVIEW: [
    ['HIRED', 'Mark hired', ''],
    ['REJECTED', 'Reject', 'danger'],
  ],
};

export default function AdminApplicants() {
  const { jobId } = useParams();
  const [apps, setApps] = useState(null);
  const [filter, setFilter] = useState('');
  const [notes, setNotes] = useState({});
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const load = () => api.jobApplications(jobId).then(setApps).catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, [jobId]);

  const decide = async (id, decision) => {
    setError('');
    setBusyId(id);
    try {
      await api.decideApplication(id, decision, notes[id] || null);
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusyId(null);
    }
  };

  const download = (a) => api.downloadCv(a.id, a.cvFileName).catch((e) => setError(e.message));

  const shown = apps?.filter((a) => !filter || a.status === filter) || [];
  const count = (s) => apps?.filter((a) => a.status === s).length || 0;
  const title = apps?.[0]?.jobTitle;

  return (
    <>
      <p className="small">
        <Link to="/admin/recruitment">← All jobs</Link>
      </p>
      <h1>Applicants{title ? `: ${title}` : ''}</h1>
      <p className="muted">Ranked by the screening bot, best fit first. Every final decision is yours.</p>

      <div className="tabs">
        <button className={`tab ${filter === '' ? 'active' : ''}`} onClick={() => setFilter('')}>
          All ({apps?.length || 0})
        </button>
        {APPLICATION_STATUSES.map((s) => (
          <button key={s} className={`tab ${filter === s ? 'active' : ''}`} onClick={() => setFilter(s)}>
            {label(s)} ({count(s)})
          </button>
        ))}
      </div>

      <ErrorBox error={error} />

      {!apps ? (
        <Loading />
      ) : shown.length === 0 ? (
        <div className="card">
          <p className="muted">No applications here yet.</p>
        </div>
      ) : (
        shown.map((a) => (
          <div key={a.id} className="card applicant">
            <div className="applicant-head">
              <div>
                <strong>{a.candidateName}</strong>
                <div className="muted small">
                  {a.candidateEmail} · applied {formatDate(a.appliedAt)}
                </div>
              </div>
              <span className={`badge badge-${a.status.toLowerCase()}`}>{label(a.status)}</span>
            </div>

            <ScoreBar score={a.finalScore} />
            <p className="muted small">
              Keyword score {a.keywordScore}
              {a.aiScore != null && ` · AI score ${a.aiScore}`} · screened by{' '}
              {a.screenedBy === 'KEYWORD+AI' ? 'keywords + AI' : 'keywords'}
            </p>

            {a.cvUnreadable && (
              <div className="warn-box">
                Couldn't read enough text from this CV (maybe a scanned image). Please open it and
                review by hand.
              </div>
            )}

            <p>{a.screeningSummary}</p>
            {a.aiSummary && (
              <p className="ai-note">
                <strong>AI notes:</strong> {a.aiSummary}
              </p>
            )}

            <dl className="details">
              <dt>Has</dt>
              <dd>{a.matchedSkills.length ? <SkillChips skills={a.matchedSkills} tone="ok" /> : '—'}</dd>
              <dt>Missing</dt>
              <dd>{a.missingSkills.length ? <SkillChips skills={a.missingSkills} tone="bad" /> : '—'}</dd>
              {a.bonusSkills.length > 0 && (
                <>
                  <dt>Bonus</dt>
                  <dd>
                    <SkillChips skills={a.bonusSkills} />
                  </dd>
                </>
              )}
              <dt>Experience</dt>
              <dd>
                ~{a.yearsExperienceDetected} {a.yearsExperienceDetected === 1 ? 'year' : 'years'}
              </dd>
            </dl>

            {a.coverNote && <p className="small">Cover note: {a.coverNote}</p>}
            {a.hrNote && <p className="small">HR note: {a.hrNote}</p>}

            <div className="decision">
              <button className="secondary" onClick={() => download(a)}>
                Download CV
              </button>
              {ACTIONS[a.status] && (
                <>
                  <input
                    placeholder="Note (optional)"
                    value={notes[a.id] || ''}
                    onChange={(e) => setNotes({ ...notes, [a.id]: e.target.value })}
                  />
                  {ACTIONS[a.status].map(([decision, text, cls]) => (
                    <button
                      key={decision}
                      className={cls}
                      disabled={busyId === a.id}
                      onClick={() => decide(a.id, decision)}
                    >
                      {text}
                    </button>
                  ))}
                </>
              )}
            </div>
          </div>
        ))
      )}
    </>
  );
}
