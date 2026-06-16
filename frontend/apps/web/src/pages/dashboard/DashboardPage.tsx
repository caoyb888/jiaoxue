import React from 'react'
import { useAuthStore } from '@edu/store'
import { Button } from '@edu/ui'
import { authApi } from '@edu/api'
import { useNavigate } from 'react-router-dom'

export default function DashboardPage() {
  const { realName, roles, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } finally {
      logout()
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      <header className="flex h-14 items-center justify-between border-b bg-white px-6 shadow-sm">
        <span className="font-semibold text-gray-900">山东管理学院智慧教学平台</span>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">{realName}</span>
          <Button variant="ghost" size="sm" onClick={handleLogout}>
            退出登录
          </Button>
        </div>
      </header>
      <main className="flex flex-1 items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-semibold text-gray-900">欢迎回来，{realName}</h2>
          <p className="mt-2 text-gray-500">角色：{roles.join('、') || '未分配'}</p>
        </div>
      </main>
    </div>
  )
}
