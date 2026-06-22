import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── AI 智能批改（S6-02/03/10）────────────────────────────────────────────────

export interface AiReviewResultVO {
  answerId: number
  questionId: number
  studentId: number
  score: string | null
  maxScore: string | null
  comment: string | null
  errorReason: string | null
  /** false 表示 AI 未能解析为有效结果，需人工复核 */
  parsed: boolean
  reviewedAt: string | null
}

export interface TeacherReviewDTO {
  score: string
  isCorrect?: number
  comment?: string
}

export const aiApi = {
  /** 查询某次发布的 AI 批改结果 */
  listReviewResults: (publishId: number | string): Promise<AiReviewResultVO[]> =>
    http.get(`/v1/ai/review/${publishId}`),

  /** 触发某次发布的主观题 AI 批改（异步，返回 taskId） */
  triggerReview: (publishId: number | string): Promise<string> =>
    http.post(`/v1/ai/review/${publishId}`),

  /** 教师覆盖批改（写回 student_answer，review_status=2 教师批改） */
  overrideReview: (answerId: number, dto: TeacherReviewDTO): Promise<void> =>
    http.put(`/v1/exam/review/${answerId}`, dto),
}

export function useAiReviewResults(publishId?: number | string) {
  return useQuery({
    queryKey: ['ai', 'review', publishId],
    queryFn: () => aiApi.listReviewResults(publishId as number | string),
    enabled: !!publishId,
    staleTime: 10_000,
  })
}

export function useTriggerAiReview(publishId: number | string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => aiApi.triggerReview(publishId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'review', publishId] }),
  })
}

export function useOverrideReview(publishId: number | string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { answerId: number; dto: TeacherReviewDTO }) =>
      aiApi.overrideReview(vars.answerId, vars.dto),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'review', publishId] }),
  })
}
