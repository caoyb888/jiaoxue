import React, { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { http } from '@edu/api'
import { QUESTION_TYPES, OPTION_TYPES } from '@edu/api'
import type { QuestionOptionVO } from '@edu/api'
import { RichTextView } from './components/RichTextEditor'

// ─── Types ─────────────────────────────────────────────────────────────────

interface ActiveQuestionVO {
  lessonQuestionId: number
  lessonId: number
  questionId: number
  questionType: number
  content: string
  options: QuestionOptionVO[]
  openedAt: string
}

interface AnswerResultVO {
  lessonQuestionId: number
  questionType: number
  isCorrect: boolean | null
  correctAnswer: string | null
  analysis: string | null
  myAnswer: string
}

// ─── API ───────────────────────────────────────────────────────────────────

// 注意：http 拦截器已解包 Result→data，故方法直接 resolve 业务数据。
const studentExamApi = {
  getActiveQuestion: (lessonId: string): Promise<ActiveQuestionVO | null> =>
    http.get(`/v1/exam/lessons/${lessonId}/active-question`),

  submitAnswer: (lessonId: string, dto: { lessonQuestionId: number; answer: string }): Promise<AnswerResultVO> =>
    http.post(`/v1/exam/lessons/${lessonId}/answers`, dto),
}

// ─── Page ──────────────────────────────────────────────────────────────────

export default function StudentAnswerPage() {
  const { lessonId } = useParams<{ lessonId: string }>()

  const { data: activeRes, isLoading } = useQuery({
    queryKey: ['student', 'active-question', lessonId],
    queryFn: () => studentExamApi.getActiveQuestion(lessonId!),
    refetchInterval: 5_000,
    enabled: !!lessonId,
  })

  const activeQuestion = activeRes

  const submitAnswer = useMutation({
    mutationFn: (dto: { lessonQuestionId: number; answer: string }) =>
      studentExamApi.submitAnswer(lessonId!, dto),
  })

  const [result, setResult] = useState<AnswerResultVO | null>(null)
  const [lastQuestionId, setLastQuestionId] = useState<number | null>(null)

  useEffect(() => {
    if (activeQuestion && activeQuestion.lessonQuestionId !== lastQuestionId) {
      setResult(null)
      setLastQuestionId(activeQuestion.lessonQuestionId)
    }
  }, [activeQuestion, lastQuestionId])

  function handleSubmit(answer: string) {
    if (!activeQuestion) return
    submitAnswer.mutate(
      { lessonQuestionId: activeQuestion.lessonQuestionId, answer },
      { onSuccess: (res) => setResult(res ?? null) }
    )
  }

  if (isLoading) {
    return <PageShell><LoadingSpinner /></PageShell>
  }

  if (!activeQuestion) {
    return (
      <PageShell>
        <div className="flex flex-col items-center justify-center h-64 gap-4 text-gray-400">
          <svg className="h-16 w-16 opacity-30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p className="text-sm">暂无进行中的题目，等待教师出题…</p>
          <p className="text-xs text-gray-300">每 5 秒自动刷新</p>
        </div>
      </PageShell>
    )
  }

  return (
    <PageShell>
      <div className="w-full max-w-lg mx-auto space-y-4">
        {/* 题目信息 */}
        <div className="flex items-center gap-2 mb-2">
          <TypeBadge type={activeQuestion.questionType} />
          <span className="text-xs text-gray-400">题目已开放作答</span>
        </div>

        {/* 题干 */}
        <div className="rounded-xl bg-white p-5 shadow-sm">
          <RichTextView html={activeQuestion.content} />
        </div>

        {/* 答题区域（未提交状态） */}
        {!result && (
          <AnswerInput
            question={activeQuestion}
            onSubmit={handleSubmit}
            isSubmitting={submitAnswer.isPending}
          />
        )}

        {/* 结果展示 */}
        {result && (
          <AnswerResult result={result} />
        )}
      </div>
    </PageShell>
  )
}

// ─── AnswerInput: 6 种题型渲染 ────────────────────────────────────────────

function AnswerInput({ question, onSubmit, isSubmitting }: {
  question: ActiveQuestionVO
  onSubmit: (answer: string) => void
  isSubmitting: boolean
}) {
  const [selected, setSelected] = useState<string[]>([])
  const [text, setText] = useState('')

  const isObjective = OPTION_TYPES.has(question.questionType) || question.questionType === 3

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    let answer = ''
    if (question.questionType === 1) {
      if (!selected[0]) return
      answer = selected[0]
    } else if (question.questionType === 2) {
      if (selected.length === 0) return
      answer = selected.sort().join(',')
    } else if (question.questionType === 3) {
      if (!selected[0]) return
      answer = selected[0]
    } else if (question.questionType === 6) {
      if (!selected[0]) return
      answer = selected[0]
    } else {
      if (!text.trim()) return
      answer = text.trim()
    }
    onSubmit(answer)
  }

  function toggleOption(label: string) {
    if (question.questionType === 2) {
      setSelected((prev) =>
        prev.includes(label) ? prev.filter((l) => l !== label) : [...prev, label]
      )
    } else {
      setSelected([label])
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {/* 单选 / 多选 / 投票 */}
      {OPTION_TYPES.has(question.questionType) && (
        <div className="space-y-2">
          {question.options.map((opt) => {
            const isSelected = selected.includes(opt.optionLabel)
            const isMulti = question.questionType === 2
            return (
              <button
                key={opt.id}
                type="button"
                onClick={() => toggleOption(opt.optionLabel)}
                className={`w-full flex items-center gap-3 rounded-xl border-2 px-4 py-3 text-left transition-all ${
                  isSelected
                    ? 'border-blue-500 bg-blue-50'
                    : 'border-gray-200 bg-white hover:border-blue-300'
                }`}
              >
                <span className={`flex h-6 w-6 flex-shrink-0 items-center justify-center border-2 text-xs font-bold transition-colors ${
                  isMulti ? 'rounded-md' : 'rounded-full'
                } ${
                  isSelected
                    ? 'border-blue-500 bg-blue-500 text-white'
                    : 'border-gray-300 text-gray-500'
                }`}>
                  {opt.optionLabel}
                </span>
                <span className={`text-sm ${isSelected ? 'font-medium text-blue-700' : 'text-gray-700'}`}>
                  {opt.content}
                </span>
              </button>
            )
          })}
        </div>
      )}

      {/* 判断题 */}
      {question.questionType === 3 && (
        <div className="flex gap-3">
          {[
            { label: '正确', value: 'true' },
            { label: '错误', value: 'false' },
          ].map(({ label, value }) => (
            <button
              key={value}
              type="button"
              onClick={() => setSelected([value])}
              className={`flex-1 rounded-xl border-2 py-3 text-sm font-medium transition-all ${
                selected[0] === value
                  ? 'border-blue-500 bg-blue-500 text-white'
                  : 'border-gray-200 bg-white text-gray-700 hover:border-blue-300'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      )}

      {/* 填空题 */}
      {question.questionType === 4 && (
        <textarea
          rows={3}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="请输入答案..."
          className="w-full rounded-xl border-2 border-gray-200 px-4 py-3 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
        />
      )}

      {/* 主观题 */}
      {question.questionType === 5 && (
        <textarea
          rows={6}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="请在此输入作答内容..."
          className="w-full rounded-xl border-2 border-gray-200 px-4 py-3 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
        />
      )}

      <button
        type="submit"
        disabled={isSubmitting || (isObjective && selected.length === 0)}
        className="w-full rounded-xl bg-blue-500 py-3 text-sm font-medium text-white hover:bg-blue-600 active:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50 transition-colors"
      >
        {isSubmitting ? '提交中...' : '提交答案'}
      </button>
    </form>
  )
}

// ─── AnswerResult ──────────────────────────────────────────────────────────

function AnswerResult({ result }: { result: AnswerResultVO }) {
  const isObjective = result.isCorrect !== null

  return (
    <div className="space-y-3">
      {/* 结果卡片 */}
      <div className={`rounded-xl p-5 ${
        isObjective
          ? result.isCorrect
            ? 'bg-green-50 border-2 border-green-400'
            : 'bg-red-50 border-2 border-red-400'
          : 'bg-blue-50 border-2 border-blue-400'
      }`}>
        <div className="flex items-center gap-3">
          <span className="text-2xl">
            {isObjective
              ? result.isCorrect ? '✅' : '❌'
              : '📝'}
          </span>
          <div>
            <p className={`text-base font-semibold ${
              isObjective
                ? result.isCorrect ? 'text-green-700' : 'text-red-700'
                : 'text-blue-700'
            }`}>
              {isObjective
                ? result.isCorrect ? '回答正确！' : '回答错误'
                : '已提交，等待教师批改'}
            </p>
            <p className="text-sm text-gray-600 mt-0.5">
              我的答案：<span className="font-medium">{result.myAnswer}</span>
            </p>
          </div>
        </div>

        {isObjective && !result.isCorrect && result.correctAnswer && (
          <div className="mt-3 rounded-lg bg-white/60 px-3 py-2">
            <p className="text-sm text-gray-700">
              正确答案：<span className="font-medium text-green-700">{result.correctAnswer}</span>
            </p>
          </div>
        )}
      </div>

      {/* 解析 */}
      {result.analysis && (
        <div className="rounded-xl bg-amber-50 border border-amber-200 p-4">
          <p className="text-xs font-semibold text-amber-700 mb-1.5">题目解析</p>
          <p className="text-sm text-amber-900 leading-relaxed">{result.analysis}</p>
        </div>
      )}
    </div>
  )
}

// ─── Utility components ────────────────────────────────────────────────────

function TypeBadge({ type }: { type: number }) {
  const colorMap: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-purple-100 text-purple-700',
    3: 'bg-green-100 text-green-700',
    4: 'bg-yellow-100 text-yellow-700',
    5: 'bg-orange-100 text-orange-700',
    6: 'bg-pink-100 text-pink-700',
  }
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${colorMap[type] ?? 'bg-gray-100 text-gray-700'}`}>
      {QUESTION_TYPES[type] ?? '未知'}
    </span>
  )
}

function LoadingSpinner() {
  return (
    <div className="flex h-48 items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
    </div>
  )
}

function PageShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-gray-50 px-4 py-6">
      {children}
    </div>
  )
}
