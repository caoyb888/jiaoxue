import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── Types ───────────────────────────────────────────────────────────────────

export interface ReviewDimensionVO {
  name: string
  weight: number
  maxScore: number
  score: number
  comment: string
}

/** 汇报点评（后端 PresentationReviewVO，S8-05）。 */
export interface PresentationReviewVO {
  lessonId: number
  studentId: number
  studentName: string | null
  totalScore: number | null
  overallComment: string | null
  /** LLM 输出是否被成功解析为结构化评分（false=降级，仅原文评语）。 */
  parsed: boolean
  dimensions: ReviewDimensionVO[]
}

export interface PresentationReviewDTO {
  lessonId: number
  studentId: number
  studentName?: string
  /** 汇报录音 ASR 转写文本（上游已转写）。 */
  transcript: string
}

// ─── API functions ────────────────────────────────────────────────────────────

export const presentationApi = {
  review: (dto: PresentationReviewDTO) =>
    http.post<PresentationReviewDTO, PresentationReviewVO>('/v1/ai/presentation/review', dto),

  getReview: (lessonId: number, studentId: number) =>
    http.get<void, PresentationReviewVO | null>(`/v1/ai/presentation/${lessonId}/student/${studentId}`),
}

// ─── React Query hooks ────────────────────────────────────────────────────────

export function usePresentationReview(lessonId: number | null, studentId: number | null) {
  return useQuery({
    queryKey: ['presentationReview', lessonId, studentId],
    queryFn: () => presentationApi.getReview(lessonId!, studentId!),
    enabled: lessonId !== null && studentId !== null,
    staleTime: 30_000,
  })
}

export function useReviewPresentation(lessonId: number | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: PresentationReviewDTO) => presentationApi.review(dto),
    onSuccess: (_data, dto) =>
      qc.invalidateQueries({ queryKey: ['presentationReview', lessonId, dto.studentId] }),
  })
}
