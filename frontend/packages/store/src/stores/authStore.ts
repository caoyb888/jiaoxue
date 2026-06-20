import { create } from 'zustand'

// accessToken 写 sessionStorage 以支持 SPA 页面刷新恢复（关 Tab 即失效）
// refreshToken 同样存 sessionStorage（不写 localStorage，关 Tab 失效）
const _storedAt = typeof window !== 'undefined' ? sessionStorage.getItem('edu_at') : null

// 从 JWT 重建用户态（roles/username/userId）。
// 这些字段只存内存、不进 sessionStorage，刷新后会丢；而 accessToken 会从
// sessionStorage 恢复 → 出现“已登录但 roles 为空、Dashboard 无模块”的问题。
// JWT payload 本身已携带这些声明，故在初始化时直接解码恢复，无需再请求接口。
interface JwtUserState {
  userId: number | null
  username: string | null
  roles: string[]
}

function decodeUserFromToken(token: string | null): JwtUserState {
  const empty: JwtUserState = { userId: null, username: null, roles: [] }
  if (!token) return empty
  try {
    // JWT 用 base64url（- 和 _ 替代 + 和 /），atob() 前需还原
    const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(b64)) as {
      userId?: number
      sub?: string
      username?: string
      roles?: string | string[]
    }
    // 后端签发的 roles 是逗号分隔字符串（如 "ROLE_TEACHER" / "ROLE_ADMIN,ROLE_TEACHER"）
    const rawRoles = payload.roles
    const roles = Array.isArray(rawRoles)
      ? rawRoles
      : typeof rawRoles === 'string'
        ? rawRoles.split(',').map((r) => r.trim()).filter(Boolean)
        : []
    return {
      userId: payload.userId ?? (payload.sub != null ? Number(payload.sub) : null),
      username: payload.username ?? null,
      roles,
    }
  } catch {
    return empty
  }
}

const _initialUser = decodeUserFromToken(_storedAt)

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
  userId: _initialUser.userId,
  username: _initialUser.username,
  // realName 不在 JWT 中，刷新后先用 username 兜底，首次登录的 getUserById 会覆盖为真实姓名
  realName: _initialUser.username,
  roles: _initialUser.roles,

  setTokens: (access, refresh) => {
    // 新 token 到手时同步重建 roles/userId/username，保证刷新令牌后用户态不丢
    const decoded = decodeUserFromToken(access)
    set({
      accessToken: access,
      userId: decoded.userId,
      username: decoded.username,
      roles: decoded.roles,
    })
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
