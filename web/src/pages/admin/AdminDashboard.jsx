import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api/client';
import { ErrorBox, Loading } from '../../components/ui';

export default function AdminDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.adminDashboard().then(setData).catch((e) => setError(e.message));
  }, []);

  if (error) return <ErrorBox error={error} />;
  if (!data) return <Loading />;

  const stats = [
    { value: data.totalEmployees, label: 'Employees', to: '/admin/employees' },
    { value: data.onboardingIncomplete, label: 'Still onboarding', to: '/admin/employees' },
    { value: data.pendingLeaveRequests, label: 'Pending leave', to: '/admin/leave' },
    { value: data.approvedLeaveRequests, label: 'Approved leave', to: '/admin/leave' },
    { value: data.rejectedLeaveRequests, label: 'Rejected leave', to: '/admin/leave' },
  ];

  return (
    <>
      <h1>HR admin overview</h1>
      <div className="stats">
        {stats.map((s) => (
          <Link key={s.label} to={s.to} className="stat stat-link">
            <span className="stat-value">{s.value}</span>
            <span className="stat-label">{s.label}</span>
          </Link>
        ))}
      </div>

      {data.pendingLeaveRequests > 0 && (
        <div className="card notice">
          <p>
            {data.pendingLeaveRequests} leave request(s) are waiting for a decision.
          </p>
          <Link className="button" to="/admin/leave">
            Review requests
          </Link>
        </div>
      )}
    </>
  );
}
