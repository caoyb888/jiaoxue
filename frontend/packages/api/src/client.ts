import axios from 'axios'

// Initialize from sessionStorage on page load (supports refresh/navigation)
let _accessToken: string | null =
  typeof window !== 'undefined' ? sessionStorage.getItem('edu_at') : null

export function setAccessToken(token: string | null) {
  _accessToken = token
  if (token) {
    sessionStorage.setItem('edu_at', token)
  } else {
    sessionStorage.removeItem('edu_at')
  }
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  if (_accessToken) {
    config.headers.Authorization = `Bearer ${_accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    // Unwrap Result<T> envelope: { code, msg, data }
    if (body && typeof body.code === 'number' && 'data' in body) {
      if (body.code !== 200) {
        return Promise.reject(new Error(body.msg || 'API error'))
      }
      return body.data
    }
    return body
  },
  async (err) => {
    if (err.response?.status === 401) {
      // 尝试用 refreshToken 续期
      const rt = sessionStorage.getItem('edu_rt')
      if (rt && !err.config._retry) {
        err.config._retry = true
        try {
          const newToken = await http.post('/v1/auth/token/refresh', null, { params: { refreshToken: rt } })
          if (newToken) {
            setAccessToken(newToken as unknown as string)
            err.config.headers.Authorization = `Bearer ${newToken}`
            return http.request(err.config)
          }
        } catch {
          // refresh 失败，跳转登录
        }
      }
      setAccessToken(null)
      sessionStorage.removeItem('edu_rt')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)
