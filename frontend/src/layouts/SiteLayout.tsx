import { Link, NavLink, Outlet } from "react-router";

export default function SiteLayout() {
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

        <span className="nav-caption">VOL. 001</span>
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
