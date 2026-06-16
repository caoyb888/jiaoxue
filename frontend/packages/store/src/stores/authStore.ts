import { create } from 'zustand'

// Token 只存内存，不写 localStorage（安全规范 S1-12）
// refreshToken 存 sessionStorage（关闭 Tab 即失效，防 XSS 泄露）
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
  accessToken: null,
  userId: null,
  username: null,
  realName: null,
  roles: [],

  setTokens: (access, refresh) => {
    set({ accessToken: access })
    // refreshToken 写 sessionStorage（不写 localStorage，关 Tab 失效）
    sessionStorage.setItem('edu_rt', refresh)
  },

  setUserInfo: (userId, username, realName, roles) =>
    set({ userId, username, realName, roles }),

  logout: () => {
    sessionStorage.removeItem('edu_rt')
    set({ accessToken: null, userId: null, username: null, realName: null, roles: [] })
  },

  isLoggedIn: () => !!get().accessToken,

  getRefreshToken: () => sessionStorage.getItem('edu_rt'),
}))
