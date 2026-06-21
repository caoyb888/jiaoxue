import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { examStudentApi } from '@edu/api'
import type { StudentAnswerVO } from '@edu/api'

/**
 * 考试结果页（S5）。
 * 交卷后跳转至此（/exam/:publishId/result），展示得分汇总与逐题批改明细。
 * 含主观题时，未批改部分提示"待教师批改"。
 */
export function ExamResultPage() {
  const { publishId } = useParams<{ publishId: string }>()
  const pid = Number(publishId)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['exam', 'my-score', pid],
    queryFn: () => examStudentApi.myScore(pid),
    enabled: !!pid,
    staleTime: 10_000,
  })

  const pending = data ? data.totalQuestions - data.gradedQuestions : 0

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b bg-white px-6 py-4">
        <div className="mx-auto flex max-w-3xl items-center justify-between">
          <h1 className="text-lg font-semibold text-gray-900">考试结果</h1>
          <div className="flex gap-3 text-sm">
            <Link to="/exam/list" className="text-gray-500 hover:text-gray-700">考试列表</Link>
            <Link to="/dashboard" className="text-gray-500 hover:text-gray-700">工作台</Link>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 py-8">
        {isLoading && <p className="py-16 text-center text-sm text-gray-400">成绩加载中…</p>}
        {isError && <p className="py-16 text-center text-sm text-red-500">成绩加载失败，请稍后重试</p>}

        {data && (
          <div className="space-y-6">
            {/* 得分总览 */}
            <div className="rounded-xl border bg-white p-6 shadow-sm">
              <div className="flex flex-col items-center">
                <p className="text-sm text-gray-500">已批改得分</p>
                <p className="mt-1 text-4xl font-bold text-blue-600">
                  {data.totalScore ?? 0}
                  <span className="ml-1 text-lg font-normal text-gray-400">/ {data.fullScore ?? '—'}</span>
                </p>
              </div>
              <div className="mt-5 grid grid-cols-3 gap-3 text-center">
                <div className="rounded-lg bg-gray-50 py-3">
                  <p className="text-xs text-gray-500">客观题答对</p>
                  <p className="mt-0.5 text-lg font-semibold text-gray-900">{data.correctCount}</p>
                </div>
                <div className="rounded-lg bg-gray-50 py-3">
                  <p className="text-xs text-gray-500">已批改</p>
                  <p className="mt-0.5 text-lg font-semibold text-gray-900">{data.gradedQuestions} / {data.totalQuestions}</p>
                </div>
                <div className="rounded-lg bg-gray-50 py-3">
                  <p className="text-xs text-gray-500">待批改</p>
                  <p className={`mt-0.5 text-lg font-semibold ${pending > 0 ? 'text-orange-500' : 'text-gray-900'}`}>{pending}</p>
                </div>
              </div>
              {pending > 0 && (
                <p className="mt-4 rounded-lg bg-amber-50 px-3 py-2 text-center text-sm text-amber-700">
                  含 {pending} 道主观题待教师批改，最终成绩以批改完成后为准
                </p>
              )}
            </div>

            {/* 逐题明细 */}
            <div className="space-y-3">
              <h2 className="text-sm font-medium uppercase tracking-wide text-gray-500">答题明细</h2>
              {data.answers.map((a, idx) => (
                <AnswerRow key={a.id} answer={a} index={idx + 1} />
              ))}
              {data.answers.length === 0 && (
                <p className="rounded-xl border border-dashed bg-white py-12 text-center text-sm text-gray-400">无答题记录</p>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  )
}

function judge(a: StudentAnswerVO): { label: string; cls: string } {
  if (a.reviewStatus === 0) return { label: '待批改', cls: 'bg-amber-100 text-amber-700' }
  if (a.isCorrect === 1) return { label: '正确', cls: 'bg-green-100 text-green-700' }
  if (a.isCorrect === 0) return { label: '错误', cls: 'bg-red-100 text-red-600' }
  return { label: '已批改', cls: 'bg-blue-100 text-blue-700' }
}

function AnswerRow({ answer, index }: { answer: StudentAnswerVO; index: number }) {
  const j = judge(answer)
  return (
    <div className="rounded-xl border bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <span className="font-medium text-gray-900">第 {index} 题</span>
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">{answer.score ?? '—'} 分</span>
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${j.cls}`}>{j.label}</span>
        </div>
      </div>
      <div className="mt-2 text-sm text-gray-600">
        <span className="text-gray-400">你的作答：</span>
        <span className="break-words">{answer.answerContent || '（未作答）'}</span>
      </div>
      {answer.comment && (
        <div className="mt-2 rounded-lg bg-gray-50 px-3 py-2 text-sm text-gray-600">
          <span className="text-gray-400">评语：</span>{answer.comment}
        </div>
      )}
    </div>
  )
}
