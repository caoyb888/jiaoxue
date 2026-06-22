import { useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  useAiReviewResults,
  useTriggerAiReview,
  useOverrideReview,
  type AiReviewResultVO,
} from '@edu/api'

/**
 * AI 批改结果展示页（S6-10）。
 * 展示主观题 AI 评分 / 批注 / 错因分析卡片，教师可在此基础上修改并覆盖为终评。
 */
export function AiReviewPage() {
  const { publishId } = useParams<{ publishId: string }>()
  const { data: results = [], isLoading } = useAiReviewResults(publishId)
  const trigger = useTriggerAiReview(publishId ?? '')

  const parsedCount = results.filter((r) => r.parsed).length
  const pendingCount = results.length - parsedCount

  return (
    <div className="max-w-3xl mx-auto py-8 px-4 space-y-6">
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-violet-100 text-violet-700 text-sm font-black">
              AI
            </span>
            智能批改结果
          </h1>
          <p className="text-xs text-gray-500 mt-1">
            共 {results.length} 题 · 已解析 {parsedCount}
            {pendingCount > 0 && <span className="text-amber-600"> · 待人工 {pendingCount}</span>}
          </p>
        </div>
        <button
          onClick={() => trigger.mutate()}
          disabled={trigger.isPending}
          className="self-start sm:self-auto bg-violet-600 hover:bg-violet-700 disabled:bg-gray-300 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          {trigger.isPending ? '已提交…' : '重新触发 AI 批改'}
        </button>
      </header>

      {trigger.isSuccess && (
        <p className="text-xs text-violet-600 bg-violet-50 rounded-lg px-3 py-2">
          AI 批改任务已提交，完成后结果将自动刷新（可稍后重进本页查看）。
        </p>
      )}

      {isLoading && <p className="text-gray-400 text-sm">加载中…</p>}

      {!isLoading && results.length === 0 && (
        <p className="text-gray-400 text-sm">
          暂无 AI 批改结果。点击右上角「重新触发 AI 批改」开始。
        </p>
      )}

      {results.map((r) => (
        <ReviewResultCard key={r.answerId} result={r} publishId={publishId ?? ''} />
      ))}
    </div>
  )
}

function ReviewResultCard({
  result,
  publishId,
}: {
  result: AiReviewResultVO
  publishId: string
}) {
  const override = useOverrideReview(publishId)
  const [score, setScore] = useState(result.score ?? '')
  const [comment, setComment] = useState(result.comment ?? '')

  const handleSave = () => {
    override.mutate({ answerId: result.answerId, dto: { score, comment } })
  }

  return (
    <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700">
          题目 #{result.questionId} · 学生 {result.studentId}
        </span>
        {result.parsed ? (
          <span className="text-xs px-2 py-0.5 rounded-full bg-violet-100 text-violet-700">
            AI 已批改
          </span>
        ) : (
          <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">
            待人工复核
          </span>
        )}
      </div>

      {/* AI 评分与分析 */}
      <div className="bg-violet-50/70 border border-violet-100 rounded-lg p-4 space-y-2">
        <div className="flex items-baseline gap-2">
          <span className="text-xs text-violet-500 font-medium">AI 评分</span>
          <span className="text-lg font-bold text-violet-700">
            {result.score ?? '—'}
          </span>
          {result.maxScore && (
            <span className="text-xs text-gray-400">/ {result.maxScore}</span>
          )}
        </div>
        {result.comment && (
          <p className="text-sm text-gray-700">
            <span className="text-xs text-violet-500 font-medium">批注：</span>
            {result.comment}
          </p>
        )}
        {result.errorReason && (
          <p className="text-sm text-gray-600">
            <span className="text-xs text-rose-500 font-medium">错因：</span>
            {result.errorReason}
          </p>
        )}
      </div>

      {/* 教师覆盖 */}
      <div className="space-y-3">
        <div className="grid grid-cols-3 gap-3">
          <div className="space-y-1">
            <label className="text-xs font-medium text-gray-600">终评得分</label>
            <input
              type="number"
              min="0"
              step="0.5"
              value={score}
              onChange={(e) => setScore(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-violet-500"
            />
          </div>
          <div className="col-span-2 space-y-1">
            <label className="text-xs font-medium text-gray-600">终评评语</label>
            <input
              type="text"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="可采纳 AI 批注或修改后保存"
              className="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-violet-500"
            />
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleSave}
            disabled={!score || override.isPending}
            className="bg-gray-900 hover:bg-black disabled:bg-gray-300 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            {override.isPending ? '保存中…' : '覆盖为终评'}
          </button>
          {override.isSuccess && (
            <span className="text-xs text-green-600">已保存为教师终评</span>
          )}
          {override.isError && (
            <span className="text-xs text-rose-600">保存失败，请重试</span>
          )}
        </div>
      </div>
    </div>
  )
}
