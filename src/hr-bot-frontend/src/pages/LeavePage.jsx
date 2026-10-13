import { useEffect, useState } from 'react';
import { leaveApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorAlert, Spinner, StatusBadge, formatDate, titleCase } from '../components/ui';

// Must match LeaveRequest.LeaveType in the backend.
const LEAVE_TYPES = ['ANNUAL', 'SICK', 'CASUAL', 'MATERNITY', 'PATERNITY', 'UNPAID'];
const today = () => new Date().toISOString().slice(0, 10);
const emptyForm = () => ({ leaveType: 'ANNUAL', startDate: today(), endDate: today(), reason: '' });

export default function LeavePage() {
  const { user } = useAuth();
  const [form, setForm] = useState(emptyForm);
  const [requests, setRequests] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = () =>
    leaveApi
      .mine()
      .then((list) => setRequests([...list].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))))
      .catch((err) => setError(err.message));

  useEffect(() => {
    load();
  }, []);

  const update = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (form.endDate < form.startDate) {
      setError('End date must be on or after the start date.');
      return;
    }
    setSubmitting(true);
    try {
      const created = await leaveApi.create({ ...form, reason: form.reason.trim() || null });
      setSuccess(`Request sent for ${created.daysRequested} day${created.daysRequested === 1 ? '' : 's'}. HR will review it.`);
      setForm(emptyForm());
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Leave</h1>
          <p className="muted">You have <strong>{user.leaveBalance}</strong> days of leave available.</p>
        </div>
      </header>

      <div className="split">
        <section className="panel">
          <h2>New request</h2>
          <form className="form" onSubmit={handleSubmit}>
            <ErrorAlert message={error} />
            {success && <div className="alert alert-success">{success}</div>}

            <label className="field">
              <span>Leave type</span>
              <select name="leaveType" value={form.leaveType} onChange={update}>
                {LEAVE_TYPES.map((t) => (
                  <option key={t} value={t}>{titleCase(t)}</option>
                ))}
              </select>
            </label>

            <div className="field-row">
              <label className="field">
                <span>Start date</span>
                <input type="date" name="startDate" value={form.startDate} onChange={update} required />
              </label>
              <label className="field">
                <span>End date</span>
                <input type="date" name="endDate" value={form.endDate} min={form.startDate} onChange={update} required />
              </label>
            </div>

            <label className="field">
              <span>Reason <em className="hint">(optional)</em></span>
              <textarea name="reason" rows={3} value={form.reason} onChange={update} />
            </label>

            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? 'Sending…' : 'Submit request'}
            </button>
          </form>
        </section>

        <section className="panel">
          <h2>My requests</h2>
          {requests === null ? (
            <Spinner />
          ) : requests.length === 0 ? (
            <p className="muted">You haven't requested any leave yet.</p>
          ) : (
            <ul className="list">
              {requests.map((req) => (
                <li key={req.id} className="list-row">
                  <div>
                    <strong>{titleCase(req.leaveType)}</strong>
                    <span className="muted small">
                      {formatDate(req.startDate)} – {formatDate(req.endDate)} · {req.daysRequested} day{req.daysRequested === 1 ? '' : 's'}
                    </span>
                    {req.reviewNote && <span className="small note">HR note: {req.reviewNote}</span>}
                  </div>
                  <StatusBadge status={req.status} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
