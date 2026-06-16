import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuthStore } from '@edu/store'

const LoginPage = lazy(() => import('../pages/auth/LoginPage'))
const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage'))

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn())
  return isLoggedIn ? <>{children}</> : <Navigate to="/login" replace />
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<div className="flex h-screen items-center justify-center text-gray-400">加载中…</div>}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/dashboard"
            element={
              <RequireAuth>
                <DashboardPage />
              </RequireAuth>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
