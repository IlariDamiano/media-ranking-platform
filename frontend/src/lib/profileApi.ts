import { getCsrfHeader } from "./authApi";

export type ProfileVisibility = "PUBLIC" | "PRIVATE";

export interface MyProfile {
  id: string;
  username: string;
  email: string;
  bio: string | null;
  visibility: ProfileVisibility;
  joinedAt: string;
}

export interface UpdateProfileDetails {
  bio: string | null;
  visibility: ProfileVisibility;
}

export async function getMyProfile(signal?: AbortSignal): Promise<MyProfile> {
  const response = await fetch("/api/profile/me", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });

  if (!response.ok) {
    throw new Error(`Unable to load your profile: HTTP ${response.status}`);
  }

  return (await response.json()) as MyProfile;
}

export async function updateMyProfile(
  details: UpdateProfileDetails,
): Promise<MyProfile> {
  const csrfHeaders = await getCsrfHeader();

  const response = await fetch("/api/profile/me", {
    method: "PATCH",
    credentials: "same-origin",
    cache: "no-store",
    headers: {
      "Content-Type": "application/json",
      ...csrfHeaders,
    },
    body: JSON.stringify(details),
  });

  if (!response.ok) {
    throw new Error(`Unable to update your profile: HTTP ${response.status}`);
  }

  return (await response.json()) as MyProfile;
}

export interface PublicProfile {
  username: string;
  bio: string | null;
  joinedAt: string;
}

export async function getPublicProfile(
  username: string,
  signal?: AbortSignal,
): Promise<PublicProfile | null> {
  const response = await fetch(
    `/api/profiles/${encodeURIComponent(username)}`,
    { signal },
  );

  // Missing and private profiles look identical to visitors.
  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Unable to load this profile: HTTP ${response.status}`);
  }

  return (await response.json()) as PublicProfile;
}
