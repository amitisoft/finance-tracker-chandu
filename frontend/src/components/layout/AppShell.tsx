import type { PropsWithChildren } from "react";
import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { VoiceAssistant } from "../assistant/VoiceAssistant";
import { useAuthStore } from "../../store/authStore";
import { useToastStore } from "../../store/toastStore";

const navItems = [
  { to: "/", label: "Dashboard" },
  { to: "/insights", label: "Insights" },
  { to: "/rules", label: "Rules" },
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
  const updateUser = useAuthStore((state) => state.updateUser);
  const pushToast = useToastStore((state) => state.pushToast);
  const [menuOpen, setMenuOpen] = useState(false);
  const [editingProfile, setEditingProfile] = useState(false);
  const [displayName, setDisplayName] = useState(user?.displayName ?? "");

  return (
    <div className="app-shell">
      <main className="content">
        <header className="topbar">
          <div className="topbar-intro">
            <div className="topbar-row">
              <div className="brand brand-inline">
                <span className="brand-mark">FA</span>
                <div className="topbar-copy">
                  <p className="eyebrow">Personal Finance Command Center</p>
                  <h1>{user?.displayName ? `${user.displayName}'s desk` : "Command center"}</h1>
                  <p className="topbar-subtitle">Monitor cash flow, act on risks early, and keep every core view one click away.</p>
                </div>
              </div>
            </div>
          </div>
        </header>

        <nav className="topbar-nav-shell" aria-label="Quick navigation">
          <div className="topbar-nav-row">
            <div className="topbar-nav">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === "/"}
                  className={({ isActive }) => (isActive ? "topbar-chip active" : "topbar-chip")}
                  onClick={() => setMenuOpen(false)}
                >
                  {item.label}
                </NavLink>
              ))}
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
          </div>
        </nav>

        {children ?? <Outlet />}
        <VoiceAssistant />
      </main>
    </div>
  );
}
