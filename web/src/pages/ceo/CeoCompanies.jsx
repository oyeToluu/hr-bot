import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { ErrorBox, Loading, formatDate } from '../../components/ui';

// The HR Bots owner: every company using the app, as a summary only.
// (The monthly / yearly payment badges come with the payments update.)
export default function CeoCompanies() {
  const [data, setData] = useState(null);
  const [search, setSearch] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    api.ownerCompanies().then(setData).catch((e) => setError(e.message));
  }, []);

  if (error) return <ErrorBox error={error} />;
  if (!data) return <Loading />;

  const q = search.toLowerCase();
  const shown = data.list.filter(
    (c) =>
      c.name.toLowerCase().includes(q) ||
      (c.industry || '').toLowerCase().includes(q) ||
      c.hrAdmins.some((h) => h.email.toLowerCase().includes(q) || h.name.toLowerCase().includes(q)),
  );
  const stats = [
    { value: data.companies, label: 'Companies' },
    { value: data.totalStaff, label: 'Staff accounts' },
  ];

  return (
    <>
      <h1>All companies</h1>
      <p className="muted">
        Every business using HR Bots. You see a summary only; each company's staff records stay
        private to that company.
      </p>
      <div className="stats">
        {stats.map((s) => (
          <div key={s.label} className="stat">
            <span className="stat-value">{s.value}</span>
            <span className="stat-label">{s.label}</span>
          </div>
        ))}
      </div>

      <input
        className="search"
        placeholder="Search by company, industry or HR contact"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <div className="card table-wrap">
        <table>
          <thead>
            <tr>
              <th>Company</th>
              <th>HR contact</th>
              <th>Staff</th>
              <th>Open jobs</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            {shown.map((c) => (
              <tr key={c.id}>
                <td>
                  {c.name}
                  {c.industry && <div className="muted small">{c.industry}</div>}
                </td>
                <td>
                  {c.hrAdmins.length === 0 && <span className="muted small">None</span>}
                  {c.hrAdmins.map((h) => (
                    <div key={h.email}>
                      {h.name}
                      <div className="muted small">{h.email}</div>
                    </div>
                  ))}
                </td>
                <td>{c.staffCount}</td>
                <td>{c.openJobs}</td>
                <td>{formatDate(c.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {shown.length === 0 && <p className="muted">No companies match.</p>}
      </div>
    </>
  );
}