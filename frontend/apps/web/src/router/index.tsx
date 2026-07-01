import React, { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useAuthStore } from '@edu/store'

const LoginPage = lazy(() => import('../pages/auth/LoginPage'))
const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage'))
const UserManagePage = lazy(() => import('../pages/admin/users/UserManagePage'))
const CourseListPage = lazy(() => import('../pages/course/CourseListPage'))
const ClassroomPage = lazy(() => import('../pages/course/ClassroomPage'))
const LiveClassPage = lazy(() => import('../pages/live/LiveClassPage'))
const GroupDiscussionPage = lazy(() => import('../pages/discussion/GroupDiscussionPage'))
const PresentationReviewPage = lazy(() => import('../pages/presentation/PresentationReviewPage'))
const GradeManagePage = lazy(() => import('../pages/grade/GradeManagePage'))
const NoticeCenterPage = lazy(() => import('../pages/notice/NoticeCenterPage'))
const MaterialManagePage = lazy(() => import('../pages/course/MaterialManagePage'))
const ClassHistoryPage = lazy(() => import('../pages/stat/ClassHistoryPage'))
const WarnListPage = lazy(() => import('../pages/stat/WarnListPage'))
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
const AiReviewPage = lazy(() =>
  import('../pages/exam/AiReviewPage').then((m) => ({ default: m.AiReviewPage })),
)
const MindmapPage = lazy(() =>
  import('../pages/ai/MindmapPage').then((m) => ({ default: m.MindmapPage })),
)
const DialoguePage = lazy(() =>
  import('../pages/ai/DialoguePage').then((m) => ({ default: m.DialoguePage })),
)
const DialogueOverviewPage = lazy(() =>
  import('../pages/ai/DialogueOverviewPage').then((m) => ({ default: m.DialogueOverviewPage })),
)
const AiQuestionGenPage = lazy(() =>
  import('../pages/exam/AiQuestionGenPage').then((m) => ({ default: m.AiQuestionGenPage })),
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
          {/* Sprint 7 教学统计 */}
          <Route path="/stat/class-history" element={<RequireAuth><ClassHistoryPage /></RequireAuth>} />
          <Route path="/stat/warn" element={<RequireAuth><WarnListPage /></RequireAuth>} />
          {/* Sprint 3 互动教学路由 */}
          <Route path="/interaction" element={<RequireAuth><InteractionEntryPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/attendance" element={<RequireAuth><AttendancePage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/attend" element={<RequireAuth><StudentAttendPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/barrage" element={<RequireAuth><BarragePage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/roll-call" element={<RequireAuth><RollCallPage /></RequireAuth>} />
          {/* Sprint 4 题库与试卷管理路由 */}
          <Route path="/exam/question-banks" element={<RequireAuth><QuestionBankPage /></RequireAuth>} />
          <Route path="/exam/ai-generate" element={<RequireAuth><AiQuestionGenPage /></RequireAuth>} />
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
          <Route path="/exam/:publishId/ai-review" element={<RequireAuth><AiReviewPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/mindmap" element={<RequireAuth><MindmapPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/ai-chat" element={<RequireAuth><DialoguePage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/ai-chat/overview" element={<RequireAuth><DialogueOverviewPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/live" element={<RequireAuth><LiveClassPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/discussion" element={<RequireAuth><GroupDiscussionPage /></RequireAuth>} />
          <Route path="/lesson/:lessonId/presentation" element={<RequireAuth><PresentationReviewPage /></RequireAuth>} />
          <Route path="/grade" element={<RequireAuth><GradeManagePage /></RequireAuth>} />
          <Route path="/notice" element={<RequireAuth><NoticeCenterPage /></RequireAuth>} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
