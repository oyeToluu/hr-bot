import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { SettingsProvider } from '../context/SettingsContext';
import Notifier from './Notifier';

const EMPLOYEE_LINKS = [
  { to: '/dashboard', label: 'My dashboard' },
  { to: '/todos', label: 'My tasks' },
  { to: '/calendar', label: 'Calendar' },
  { to: '/leave', label: 'Leave requests' },
  { to: '/onboarding', label: 'Onboarding bot' },
  { to: '/policy', label: 'HR assistant' },
  { to: '/settings', label: 'Alerts & settings' },
];

const ADMIN_LINKS = [
  { to: '/admin', label: 'Admin overview', end: true },
  { to: '/admin/leave', label: 'Leave approvals' },
  { to: '/admin/employees', label: 'Employees' },
  { to: '/admin/tasks', label: 'Assign tasks' },
  { to: '/admin/content', label: 'FAQs & policies' },
  { to: '/admin/recruitment', label: 'Recruitment' },
];

export default function Layout() {
  const { employee, isAdmin, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const close = () => setMenuOpen(false);

  return (
    <SettingsProvider>
      <div className="shell">
        <header className="topbar">
          <button className="menu-btn" onClick={() => setMenuOpen(!menuOpen)} aria-label="Menu">
            ☰
          </button>
          <span className="brand">
            <span className="brand-dot" aria-hidden="true" />
            HR Bots
          </span>
          <span className="topbar-user">
            {employee.fullName}
            <span className="role-tag">{isAdmin ? 'HR Admin' : 'Employee'}</span>
          </span>
        </header>

        <nav className={`sidebar ${menuOpen ? 'open' : ''}`}>
          {isAdmin && (
            <>
              <p className="nav-heading">HR admin</p>
              {ADMIN_LINKS.map((l) => (
                <NavLink key={l.to} to={l.to} end={l.end} onClick={close}>
                  {l.label}
                </NavLink>
              ))}
              <p className="nav-heading">Employee tools</p>
            </>
          )}
          {EMPLOYEE_LINKS.map((l) => (
            <NavLink key={l.to} to={l.to} onClick={close}>
              {l.label}
            </NavLink>
          ))}
          <button className="link-btn logout" onClick={logout}>
            Log out
          </button>
        </nav>

        <main className="content">
          <Outlet />
        </main>
        <Notifier />
      </div>
    </SettingsProvider>
  );
}