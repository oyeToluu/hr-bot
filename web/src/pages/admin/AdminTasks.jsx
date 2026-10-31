import { useEffect, useState } from 'react';
import { api } from '../../api/client';
import { ErrorBox, Loading } from '../../components/ui';
import { DueChip } from '../../components/DueChip';

// HR admins give tasks to employees and follow up on the tasks HR gave.
export default function AdminTasks() {
  const [employees, setEmployees] = useState(null);
  const [employeeId, setEmployeeId] = useState('');
  const [tasks, setTasks] = useState([]);
  const [title, setTitle] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    api
      .listEmployees()
      .then((list) => {
        const staff = list.filter((e) => e.role === 'EMPLOYEE');
        setEmployees(staff);
        if (staff.length) setEmployeeId(staff[0].id);
      })
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!employeeId) return;
    api.employeeTodos(employeeId).then(setTasks).catch((e) => setError(e.message));
  }, [employeeId]);

  const assign = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setMessage('');
    try {
      await api.assignTodo(employeeId, title.trim(), dueDate || null, notes.trim() || null);
      const who = employees.find((x) => x.id === employeeId);
      setMessage(`Task added to ${who ? who.fullName : 'the employee'}'s list.`);
      setTitle('');
      setDueDate('');
      setNotes('');
      setTasks(await api.employeeTodos(employeeId));
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  if (!employees && !error) return <Loading />;

  const selected = (employees || []).find((x) => x.id === employeeId);
  const openCount = tasks.filter((t) => !t.done).length;

  return (
    <>
      <h1>Assign tasks</h1>
      <p className="muted">
        Give an employee a task. It appears on their to-do list straight away. You only see tasks
        HR has given; employees' personal tasks stay private.
      </p>
      <ErrorBox error={error} />
      {message && <div className="success-box">{message}</div>}

      <form className="card form-grid" onSubmit={assign}>
        <label className="full">
          Employee
          <select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} required>
            {(employees || []).map((e) => (
              <option key={e.id} value={e.id}>
                {e.fullName} ({e.department || 'No department'})
              </option>
            ))}
          </select>
        </label>
        <label className="full">
          Task
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Submit your bank details"
            maxLength={200}
            required
          />
        </label>
        <label>
          <span>
            Due date <span className="muted small">(optional)</span>
          </span>
          <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
        </label>
        <label className="full">
          <span>
            Note for the employee <span className="muted small">(optional)</span>
          </span>
          <input value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={1000} />
        </label>
        <div className="full">
          <button type="submit" disabled={busy || !employeeId || !title.trim()}>
            {busy ? 'Assigning…' : 'Assign task'}
          </button>
        </div>
      </form>

      {selected && (
        <div className="card">
          <div className="card-head">
            <h2>Tasks you've given {selected.fullName}</h2>
            <span className="muted small">
              {openCount} open, {tasks.length - openCount} done
            </span>
          </div>
          {tasks.length === 0 ? (
            <p className="muted">You haven't given {selected.fullName} any tasks yet.</p>
          ) : (
            <ul className="todo-list">
              {tasks.map((t) => (
                <li key={t.id} className={`todo-row ${t.done ? 'is-done' : ''}`}>
                  <span className={`todo-status ${t.done ? 'is-done' : ''}`} aria-hidden="true">
                    {t.done ? '✓' : ''}
                  </span>
                  <div className="todo-body">
                    <span className="todo-title">{t.title}</span>
                    <div className="todo-meta">
                      {!t.done && <DueChip date={t.dueDate} />}
                      <span className="todo-from">From {t.assignedByName || 'HR'}</span>
                      <span className="muted small">{t.done ? 'Done' : 'Not done yet'}</span>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </>
  );
}