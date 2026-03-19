import type { PropsWithChildren } from "react";
import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import { useToastStore } from "../../store/toastStore";

const navItems = [
  { to: "/", label: "Dashboard", accent: "01" },
  { to: "/transactions", label: "Transactions", accent: "02" },
  { to: "/budgets", label: "Budgets", accent: "03" },
  { to: "/goals", label: "Goals", accent: "04" },
  { to: "/reports", label: "Reports", accent: "05" },
  { to: "/recurring", label: "Recurring", accent: "06" },
  { to: "/accounts", label: "Accounts", accent: "07" }
];

export function AppShell({ children }: PropsWithChildren) {
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);
  const updateUser = useAuthStore((state) => state.updateUser);
  const pushToast = useToastStore((state) => state.pushToast);
  const [menuOpen, setMenuOpen] = useState(false);
  const [editingProfile, setEditingProfile] = useState(false);
  const [displayName, setDisplayName] = useState(user?.displayName ?? "");

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-header">
          <div className="brand">
            <span className="brand-mark">FA</span>
            <div>
              <strong>Finance Atlas</strong>
              <p>Compact wealth desk</p>
            </div>
          </div>
          <div className="sidebar-user">
            <strong>{user?.displayName ?? "Guest mode"}</strong>
            <span>{user?.email ?? "Track every rupee with clarity"}</span>
          </div>
        </div>
        <nav className="nav">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.to === "/"} className="nav-link">
              <span className="nav-link-mark">{item.accent}</span>
              <span className="nav-link-copy">
                <strong>{item.label}</strong>
              </span>
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="content">
        <header className="topbar">
          <div>
            <p className="eyebrow">Personal Finance Command Center</p>
            <h1>{user?.displayName ? `${user.displayName}'s desk` : "Command center"}</h1>
          </div>
          <div className="profile-menu">
            <button className="profile-pill profile-pill-button" onClick={() => setMenuOpen((current) => !current)} type="button">
              <strong>{user?.displayName ?? "Guest"}</strong>
              <span>{user?.email ?? "Local mode"}</span>
            </button>
            {menuOpen ? (
              <div className="profile-dropdown">
                <div className="profile-dropdown-section">
                  <p className="eyebrow">Profile</p>
                  {editingProfile ? (
                    <form
                      className="profile-edit-form"
                      onSubmit={(event) => {
                        event.preventDefault();
                        const nextName = displayName.trim();
                        if (nextName.length < 2) {
                          pushToast("Display name must be at least 2 characters.", "info");
                          return;
                        }
                        updateUser({ displayName: nextName });
                        setEditingProfile(false);
                        setMenuOpen(false);
                        pushToast("Profile updated locally in this session.", "success");
                      }}
                    >
                      <input maxLength={120} onChange={(event) => setDisplayName(event.target.value)} value={displayName} />
                      <div className="row-actions">
                        <button className="inline-link-button" type="submit">
                          Save
                        </button>
                        <button className="inline-link-button danger" onClick={() => setEditingProfile(false)} type="button">
                          Cancel
                        </button>
                      </div>
                    </form>
                  ) : (
                    <>
                      <strong>{user?.displayName ?? "Guest"}</strong>
                      <span>{user?.email ?? "Local mode"}</span>
                      <button className="inline-link-button" onClick={() => setEditingProfile(true)} type="button">
                        Edit profile
                      </button>
                    </>
                  )}
                </div>
                <button className="inline-link-button danger" onClick={logout} type="button">
                  Logout
                </button>
              </div>
            ) : null}
          </div>
        </header>
        {children ?? <Outlet />}
      </main>
    </div>
  );
}
