import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { ErrorBox, Loading, formatDate, label } from '../../components/ui';

export default function AdminEmployees() {
  const [employees, setEmployees] = useState(null);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    api.listEmployees().then(setEmployees).catch((e) => setError(e.message));
  }, []);

  if (error) return <ErrorBox error={error} />;
  if (!employees) return <Loading />;

  const q = search.toLowerCase();
  const shown = employees.filter(
    (e) =>
      e.fullName.toLowerCase().includes(q) ||
      e.email.toLowerCase().includes(q) ||
      (e.department || '').toLowerCase().includes(q)
  );

  return (
    <>
      <h1>Employees</h1>
      <input
        className="search"
        placeholder="Search by name, email or department"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      <div className="card table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Department</th>
              <th>Role</th>
              <th>Started</th>
              <th>Leave left</th>
              <th>Onboarding</th>
            </tr>
          </thead>
          <tbody>
            {shown.map((e) => (
              <tr key={e.id}>
                <td>
                  {e.fullName}
                  <div className="muted small">{e.email}</div>
                </td>
                <td>{e.department}</td>
                <td>{label(e.role)}</td>
                <td>{formatDate(e.dateStarted)}</td>
                <td>{e.leaveBalance}</td>
                <td>
                  <span className={`badge ${e.onboardingComplete ? 'badge-approved' : 'badge-pending'}`}>
                    {e.onboardingComplete ? 'Complete' : 'In progress'}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {shown.length === 0 && <p className="muted">No employees match.</p>}
      </div>
    </>
  );
}
