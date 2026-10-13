import { Navigate, Outlet, useLocation } from "react-router";

import { useAuth } from "../context/authStore";

export default function RequireAuth() {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="media-state" role="status">
        <p>Checking your session...</p>
      </div>
    );
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{
          from: location.pathname + location.search + location.hash,
        }}
      />
    );
  }

  return <Outlet />;
}
