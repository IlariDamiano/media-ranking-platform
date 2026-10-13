import { useEffect, useState, type ReactNode } from "react";

import {
  getCurrentUser,
  loginAccount,
  logoutAccount,
  type AuthUser,
  type LoginCredentials,
} from "../lib/authApi";

import { AuthContext } from "./authStore";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();

    getCurrentUser(controller.signal)
      .then((currentUser) => {
        if (!controller.signal.aborted) {
          setUser(currentUser);
        }
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          console.error("Unable to restore session:", error);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, []);

  async function signIn(credentials: LoginCredentials) {
    const authenticatedUser = await loginAccount(credentials);
    setUser(authenticatedUser);
  }

  async function signOut() {
    await logoutAccount();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}
