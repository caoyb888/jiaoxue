import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { examPublishApi, useMyClasses } from '@edu/api'
import type { ExamPublishVO, StudentExamListVO } from '@edu/api'
import { useAuthStore } from '@edu/store'

/**
 * 考试发布列表页（S5 枢纽页）。
 * 教师/管理员：查看自己发布的考试 → 进入监考大屏。
 * 学生：选择班级 → 查看待考列表 → 进入考试。
 */
export function ExamListPage() {
  const roles = useAuthStore((s) => s.roles)
  const isTeacher = roles.includes('ROLE_TEACHER') || roles.includes('ROLE_ADMIN')

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b bg-white px-6 py-4">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <div>
            <h1 className="text-lg font-semibold text-gray-900">在线考试</h1>
            <p className="text-sm text-gray-500">{isTeacher ? '管理与监考你发布的考试' : '查看并进入你的考试'}</p>
          </div>
          <div className="flex items-center gap-3">
            {isTeacher && (
              <Link
                to="/exam/publish"
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                发布考试
              </Link>
            )}
            <Link to="/dashboard" className="text-sm text-gray-500 hover:text-gray-700">
              返回工作台
            </Link>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-8">
        {isTeacher ? <TeacherExamList /> : <StudentExamList />}
      </main>
    </div>
  )
}

function fmt(dt: string | null): string {
  if (!dt) return '—'
  return dt.replace('T', ' ').slice(0, 16)
}

const STATUS_STYLE: Record<number, string> = {
  0: 'bg-gray-100 text-gray-600',
  1: 'bg-green-100 text-green-700',
  2: 'bg-gray-100 text-gray-500',
  3: 'bg-red-100 text-red-600',
}

function StatusBadge({ status, label }: { status: number; label: string }) {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLE[status] ?? 'bg-gray-100 text-gray-600'}`}>
      {label}
    </span>
  )
}

// ─── 教师视角 ─────────────────────────────────────────────────────────────────

function TeacherExamList() {
  const [status, setStatus] = useState<number | undefined>(undefined)
  const { data, isLoading, isError } = useQuery({
    queryKey: ['examPublishes', 'teacher', status],
    queryFn: () => examPublishApi.listByTeacher({ status, page: 1, size: 50 }),
    staleTime: 15_000,
  })

  const items = data?.list ?? []

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-2">
        {[
          { v: undefined, label: '全部' },
          { v: 0, label: '未开始' },
          { v: 1, label: '进行中' },
          { v: 2, label: '已结束' },
        ].map((f) => (
          <button
            key={f.label}
            onClick={() => setStatus(f.v)}
            className={`rounded-lg border px-3 py-1.5 text-sm ${
              status === f.v ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {isLoading && <p className="py-12 text-center text-sm text-gray-400">加载中…</p>}
      {isError && <p className="py-12 text-center text-sm text-red-500">加载失败，请稍后重试</p>}
      {!isLoading && !isError && items.length === 0 && (
        <div className="rounded-xl border border-dashed bg-white py-16 text-center">
          <p className="text-sm text-gray-400">暂无考试。请到「试卷管理」组卷后发布考试。</p>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {items.map((e: ExamPublishVO) => (
          <div key={e.id} className="rounded-xl border bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="font-semibold text-gray-900">试卷 #{e.paperId}</h3>
                <p className="mt-0.5 text-xs text-gray-400">发布 #{e.id} · 班级 #{e.classId}</p>
              </div>
              <StatusBadge status={e.status} label={e.statusLabel} />
            </div>
            <dl className="mt-3 space-y-1 text-sm text-gray-600">
              <div className="flex justify-between"><dt className="text-gray-400">时间</dt><dd>{fmt(e.startTime)} ~ {fmt(e.endTime)}</dd></div>
              <div className="flex justify-between"><dt className="text-gray-400">时长</dt><dd>{e.durationMin} 分钟</dd></div>
            </dl>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {e.hasPassword === true && <Tag>密码</Tag>}
              {e.enableMonitor === 1 && <Tag>监考</Tag>}
              {e.faceVerifyType > 0 && <Tag>人脸核验</Tag>}
              {e.shuffleQuestion === 1 && <Tag>题序乱序</Tag>}
            </div>
            <div className="mt-4 flex gap-2">
              <Link
                to={`/exam/monitor?publishId=${e.id}`}
                className="flex-1 rounded-lg bg-cyan-600 px-3 py-2 text-center text-sm font-medium text-white hover:bg-cyan-700"
              >
                进入监考
              </Link>
              <Link
                to={`/exam/${e.id}/ai-review`}
                className="flex-1 rounded-lg bg-violet-600 px-3 py-2 text-center text-sm font-medium text-white hover:bg-violet-700"
              >
                AI 批改
              </Link>
            </div>
          </div>
        ))}
      </div>
      {items.length > 0 && (
        <p className="text-center text-xs text-gray-400">
          阅卷请从「进入监考」选择具体学生进入；学生提交列表暂未独立成页（见待办）
        </p>
      )}
    </div>
  )
}

function Tag({ children }: { children: React.ReactNode }) {
  return <span className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-500">{children}</span>
}

// ─── 学生视角 ─────────────────────────────────────────────────────────────────

function StudentExamList() {
  const { data: classes, isLoading: classesLoading } = useMyClasses()
  const [classId, setClassId] = useState<number | null>(null)
  const effectiveClassId = classId ?? classes?.[0]?.id ?? null

  const { data: exams, isLoading, isError } = useQuery({
    queryKey: ['examPublishes', 'student', effectiveClassId],
    queryFn: () => examPublishApi.listForStudent(effectiveClassId!),
    enabled: !!effectiveClassId,
    staleTime: 15_000,
  })

  if (classesLoading) return <p className="py-12 text-center text-sm text-gray-400">加载班级…</p>
  if (!classes || classes.length === 0) {
    return <p className="py-12 text-center text-sm text-gray-400">你还没有加入任何教学班</p>
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <label className="text-sm text-gray-500">班级</label>
        <select
          value={effectiveClassId ?? ''}
          onChange={(e) => setClassId(Number(e.target.value))}
          className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {classes.map((c) => (
            <option key={c.id} value={c.id}>{c.className}（{c.courseName}）</option>
          ))}
        </select>
      </div>

      {isLoading && <p className="py-12 text-center text-sm text-gray-400">加载中…</p>}
      {isError && <p className="py-12 text-center text-sm text-red-500">加载失败，请稍后重试</p>}
      {!isLoading && !isError && (exams?.length ?? 0) === 0 && (
        <div className="rounded-xl border border-dashed bg-white py-16 text-center">
          <p className="text-sm text-gray-400">该班级暂无考试</p>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {exams?.map((e: StudentExamListVO) => {
          const canEnter = e.status === 1 && !e.submitted
          return (
            <div key={e.publishId} className="rounded-xl border bg-white p-5 shadow-sm">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-gray-900">{e.paperTitle}</h3>
                <StatusBadge status={e.status} label={e.statusLabel} />
              </div>
              <dl className="mt-3 space-y-1 text-sm text-gray-600">
                <div className="flex justify-between"><dt className="text-gray-400">时间</dt><dd>{fmt(e.startTime)} ~ {fmt(e.endTime)}</dd></div>
                <div className="flex justify-between"><dt className="text-gray-400">时长</dt><dd>{e.durationMin} 分钟</dd></div>
              </dl>
              <div className="mt-3 flex flex-wrap gap-1.5">
                {e.hasPassword === true && <Tag>需密码</Tag>}
                {e.faceVerifyType > 0 && <Tag>人脸核验</Tag>}
                {e.submitted && <Tag>已交卷</Tag>}
                {e.entered && !e.submitted && <Tag>已进入</Tag>}
              </div>
              <div className="mt-4">
                {canEnter ? (
                  <Link
                    to={`/exam/${e.publishId}/enter`}
                    className="block rounded-lg bg-blue-600 px-3 py-2 text-center text-sm font-medium text-white hover:bg-blue-700"
                  >
                    {e.entered ? '继续考试' : '进入考试'}
                  </Link>
                ) : (
                  <button
                    disabled
                    className="block w-full cursor-not-allowed rounded-lg bg-gray-100 px-3 py-2 text-center text-sm font-medium text-gray-400"
                  >
                    {e.submitted ? '已交卷' : e.status === 0 ? '未开始' : '已结束'}
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
