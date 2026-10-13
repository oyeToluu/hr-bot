import { useEffect, useMemo, useState } from 'react';
import { employeeApi } from '../../api/client';
import { ErrorAlert, Spinner, formatDate } from '../../components/ui';

// GET /api/employees (HR admin only)
export default function EmployeesPage() {
  const [employees, setEmployees] = useState(null);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    employeeApi
      .list()
      .then((list) => setEmployees([...list].sort((a, b) => a.fullName.localeCompare(b.fullName))))
      .catch((err) => {
        setError(err.message);
        setEmployees([]);
      });
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!employees || !q) return employees;
    return employees.filter((e) =>
      [e.fullName, e.email, e.department].some((v) => v?.toLowerCase().includes(q))
    );
  }, [employees, search]);

  return (
    <div className="page">
      <header className="page-header row">
        <div>
          <h1>Employees</h1>
          <p className="muted">{employees ? `${employees.length} people` : 'Staff records'}</p>
        </div>
        <input
          className="search"
          type="search"
          placeholder="Search name, email, department"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search employees"
        />
      </header>

      <ErrorAlert message={error} />

      <section className="panel table-wrap">
        {filtered === null ? (
          <Spinner />
        ) : filtered.length === 0 ? (
          <p className="muted">No employees found.</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Department</th>
                <th>Role</th>
                <th>Started</th>
                <th className="num">Leave left</th>
                <th>Onboarding</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((e) => (
                <tr key={e.id}>
                  <td>
                    <strong>{e.fullName}</strong>
                    <span className="muted small block">{e.email}</span>
                  </td>
                  <td>{e.department || '—'}</td>
                  <td>{e.role === 'HR_ADMIN' ? 'HR Admin' : 'Employee'}</td>
                  <td>{formatDate(e.dateStarted)}</td>
                  <td className="num">{e.leaveBalance}</td>
                  <td>
                    <span className={`badge ${e.onboardingComplete ? 'badge-approved' : 'badge-pending'}`}>
                      {e.onboardingComplete ? 'Complete' : 'In progress'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
