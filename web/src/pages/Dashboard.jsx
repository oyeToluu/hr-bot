import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { ErrorBox, Loading, StatusBadge, formatDate, label } from '../components/ui';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.myDashboard().then(setData).catch((e) => setError(e.message));
  }, []);

  if (error) return <ErrorBox error={error} />;
  if (!data) return <Loading />;

  const { profile, recentLeaveRequests, pendingCount } = data;

  return (
    <>
      <h1>Welcome, {profile.fullName.split(' ')[0]}</h1>

      <div className="stats">
        <div className="stat">
          <span className="stat-value">{profile.leaveBalance}</span>
          <span className="stat-label">Leave days left</span>
        </div>
        <div className="stat">
          <span className="stat-value">{pendingCount}</span>
          <span className="stat-label">Pending requests</span>
        </div>
        <div className="stat">
          <span className="stat-value">{profile.onboardingComplete ? 'Done' : 'In progress'}</span>
          <span className="stat-label">Onboarding</span>
        </div>
      </div>

      {!profile.onboardingComplete && (
        <div className="card notice">
          <p>
            You haven't finished onboarding yet. The onboarding bot can answer your first-week
            questions.
          </p>
          <Link className="button" to="/onboarding">
            Open onboarding bot
          </Link>
        </div>
      )}

      <div className="card">
        <h2>My profile</h2>
        <dl className="details">
          <dt>Email</dt>
          <dd>{profile.email}</dd>
          <dt>Department</dt>
          <dd>{profile.department}</dd>
          <dt>Started</dt>
          <dd>{formatDate(profile.dateStarted)}</dd>
        </dl>
      </div>

      <div className="card">
        <div className="card-head">
          <h2>Recent leave requests</h2>
          <Link to="/leave">Request leave</Link>
        </div>
        {recentLeaveRequests.length === 0 ? (
          <p className="muted">No leave requests yet.</p>
        ) : (
          <ul className="list">
            {recentLeaveRequests.map((r) => (
              <li key={r.id}>
                <div>
                  <strong>{label(r.leaveType)}</strong> · {r.daysRequested} day(s)
                  <div className="muted small">
                    {formatDate(r.startDate)} – {formatDate(r.endDate)}
                  </div>
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
