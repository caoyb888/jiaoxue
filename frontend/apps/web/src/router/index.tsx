import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuthStore } from '@edu/store'

const LoginPage = lazy(() => import('../pages/auth/LoginPage'))
const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage'))
const UserManagePage = lazy(() => import('../pages/admin/users/UserManagePage'))
const CourseListPage = lazy(() => import('../pages/course/CourseListPage'))
const ClassroomPage = lazy(() => import('../pages/course/ClassroomPage'))
const MaterialManagePage = lazy(() => import('../pages/course/MaterialManagePage'))
const InteractionEntryPage = lazy(() => import('../pages/interaction/InteractionEntryPage'))
const AttendancePage = lazy(() => import('../pages/interaction/AttendancePage'))
const StudentAttendPage = lazy(() => import('../pages/interaction/StudentAttendPage'))
const BarragePage = lazy(() => import('../pages/interaction/BarragePage'))
const RollCallPage = lazy(() => import('../pages/interaction/RollCallPage'))
const QuestionBankPage = lazy(() => import('../pages/exam/QuestionBankPage'))
const ExamPaperPage = lazy(() => import('../pages/exam/ExamPaperPage'))
const StudentAnswerPage = lazy(() => import('../pages/exam/StudentAnswerPage'))

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
          <Route
            path="/admin/users"
            element={
              <RequireAuth>
                <UserManagePage />
              </RequireAuth>
            }
          />
          <Route path="/courses" element={<RequireAuth><CourseListPage /></RequireAuth>} />
          <Route path="/course/:classId/classroom" element={<RequireAuth><ClassroomPage /></RequireAuth>} />
          <Route path="/materials" element={<RequireAuth><MaterialManagePage /></RequireAuth>} />
          {/* Sprint 3 互动教学路由 */}
          <Route path="/interaction" element={<RequireAuth><InteractionEntryPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/attendance" element={<RequireAuth><AttendancePage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/attend" element={<RequireAuth><StudentAttendPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/barrage" element={<RequireAuth><BarragePage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/roll-call" element={<RequireAuth><RollCallPage /></RequireAuth>} />
          {/* Sprint 4 题库与试卷管理路由 */}
          <Route path="/exam/question-banks" element={<RequireAuth><QuestionBankPage /></RequireAuth>} />
          <Route path="/exam/papers" element={<RequireAuth><ExamPaperPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/answer" element={<RequireAuth><StudentAnswerPage /></RequireAuth>} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
