import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Public-facing shell for the careers site (job board, applying).
export default function CareersLayout() {
  const { candidate, employee, logout } = useAuth();

  return (
    <div className="careers-shell">
      <header className="careers-top">
        <Link to="/careers" className="brand careers-brand">
          HR Bots <span className="muted">Careers</span>
        </Link>
        <nav className="careers-nav">
          <NavLink to="/careers" end>
            Open jobs
          </NavLink>
          {candidate && <NavLink to="/careers/applications">My applications</NavLink>}
          {candidate ? (
            <>
              <span className="muted small">{candidate.fullName}</span>
              <button className="link-btn" onClick={logout}>
                Log out
              </button>
            </>
          ) : employee ? (
            <Link to="/">Back to staff portal</Link>
          ) : (
            <>
              <NavLink to="/careers/login">Log in</NavLink>
              <Link className="button" to="/careers/register">
                Create account
              </Link>
            </>
          )}
        </nav>
      </header>
      <main className="careers-content">
        <Outlet />
      </main>
    </div>
  );
}
