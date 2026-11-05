import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../../api/client';
import { ErrorBox, Loading, SkillChips } from '../../components/ui';

// Public job board: no login needed to browse.
// /careers shows every company's jobs; /careers?company=<slug> shows one company's.
export default function Careers() {
  const [params] = useSearchParams();
  const slug = params.get('company');
  const [jobs, setJobs] = useState(null);
  const [company, setCompany] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setJobs(null);
    setError('');
    setCompany(null);
    api.openJobs(slug).then(setJobs).catch((e) => setError(e.message));
    if (slug) api.publicCompany(slug).then(setCompany).catch(() => {});
  }, [slug]);

  return (
    <>
      <h1>{company ? `Jobs at ${company.name}` : 'Open positions'}</h1>
      <p className="muted">
        Upload your CV and the screening assistant checks it against the role straight away. The
        company's HR team reviews every shortlisted application, and you can follow your
        application here.
      </p>
      {slug && (
        <p className="small">
          <Link to="/careers">← See jobs at every company</Link>
        </p>
      )}
      <ErrorBox error={error} />
      {!jobs && !error && <Loading />}
      {jobs && jobs.length === 0 && <p className="muted">No open positions right now.</p>}
      <div className="job-grid">
        {jobs?.map((job) => (
          <div key={job.id} className="card job-card">
            {job.companyName && <p className="eyebrow">{job.companyName}</p>}
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