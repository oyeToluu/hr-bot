import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { api } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { CandidateStatus, ErrorBox, Loading, SkillChips } from '../../components/ui';

const ACCEPT = '.pdf,.docx,.txt,.zip';
const MAX_BYTES = 5 * 1024 * 1024;

export default function JobDetail() {
  const { id } = useParams();
  const location = useLocation();
  const { candidate, employee } = useAuth();
  const [job, setJob] = useState(null);
  const [file, setFile] = useState(null);
  const [coverNote, setCoverNote] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.openJob(id).then(setJob).catch((e) => setError(e.message));
  }, [id]);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    if (!file) return setError('Please choose your CV file.');
    if (file.size > MAX_BYTES) return setError('File is too large (max 5 MB).');
    setBusy(true);
    try {
      setResult(await api.applyForJob(id, file, coverNote));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  if (!job) return error ? <ErrorBox error={error} /> : <Loading />;

  return (
    <>
      <p className="small">
        <Link to="/careers">← All jobs</Link>
      </p>
      <h1>{job.title}</h1>
      <p className="muted">{[job.department, job.location].filter(Boolean).join(' · ')}</p>

      <div className="card">
        {job.description && <p>{job.description}</p>}
        <dl className="details">
          <dt>Required skills</dt>
          <dd>
            <SkillChips skills={job.requiredSkills} />
          </dd>
          {job.niceToHaveSkills?.length > 0 && (
            <>
              <dt>Nice to have</dt>
              <dd>
                <SkillChips skills={job.niceToHaveSkills} />
              </dd>
            </>
          )}
          {job.minYearsExperience > 0 && (
            <>
              <dt>Experience</dt>
              <dd>{job.minYearsExperience}+ years</dd>
            </>
          )}
          {job.educationKeywords?.length > 0 && (
            <>
              <dt>Education</dt>
              <dd>{job.educationKeywords.join(' / ')}</dd>
            </>
          )}
        </dl>
      </div>

      {result ? (
        <div className="card">
          <h2>Application received</h2>
          <p>
            Status: <CandidateStatus status={result.status} />
          </p>
          {result.cvUnreadable ? (
            <p className="muted">
              We couldn't read much text from your CV (it may be a scanned image), so a member of
              our HR team will review it by hand.
            </p>
          ) : (
            <>
              {result.matchedSkills.length > 0 && (
                <p>
                  Skills we found in your CV: <SkillChips skills={result.matchedSkills} tone="ok" />
                </p>
              )}
              {result.missingSkills.length > 0 && (
                <p>
                  Skills we couldn't find:{' '}
                  <SkillChips skills={result.missingSkills} tone="bad" />
                </p>
              )}
            </>
          )}
          <Link to="/careers/applications">Track your applications</Link>
        </div>
      ) : candidate ? (
        <form className="card form-grid" onSubmit={submit}>
          <h2>Apply for this job</h2>
          <ErrorBox error={error} />
          <label className="full">
            Your CV (PDF, Word .docx, .txt, or a .zip containing one, max 5 MB)
            <input type="file" accept={ACCEPT} onChange={(e) => setFile(e.target.files[0] || null)} />
          </label>
          <label className="full">
            Cover note <span className="muted">(optional)</span>
            <textarea rows={3} value={coverNote} onChange={(e) => setCoverNote(e.target.value)} />
          </label>
          <div className="full">
            <button type="submit" disabled={busy}>
              {busy ? 'Uploading and checking your CV…' : 'Submit application'}
            </button>
          </div>
        </form>
      ) : employee ? (
        <div className="card notice">
          <p>You're logged in with a staff account. Staff accounts can't apply here.</p>
        </div>
      ) : (
        <div className="card notice">
          <p>Log in or create a free candidate account to apply.</p>
          <div className="row">
            <Link className="button" to="/careers/login" state={{ from: location }}>
              Log in
            </Link>
            <Link className="button secondary-link" to="/careers/register" state={{ from: location }}>
              Create account
            </Link>
          </div>
        </div>
      )}
    </>
  );
}
