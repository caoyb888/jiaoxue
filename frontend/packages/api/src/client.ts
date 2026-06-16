import axios from 'axios'

let _accessToken: string | null = null

export function setAccessToken(token: string | null) {
  _accessToken = token
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
  (res) => res.data,
  async (err) => {
    if (err.response?.status === 401) {
      // 尝试用 refreshToken 续期
      const rt = sessionStorage.getItem('edu_rt')
      if (rt && !err.config._retry) {
        err.config._retry = true
        try {
          const res: any = await http.post('/v1/auth/token/refresh', null, { params: { refreshToken: rt } })
          const newToken = res?.data?.accessToken
          if (newToken) {
            setAccessToken(newToken)
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
