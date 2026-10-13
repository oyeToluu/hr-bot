import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export function ForbiddenPage() {
  const { isAdmin } = useAuth();
  return (
    <div className="center-screen">
      <div className="status-box">
        <span className="status-code">403</span>
        <h1>That page is for HR admins</h1>
        <p className="muted">Your account doesn't have access to it.</p>
        <Link className="btn-primary" to={isAdmin ? '/admin' : '/dashboard'}>Back to dashboard</Link>
      </div>
    </div>
  );
}

export function NotFoundPage() {
  return (
    <div className="center-screen">
      <div className="status-box">
        <span className="status-code">404</span>
        <h1>Page not found</h1>
        <Link className="btn-primary" to="/">Go home</Link>
      </div>
    </div>
  );
}
