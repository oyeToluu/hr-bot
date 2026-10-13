import { useCallback, useEffect, useState } from 'react';
import { employeeApi, leaveApi } from '../../api/client';
import { ErrorAlert, Spinner, StatusBadge, formatDate, titleCase } from '../../components/ui';

const FILTERS = ['PENDING', 'APPROVED', 'REJECTED', 'ALL'];

// HR view of all leave requests, with approve / reject.
// Leave requests only hold employeeId, so we load /api/employees once to show names.
export default function AdminLeavePage() {
  const [filter, setFilter] = useState('PENDING');
  const [requests, setRequests] = useState(null);
  const [people, setPeople] = useState({});
  const [notes, setNotes] = useState({});
  const [busyId, setBusyId] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    employeeApi
      .list()
      .then((list) => setPeople(Object.fromEntries(list.map((e) => [e.id, e]))))
      .catch(() => {});
  }, []);

  const load = useCallback(() => {
    setRequests(null);
    leaveApi
      .all(filter === 'ALL' ? null : filter)
      .then((list) => setRequests([...list].sort((a, b) => (a.startDate || '').localeCompare(b.startDate || ''))))
      .catch((err) => {
        setError(err.message);
        setRequests([]);
      });
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  const decide = async (id, decision) => {
    setBusyId(id);
    setError('');
    try {
      await leaveApi.decide(id, decision, notes[id]?.trim() || null);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Leave approvals</h1>
          <p className="muted">Review and decide on employee leave requests.</p>
        </div>
      </header>

      <div className="tabs" role="tablist">
        {FILTERS.map((f) => (
          <button
            key={f}
            type="button"
            role="tab"
            aria-selected={filter === f}
            className={`tab ${filter === f ? 'active' : ''}`}
            onClick={() => setFilter(f)}
          >
            {titleCase(f)}
          </button>
        ))}
      </div>

      <ErrorAlert message={error} />

      <section className="panel">
        {requests === null ? (
          <Spinner />
        ) : requests.length === 0 ? (
          <p className="muted">No {filter === 'ALL' ? '' : filter.toLowerCase()} requests.</p>
        ) : (
          <ul className="list">
            {requests.map((req) => {
              const person = people[req.employeeId];
              return (
                <li key={req.id} className="list-row review-row">
                  <div className="review-main">
                    <strong>{person ? person.fullName : 'Employee'}</strong>
                    <span className="muted small">
                      {person?.department ? `${person.department} · ` : ''}
                      {titleCase(req.leaveType)} · {formatDate(req.startDate)} – {formatDate(req.endDate)} · {req.daysRequested} day{req.daysRequested === 1 ? '' : 's'}
                    </span>
                    {req.reason && <span className="small note">“{req.reason}”</span>}
                    {req.reviewNote && <span className="small note">HR note: {req.reviewNote}</span>}
                  </div>

                  {req.status === 'PENDING' ? (
                    <div className="review-actions">
                      <input
                        className="note-input"
                        placeholder="Note (optional)"
                        value={notes[req.id] || ''}
                        onChange={(e) => setNotes({ ...notes, [req.id]: e.target.value })}
                        aria-label="Note for employee"
                      />
                      <button type="button" className="btn-primary btn-sm" disabled={busyId === req.id} onClick={() => decide(req.id, 'APPROVED')}>
                        Approve
                      </button>
                      <button type="button" className="btn-danger btn-sm" disabled={busyId === req.id} onClick={() => decide(req.id, 'REJECTED')}>
                        Reject
                      </button>
                    </div>
                  ) : (
                    <StatusBadge status={req.status} />
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}
