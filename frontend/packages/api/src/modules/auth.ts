import { http } from '../client'

export interface TokenVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export interface SmsLoginDTO {
  phone: string
  code: string
}

export const authApi = {
  sendSmsCode: (phone: string) =>
    http.post<void, void>(`/v1/auth/sms/send`, null, { params: { phone } }),

  loginByPhone: (dto: SmsLoginDTO) =>
    http.post<SmsLoginDTO, TokenVO>('/v1/auth/login/phone', dto),

  refreshToken: (refreshToken: string) =>
    http.post<void, TokenVO>('/v1/auth/token/refresh', null, {
      params: { refreshToken },
    }),

  logout: () =>
    http.post<void, void>('/v1/auth/logout'),
}
