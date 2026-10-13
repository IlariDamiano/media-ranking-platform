import { useEffect, useState } from "react";
import { useParams } from "react-router";

import { getPublicProfile, type PublicProfile } from "../lib/profileApi";

export default function PublicProfilePage() {
  const { username = "" } = useParams();

  return <PublicProfileContent key={username} username={username} />;
}

function PublicProfileContent({ username }: { username: string }) {
  const [profile, setProfile] = useState<PublicProfile | null>(null);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    getPublicProfile(username, controller.signal);

    getPublicProfile(username, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setProfile(result);
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted) {
          setError(
            err instanceof Error ? err.message : "Unable to load this profile.",
          );
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [username]);

  if (loading) {
    return (
      <section className="media-state" role="status">
        <p>Searching the Archive...</p>
      </section>
    );
  }

  if (error) {
    return (
      <section className="media-state" role="alert">
        <h2>PROFILE UNAVAILABLE.</h2>
        <p>{error}</p>
      </section>
    );
  }

  if (!profile) {
    return (
      <section className="media-state">
        <h2>PROFILE NOT FOUND.</h2>
        <p>This profile is unavailable or does not exist.</p>
      </section>
    );
  }

  const joinedDate = new Date(profile.joinedAt).toLocaleDateString(undefined, {
    month: "long",
    year: "numeric",
  });

  return (
    <section className="public-profile-page">
      <p className="eyebrow">AKASHIC ARCHIVE / USER PROFILE</p>

      <div className="public-profile-card">
        <div className="public-profile-avatar" aria-hidden="true">
          {profile.username.charAt(0).toUpperCase()}
        </div>

        <div className="public-profile-info">
          <h1>{profile.username}</h1>

          <p className="public-profile-joined">Member since {joinedDate}</p>

          <h2>ABOUT</h2>

          <p className="public-profile-bio">
            {profile.bio || "This user hasn't written a biography yet."}
          </p>
        </div>
      </div>
    </section>
  );
}
