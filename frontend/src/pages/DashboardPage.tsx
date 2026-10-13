import { useEffect, useState } from "react";
import { Link } from "react-router";

import { getMyProfile, type MyProfile } from "../lib/profileApi";

export default function DashboardPage() {
  const [profile, setProfile] = useState<MyProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    getMyProfile(controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) {
          setProfile(data);
        }
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error
              ? err.message
              : "Unable to load your dashboard.",
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, []);

  if (loading) {
    return (
      <section className="media-state" role="status">
        <p>Loading your Archive...</p>
      </section>
    );
  }

  if (error || !profile) {
    return (
      <section className="media-state" role="alert">
        <h2>UNABLE TO OPEN YOUR ARCHIVE.</h2>
        <p>{error || "Profile information is unavailable."}</p>
      </section>
    );
  }

  const joinedDate = new Date(profile.joinedAt).toLocaleDateString(undefined, {
    month: "long",
    year: "numeric",
  });

  return (
    <section className="dashboard-page" aria-labelledby="dashboard-title">
      <p className="eyebrow">AKASHIC ARCHIVE / PERSONAL SPACE</p>

      <h1 id="dashboard-title">WELCOME BACK, {profile.username}.</h1>

      <p className="dashboard-intro">
        Your stories, your collections, your universe.
      </p>

      <div className="dashboard-panel">
        <h2>YOUR PROFILE</h2>

        <p>{profile.bio || "Your story is just beginning."}</p>

        <dl className="dashboard-details">
          <div>
            <dt>Username</dt>
            <dd>{profile.username}</dd>
          </div>

          <div>
            <dt>Email</dt>
            <dd>{profile.email}</dd>
          </div>

          <div>
            <dt>Profile visibility</dt>
            <dd>{profile.visibility}</dd>
          </div>

          <div>
            <dt>Member since</dt>
            <dd>{joinedDate}</dd>
          </div>
        </dl>
        {profile.visibility === "PUBLIC" && (
          <Link
            to={`/users/${encodeURIComponent(profile.username)}`}
            className="dashboard-public-link"
          >
            View your public profile →
          </Link>
        )}
      </div>

      <h2 className="dashboard-section-title">EXPLORE YOUR ARCHIVE</h2>
      <p className="dashboard-settings-link">
        <Link to="/settings/profile">
          Edit your profile and privacy settings →
        </Link>
      </p>
      <div className="dashboard-links">
        <Link to="/library">
          <strong>MY LIBRARY</strong>
          <span>Track everything you watch and read →</span>
        </Link>

        <Link to="/lists">
          <strong>MY COLLECTIONS</strong>
          <span>Build your personal rankings and lists →</span>
        </Link>
      </div>
    </section>
  );
}
