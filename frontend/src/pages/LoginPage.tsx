import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { useAuth } from "../context/authStore";

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const requestedPath = (location.state as { from?: string } | null)?.from;

  const returnTo =
    requestedPath?.startsWith("/") && !requestedPath.startsWith("//")
      ? requestedPath
      : "/";
  const { signIn } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    setLoading(true);
    setError("");

    try {
      await signIn({ email, password });
      navigate(returnTo, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to sign in.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-page" aria-labelledby="login-title">
      <div className="auth-container">
        <p className="eyebrow">AKASHIC ARCHIVE / ACCOUNT ACCESS</p>

        <h1 id="login-title">WELCOME BACK.</h1>

        <p className="auth-description">
          Every story. One universe. Continue yours.
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label htmlFor="login-email">Email address</label>
          <input
            id="login-email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />

          <label htmlFor="login-password">Password</label>
          <input
            id="login-password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />

          {error && (
            <p role="alert" className="auth-error">
              {error}
            </p>
          )}

          <button type="submit" disabled={loading}>
            {loading ? "SIGNING IN..." : "ENTER THE ARCHIVE"}
          </button>
        </form>
        <p className="auth-switch">
          New to Akashic? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </section>
  );
}
