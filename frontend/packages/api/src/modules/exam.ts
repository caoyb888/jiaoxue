import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── Types ───────────────────────────────────────────────────────────────────

export interface QuestionBankVO {
  id: number
  bankName: string
  description: string
  isPublic: number
  deptId: number
  teacherId: number
  questionCount?: number
}

export interface QuestionOptionVO {
  id: number
  optionLabel: string
  content: string
  isCorrect: number | null
  sortOrder: number
}

export interface QuestionVO {
  id: number
  bankId: number
  type: number
  content: string
  answer: string | null
  analysis: string | null
  score: string
  difficulty: number | null
  options: QuestionOptionVO[]
}

export interface QuestionBankCreateDTO {
  bankName: string
  description?: string
  isPublic: number
}

export interface QuestionOptionDTO {
  optionLabel: string
  content: string
  isCorrect: number
}

export interface QuestionCreateDTO {
  bankId: number
  type: number
  content: string
  answer?: string
  analysis?: string
  score: string
  difficulty?: number
  options?: QuestionOptionDTO[]
}

export interface PageResult<T> {
  list: T[]
  total: number
  pages: number
  current: number
}

export const QUESTION_TYPES: Record<number, string> = {
  1: '单选题',
  2: '多选题',
  3: '判断题',
  4: '填空题',
  5: '主观题',
  6: '投票题',
}

export const OPTION_TYPES = new Set([1, 2, 6])

// ─── API ─────────────────────────────────────────────────────────────────────

export const examApi = {
  listBanks: (): Promise<{ code: number; data: QuestionBankVO[] }> =>
    http.get('/v1/exam/banks'),

  createBank: (dto: QuestionBankCreateDTO): Promise<{ code: number; data: QuestionBankVO }> =>
    http.post('/v1/exam/banks', dto),

  deleteBank: (bankId: number): Promise<{ code: number }> =>
    http.delete(`/v1/exam/banks/${bankId}`),

  listQuestions: (bankId: number, params?: { page?: number; size?: number; keyword?: string }): Promise<{ code: number; data: PageResult<QuestionVO> }> =>
    http.get(`/v1/exam/banks/${bankId}/questions`, { params: { page: 1, size: 20, ...params } }),

  createQuestion: (dto: QuestionCreateDTO): Promise<{ code: number; data: QuestionVO }> =>
    http.post('/v1/exam/questions', dto),

  deleteQuestion: (questionId: number): Promise<{ code: number }> =>
    http.delete(`/v1/exam/questions/${questionId}`),
}

// ─── Hooks ───────────────────────────────────────────────────────────────────

export function useQuestionBanks() {
  return useQuery({
    queryKey: ['exam', 'banks'],
    queryFn: () => examApi.listBanks().then((r) => r.data ?? []),
    staleTime: 60_000,
  })
}

export function useQuestions(bankId: number | null, keyword?: string) {
  return useQuery({
    queryKey: ['exam', 'questions', bankId, keyword],
    queryFn: () => examApi.listQuestions(bankId!, { keyword }).then((r) => r.data),
    enabled: !!bankId,
    staleTime: 30_000,
  })
}

export function useCreateBank() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: QuestionBankCreateDTO) => examApi.createBank(dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['exam', 'banks'] })
    },
  })
}

export function useCreateQuestion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: QuestionCreateDTO) => examApi.createQuestion(dto),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'questions', variables.bankId] })
    },
  })
}

export function useDeleteQuestion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ questionId, bankId }: { questionId: number; bankId: number }) =>
      examApi.deleteQuestion(questionId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'questions', variables.bankId] })
    },
  })
}
