import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router";
import { useAuth } from "../context/authStore";

const accountSections = [
  { to: "/dashboard", label: "Overview" },
  { to: "/library", label: "Library" },
  { to: "/lists", label: "My Lists" },
  { to: "/settings/profile", label: "Profile & Privacy" },
];

function AccountNavigation() {
  return (
    <>
      {accountSections.map((section) => (
        <NavLink
          key={section.to}
          to={section.to}
          end
          className={({ isActive }) =>
            isActive ? "account-nav-link active" : "account-nav-link"
          }
        >
          {section.label}
        </NavLink>
      ))}
    </>
  );
}

export default function AccountLayout() {
  const navigate = useNavigate();
  const { user, signOut } = useAuth();

  const [signingOut, setSigningOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");

  async function handleLogout() {
    if (signingOut) return;

    setSigningOut(true);
    setLogoutError("");

    try {
      await signOut();
      navigate("/", { replace: true });
    } catch {
      setLogoutError("Unable to sign out. Please try again.");
    } finally {
      setSigningOut(false);
    }
  }

  return (
    <div className="account-shell">
      <aside className="account-sidebar">
        <p className="eyebrow">YOUR ARCHIVE</p>

        <p className="account-sidebar-username">{user?.username}</p>

        <nav className="account-section-links" aria-label="Account navigation">
          <AccountNavigation />
        </nav>
        <button
          type="button"
          className="account-logout"
          onClick={handleLogout}
          disabled={signingOut}
        >
          {signingOut ? "Signing out..." : "Sign out"}
        </button>
      </aside>

      <div className="account-content">
        <div className="account-mobile-toolbar">
          <nav className="account-mobile-tabs" aria-label="Account navigation">
            <AccountNavigation />
          </nav>

          <button
            type="button"
            className="account-logout account-logout-mobile"
            onClick={handleLogout}
            disabled={signingOut}
          >
            {signingOut ? "Signing out..." : "Sign out"}
          </button>
        </div>

        {logoutError && (
          <p className="account-logout-error" role="alert">
            {logoutError}
          </p>
        )}

        <Outlet />
      </div>
    </div>
  );
}
