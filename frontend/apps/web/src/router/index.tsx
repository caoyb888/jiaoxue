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
// Sprint 5 在线考试与监考（命名导出，需取 default）
const ExamEnterPage = lazy(() =>
  import('../pages/exam/ExamEnterPage').then((m) => ({ default: m.ExamEnterPage })),
)
const ExamAnswerPage = lazy(() =>
  import('../pages/exam/ExamAnswerPage').then((m) => ({ default: m.ExamAnswerPage })),
)
const MonitorDashboardPage = lazy(() =>
  import('../pages/exam/MonitorDashboardPage').then((m) => ({ default: m.MonitorDashboardPage })),
)
const ReviewPage = lazy(() =>
  import('../pages/exam/ReviewPage').then((m) => ({ default: m.ReviewPage })),
)
const ExamListPage = lazy(() =>
  import('../pages/exam/ExamListPage').then((m) => ({ default: m.ExamListPage })),
)
const FaceVerifyPage = lazy(() =>
  import('../pages/exam/FaceVerifyPage').then((m) => ({ default: m.FaceVerifyPage })),
)
const ExamResultPage = lazy(() =>
  import('../pages/exam/ExamResultPage').then((m) => ({ default: m.ExamResultPage })),
)
const PublishConfigPage = lazy(() =>
  import('../pages/exam/PublishConfigPage').then((m) => ({ default: m.PublishConfigPage })),
)

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
          {/* Sprint 5 在线考试与监考（正式考试，区别于上方随堂答题 StudentAnswerPage） */}
          <Route path="/exam/list" element={<RequireAuth><ExamListPage /></RequireAuth>} />
          <Route path="/exam/publish" element={<RequireAuth><PublishConfigPage /></RequireAuth>} />
          <Route path="/exam/monitor" element={<RequireAuth><MonitorDashboardPage /></RequireAuth>} />
          <Route path="/exam/:publishId/enter" element={<RequireAuth><ExamEnterPage /></RequireAuth>} />
          <Route path="/exam/:publishId/face-verify" element={<RequireAuth><FaceVerifyPage /></RequireAuth>} />
          <Route path="/exam/:publishId/answer" element={<RequireAuth><ExamAnswerPage /></RequireAuth>} />
          <Route path="/exam/:publishId/result" element={<RequireAuth><ExamResultPage /></RequireAuth>} />
          <Route path="/exam/:publishId/review/:studentId" element={<RequireAuth><ReviewPage /></RequireAuth>} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
