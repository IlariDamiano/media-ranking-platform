export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: "USER" | "ADMIN";
}

interface CsrfResponse {
  headerName: string;
  token: string;
  parameterName: string;
}

export async function getCurrentUser(
  signal?: AbortSignal,
): Promise<AuthUser | null> {
  const response = await fetch("/api/auth/me", {
    credentials: "same-origin",
    signal,
  });

  // A visitor without an account session is not an error.
  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Session check failed: HTTP ${response.status}`);
  }

  return (await response.json()) as AuthUser;
}

export async function getCsrfHeader(): Promise<Record<string, string>> {
  const response = await fetch("/api/auth/csrf", {
    credentials: "same-origin",
  });

  if (!response.ok) {
    throw new Error(`CSRF request failed: HTTP ${response.status}`);
  }

  const csrf = (await response.json()) as CsrfResponse;

  return {
    [csrf.headerName]: csrf.token,
  };
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterDetails extends LoginCredentials {
  username: string;
}

async function postWithCsrf(url: string, data?: object): Promise<Response> {
  const csrfHeaders = await getCsrfHeader();

  return fetch(url, {
    method: "POST",
    credentials: "same-origin",
    headers: {
      ...csrfHeaders,
      ...(data ? { "Content-Type": "application/json" } : {}),
    },
    ...(data ? { body: JSON.stringify(data) } : {}),
  });
}

export async function registerAccount(
  details: RegisterDetails,
): Promise<AuthUser> {
  const response = await postWithCsrf("/api/auth/register", details);

  if (response.status === 409) {
    throw new Error("Username or email is already registered.");
  }

  if (!response.ok) {
    throw new Error(`Registration failed: HTTP ${response.status}`);
  }

  return (await response.json()) as AuthUser;
}

export async function loginAccount(
  credentials: LoginCredentials,
): Promise<AuthUser> {
  const response = await postWithCsrf("/api/auth/login", credentials);

  if (response.status === 401) {
    throw new Error("Invalid email or password.");
  }

  if (!response.ok) {
    throw new Error(`Login failed: HTTP ${response.status}`);
  }

  const user = await getCurrentUser();

  if (!user) {
    throw new Error("Login succeeded, but the session could not be verified.");
  }

  return user;
}

export async function logoutAccount(): Promise<void> {
  const response = await postWithCsrf("/api/auth/logout");

  if (!response.ok) {
    throw new Error(`Logout failed: HTTP ${response.status}`);
  }
}
