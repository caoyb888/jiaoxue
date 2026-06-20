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

// ─── Exam Publish Types (S5 发布列表) ─────────────────────────────────────────

/** 教师视角发布 VO（含全部配置，不含密码明文/散列）。 */
export interface ExamPublishVO {
  id: number
  paperId: number
  classId: number
  teacherId: number
  startTime: string
  endTime: string
  durationMin: number
  hasPassword: boolean
  enableMonitor: number
  faceVerifyType: number
  answerShowAt: string | null
  allowCopy: number
  shuffleQuestion: number
  shuffleOption: number
  /** 实时状态：0-未开始 1-进行中 2-已结束 3-已取消 */
  status: number
  statusLabel: string
  createdAt: string
  updatedAt: string
}

/** 学生端考试列表 VO（不含题目内容）。 */
export interface StudentExamListVO {
  publishId: number
  paperId: number
  paperTitle: string
  startTime: string
  endTime: string
  durationMin: number
  hasPassword: boolean
  enableMonitor: number
  faceVerifyType: number
  status: number
  statusLabel: string
  entered: boolean
  submitted: boolean
}

export interface ExamPublishQueryParams {
  classId?: number
  status?: number
  page?: number
  size?: number
}

/** 发布考试请求（对应后端 ExamPublishCreateDTO）。 */
export interface ExamPublishCreateDTO {
  paperId: number
  classId: number
  /** ISO 本地时间，如 2026-06-20T14:00:00 */
  startTime: string
  endTime: string
  durationMin: number
  /** 明文密码，后端加密存储；空/省略=不设密码 */
  password?: string
  /** 0-不开启 1-开启在线监考 */
  enableMonitor: number
  /** 0-不核验 1-证件照 2-现场拍照 */
  faceVerifyType: number
  /** null/省略=发布后立即可查答案 */
  answerShowAt?: string | null
  /** 0-禁止复制 1-允许 */
  allowCopy: number
  /** 0-固定顺序 1-乱序题目 */
  shuffleQuestion: number
  /** 0-固定顺序 1-乱序选项 */
  shuffleOption: number
}

/** 人脸核验结果（C6：无原始照片字段）。 */
export interface FaceVerifyResultVO {
  passed: boolean
  score: number
  sessionStatus: string
  message: string
}

/** 单题作答与批改明细。 */
export interface StudentAnswerVO {
  id: number
  publishId: number
  questionId: number
  studentId: number
  answerContent: string
  score: number | null
  /** null=未批改 0=错误 1=正确 */
  isCorrect: number | null
  comment: string | null
  /** 0-未批改 1-自动批改完成 2-教师已批改 */
  reviewStatus: number
  submittedAt: string
  attachments: string[] | null
}

/** 学生本次考试得分汇总。 */
export interface ExamScoreSummaryVO {
  publishId: number
  studentId: number
  /** 已批改题目得分总和（未批改题不计入） */
  totalScore: number | null
  fullScore: number | null
  totalQuestions: number
  gradedQuestions: number
  correctCount: number
  answers: StudentAnswerVO[]
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
  // 注意：http 拦截器已解包 Result→data，故方法直接 resolve 业务数据（非 { code, data }）。
  enterExam: (publishId: number, dto?: ExamEnterDTO): Promise<ExamEnterVO> =>
    http.post(`/v1/exam/publishes/${publishId}/enter`, dto ?? {}),

  getQuestionsPage: (publishId: number, page: number): Promise<ExamQuestionPageVO> =>
    http.get(`/v1/exam/publishes/${publishId}/questions`, { params: { page } }),

  submitExam: (publishId: number, dto: SubmitAnswerDTO): Promise<SubmitResultVO> =>
    http.post(`/v1/exam/publishes/${publishId}/submit`, dto),

  heartbeat: (publishId: number): Promise<HeartbeatVO> =>
    http.put(`/v1/exam/publishes/${publishId}/heartbeat`),

  reportMonitorEvent: (publishId: number, eventType: string, detail?: string): Promise<void> =>
    http.post(`/v1/exam/publishes/${publishId}/monitor/event`, { eventType, detail }),

  faceVerify: (publishId: number, livePhotoBase64: string): Promise<FaceVerifyResultVO> =>
    http.post(`/v1/exam/publishes/${publishId}/face-verify`, { livePhotoBase64 }),

  /** 学生查询自己本次考试的成绩汇总（含逐题批改明细）。 */
  myScore: (publishId: number): Promise<ExamScoreSummaryVO> =>
    http.get(`/v1/exam/publishes/${publishId}/my-score`),
}

/** S5 考试发布 API（教师/学生双视角）。返回值均为已解包业务数据。 */
export const examPublishApi = {
  /** 教师：发布考试。 */
  publish: (dto: ExamPublishCreateDTO): Promise<ExamPublishVO> =>
    http.post('/v1/exam/publishes', dto),

  /** 教师：分页查询自己发布的考试。 */
  listByTeacher: (params?: ExamPublishQueryParams): Promise<PageResult<ExamPublishVO>> =>
    http.get('/v1/exam/publishes', { params }),

  /** 通用：按 id 取发布详情。 */
  getById: (publishId: number): Promise<ExamPublishVO> =>
    http.get(`/v1/exam/publishes/${publishId}`),

  /** 学生：查询某班级的考试列表（含是否已进入/已交卷）。 */
  listForStudent: (classId: number): Promise<StudentExamListVO[]> =>
    http.get('/v1/exam/publishes/student/list', { params: { classId } }),
}

// 注意：http 拦截器已解包 Result→data，故方法直接 resolve 业务数据（对齐 examStudentApi/examPublishApi）。
export const examApi = {
  listBanks: (): Promise<QuestionBankVO[]> =>
    http.get('/v1/exam/banks'),

  createBank: (dto: QuestionBankCreateDTO): Promise<QuestionBankVO> =>
    http.post('/v1/exam/banks', dto),

  deleteBank: (bankId: number): Promise<void> =>
    http.delete(`/v1/exam/banks/${bankId}`),

  listQuestions: (bankId: number, params?: { page?: number; size?: number; keyword?: string }): Promise<PageResult<QuestionVO>> =>
    http.get(`/v1/exam/banks/${bankId}/questions`, { params: { page: 1, size: 20, ...params } }),

  createQuestion: (dto: QuestionCreateDTO): Promise<QuestionVO> =>
    http.post('/v1/exam/questions', dto),

  deleteQuestion: (questionId: number): Promise<void> =>
    http.delete(`/v1/exam/questions/${questionId}`),

  listPapers: (classId: number): Promise<ExamPaperVO[]> =>
    http.get('/v1/exam/papers', { params: { classId } }),

  createPaper: (dto: ExamPaperCreateDTO): Promise<ExamPaperVO> =>
    http.post('/v1/exam/papers', dto),

  deletePaper: (paperId: number): Promise<void> =>
    http.delete(`/v1/exam/papers/${paperId}`),

  listPaperQuestions: (paperId: number): Promise<PaperQuestionVO[]> =>
    http.get(`/v1/exam/papers/${paperId}/questions`),

  addQuestionToPaper: (paperId: number, dto: PaperQuestionAddDTO): Promise<PaperQuestionVO> =>
    http.post(`/v1/exam/papers/${paperId}/questions`, dto),

  updatePaperQuestion: (paperId: number, pqId: number, dto: PaperQuestionUpdateDTO): Promise<void> =>
    http.put(`/v1/exam/papers/${paperId}/questions/${pqId}`, dto),

  removePaperQuestion: (paperId: number, pqId: number): Promise<void> =>
    http.delete(`/v1/exam/papers/${paperId}/questions/${pqId}`),

  reorderPaperQuestions: (paperId: number, orderedIds: number[]): Promise<void> =>
    http.put(`/v1/exam/papers/${paperId}/questions/reorder`, { orderedIds }),
}

// ─── Hooks ───────────────────────────────────────────────────────────────────

export function useQuestionBanks() {
  return useQuery({
    queryKey: ['exam', 'banks'],
    queryFn: () => examApi.listBanks(),
    staleTime: 60_000,
  })
}

export function useQuestions(bankId: number | null, keyword?: string) {
  return useQuery({
    queryKey: ['exam', 'questions', bankId, keyword],
    queryFn: () => examApi.listQuestions(bankId!, { keyword }),
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
    mutationFn: ({ questionId }: { questionId: number; bankId: number }) =>
      examApi.deleteQuestion(questionId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'questions', variables.bankId] })
    },
  })
}

export function useExamPapers(classId: number | null) {
  return useQuery({
    queryKey: ['exam', 'papers', classId],
    queryFn: () => examApi.listPapers(classId!),
    enabled: !!classId,
    staleTime: 30_000,
  })
}

export function usePaperQuestions(paperId: number | null) {
  return useQuery({
    queryKey: ['exam', 'paper-questions', paperId],
    queryFn: () => examApi.listPaperQuestions(paperId!),
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
    mutationFn: ({ paperId }: { paperId: number; classId: number }) =>
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
