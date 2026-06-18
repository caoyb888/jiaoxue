import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── Student Exam Types ───────────────────────────────────────────────────────

export interface ExamEnterDTO {
  password?: string
}

export interface PaperQuestionDetailVO {
  id: number
  questionId: number
  score: string
  sortOrder: number
  question: {
    id: number
    type: number
    content: string
    answer: string | null
    options: QuestionOptionVO[]
  }
}

export interface ExamEnterVO {
  publishId: number
  paperId: number
  startTime: string
  endTime: string
  durationMin: number
  enableMonitor: number
  faceVerifyType: number
  allowCopy: number
  shuffleQuestion: number
  sessionStatus: string
  faceVerifyPassed: number | null
  firstEnter: boolean
  questions: PaperQuestionDetailVO[]
  totalQuestions: number
  totalPages: number
  currentPage: number
}

export interface ExamQuestionPageVO {
  questions: PaperQuestionDetailVO[]
  totalQuestions: number
  totalPages: number
  currentPage: number
  pageSize: number
}

export interface AnswerItemDTO {
  questionId: number
  answerContent: string
}

export interface SubmitAnswerDTO {
  answers: AnswerItemDTO[]
  submitType?: string
  clientSubmitAt?: string
}

export interface SubmitResultVO {
  publishId: number
  studentId: number
  submittedCount: number
  gradeResults: unknown[]
}

export interface HeartbeatVO {
  sessionStatus: string
  lastHeartbeatAt: string
}

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

export interface ExamPaperVO {
  id: number
  classId: number
  paperName: string
  description: string | null
  totalScore: string
  questionCount: number
  status: number
  createdAt: string
}

export interface PaperQuestionVO {
  id: number
  paperId: number
  questionId: number
  score: string
  sortOrder: number
  questionType: number
  content: string
  answer: string | null
  analysis: string | null
  options: QuestionOptionVO[]
}

export interface ExamPaperCreateDTO {
  classId: number
  paperName: string
  description?: string
}

export interface PaperQuestionAddDTO {
  questionId: number
  score: string
}

export interface PaperQuestionUpdateDTO {
  score?: string
  sortOrder?: number
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

export const examStudentApi = {
  enterExam: (publishId: number, dto?: ExamEnterDTO): Promise<{ code: number; data: ExamEnterVO }> =>
    http.post(`/v1/exam/publishes/${publishId}/enter`, dto ?? {}),

  getQuestionsPage: (publishId: number, page: number): Promise<{ code: number; data: ExamQuestionPageVO }> =>
    http.get(`/v1/exam/publishes/${publishId}/questions`, { params: { page } }),

  submitExam: (publishId: number, dto: SubmitAnswerDTO): Promise<{ code: number; data: SubmitResultVO }> =>
    http.post(`/v1/exam/publishes/${publishId}/submit`, dto),

  heartbeat: (publishId: number): Promise<{ code: number; data: HeartbeatVO }> =>
    http.put(`/v1/exam/publishes/${publishId}/heartbeat`),

  reportMonitorEvent: (publishId: number, eventType: string, detail?: string): Promise<{ code: number }> =>
    http.post(`/v1/exam/publishes/${publishId}/monitor/event`, { eventType, detail }),

  faceVerify: (publishId: number, livePhotoBase64: string): Promise<{ code: number; data: { passed: boolean; score: number; sessionStatus: string } }> =>
    http.post(`/v1/exam/publishes/${publishId}/face-verify`, { livePhotoBase64 }),
}

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

  listPapers: (classId: number): Promise<{ code: number; data: ExamPaperVO[] }> =>
    http.get('/v1/exam/papers', { params: { classId } }),

  createPaper: (dto: ExamPaperCreateDTO): Promise<{ code: number; data: ExamPaperVO }> =>
    http.post('/v1/exam/papers', dto),

  deletePaper: (paperId: number): Promise<{ code: number }> =>
    http.delete(`/v1/exam/papers/${paperId}`),

  listPaperQuestions: (paperId: number): Promise<{ code: number; data: PaperQuestionVO[] }> =>
    http.get(`/v1/exam/papers/${paperId}/questions`),

  addQuestionToPaper: (paperId: number, dto: PaperQuestionAddDTO): Promise<{ code: number; data: PaperQuestionVO }> =>
    http.post(`/v1/exam/papers/${paperId}/questions`, dto),

  updatePaperQuestion: (paperId: number, pqId: number, dto: PaperQuestionUpdateDTO): Promise<{ code: number }> =>
    http.put(`/v1/exam/papers/${paperId}/questions/${pqId}`, dto),

  removePaperQuestion: (paperId: number, pqId: number): Promise<{ code: number }> =>
    http.delete(`/v1/exam/papers/${paperId}/questions/${pqId}`),

  reorderPaperQuestions: (paperId: number, orderedIds: number[]): Promise<{ code: number }> =>
    http.put(`/v1/exam/papers/${paperId}/questions/reorder`, { orderedIds }),
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

export function useExamPapers(classId: number | null) {
  return useQuery({
    queryKey: ['exam', 'papers', classId],
    queryFn: () => examApi.listPapers(classId!).then((r) => r.data ?? []),
    enabled: !!classId,
    staleTime: 30_000,
  })
}

export function usePaperQuestions(paperId: number | null) {
  return useQuery({
    queryKey: ['exam', 'paper-questions', paperId],
    queryFn: () => examApi.listPaperQuestions(paperId!).then((r) => r.data ?? []),
    enabled: !!paperId,
    staleTime: 15_000,
  })
}

export function useCreatePaper() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: ExamPaperCreateDTO) => examApi.createPaper(dto),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'papers', variables.classId] })
    },
  })
}

export function useDeletePaper() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId, classId }: { paperId: number; classId: number }) =>
      examApi.deletePaper(paperId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'papers', variables.classId] })
    },
  })
}

export function useAddQuestionToPaper() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId, dto }: { paperId: number; dto: PaperQuestionAddDTO }) =>
      examApi.addQuestionToPaper(paperId, dto),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'paper-questions', variables.paperId] })
    },
  })
}

export function useUpdatePaperQuestion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId, pqId, dto }: { paperId: number; pqId: number; dto: PaperQuestionUpdateDTO }) =>
      examApi.updatePaperQuestion(paperId, pqId, dto),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'paper-questions', variables.paperId] })
    },
  })
}

export function useRemovePaperQuestion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId, pqId }: { paperId: number; pqId: number }) =>
      examApi.removePaperQuestion(paperId, pqId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'paper-questions', variables.paperId] })
    },
  })
}

export function useReorderPaperQuestions() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId, orderedIds }: { paperId: number; orderedIds: number[] }) =>
      examApi.reorderPaperQuestions(paperId, orderedIds),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'paper-questions', variables.paperId] })
    },
  })
}
