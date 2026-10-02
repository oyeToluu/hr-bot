import { useEffect, useState } from 'react';
import { api, LEAVE_TYPES } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { ErrorBox, Loading, StatusBadge, formatDate, label } from '../components/ui';

const EMPTY = { leaveType: 'ANNUAL', startDate: '', endDate: '', reason: '' };

export default function Leave() {
  const { employee, setEmployee } = useAuth();
  const [form, setForm] = useState(EMPTY);
  const [requests, setRequests] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () =>
    api.myLeave().then(setRequests).catch((e) => setError(e.message));

  useEffect(() => {
    load();
  }, []);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setBusy(true);
    try {
      const created = await api.createLeave(
        form.leaveType,
        form.startDate,
        form.endDate,
        form.reason || null
      );
      setSuccess(`Request sent: ${created.daysRequested} day(s) of ${label(created.leaveType)} leave.`);
      setForm(EMPTY);
      await load();
      setEmployee(await api.me()); // refresh leave balance
    } catch (err) {
      const fields = Object.entries(err.fieldErrors || {})
        .map(([f, m]) => `${f}: ${m}`)
        .join(', ');
      setError(fields ? `${err.message} (${fields})` : err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <h1>Leave requests</h1>

      <form className="card form-grid" onSubmit={submit}>
        <h2>New request</h2>
        <p className="muted small full">You have {employee.leaveBalance} leave day(s) left.</p>
        <ErrorBox error={error} />
        {success && <div className="success-box">{success}</div>}

        <label>
          Leave type
          <select value={form.leaveType} onChange={update('leaveType')}>
            {LEAVE_TYPES.map((t) => (
              <option key={t} value={t}>
                {label(t)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Start date
          <input type="date" value={form.startDate} onChange={update('startDate')} required />
        </label>
        <label>
          End date
          <input
            type="date"
            value={form.endDate}
            min={form.startDate || undefined}
            onChange={update('endDate')}
            required
          />
        </label>
        <label className="full">
          Reason <span className="muted">(optional)</span>
          <textarea rows={2} value={form.reason} onChange={update('reason')} />
        </label>
        <button type="submit" disabled={busy}>
          {busy ? 'Sending…' : 'Submit request'}
        </button>
      </form>

      <div className="card">
        <h2>My history</h2>
        {!requests ? (
          <Loading />
        ) : requests.length === 0 ? (
          <p className="muted">No leave requests yet.</p>
        ) : (
          <ul className="list">
            {requests.map((r) => (
              <li key={r.id}>
                <div>
                  <strong>{label(r.leaveType)}</strong> · {r.daysRequested} day(s)
                  <div className="muted small">
                    {formatDate(r.startDate)} – {formatDate(r.endDate)}
                    {r.reason && ` · ${r.reason}`}
                  </div>
                  {r.reviewNote && <div className="small">HR note: {r.reviewNote}</div>}
                </div>
                <StatusBadge status={r.status} />
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}
