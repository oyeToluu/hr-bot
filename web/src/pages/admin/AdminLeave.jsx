import { useEffect, useState } from 'react';
import { api, LEAVE_STATUSES } from '../../api/client';
import { ErrorBox, Loading, StatusBadge, formatDate, label } from '../../components/ui';

export default function AdminLeave() {
  const [filter, setFilter] = useState('PENDING');
  const [requests, setRequests] = useState(null);
  const [employees, setEmployees] = useState({});
  const [notes, setNotes] = useState({});
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  // Map employeeId -> name so the list shows names, not ids
  useEffect(() => {
    api
      .listEmployees()
      .then((list) => setEmployees(Object.fromEntries(list.map((e) => [e.id, e]))))
      .catch(() => {});
  }, []);

  const load = (status) => {
    setRequests(null);
    api.allLeave(status).then(setRequests).catch((e) => setError(e.message));
  };

  useEffect(() => {
    load(filter);
  }, [filter]);

  const decide = async (id, decision) => {
    setError('');
    setBusyId(id);
    try {
      await api.decideLeave(id, decision, notes[id] || null);
      load(filter);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <>
      <h1>Leave approvals</h1>

      <div className="tabs">
        {['', ...LEAVE_STATUSES].map((s) => (
          <button
            key={s || 'ALL'}
            className={`tab ${filter === s ? 'active' : ''}`}
            onClick={() => setFilter(s)}
          >
            {s ? label(s) : 'All'}
          </button>
        ))}
      </div>

      <ErrorBox error={error} />

      <div className="card">
        {!requests ? (
          <Loading />
        ) : requests.length === 0 ? (
          <p className="muted">No requests here.</p>
        ) : (
          <ul className="list">
            {requests.map((r) => {
              const who = employees[r.employeeId];
              return (
                <li key={r.id} className="leave-item">
                  <div className="leave-main">
                    <div>
                      <strong>{who ? who.fullName : r.employeeId}</strong>
                      {who && <span className="muted small"> · {who.department}</span>}
                    </div>
                    <div>
                      {label(r.leaveType)} · {r.daysRequested} day(s) ·{' '}
                      {formatDate(r.startDate)} – {formatDate(r.endDate)}
                    </div>
                    {r.reason && <div className="muted small">Reason: {r.reason}</div>}
                    {r.reviewNote && <div className="small">Note: {r.reviewNote}</div>}
                  </div>

                  {r.status === 'PENDING' ? (
                    <div className="decision">
                      <input
                        placeholder="Note (optional)"
                        value={notes[r.id] || ''}
                        onChange={(e) => setNotes({ ...notes, [r.id]: e.target.value })}
                      />
                      <button disabled={busyId === r.id} onClick={() => decide(r.id, 'APPROVED')}>
                        Approve
                      </button>
                      <button
                        className="danger"
                        disabled={busyId === r.id}
                        onClick={() => decide(r.id, 'REJECTED')}
                      >
                        Reject
                      </button>
                    </div>
                  ) : (
                    <StatusBadge status={r.status} />
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </>
  );
}
