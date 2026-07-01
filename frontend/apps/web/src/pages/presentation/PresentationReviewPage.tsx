import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { usePresentationReview, useReviewPresentation } from '@edu/api'
import { EChart } from '../../components/EChart'
import { buildReviewRadarOption } from './reviewRadar'

/**
 * 汇报点评页（S8-13）：教师提交汇报转写触发 AI 多维评分，雷达图 + 评语展示。
 *
 * <p>汇报视频 ASR 转写为上游流程，本页以转写文本作为评分输入。
 */
export default function PresentationReviewPage() {
  const { lessonId: lessonIdParam } = useParams<{ lessonId: string }>()
  const lessonId = lessonIdParam ? Number(lessonIdParam) : null

  const [studentId, setStudentId] = useState<number | null>(null)
  const [studentName, setStudentName] = useState('')
  const [transcript, setTranscript] = useState('')
  const [videoName, setVideoName] = useState<string | null>(null)

  const { data: existing } = usePresentationReview(lessonId, studentId)
  const review = useReviewPresentation(lessonId)

  // 优先展示本次评分结果，否则展示已存在的历史评分
  const result = review.data ?? existing ?? null
  const radarOption = useMemo(
    () => (result ? buildReviewRadarOption(result.dimensions) : null),
    [result],
  )

  const canSubmit = lessonId !== null && studentId !== null && transcript.trim().length > 0

  const handleReview = () => {
    if (!canSubmit) return
    review.mutate({
      lessonId: lessonId!,
      studentId: studentId!,
      studentName: studentName.trim() || undefined,
      transcript: transcript.trim(),
    })
  }

  return (
    <div className="min-h-screen bg-gray-900 p-4 text-gray-100 md:p-6">
      <h1 className="mb-4 text-lg font-semibold">汇报点评 · 课堂 {lessonId ?? '-'}</h1>

      <div className="grid gap-4 lg:grid-cols-2">
        {/* 左：输入区 */}
        <section className="space-y-3 rounded-xl bg-gray-800 p-4">
          <div className="grid grid-cols-2 gap-3">
            <label className="block">
              <span className="mb-1 block text-xs text-gray-400">学生ID</span>
              <input
                type="number"
                value={studentId ?? ''}
                onChange={(e) => setStudentId(e.target.value ? Number(e.target.value) : null)}
                className="w-full rounded-lg border border-gray-600 bg-gray-700 px-3 py-2 text-sm outline-none focus:border-blue-500"
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-gray-400">学生姓名（可选）</span>
              <input
                value={studentName}
                onChange={(e) => setStudentName(e.target.value)}
                className="w-full rounded-lg border border-gray-600 bg-gray-700 px-3 py-2 text-sm outline-none focus:border-blue-500"
              />
            </label>
          </div>

          <label className="block">
            <span className="mb-1 block text-xs text-gray-400">汇报视频（可选，ASR 转写为上游流程）</span>
            <input
              type="file"
              accept="video/*"
              onChange={(e) => setVideoName(e.target.files?.[0]?.name ?? null)}
              className="w-full text-xs text-gray-400 file:mr-3 file:rounded-md file:border-0 file:bg-gray-600 file:px-3 file:py-1.5 file:text-gray-100"
            />
            {videoName && <span className="mt-1 block text-xs text-gray-500">已选择：{videoName}</span>}
          </label>

          <label className="block">
            <span className="mb-1 block text-xs text-gray-400">汇报转写文本</span>
            <textarea
              value={transcript}
              onChange={(e) => setTranscript(e.target.value)}
              rows={8}
              placeholder="粘贴或输入汇报录音的 ASR 转写文本…"
              className="w-full resize-y rounded-lg border border-gray-600 bg-gray-700 px-3 py-2 text-sm outline-none focus:border-blue-500"
            />
          </label>

          <button
            onClick={handleReview}
            disabled={!canSubmit || review.isPending}
            className="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {review.isPending ? 'AI 评分中…' : '发起 AI 点评'}
          </button>
          {review.isError && <p className="text-xs text-red-400">评分失败，请重试</p>}
        </section>

        {/* 右：结果区 */}
        <section className="rounded-xl bg-gray-800 p-4">
          {result ? (
            <div className="space-y-4">
              <div className="flex items-baseline justify-between">
                <h2 className="text-sm font-medium text-gray-300">
                  {result.studentName || `学生 ${result.studentId}`} 评分
                </h2>
                <span className="text-2xl font-bold text-blue-400">
                  {result.totalScore ?? '-'}
                  <span className="ml-1 text-xs font-normal text-gray-500">总分</span>
                </span>
              </div>

              {!result.parsed && (
                <p className="rounded-md bg-amber-900/40 px-3 py-2 text-xs text-amber-300">
                  AI 未能结构化解析，以下为降级评语。
                </p>
              )}

              {radarOption && result.dimensions.length > 0 && (
                <EChart option={radarOption} className="h-64 w-full" />
              )}

              <ul className="space-y-2">
                {result.dimensions.map((d) => (
                  <li key={d.name} className="rounded-lg bg-gray-700/50 px-3 py-2">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-200">{d.name}</span>
                      <span className="text-gray-300">
                        {d.score} / {d.maxScore}
                        <span className="ml-1 text-xs text-gray-500">
                          ×{(d.weight * 100).toFixed(0)}%
                        </span>
                      </span>
                    </div>
                    {d.comment && <p className="mt-1 text-xs text-gray-400">{d.comment}</p>}
                  </li>
                ))}
              </ul>

              {result.overallComment && (
                <div>
                  <h3 className="mb-1 text-xs font-semibold text-gray-300">总体评语</h3>
                  <p className="whitespace-pre-wrap text-sm text-gray-200">{result.overallComment}</p>
                </div>
              )}
            </div>
          ) : (
            <p className="mt-8 text-center text-sm text-gray-500">
              提交学生汇报转写后，此处展示 AI 多维评分雷达图与评语。
            </p>
          )}
        </section>
      </div>
    </div>
  )
}
