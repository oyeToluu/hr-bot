// Temporary page for features we haven't wired to the API yet.
export default function PlaceholderPage({ title, description, endpoint }) {
  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{title}</h1>
          <p className="muted">{description}</p>
        </div>
      </header>
      <div className="empty">
        <p>This screen is next on the build list.</p>
        {endpoint && <code>{endpoint}</code>}
      </div>
    </div>
  );
}
