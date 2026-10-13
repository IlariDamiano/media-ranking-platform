import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router";

import { useAuth } from "../context/authStore";

export default function SiteLayout() {
  const navigate = useNavigate();
  const { user, loading, signOut } = useAuth();

  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");

  async function handleLogout() {
    if (loggingOut) return;

    setLoggingOut(true);
    setLogoutError("");

    try {
      await signOut();
      navigate("/", { replace: true });
    } catch {
      setLogoutError("Unable to log out. Please try again.");
    } finally {
      setLoggingOut(false);
    }
  }

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>

      <header className="site-nav">
        <Link to="/" className="brand" aria-label="Akashic Archive home">
          <img
            className="brand-logo-horizontal"
            src="/branding/akashic-logo-horizontal.webp"
            alt=""
          />
          <img
            className="brand-logo-emblem"
            src="/branding/akashic-emblem.webp"
            alt=""
          />
        </Link>

        <nav className="nav-links" aria-label="Main navigation">
          <NavLink to="/" end>
            Discover
          </NavLink>

          <NavLink to="/library">Library</NavLink>

          <NavLink to="/lists">Lists</NavLink>
        </nav>

        <div className="nav-actions">
          <span className="nav-caption">VOL. 001</span>

          {loading ? (
            <span className="nav-session-status" role="status">
              Checking session...
            </span>
          ) : user ? (
            <>
              <span className="nav-username" title={user.username}>
                {user.username}
              </span>

              <button
                type="button"
                className="nav-logout"
                onClick={handleLogout}
                disabled={loggingOut}
              >
                {loggingOut ? "Signing out..." : "Logout"}
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className="nav-login">
                Login
              </NavLink>

              <NavLink to="/register" className="nav-register">
                Register
              </NavLink>
            </>
          )}

          {logoutError && (
            <span className="nav-auth-error" role="alert">
              {logoutError}
            </span>
          )}
        </div>
      </header>

      <main id="main-content" className="page-container">
        <Outlet />
      </main>

      <footer className="site-footer">
        <span>AKASHIC ARCHIVE / EVERY STORY. ONE UNIVERSE.</span>
        <span>DISCOVER · TRACK · CURATE</span>
      </footer>
    </div>
  );
}
