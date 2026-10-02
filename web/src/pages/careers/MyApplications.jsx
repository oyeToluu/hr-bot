import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { CandidateStatus, ErrorBox, Loading, SkillChips, formatDate } from '../../components/ui';

export default function MyApplications() {
  const [apps, setApps] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.myApplications().then(setApps).catch((e) => setError(e.message));
  }, []);

  return (
    <>
      <h1>My applications</h1>
      <ErrorBox error={error} />
      {!apps && !error && <Loading />}
      {apps && apps.length === 0 && (
        <div className="card">
          <p className="muted">You haven't applied for anything yet.</p>
          <Link className="button" to="/careers">
            Browse open jobs
          </Link>
        </div>
      )}
      {apps && apps.length > 0 && (
        <div className="card">
          <ul className="list">
            {apps.map((a) => (
              <li key={a.id} className="leave-item">
                <div className="leave-main">
                  <strong>{a.jobTitle}</strong>
                  <div className="muted small">Applied {formatDate(a.appliedAt)}</div>
                  {a.missingSkills.length > 0 && (
                    <div className="small">
                      Skills not found in your CV: <SkillChips skills={a.missingSkills} tone="bad" />
                    </div>
                  )}
                </div>
                <CandidateStatus status={a.status} />
              </li>
            ))}
          </ul>
        </div>
      )}
    </>
  );
}
