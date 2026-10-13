import { Link, NavLink, Outlet } from "react-router";

import { useAuth } from "../context/authStore";

export default function SiteLayout() {
  const { user, loading } = useAuth();

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
            <Link
              to="/dashboard"
              className="nav-account-link"
              aria-label={`Go to your dashboard, ${user.username}`}
            >
              <span className="nav-account-avatar" aria-hidden="true">
                {user.username.charAt(0).toUpperCase()}
              </span>

              <span className="nav-username" title={user.username}>
                {user.username}
              </span>
            </Link>
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
