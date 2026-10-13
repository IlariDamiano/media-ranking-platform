import { createContext, useContext } from "react";

import type { AuthUser, LoginCredentials } from "../lib/authApi";

export interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  signIn: (credentials: LoginCredentials) => Promise<void>;
  signOut: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
