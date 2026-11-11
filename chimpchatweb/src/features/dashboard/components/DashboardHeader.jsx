import { Link } from "react-router-dom";

export function DashboardHeader({ user }) {
  const email = user.email ?? "";
  const profileInitial = email.trim().charAt(0).toUpperCase();

  return (
    <header className="dashboard-topbar">
      <Link
        className="dashboard-brand"
        to="/dashboard"
        aria-label="ChimpChat home"
      >
        <span className="dashboard-brand-mark">
          <img src="/logo-transparent.png" alt="" />
        </span>
        <span>chimpchat</span>
      </Link>
      <div className="dashboard-profile" aria-label="Signed-in profile">
        <span className="dashboard-profile-email">{email}</span>
        <span className="dashboard-avatar" aria-hidden="true">
          {profileInitial}
        </span>
      </div>
    </header>
  );
}
