import { NavLink } from "react-router-dom";

export function DashboardSidebar({ items, onSignOut }) {
  return (
    <aside className="dashboard-sidebar">
      <nav className="dashboard-nav" aria-label="Main navigation">
        {items.map((item) => (
          <NavLink
            className={({ isActive }) =>
              `dashboard-nav-item${isActive ? " is-active" : ""}`
            }
            to={item.to}
            key={item.to}
            end={item.end}
          >
            {item.icon === "home" && (
              <svg
                className="dashboard-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
                focusable="false"
              >
                <path d="m3 10 9-7 9 7" />
                <path d="M5 9v11h14V9M9 20v-6h6v6" />
              </svg>
            )}
            {item.icon === "automation" && (
              <svg
                className="dashboard-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.7"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
                focusable="false"
              >
                <path d="M4 7h10" />
                <path d="M4 17h6" />
                <circle cx="17" cy="7" r="3" />
                <circle cx="13" cy="17" r="3" />
                <path d="M20 7h1M3 17h1" />
              </svg>
            )}
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
      <button
        className="dashboard-signout"
        type="button"
        onClick={onSignOut}
      >
        <svg
          className="dashboard-icon"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.7"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
          focusable="false"
        >
          <path d="M10 17l5-5-5-5m5 5H3" />
          <path d="M12 3h7a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-7" />
        </svg>
        <span>Sign out</span>
      </button>
    </aside>
  );
}
