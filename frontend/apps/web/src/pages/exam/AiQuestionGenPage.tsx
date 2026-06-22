import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  useQuestionBanks,
  QUESTION_TYPES,
  useStartQuestionGen,
  useQuestionGenStatus,
  useGeneratedQuestions,
} from '@edu/api'

const TYPE_OPTIONS = [1, 2, 3, 4, 5]
const STATUS_LABEL: Record<string, string> = {
  PENDING: '排队中',
  GENERATING: 'AI 出题中…',
  DONE: '已完成',
  FAILED: '生成失败',
}

/**
 * 一键 AI 出题页（S6-13）。表单 → 异步任务轮询 → 生成题目预览（题目已入库）。
 */
export function AiQuestionGenPage() {
  const { data: banks = [] } = useQuestionBanks()
  const start = useStartQuestionGen()

  const [bankId, setBankId] = useState<number | ''>('')
  const [topic, setTopic] = useState('')
  const [types, setTypes] = useState<number[]>([1])
  const [count, setCount] = useState(5)
  const [difficulty, setDifficulty] = useState(3)
  const [taskId, setTaskId] = useState<string | null>(null)

  const { data: task } = useQuestionGenStatus(taskId ?? undefined)
  const isDone = task?.status === 'DONE'
  const { data: questions = [] } = useGeneratedQuestions(taskId ?? undefined, isDone)

  const toggleType = (t: number) =>
    setTypes((prev) => (prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t]))

  const submit = () => {
    if (bankId === '' || !topic.trim() || types.length === 0) return
    start.mutate(
      { bankId: Number(bankId), topic: topic.trim(), types, count, difficulty },
      { onSuccess: (id) => setTaskId(id) },
    )
  }

  const reset = () => {
    setTaskId(null)
    start.reset()
  }

  return (
    <div className="max-w-3xl mx-auto py-8 px-4 space-y-6">
      <header>
        <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
          <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-amber-100 text-amber-700 text-sm font-black">
            AI
          </span>
          一键智能出题
        </h1>
        <p className="text-xs text-gray-500 mt-1">填写要求，AI 异步生成题目并自动入库到所选题库</p>
      </header>

      {/* 表单 */}
      {!taskId && (
        <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="text-xs font-medium text-gray-600">目标题库</label>
              <select
                value={bankId}
                onChange={(e) => setBankId(e.target.value === '' ? '' : Number(e.target.value))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
              >
                <option value="">请选择题库</option>
                {banks.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.bankName}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium text-gray-600">知识点 / 主题</label>
              <input
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                placeholder="如：TCP 三次握手"
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-medium text-gray-600">题型</label>
            <div className="flex flex-wrap gap-2">
              {TYPE_OPTIONS.map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => toggleType(t)}
                  className={`text-sm px-3 py-1.5 rounded-lg border transition-colors ${
                    types.includes(t)
                      ? 'bg-amber-600 border-amber-600 text-white'
                      : 'border-gray-300 text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  {QUESTION_TYPES[t]}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="text-xs font-medium text-gray-600">题目数量（1–20）</label>
              <input
                type="number"
                min={1}
                max={20}
                value={count}
                onChange={(e) => setCount(Math.min(20, Math.max(1, Number(e.target.value) || 1)))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-medium text-gray-600">难度（1 易 – 5 难）</label>
              <input
                type="number"
                min={1}
                max={5}
                value={difficulty}
                onChange={(e) => setDifficulty(Math.min(5, Math.max(1, Number(e.target.value) || 1)))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
              />
            </div>
          </div>

          <button
            onClick={submit}
            disabled={start.isPending || bankId === '' || !topic.trim() || types.length === 0}
            className="bg-amber-600 hover:bg-amber-700 disabled:bg-gray-300 text-white text-sm font-semibold px-5 py-2.5 rounded-lg transition-colors"
          >
            {start.isPending ? '提交中…' : '开始生成'}
          </button>
        </div>
      )}

      {/* 进度 + 结果 */}
      {taskId && (
        <div className="space-y-4">
          <div className="bg-white rounded-xl shadow-sm p-5 flex items-center justify-between">
            <div className="flex items-center gap-3">
              {task && !['DONE', 'FAILED'].includes(task.status) && (
                <span className="w-4 h-4 border-2 border-amber-500 border-t-transparent rounded-full animate-spin" />
              )}
              <div>
                <p className="text-sm font-medium text-gray-800">
                  {task ? STATUS_LABEL[task.status] ?? task.status : '查询中…'}
                </p>
                {isDone && (
                  <p className="text-xs text-gray-500">已生成并入库 {task?.generatedCount ?? 0} 道题</p>
                )}
                {task?.status === 'FAILED' && (
                  <p className="text-xs text-rose-600">{task.errorMsg ?? '生成失败，请重试'}</p>
                )}
              </div>
            </div>
            <button
              onClick={reset}
              className="text-sm text-gray-500 hover:text-gray-700 underline"
            >
              重新出题
            </button>
          </div>

          {isDone && (
            <>
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold text-gray-700">生成题目预览</h2>
                {task && (
                  <Link
                    to="/exam/question-banks"
                    className="text-xs text-amber-700 hover:underline"
                  >
                    前往题库管理 →
                  </Link>
                )}
              </div>
              {questions.map((q, i) => (
                <div key={q.id} className="bg-white rounded-xl shadow-sm p-5 space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">
                      {QUESTION_TYPES[q.type] ?? '题目'}
                    </span>
                    <span className="text-xs text-gray-400">第 {i + 1} 题 · 难度 {q.difficulty}</span>
                  </div>
                  <p className="text-sm text-gray-800 whitespace-pre-wrap">{q.content}</p>
                  {q.answer && (
                    <p className="text-xs text-gray-600">
                      <span className="font-medium text-green-600">答案：</span>
                      {q.answer}
                    </p>
                  )}
                  {q.analysis && (
                    <p className="text-xs text-gray-500">
                      <span className="font-medium">解析：</span>
                      {q.analysis}
                    </p>
                  )}
                </div>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  )
}
