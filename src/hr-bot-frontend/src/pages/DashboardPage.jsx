import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorAlert, StatusBadge, formatDate, titleCase } from '../components/ui';

// Employee home: GET /api/dashboard/me -> { profile, recentLeaveRequests, pendingCount }
export default function DashboardPage() {
  const { user, setUser } = useAuth();
  const [recent, setRecent] = useState([]);
  const [pendingCount, setPendingCount] = useState(0);
  const [error, setError] = useState('');

  useEffect(() => {
    dashboardApi
      .mine()
      .then((data) => {
        setUser(data.profile);
        setRecent(data.recentLeaveRequests || []);
        setPendingCount(data.pendingCount || 0);
      })
      .catch((err) => setError(err.message));
  }, [setUser]);

  const firstName = user.fullName.split(' ')[0];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Good to see you, {firstName}</h1>
          <p className="muted">{user.department} · {user.email}</p>
        </div>
      </header>

      <ErrorAlert message={error} />

      <section className="stat-grid">
        <div className="stat">
          <span className="stat-label">Leave balance</span>
          <span className="stat-value">{user.leaveBalance ?? '—'}<small> days</small></span>
        </div>
        <div className="stat">
          <span className="stat-label">Pending requests</span>
          <span className="stat-value">{pendingCount}</span>
        </div>
        <div className="stat">
          <span className="stat-label">Onboarding</span>
          <span className={`stat-value ${user.onboardingComplete ? 'ok' : 'pending'}`}>
            {user.onboardingComplete ? 'Complete' : 'In progress'}
          </span>
        </div>
      </section>

      <section className="card-grid">
        <Link to="/leave" className="action-card">
          <h3>Request leave</h3>
          <p>Book time off and see the status of past requests.</p>
        </Link>
        <Link to="/onboarding" className="action-card">
          <h3>Onboarding assistant</h3>
          <p>Ask the bot anything about your first weeks.</p>
        </Link>
        <Link to="/policy" className="action-card">
          <h3>Policy Q&amp;A</h3>
          <p>Get answers from the company handbook.</p>
        </Link>
      </section>

      <section className="panel">
        <div className="panel-head">
          <h2>Recent leave requests</h2>
          <Link to="/leave">View all</Link>
        </div>
        {recent.length === 0 ? (
          <p className="muted">No leave requests yet.</p>
        ) : (
          <ul className="list">
            {recent.map((req) => (
              <li key={req.id} className="list-row">
                <div>
                  <strong>{titleCase(req.leaveType)}</strong>
                  <span className="muted small">
                    {formatDate(req.startDate)} – {formatDate(req.endDate)} · {req.daysRequested} day{req.daysRequested === 1 ? '' : 's'}
                  </span>
                </div>
                <StatusBadge status={req.status} />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
