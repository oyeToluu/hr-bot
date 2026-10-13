import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import LeavePage from './pages/LeavePage';
import OnboardingPage from './pages/OnboardingPage';
import PolicyPage from './pages/PolicyPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminLeavePage from './pages/admin/AdminLeavePage';
import EmployeesPage from './pages/admin/EmployeesPage';
import PlaceholderPage from './pages/PlaceholderPage';
import { ForbiddenPage, NotFoundPage } from './pages/StatusPages';

const homeFor = (user) => (user.role === 'HR_ADMIN' ? '/admin' : '/dashboard');

// Logged-in users who open /login or /register go straight to their home page.
function GuestOnly({ children }) {
  const { user, isLoading } = useAuth();
  if (isLoading) return null;
  if (user) return <Navigate to={homeFor(user)} replace />;
  return children;
}

function HomeRedirect() {
  const { user, isLoading } = useAuth();
  if (isLoading) return null;
  return <Navigate to={user ? homeFor(user) : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />
      <Route path="/forbidden" element={<ForbiddenPage />} />

      {/* Staff pages: EMPLOYEE and HR_ADMIN (matches /api/** rule in SecurityConfig) */}
      <Route element={<ProtectedRoute roles={['EMPLOYEE', 'HR_ADMIN']} />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/leave" element={<LeavePage />} />
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/policy" element={<PolicyPage />} />
        </Route>
      </Route>

      {/* HR admin pages */}
      <Route element={<ProtectedRoute roles={['HR_ADMIN']} />}>
        <Route element={<AppLayout />}>
          <Route path="/admin" element={<AdminDashboardPage />} />
          <Route path="/admin/leave" element={<AdminLeavePage />} />
          <Route path="/admin/employees" element={<EmployeesPage />} />
          <Route
            path="/admin/jobs"
            element={<PlaceholderPage title="Recruitment" description="Job postings and CV screening." endpoint="/api/jobs/manage" />}
          />
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
