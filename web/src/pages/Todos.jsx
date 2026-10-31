import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { ErrorBox, Loading } from '../components/ui';
import { DueChip } from '../components/DueChip';

export default function Todos() {
  const [items, setItems] = useState(null);
  const [title, setTitle] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const load = () =>
    api.myTodos().then(setItems).catch((e) => setError(e.message));

  useEffect(() => {
    load();
  }, []);

  const add = async (e) => {
    e.preventDefault();
    if (!title.trim()) return;
    setBusy(true);
    setError('');
    try {
      await api.createTodo(title.trim(), dueDate || null);
      setTitle('');
      setDueDate('');
      await load();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (item) => {
    setError('');
    // Tick it straight away so the list feels instant, then save
    setItems((list) => list.map((t) => (t.id === item.id ? { ...t, done: !t.done } : t)));
    try {
      await api.updateTodo(item.id, { done: !item.done });
      await load();
    } catch (err) {
      setError(err.message);
      await load();
    }
  };

  const remove = async (item) => {
    setError('');
    try {
      await api.deleteTodo(item.id);
      setItems((list) => list.filter((t) => t.id !== item.id));
    } catch (err) {
      setError(err.message);
    }
  };

  if (!items && !error) return <Loading />;

  const open = (items || []).filter((t) => !t.done);
  const done = (items || []).filter((t) => t.done);

  return (
    <>
      <h1>My tasks</h1>
      <p className="muted">Your own to-dos, plus anything HR has asked you to do.</p>
      <ErrorBox error={error} />

      <form className="card todo-add" onSubmit={add}>
        <label className="todo-add-title">
          New task
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Book my eye test"
            maxLength={200}
          />
        </label>
        <label className="todo-add-date">
          <span>
            Due date <span className="muted small">(optional)</span>
          </span>
          <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
        </label>
        <button type="submit" disabled={busy || !title.trim()}>
          {busy ? 'Adding…' : 'Add task'}
        </button>
      </form>

      <div className="card">
        <h2>To do</h2>
        {open.length === 0 ? (
          <p className="muted">Nothing to do right now. Add a task above when something comes up.</p>
        ) : (
          <ul className="todo-list">
            {open.map((t) => (
              <TodoRow key={t.id} item={t} onToggle={toggle} onDelete={remove} />
            ))}
          </ul>
        )}
      </div>

      {done.length > 0 && (
        <details className="card todo-done">
          <summary>
            <h2>Completed ({done.length})</h2>
          </summary>
          <ul className="todo-list">
            {done.map((t) => (
              <TodoRow key={t.id} item={t} onToggle={toggle} onDelete={remove} />
            ))}
          </ul>
        </details>
      )}
    </>
  );
}

function TodoRow({ item, onToggle, onDelete }) {
  return (
    <li className={`todo-row ${item.done ? 'is-done' : ''}`}>
      <input
        type="checkbox"
        className="todo-check"
        checked={item.done}
        onChange={() => onToggle(item)}
        aria-label={item.done ? `Mark "${item.title}" as not done` : `Mark "${item.title}" as done`}
      />
      <div className="todo-body">
        <span className="todo-title">{item.title}</span>
        <div className="todo-meta">
          {!item.done && <DueChip date={item.dueDate} />}
          {item.assignedByHr && <span className="todo-from">From {item.assignedByName || 'HR'}</span>}
        </div>
        {item.notes && <p className="muted small todo-notes">{item.notes}</p>}
      </div>
      {!item.assignedByHr && (
        <button type="button" className="link-btn todo-delete" onClick={() => onDelete(item)}>
          Delete
        </button>
      )}
    </li>
  );
}