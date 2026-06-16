import React, { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@edu/api'
import { useAuthStore } from '@edu/store'
import { Button, Input } from '@edu/ui'
import { maskPhone } from '@edu/utils'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setTokens } = useAuthStore()

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
          {/* Logo & Title */}
          <div className="mb-8 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-xl bg-blue-600 text-2xl font-bold text-white">
              智
            </div>
            <h1 className="text-xl font-semibold text-gray-900">山东管理学院</h1>
            <p className="mt-1 text-sm text-gray-500">智慧教学平台</p>
          </div>

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
                  {sendSmsMutation.isPending
                    ? '发送中…'
                    : countdown > 0
                      ? `${countdown}s`
                      : '获取验证码'}
                </button>
              }
            />

            {loginMutation.isError && (
              <p className="text-center text-sm text-red-500">
                验证码错误或已过期，请重新获取
              </p>
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

          <p className="mt-6 text-center text-xs text-gray-400">
            登录即代表同意《用户协议》和《隐私政策》
          </p>
        </div>
      </div>
    </div>
  )
}
