import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import CareersLayout from './components/CareersLayout';
import { RequireAdmin, RequireAuth, RequireCandidate } from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Leave from './pages/Leave';
import Onboarding from './pages/Onboarding';
import Policy from './pages/Policy';
import Todos from './pages/Todos';
import AdminTasks from './pages/admin/AdminTasks';
import Calendar from './pages/Calendar';
import Settings from './pages/Settings';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminLeave from './pages/admin/AdminLeave';
import AdminEmployees from './pages/admin/AdminEmployees';
import AdminContent from './pages/admin/AdminContent';
import AdminRecruitment from './pages/admin/AdminRecruitment';
import AdminApplicants from './pages/admin/AdminApplicants';
import Careers from './pages/careers/Careers';
import JobDetail from './pages/careers/JobDetail';
import CandidateLogin from './pages/careers/CandidateLogin';
import CandidateRegister from './pages/careers/CandidateRegister';
import MyApplications from './pages/careers/MyApplications';

export default function App() {
  const { employee, candidate, isAdmin, loading } = useAuth();

  if (loading) return <div className="center-screen">Loading…</div>;

  const home = isAdmin ? '/admin' : '/dashboard';
  // Where "/" and unknown URLs go, for whoever is logged in
  const landing = employee ? home : candidate ? '/careers/applications' : '/login';

   return (
    <Routes>
      <Route path="/login" element={employee ? <Navigate to={home} replace /> : <Login />} />
      <Route path="/register" element={employee ? <Navigate to={home} replace /> : <Register />} />

      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/leave" element={<Leave />} />
        <Route path="/onboarding" element={<Onboarding />} />
        <Route path="/policy" element={<Policy />} />
        <Route path="/todos" element={<Todos />} />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="/settings" element={<Settings />} />

        <Route path="/admin" element={<RequireAdmin><AdminDashboard /></RequireAdmin>} />
        <Route path="/admin/leave" element={<RequireAdmin><AdminLeave /></RequireAdmin>} />
        <Route path="/admin/employees" element={<RequireAdmin><AdminEmployees /></RequireAdmin>} />
        <Route path="/admin/content" element={<RequireAdmin><AdminContent /></RequireAdmin>} />
        <Route path="/admin/tasks" element={<RequireAdmin><AdminTasks /></RequireAdmin>} />
        <Route path="/admin/recruitment" element={<RequireAdmin><AdminRecruitment /></RequireAdmin>} />
        <Route path="/admin/recruitment/:jobId" element={<RequireAdmin><AdminApplicants /></RequireAdmin>} />
      </Route>

      {/* Careers site: public job board + candidate accounts */}
      <Route element={<CareersLayout />}>
        <Route path="/careers" element={<Careers />} />
        <Route path="/careers/jobs/:id" element={<JobDetail />} />
        <Route path="/careers/login" element={<CandidateLogin />} />
        <Route path="/careers/register" element={<CandidateRegister />} />
        <Route path="/careers/applications" element={<RequireCandidate><MyApplications /></RequireCandidate>} />
      </Route>

      <Route path="*" element={<Navigate to={landing} replace />} />
    </Routes>
  );
}