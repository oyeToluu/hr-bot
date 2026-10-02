import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// Any logged-in employee
export function RequireAuth({ children }) {
  const { employee } = useAuth();
  const location = useLocation();
  if (!employee) return <Navigate to="/login" replace state={{ from: location }} />;
  return children;
}

// HR admins only. The backend enforces this too (SecurityConfig) -
// this just keeps employees from seeing pages they can't use.
export function RequireAdmin({ children }) {
  const { isAdmin } = useAuth();
  if (!isAdmin) return <Navigate to="/dashboard" replace />;
  return children;
}

// Logged-in job applicants only
export function RequireCandidate({ children }) {
  const { candidate } = useAuth();
  const location = useLocation();
  if (!candidate) return <Navigate to="/careers/login" replace state={{ from: location }} />;
  return children;
}
