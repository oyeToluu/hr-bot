import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const staffLinks = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/leave', label: 'Leave' },
  { to: '/onboarding', label: 'Onboarding' },
  { to: '/policy', label: 'Policy Q&A' },
];

const adminLinks = [
  { to: '/admin', label: 'Admin panel' },
  { to: '/admin/leave', label: 'Leave approvals' },
  { to: '/admin/employees', label: 'Employees' },
  { to: '/admin/jobs', label: 'Recruitment' },
];

export default function AppLayout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user.fullName
    .split(' ')
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">HR</span>
          <span className="brand-name">HR Bot</span>
        </div>

        <nav className="nav">
          <p className="nav-heading">Workspace</p>
          {staffLinks.map((link) => (
            <NavLink key={link.to} to={link.to} className="nav-link">
              {link.label}
            </NavLink>
          ))}

          {isAdmin && (
            <>
              <p className="nav-heading">HR admin</p>
              {adminLinks.map((link) => (
                <NavLink key={link.to} to={link.to} end className="nav-link">
                  {link.label}
                </NavLink>
              ))}
            </>
          )}
        </nav>

        <div className="sidebar-user">
          <span className="avatar">{initials}</span>
          <div className="sidebar-user-text">
            <span className="sidebar-user-name">{user.fullName}</span>
            <span className="sidebar-user-role">{isAdmin ? 'HR Admin' : 'Employee'}</span>
          </div>
          <button type="button" className="btn-ghost" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>

      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
