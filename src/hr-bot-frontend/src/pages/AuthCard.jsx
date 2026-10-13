// Shared two-column frame for the login and register pages.
export default function AuthCard({ title, subtitle, children }) {
  return (
    <div className="auth-page">
      <section className="auth-panel">
        <div className="brand brand-light">
          <span className="brand-mark">HR</span>
          <span className="brand-name">HR Bot</span>
        </div>
        <div className="auth-pitch">
          <h2>Your HR desk, open around the clock.</h2>
          <ul>
            <li>Request leave and track approvals</li>
            <li>Get onboarding answers instantly</li>
            <li>Ask questions about company policy</li>
          </ul>
        </div>
        <p className="auth-foot">AI for Business &amp; Productivity</p>
      </section>

      <section className="auth-form-side">
        <div className="auth-card">
          <h1>{title}</h1>
          <p className="muted">{subtitle}</p>
          {children}
        </div>
      </section>
    </div>
  );
}
