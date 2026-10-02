import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { ErrorBox, Loading, formatDate } from '../../components/ui';

const EMPTY = {
  title: '',
  department: '',
  location: '',
  description: '',
  requiredSkills: '',
  niceToHaveSkills: '',
  minYearsExperience: 0,
  educationKeywords: '',
  autoRejectBelow: 40,
  shortlistAt: 70,
};

// "Java, Spring Boot ,  Git" -> ["Java", "Spring Boot", "Git"]
const toList = (text) =>
  text
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

export default function AdminRecruitment() {
  const [jobs, setJobs] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () => api.manageJobs().then(setJobs).catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const create = async (e) => {
    e.preventDefault();
    setFormError('');
    setBusy(true);
    try {
      await api.createJob({
        ...form,
        requiredSkills: toList(form.requiredSkills),
        niceToHaveSkills: toList(form.niceToHaveSkills),
        educationKeywords: toList(form.educationKeywords),
        minYearsExperience: Number(form.minYearsExperience),
        autoRejectBelow: Number(form.autoRejectBelow),
        shortlistAt: Number(form.shortlistAt),
      });
      setForm(EMPTY);
      setShowForm(false);
      load();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const toggleOpen = async (job) => {
    setError('');
    try {
      await (job.open ? api.closeJob(job.id) : api.reopenJob(job.id));
      load();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <>
      <div className="card-head">
        <h1>Recruitment</h1>
        <button onClick={() => setShowForm(!showForm)}>{showForm ? 'Cancel' : 'New job'}</button>
      </div>
      <p className="muted">
        Candidates apply on the <Link to="/careers">careers page</Link>. The screening bot scores
        every CV against the job and sorts it for you; you make the final call.
      </p>

      {showForm && (
        <form className="card form-grid" onSubmit={create}>
          <h2>Post a new job</h2>
          <ErrorBox error={formError} />
          <label className="full">
            Job title
            <input value={form.title} onChange={update('title')} required />
          </label>
          <label>
            Department
            <input value={form.department} onChange={update('department')} />
          </label>
          <label>
            Location
            <input value={form.location} onChange={update('location')} />
          </label>
          <label className="full">
            Description
            <textarea rows={3} value={form.description} onChange={update('description')} />
          </label>
          <label className="full">
            Required skills <span className="muted">(comma separated, worth 60 points)</span>
            <input
              value={form.requiredSkills}
              onChange={update('requiredSkills')}
              placeholder="Java, Spring Boot, MongoDB"
            />
          </label>
          <label className="full">
            Nice-to-have skills <span className="muted">(worth 15 points)</span>
            <input value={form.niceToHaveSkills} onChange={update('niceToHaveSkills')} />
          </label>
          <label>
            Minimum years of experience <span className="muted">(15 points)</span>
            <input type="number" min={0} max={40} value={form.minYearsExperience} onChange={update('minYearsExperience')} />
          </label>
          <label>
            Education keywords <span className="muted">(10 points)</span>
            <input value={form.educationKeywords} onChange={update('educationKeywords')} placeholder="BSc, HND" />
          </label>
          <label>
            Auto-reject below score
            <input type="number" min={0} max={100} value={form.autoRejectBelow} onChange={update('autoRejectBelow')} />
          </label>
          <label>
            Shortlist at score
            <input type="number" min={0} max={100} value={form.shortlistAt} onChange={update('shortlistAt')} />
          </label>
          <div className="full">
            <button type="submit" disabled={busy}>
              {busy ? 'Posting…' : 'Post job'}
            </button>
          </div>
        </form>
      )}

      <ErrorBox error={error} />
      <div className="card">
        {!jobs ? (
          <Loading />
        ) : jobs.length === 0 ? (
          <p className="muted">No jobs posted yet.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Job</th>
                  <th>Applications</th>
                  <th>Status</th>
                  <th>Posted</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {jobs.map((j) => (
                  <tr key={j.id}>
                    <td>
                      <Link to={`/admin/recruitment/${j.id}`}>
                        <strong>{j.title}</strong>
                      </Link>
                      <div className="muted small">{[j.department, j.location].filter(Boolean).join(' · ')}</div>
                    </td>
                    <td>{j.applications}</td>
                    <td>
                      <span className={`badge badge-${j.open ? 'approved' : 'cancelled'}`}>
                        {j.open ? 'Open' : 'Closed'}
                      </span>
                    </td>
                    <td>{formatDate(j.createdAt)}</td>
                    <td className="row">
                      <Link className="button" to={`/admin/recruitment/${j.id}`}>
                        Applicants
                      </Link>
                      <button className="secondary" onClick={() => toggleOpen(j)}>
                        {j.open ? 'Close' : 'Reopen'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
