import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../../api/client';
import { ErrorAlert, Spinner } from '../../components/ui';

// GET /api/dashboard/admin -> { totalEmployees, onboardingIncomplete,
//   pendingLeaveRequests, approvedLeaveRequests, rejectedLeaveRequests }
export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    dashboardApi.admin().then(setStats).catch((err) => setError(err.message));
  }, []);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Admin panel</h1>
          <p className="muted">Company-wide HR overview.</p>
        </div>
      </header>

      <ErrorAlert message={error} />

      {!stats && !error ? (
        <Spinner />
      ) : stats && (
        <>
          <section className="stat-grid">
            <div className="stat">
              <span className="stat-label">Employees</span>
              <span className="stat-value">{stats.totalEmployees}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Onboarding incomplete</span>
              <span className="stat-value pending">{stats.onboardingIncomplete}</span>
            </div>
            <div className="stat stat-highlight">
              <span className="stat-label">Leave awaiting review</span>
              <span className="stat-value">{stats.pendingLeaveRequests}</span>
              <Link to="/admin/leave" className="small">Review now →</Link>
            </div>
          </section>

          <section className="stat-grid two">
            <div className="stat">
              <span className="stat-label">Approved leave requests</span>
              <span className="stat-value ok">{stats.approvedLeaveRequests}</span>
            </div>
            <div className="stat">
              <span className="stat-label">Rejected leave requests</span>
              <span className="stat-value danger">{stats.rejectedLeaveRequests}</span>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
