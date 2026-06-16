import React, { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@edu/api'
import { setAccessToken } from '@edu/api'
import { useAuthStore } from '@edu/store'
import { Button, Input } from '@edu/ui'

type Tab = 'phone' | 'wechat'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setTokens } = useAuthStore()

  const [tab, setTab] = useState<Tab>('phone')
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [phoneError, setPhoneError] = useState('')
  const [countdown, setCountdown] = useState(0)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const phoneRegex = /^1[3-9]\d{9}$/

  const sendSmsMutation = useMutation({
    mutationFn: () => authApi.sendSmsCode(phone),
    onSuccess: () => {
      setCountdown(60)
      timerRef.current = setInterval(() => {
        setCountdown((c) => {
          if (c <= 1) {
            if (timerRef.current) clearInterval(timerRef.current)
            return 0
          }
          return c - 1
        })
      }, 1000)
    },
  })

  const loginMutation = useMutation({
    mutationFn: () => authApi.loginByPhone({ phone, code }),
    onSuccess: (data) => {
      setAccessToken(data.accessToken)
      setTokens(data.accessToken, data.refreshToken)
      navigate('/dashboard', { replace: true })
    },
  })

  const handleSendCode = () => {
    if (!phoneRegex.test(phone)) {
      setPhoneError('请输入正确的手机号')
      return
    }
    setPhoneError('')
    sendSmsMutation.mutate()
  }

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault()
    if (!phoneRegex.test(phone)) {
      setPhoneError('请输入正确的手机号')
      return
    }
    if (code.length !== 6) return
    loginMutation.mutate()
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 px-4">
      <div className="w-full max-w-sm">
        <div className="rounded-2xl bg-white px-8 py-10 shadow-lg">
          {/* Logo */}
          <div className="mb-6 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-blue-600 text-2xl font-bold text-white">
              智
            </div>
            <h1 className="text-xl font-semibold text-gray-900">山东管理学院</h1>
            <p className="mt-1 text-sm text-gray-500">智慧教学平台</p>
          </div>

          {/* Tab */}
          <div className="mb-6 flex rounded-lg bg-gray-100 p-1">
            <button
              type="button"
              onClick={() => setTab('phone')}
              className={`flex-1 rounded-md py-1.5 text-sm font-medium transition-colors ${
                tab === 'phone' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              手机号登录
            </button>
            <button
              type="button"
              onClick={() => setTab('wechat')}
              className={`flex-1 rounded-md py-1.5 text-sm font-medium transition-colors ${
                tab === 'wechat' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              微信扫码
            </button>
          </div>

          {tab === 'phone' ? (
            <form onSubmit={handleLogin} className="flex flex-col gap-4">
              <Input
                label="手机号"
                type="tel"
                placeholder="请输入手机号"
                value={phone}
                onChange={(e) => {
                  setPhone(e.target.value)
                  setPhoneError('')
                }}
                error={phoneError}
                maxLength={11}
                required
              />

              <Input
                label="验证码"
                type="text"
                placeholder="6位验证码"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                maxLength={6}
                required
                suffix={
                  <button
                    type="button"
                    onClick={handleSendCode}
                    disabled={countdown > 0 || sendSmsMutation.isPending}
                    className="mr-1 rounded px-3 py-1.5 text-xs font-medium text-blue-600 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:text-gray-400"
                  >
                    {sendSmsMutation.isPending ? '发送中…' : countdown > 0 ? `${countdown}s` : '获取验证码'}
                  </button>
                }
              />

              {loginMutation.isError && (
                <p className="text-center text-sm text-red-500">验证码错误或已过期，请重新获取</p>
              )}

              <Button
                type="submit"
                fullWidth
                size="lg"
                loading={loginMutation.isPending}
                disabled={phone.length !== 11 || code.length !== 6}
                className="mt-2"
              >
                登录
              </Button>
            </form>
          ) : (
            /* 微信扫码区域 */
            <div className="flex flex-col items-center gap-4 py-4">
              <div className="flex h-48 w-48 items-center justify-center rounded-xl border-2 border-dashed border-gray-200 bg-gray-50">
                <div className="text-center">
                  <svg className="mx-auto mb-2 h-12 w-12 text-green-500" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18z"/>
                  </svg>
                  <p className="text-sm text-gray-400">微信小程序扫码</p>
                  <p className="text-xs text-gray-300">（开发环境暂不可用）</p>
                </div>
              </div>
              <p className="text-center text-xs text-gray-400">
                使用微信扫描上方二维码，通过微信小程序登录
              </p>
            </div>
          )}

          <p className="mt-6 text-center text-xs text-gray-400">
            登录即代表同意《用户协议》和《隐私政策》
          </p>
        </div>
      </div>
    </div>
  )
}
