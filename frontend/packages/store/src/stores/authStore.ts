import { create } from 'zustand'

// accessToken 写 sessionStorage 以支持 SPA 页面刷新恢复（关 Tab 即失效）
// refreshToken 同样存 sessionStorage（不写 localStorage，关 Tab 失效）
const _storedAt = typeof window !== 'undefined' ? sessionStorage.getItem('edu_at') : null

interface AuthState {
  accessToken: string | null
  userId: number | null
  username: string | null
  realName: string | null
  roles: string[]
  setTokens: (access: string, refresh: string) => void
  setUserInfo: (userId: number, username: string, realName: string, roles: string[]) => void
  logout: () => void
  isLoggedIn: () => boolean
  getRefreshToken: () => string | null
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  accessToken: _storedAt,
  userId: null,
  username: null,
  realName: null,
  roles: [],

  setTokens: (access, refresh) => {
    set({ accessToken: access })
    sessionStorage.setItem('edu_at', access)
    sessionStorage.setItem('edu_rt', refresh)
  },

  setUserInfo: (userId, username, realName, roles) =>
    set({ userId, username, realName, roles }),

  logout: () => {
    sessionStorage.removeItem('edu_at')
    sessionStorage.removeItem('edu_rt')
    set({ accessToken: null, userId: null, username: null, realName: null, roles: [] })
  },

  isLoggedIn: () => !!get().accessToken,

  getRefreshToken: () => sessionStorage.getItem('edu_rt'),
}))
