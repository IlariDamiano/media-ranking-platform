import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router";

import {
  getMyProfile,
  updateMyProfile,
  type ProfileVisibility,
} from "../lib/profileApi";

export default function ProfileSettingsPage() {
  const [bio, setBio] = useState("");
  const [visibility, setVisibility] = useState<ProfileVisibility>("PUBLIC");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    getMyProfile(controller.signal)
      .then((profile) => {
        if (controller.signal.aborted) return;

        setBio(profile.bio ?? "");
        setVisibility(profile.visibility);
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;

        setError(
          err instanceof Error ? err.message : "Unable to load your profile.",
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, []);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;

    setSaving(true);
    setError("");
    setMessage("");

    try {
      const updated = await updateMyProfile({
        bio: bio.trim() || null,
        visibility,
      });

      setBio(updated.bio ?? "");
      setVisibility(updated.visibility);
      setMessage("Your profile settings have been saved.");
    } catch (err) {
      setLoadError(
        err instanceof Error ? err.message : "Unable to load your profile.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="profile-settings-page">
      <p className="eyebrow">AKASHIC ARCHIVE / SETTINGS</p>
      <h1>EDIT YOUR PROFILE.</h1>

      <p className="auth-description">
        Personalize how you appear throughout the Archive.
      </p>

      {loading ? (
        <p role="status">Loading your profile...</p>
      ) : loadError ? (
        <div className="media-state" role="alert">
          <h2>PROFILE UNAVAILABLE.</h2>
          <p>{loadError}</p>
          <p>Please try refreshing the page.</p>
        </div>
      ) : (
        <form className="auth-form" onSubmit={handleSave}>
          <label htmlFor="profile-bio">Your biography</label>

          <textarea
            id="profile-bio"
            rows={5}
            maxLength={500}
            value={bio}
            disabled={saving}
            onChange={(event) => {
              setBio(event.target.value);
              setMessage("");
            }}
            placeholder="Tell the Archive about yourself..."
          />

          <p className="auth-hint">{bio.length} / 500 characters</p>

          <label htmlFor="profile-visibility">Profile visibility</label>

          <select
            id="profile-visibility"
            value={visibility}
            disabled={saving}
            onChange={(event) => {
              setVisibility(event.target.value as ProfileVisibility);
              setMessage("");
            }}
          >
            <option value="PUBLIC">Public</option>
            <option value="PRIVATE">Private</option>
          </select>

          <p className="auth-hint">
            Your lists, ratings and library remain private by default, even when
            your profile is public.
          </p>

          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}

          {message && (
            <p className="auth-feedback is-valid" role="status">
              {message}
            </p>
          )}

          <button type="submit" disabled={saving}>
            {saving ? "SAVING..." : "SAVE CHANGES"}
          </button>
        </form>
      )}

      <p className="auth-switch">
        <Link to="/dashboard">← Back to your dashboard</Link>
      </p>
    </section>
  );
}
