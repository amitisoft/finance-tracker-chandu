import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { AuthResponse, AuthUser } from "../types/api";

type AuthState = {
  user: AuthUser | null;
  accessToken: string | null;
  refreshToken: string | null;
  setAuth: (response: AuthResponse) => void;
  logout: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      setAuth: (response) =>
        set({
          user: response.user,
          accessToken: response.accessToken,
          refreshToken: response.refreshToken
        }),
      logout: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null
        })
    }),
    {
      name: "finance-auth"
    }
  )
);
