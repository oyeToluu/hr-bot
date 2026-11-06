import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { SettingsProvider } from '../context/SettingsContext';
import Notifier, { sendAlert } from './Notifier';

const EMPLOYEE_LINKS = [
  { to: '/dashboard', label: 'My dashboard' },
  { to: '/todos', label: 'My tasks' },
  { to: '/calendar', label: 'Calendar' },
  { to: '/leave', label: 'Leave requests' },
  { to: '/onboarding', label: 'Onboarding bot' },
  { to: '/policy', label: 'HR assistant' },
  { to: '/help', label: 'My HR requests', badge: 'mine' },
  { to: '/settings', label: 'Alerts & settings' },
];

const ADMIN_LINKS = [
  { to: '/admin', label: 'Admin overview', end: true },
  { to: '/admin/leave', label: 'Leave approvals' },
  { to: '/admin/employees', label: 'Employees' },
  { to: '/admin/company', label: 'Company & invites' },
  { to: '/admin/support', label: 'Help requests', badge: 'inbox' },
  { to: '/admin/tasks', label: 'Assign tasks' },
  { to: '/admin/content', label: 'FAQs & policies' },
  { to: '/admin/recruitment', label: 'Recruitment' },
];

export default function Layout() {
  const { employee, isAdmin, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [company, setCompany] = useState(null);
  const [owner, setOwner] = useState(false);
  const [counts, setCounts] = useState({ mine: 0, inbox: 0 });
  const lastUnread = useRef(null);
  const close = () => setMenuOpen(false);

  // The company this person works for, shown next to the logo; and whether they own HR Bots
  useEffect(() => {
    api.myCompany().then(setCompany).catch(() => {});
    api.ownerCheck().then((r) => setOwner(r.owner)).catch(() => {});
  }, []);

  // Help requests: badges in the menu, and an alert when something new arrives
  useEffect(() => {
    const check = async () => {
      try {
        const mine = await api.mySupportCounts();
        const inbox = isAdmin ? await api.supportInboxCounts() : { unread: 0 };
        setCounts({ mine: mine.unread, inbox: inbox.unread });
        const prev = lastUnread.current;
        if (prev) {
          if (mine.unread > prev.mine) sendAlert('HR replied', 'HR replied to your request. Open My HR requests to read it.');
          if (inbox.unread > prev.inbox) sendAlert('New help request', 'An employee asked for a person in HR.');
        }
        lastUnread.current = { mine: mine.unread, inbox: inbox.unread };
      } catch {
        // offline for a moment: try again next time
      }
    };
    check();
    const timer = setInterval(check, 60 * 1000);
    return () => clearInterval(timer);
  }, [isAdmin]);

  const badge = (key) => (key && counts[key] > 0 ? <span className="nav-badge">{counts[key]}</span> : null);

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
            {company && <span className="brand-company">{company.name}</span>}
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
                  {badge(l.badge)}
                </NavLink>
              ))}
              <p className="nav-heading">Employee tools</p>
            </>
          )}
          {EMPLOYEE_LINKS.map((l) => (
            <NavLink key={l.to} to={l.to} onClick={close}>
              {l.label}
              {badge(l.badge)}
            </NavLink>
          ))}
          {owner && (
            <>
              <p className="nav-heading">HR Bots owner</p>
              <NavLink to="/ceo" onClick={close}>
                All companies
              </NavLink>
            </>
          )}
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