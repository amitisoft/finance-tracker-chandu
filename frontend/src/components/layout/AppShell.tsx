import type { PropsWithChildren } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";

const navItems = [
  { to: "/", label: "Dashboard" },
  { to: "/transactions", label: "Transactions" },
  { to: "/budgets", label: "Budgets" },
  { to: "/goals", label: "Goals" },
  { to: "/reports", label: "Reports" },
  { to: "/recurring", label: "Recurring" },
  { to: "/accounts", label: "Accounts" }
];

export function AppShell({ children }: PropsWithChildren) {
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">PF</span>
          <div>
            <strong>Finance Atlas</strong>
            <p>Hackathon Edition</p>
          </div>
        </div>
        <nav className="nav">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.to === "/"} className="nav-link">
              {item.label}
            </NavLink>
          ))}
        </nav>
        <button className="ghost-button" onClick={logout} type="button">
          Sign out
        </button>
      </aside>
      <main className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Personal Finance Tracker</p>
            <h1>{user?.displayName ? `${user.displayName}'s workspace` : "Command center"}</h1>
          </div>
          <div className="profile-pill">
            <strong>{user?.displayName ?? "Guest"}</strong>
            <span>{user?.email ?? "Local mode"}</span>
          </div>
        </header>
        {children ?? <Outlet />}
      </main>
    </div>
  );
}
