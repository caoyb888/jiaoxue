import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  userId: number | null
  username: string | null
  realName: string | null
  roles: string[]
  setTokens: (access: string, refresh: string) => void
  setUserInfo: (userId: number, username: string, realName: string, roles: string[]) => void
  logout: () => void
  isLoggedIn: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      username: null,
      realName: null,
      roles: [],

      setTokens: (access, refresh) => {
        set({ accessToken: access, refreshToken: refresh })
        localStorage.setItem('access_token', access)
        localStorage.setItem('refresh_token', refresh)
      },

      setUserInfo: (userId, username, realName, roles) =>
        set({ userId, username, realName, roles }),

      logout: () => {
        localStorage.removeItem('access_token')
        localStorage.removeItem('refresh_token')
        set({ accessToken: null, refreshToken: null, userId: null, username: null, realName: null, roles: [] })
      },

      isLoggedIn: () => !!get().accessToken,
    }),
    {
      name: 'edu-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userId: state.userId,
        username: state.username,
        realName: state.realName,
        roles: state.roles,
      }),
    },
  ),
)
