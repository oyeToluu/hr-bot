import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { ErrorBox, Loading, StatusBadge, formatDate, label } from '../components/ui';
import AiHrHero from '../components/AiHrHero';
import { DueChip } from '../components/DueChip';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [todos, setTodos] = useState([]);

  useEffect(() => {
    api.myDashboard().then(setData).catch((e) => setError(e.message));
    // The task card is a bonus: if it fails to load, the rest of the dashboard still shows
    api.myTodos().then(setTodos).catch(() => setTodos([]));
  }, []);

  // Tick a task off right from the dashboard
  const completeTodo = async (todo) => {
    setTodos((list) => list.map((t) => (t.id === todo.id ? { ...t, done: true } : t)));
    try {
      await api.updateTodo(todo.id, { done: true });
    } catch {
      setTodos((list) => list.map((t) => (t.id === todo.id ? { ...t, done: false } : t)));
    }
  };

  if (error) return <ErrorBox error={error} />;
  if (!data) return <Loading />;

  const { profile, recentLeaveRequests, pendingCount } = data;
  const openTodos = todos.filter((t) => !t.done);

  return (
    <>
      <section className="welcome-banner">
        <div className="welcome-text">
          <h1 className="display">Welcome back, {profile.fullName.split(' ')[0]}</h1>
          <p>
            You have <strong>{profile.leaveBalance} leave days</strong> left
            {pendingCount > 0
              ? ` and ${pendingCount} request${pendingCount === 1 ? '' : 's'} waiting for HR.`
              : ' and nothing waiting for HR.'}
          </p>
          <Link className="button" to="/policy">
            Ask the HR assistant
          </Link>
        </div>
        <AiHrHero size="sm" />
      </section>

      <div className="stats">
        <div className="stat">
          <span className="stat-value">{profile.leaveBalance}</span>
          <span className="stat-label">Leave days left</span>
        </div>
        <div className="stat">
          <span className="stat-value">{pendingCount}</span>
          <span className="stat-label">Pending requests</span>
        </div>
        <div className="stat">
          <span className="stat-value">{profile.onboardingComplete ? 'Done' : 'In progress'}</span>
          <span className="stat-label">Onboarding</span>
        </div>
      </div>

      {!profile.onboardingComplete && (
        <div className="card notice">
          <p>
            You haven't finished onboarding yet. The onboarding bot can answer your first-week
            questions.
          </p>
          <Link className="button" to="/onboarding">
            Open onboarding bot
          </Link>
        </div>
      )}

      <div className="card">
        <div className="card-head">
          <h2>My tasks</h2>
          <Link to="/todos">{openTodos.length > 3 ? `See all ${openTodos.length}` : 'Open my tasks'}</Link>
        </div>
        {openTodos.length === 0 ? (
          <p className="muted">You're all caught up.</p>
        ) : (
          <ul className="todo-list">
            {openTodos.slice(0, 3).map((t) => (
              <li key={t.id} className="todo-row">
                <input
                  type="checkbox"
                  className="todo-check"
                  checked={false}
                  onChange={() => completeTodo(t)}
                  aria-label={`Mark "${t.title}" as done`}
                />
                <div className="todo-body">
                  <span className="todo-title">{t.title}</span>
                  <div className="todo-meta">
                    <DueChip date={t.dueDate} />
                    {t.assignedByHr && <span className="todo-from">From {t.assignedByName || 'HR'}</span>}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="card">
        <h2>My profile</h2>
        <dl className="details">
          <dt>Email</dt>
          <dd>{profile.email}</dd>
          <dt>Department</dt>
          <dd>{profile.department}</dd>
          <dt>Started</dt>
          <dd>{formatDate(profile.dateStarted)}</dd>
        </dl>
      </div>

      <div className="card">
        <div className="card-head">
          <h2>Recent leave requests</h2>
          <Link to="/leave">Request leave</Link>
        </div>
        {recentLeaveRequests.length === 0 ? (
          <p className="muted">No leave requests yet.</p>
        ) : (
          <ul className="list">
            {recentLeaveRequests.map((r) => (
              <li key={r.id}>
                <div>
                  <strong>{label(r.leaveType)}</strong> · {r.daysRequested} day(s)
                  <div className="muted small">
                    {formatDate(r.startDate)} – {formatDate(r.endDate)}
                  </div>
                </div>
                <StatusBadge status={r.status} />
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}