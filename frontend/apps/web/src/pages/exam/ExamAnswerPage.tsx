import { useState, useCallback, useEffect, useRef } from 'react'
import { useParams, useLocation, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { examStudentApi } from '@edu/api'
import type { ExamEnterVO, AnswerItemDTO, PaperQuestionDetailVO } from '@edu/api'
import { ExamWatermark } from './components/ExamWatermark'
import { useExamAutoSave, loadExamDraft } from '@/hooks/useExamAutoSave'
import { useAutoSubmit } from '@/hooks/useAutoSubmit'
import { useExamMonitor } from '@/hooks/useExamMonitor'
import { useAuthStore } from '@edu/store'

/**
 * 考试答题页（S5-11 水印 + S5-12 监考 + S5-09 自动保存 + S5-10 自动交卷打散）。
 */
export function ExamAnswerPage() {
  const { publishId } = useParams<{ publishId: string }>()
  const { state } = useLocation()
  const navigate = useNavigate()
  const enterData = state?.enterData as ExamEnterVO | undefined

  const { userId, realName } = useAuthStore()
  const pid = Number(publishId)

  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [submitted, setSubmitted] = useState(false)
  const [currentPage, setCurrentPage] = useState(1)
  const [questions, setQuestions] = useState<PaperQuestionDetailVO[]>(enterData?.questions ?? [])

  // 恢复 IndexedDB 草稿
  useEffect(() => {
    if (!userId || !pid) return
    loadExamDraft(pid, userId).then((draft) => {
      if (draft && draft.length > 0) {
        const restored: Record<number, string> = {}
        draft.forEach((a) => { restored[a.questionId] = a.answerContent })
        setAnswers(restored)
      }
    })
  }, [pid, userId])

  // 当前答案转 AnswerItemDTO[]
  const getAnswers = useCallback((): AnswerItemDTO[] =>
    Object.entries(answers).map(([qId, content]) => ({
      questionId: Number(qId),
      answerContent: content,
    })), [answers])

  // S5-09: 15s 自动草稿保存
  const { clearDraft } = useExamAutoSave(pid, userId ?? 0, getAnswers, !submitted)

  // S5-12: 监考事件监听
  useExamMonitor(pid, {
    allowCopy: enterData?.allowCopy === 1,
    enabled: !submitted && !!enterData?.enableMonitor,
  })

  const submitMutation = useMutation({
    mutationFn: (submitType: 'MANUAL' | 'AUTO') =>
      examStudentApi.submitExam(pid, {
        answers: getAnswers(),
        submitType,
        clientSubmitAt: new Date().toISOString(),
      }),
    onSuccess: async () => {
      setSubmitted(true)
      await clearDraft()
      navigate(`/exam/${pid}/result`)
    },
  })

  // S5-10: 自动交卷打散（用 userId 末两位模拟学号末两位）
  useAutoSubmit(
    enterData?.endTime ?? null,
    String(userId ?? 0).padStart(2, '0'),
    (type) => { if (!submitted) submitMutation.mutate(type) },
    !submitted,
  )

  // 心跳（30s）
  const heartbeatRef = useRef<ReturnType<typeof setInterval>>()
  useEffect(() => {
    if (submitted || !pid) return
    heartbeatRef.current = setInterval(() => {
      examStudentApi.heartbeat(pid).catch(() => {})
    }, 30_000)
    return () => clearInterval(heartbeatRef.current)
  }, [pid, submitted])

  if (!enterData) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">请先进入考试</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 pb-24">
      {/* 全屏水印（S5-11） */}
      {realName && <ExamWatermark name={realName} studentId={String(userId ?? '')} />}

      {/* 顶部信息栏 */}
      <header className="sticky top-0 z-30 bg-white shadow-sm px-4 py-3 flex items-center justify-between">
        <div className="text-sm text-gray-600">
          第 {currentPage}/{enterData.totalPages} 页 · 共 {enterData.totalQuestions} 题
        </div>
        <div className="text-sm font-mono text-red-600">
          {enterData.endTime ? new Date(enterData.endTime).toLocaleTimeString() : '--'} 结束
        </div>
      </header>

      {/* 题目列表 */}
      <main className="max-w-2xl mx-auto px-4 py-6 space-y-6">
        {questions.map((pq, idx) => (
          <QuestionCard
            key={pq.id}
            index={idx + 1 + (currentPage - 1) * 10}
            pq={pq}
            value={answers[pq.questionId] ?? ''}
            onChange={(v) => setAnswers((prev) => ({ ...prev, [pq.questionId]: v }))}
          />
        ))}
      </main>

      {/* 底部翻页+交卷 */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t px-4 py-3 flex items-center justify-between z-30">
        <div className="flex gap-2">
          <button
            onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
            disabled={currentPage <= 1}
            className="px-4 py-2 text-sm border rounded-lg disabled:opacity-40"
          >
            上一页
          </button>
          <button
            onClick={() => {
              const nextPage = currentPage + 1
              setCurrentPage(nextPage)
              examStudentApi.getQuestionsPage(pid, nextPage).then((r) => {
                setQuestions(r.questions)
              })
            }}
            disabled={currentPage >= enterData.totalPages}
            className="px-4 py-2 text-sm border rounded-lg disabled:opacity-40"
          >
            下一页
          </button>
        </div>
        <button
          onClick={() => { if (window.confirm('确认交卷？交卷后无法修改。')) submitMutation.mutate('MANUAL') }}
          disabled={submitted || submitMutation.isPending}
          className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white text-sm font-semibold px-6 py-2 rounded-lg transition-colors"
        >
          {submitted ? '已交卷' : submitMutation.isPending ? '提交中...' : '交卷'}
        </button>
      </div>
    </div>
  )
}

function QuestionCard({
  index,
  pq,
  value,
  onChange,
}: {
  index: number
  pq: PaperQuestionDetailVO
  value: string
  onChange: (v: string) => void
}) {
  const q = pq.question
  const isObjective = [1, 2, 3, 6].includes(q.type)

  return (
    <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
      <div className="flex items-start gap-2">
        <span className="text-sm font-semibold text-blue-600 shrink-0">第{index}题</span>
        <p className="text-sm text-gray-800 leading-relaxed">{q.content}</p>
        <span className="ml-auto text-xs text-gray-400 shrink-0">{pq.score}分</span>
      </div>

      {isObjective && q.options.length > 0 ? (
        <div className="space-y-2">
          {q.options.map((opt) => (
            <label key={opt.id} className="flex items-center gap-3 p-3 rounded-lg hover:bg-gray-50 cursor-pointer">
              <input
                type={q.type === 2 ? 'checkbox' : 'radio'}
                name={`q_${pq.questionId}`}
                value={opt.optionLabel}
                checked={
                  q.type === 2
                    ? value.split('').includes(opt.optionLabel)
                    : value === opt.optionLabel
                }
                onChange={() => {
                  if (q.type === 2) {
                    const set = new Set(value.split(''))
                    set.has(opt.optionLabel) ? set.delete(opt.optionLabel) : set.add(opt.optionLabel)
                    onChange([...set].sort().join(''))
                  } else {
                    onChange(opt.optionLabel)
                  }
                }}
                className="accent-blue-600"
              />
              <span className="text-sm text-gray-700">
                <span className="font-medium">{opt.optionLabel}.</span> {opt.content}
              </span>
            </label>
          ))}
        </div>
      ) : (
        <textarea
          rows={5}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="请输入答案..."
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      )}
    </div>
  )
}
