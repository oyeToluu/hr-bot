import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { ErrorBox, Loading, SkillChips } from '../../components/ui';

// Public job board: no login needed to browse.
export default function Careers() {
  const [jobs, setJobs] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.openJobs().then(setJobs).catch((e) => setError(e.message));
  }, []);

  return (
    <>
      <h1>Open positions</h1>
      <p className="muted">
        Upload your CV and our screening assistant checks it against the role straight away. A
        member of our HR team reviews every shortlisted application.
      </p>
      <ErrorBox error={error} />
      {!jobs && !error && <Loading />}
      {jobs && jobs.length === 0 && <p className="muted">No open positions right now.</p>}
      <div className="job-grid">
        {jobs?.map((job) => (
          <div key={job.id} className="card job-card">
            <h2>{job.title}</h2>
            <p className="muted small">
              {[job.department, job.location].filter(Boolean).join(' · ')}
            </p>
            {job.description && <p className="job-desc">{job.description}</p>}
            <SkillChips skills={job.requiredSkills} />
            <Link className="button" to={`/careers/jobs/${job.id}`}>
              View and apply
            </Link>
          </div>
        ))}
      </div>
    </>
  );
}
