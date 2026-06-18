import React, { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { http } from '@edu/api'

interface StudentAnswerVO {
  id: number
  questionId: number
  studentId: number
  answerContent: string
  score: string | null
  isCorrect: number | null
  comment: string | null
  reviewStatus: number
  attachments?: string[]
}

interface ReviewDTO {
  score: string
  isCorrect?: number
  comment?: string
}

const REVIEW_STATUS: Record<number, string> = {
  0: '未批改',
  1: '自动批改',
  2: '教师批改',
}

/**
 * 教师阅卷页（S5-13）。
 * 展示单个学生的全部作答，支持逐题打分+评语（主观题）。
 */
export function ReviewPage() {
  const { publishId, studentId } = useParams<{ publishId: string; studentId: string }>()

  const { data: answers = [] } = useQuery({
    queryKey: ['review', 'answers', publishId, studentId],
    queryFn: (): Promise<StudentAnswerVO[]> =>
      http.get(`/v1/exam/publishes/${publishId}/students/${studentId}/answers`)
        .then((r: { data: StudentAnswerVO[] }) => r.data ?? []),
    enabled: !!publishId && !!studentId,
    staleTime: 10_000,
  })

  return (
    <div className="max-w-3xl mx-auto py-8 px-4 space-y-6">
      <h1 className="text-xl font-bold text-gray-900">
        阅卷 — 学生 {studentId}
      </h1>

      {answers.length === 0 && (
        <p className="text-gray-400 text-sm">暂无作答记录</p>
      )}

      {answers.map((ans) => (
        <AnswerCard
          key={ans.id}
          answer={ans}
          publishId={Number(publishId)}
        />
      ))}
    </div>
  )
}

function AnswerCard({ answer, publishId }: { answer: StudentAnswerVO; publishId: number }) {
  const qc = useQueryClient()
  const [score, setScore] = useState(answer.score ?? '')
  const [comment, setComment] = useState(answer.comment ?? '')
  const [isCorrect, setIsCorrect] = useState<number | undefined>(answer.isCorrect ?? undefined)
  const isSubjective = ![1, 2, 3, 6].includes(0) // 由题型判断（此处简化展示）

  const reviewMutation = useMutation({
    mutationFn: (dto: ReviewDTO) => http.put(`/v1/exam/review/${answer.id}`, dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['review', 'answers'] })
    },
  })

  const handleSubmit = () => {
    reviewMutation.mutate({ score, isCorrect, comment })
  }

  return (
    <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700">题目 #{answer.questionId}</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${
          answer.reviewStatus === 2
            ? 'bg-green-100 text-green-700'
            : answer.reviewStatus === 1
            ? 'bg-blue-100 text-blue-700'
            : 'bg-gray-100 text-gray-500'
        }`}>
          {REVIEW_STATUS[answer.reviewStatus]}
        </span>
      </div>

      {/* 学生答案 */}
      <div className="bg-gray-50 rounded-lg p-4">
        <p className="text-sm text-gray-800 whitespace-pre-wrap">
          {answer.answerContent || <span className="text-gray-400">（未作答）</span>}
        </p>
        {answer.attachments && answer.attachments.length > 0 && (
          <div className="flex gap-2 mt-2 flex-wrap">
            {answer.attachments.map((url, i) => (
              <a key={i} href={url} target="_blank" rel="noreferrer"
                className="text-xs text-blue-600 underline">
                附件{i + 1}
              </a>
            ))}
          </div>
        )}
      </div>

      {/* 批改表单 */}
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1">
          <label className="text-xs font-medium text-gray-600">得分</label>
          <input
            type="number"
            min="0"
            step="0.5"
            value={score}
            onChange={(e) => setScore(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs font-medium text-gray-600">是否正确</label>
          <select
            value={isCorrect ?? ''}
            onChange={(e) => setIsCorrect(e.target.value === '' ? undefined : Number(e.target.value))}
            className="w-full border border-gray-300 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">不评定</option>
            <option value="1">正确</option>
            <option value="0">错误</option>
          </select>
        </div>
      </div>

      <div className="space-y-1">
        <label className="text-xs font-medium text-gray-600">评语（可选）</label>
        <textarea
          rows={2}
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="请输入批改评语..."
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <button
        onClick={handleSubmit}
        disabled={!score || reviewMutation.isPending}
        className="bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
      >
        {reviewMutation.isPending ? '保存中...' : '保存批改'}
      </button>
    </div>
  )
}
