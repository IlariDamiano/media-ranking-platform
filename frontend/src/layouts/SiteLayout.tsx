import { Link, NavLink, Outlet } from "react-router";

export default function SiteLayout() {
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>

      <header className="site-nav">
        <Link to="/" className="brand" aria-label="Media Ranking home">
          <span className="brand-mark">M/</span>
          <span className="brand-name">MEDIA RANKING</span>
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
        <span>MEDIA RANKING / NEO-CHROME</span>
        <span>DISCOVER · TRACK · CURATE</span>
      </footer>
    </div>
  );
}
