import { useState, type FormEvent } from "react";
import { Link } from "react-router";
import { registerAccount } from "../lib/authApi";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,30}$/;

export default function RegisterPage() {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [showPasswords, setShowPasswords] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [registered, setRegistered] = useState(false);

  const usernameValid = USERNAME_PATTERN.test(username.trim());
  const emailValid = EMAIL_PATTERN.test(email.trim());
  const passwordValid = password.length >= 9 && password.length <= 64;
  const passwordsMatch =
    confirmPassword.length > 0 && password === confirmPassword;

  const passwordChecks = [
    {
      label: "9–64 characters (required)",
      passed: passwordValid,
    },
    {
      label: "14+ characters (recommended)",
      passed: password.length >= 14,
    },
    {
      label: "Uppercase letter (optional)",
      passed: /[A-Z]/.test(password),
    },
    {
      label: "Lowercase letter (optional)",
      passed: /[a-z]/.test(password),
    },
    {
      label: "Number (optional)",
      passed: /[0-9]/.test(password),
    },
    {
      label: "Symbol (optional)",
      passed: /[^A-Za-z0-9]/.test(password),
    },
  ];

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (loading) return;

    if (!usernameValid || !emailValid || !passwordValid) {
      setError("Please correct the highlighted requirements.");
      return;
    }

    if (!passwordsMatch) {
      setError("Your passwords do not match.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      await registerAccount({
        username: username.trim(),
        email: email.trim(),
        password,
      });

      setRegistered(true);
      setPassword("");
      setConfirmPassword("");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Unable to create your account.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="auth-page" aria-labelledby="register-title">
      <div className="auth-container">
        <p className="eyebrow">AKASHIC ARCHIVE / NEW ACCOUNT</p>

        <h1 id="register-title">JOIN THE ARCHIVE.</h1>

        <p className="auth-description">
          Every story. One universe. Your journey starts here.
        </p>

        {registered ? (
          <div className="auth-success" role="status">
            <h2>ACCOUNT CREATED.</h2>
            <p>Welcome to Akashic Archive. Your account is ready.</p>
            <Link to="/login">Continue to sign in →</Link>
          </div>
        ) : (
          <>
            <form className="auth-form" onSubmit={handleSubmit}>
              <label htmlFor="register-username">Username</label>
              <input
                id="register-username"
                type="text"
                autoComplete="username"
                minLength={3}
                maxLength={30}
                pattern="[A-Za-z0-9_]{3,30}"
                aria-describedby="username-help"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
              <p id="username-help" className="auth-hint">
                {username.length === 0
                  ? "3–30 characters. Letters, numbers and underscores."
                  : usernameValid
                    ? "✓ Username format looks good."
                    : "Use 3–30 letters, numbers or underscores."}
              </p>

              <label htmlFor="register-email">Email address</label>
              <input
                id="register-email"
                type="email"
                autoComplete="email"
                maxLength={254}
                aria-describedby="email-help"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
              <p id="email-help" className="auth-hint">
                {email.length === 0
                  ? "Enter the email address for your account."
                  : emailValid
                    ? "✓ Email format looks correct."
                    : "Enter an email such as name@example.com."}
              </p>

              <label htmlFor="register-password">Password</label>
              <div className="auth-password-field">
                <input
                  id="register-password"
                  type={showPasswords ? "text" : "password"}
                  autoComplete="new-password"
                  minLength={9}
                  maxLength={64}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />

                <button
                  type="button"
                  className="auth-password-toggle"
                  onClick={() => setShowPasswords((current) => !current)}
                  aria-label={
                    showPasswords ? "Hide passwords" : "Show passwords"
                  }
                  aria-pressed={showPasswords}
                >
                  {showPasswords ? "Hide" : "Show"}
                </button>
              </div>

              <ul className="auth-checklist" aria-label="Password guidance">
                {passwordChecks.map((check) => (
                  <li
                    key={check.label}
                    className={check.passed ? "is-met" : ""}
                  >
                    <span aria-hidden="true">{check.passed ? "✓" : "○"}</span>
                    <span>{check.label}</span>
                  </li>
                ))}
              </ul>

              <label htmlFor="register-confirm-password">
                Confirm password
              </label>
              <input
                id="register-confirm-password"
                type={showPasswords ? "text" : "password"}
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />

              {confirmPassword.length > 0 && (
                <p
                  className={
                    passwordsMatch
                      ? "auth-feedback is-valid"
                      : "auth-feedback is-invalid"
                  }
                  aria-live="polite"
                >
                  {passwordsMatch
                    ? "✓ Passwords match."
                    : "Passwords do not match yet."}
                </p>
              )}

              {error && (
                <p className="auth-error" role="alert">
                  {error}
                </p>
              )}

              <button type="submit" disabled={loading}>
                {loading ? "CREATING ACCOUNT..." : "JOIN THE ARCHIVE"}
              </button>
            </form>

            <p className="auth-switch">
              Already have an account? <Link to="/login">Sign in</Link>
            </p>
          </>
        )}
      </div>
    </section>
  );
}
