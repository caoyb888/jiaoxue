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
  teacherId: number
  deptId: number
  bankName: string
  description: string
  isPublic: number
  createdAt: string
  updatedAt: string
  /** 当前用户是否可改/删（creator 才可） */
  editable: boolean
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
  /** BigDecimal → JSON number */
  score: number
  difficulty: number | null
  reviewRule: string | null
  creatorId: number
  createdAt: string
  updatedAt: string
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
  score: number
  difficulty?: number
  reviewRule?: string
  options?: QuestionOptionDTO[]
}

/** 后端通用分页结构 cn.smu.edu.common.result.PageResult */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
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
  creatorId: number
  title: string
  /** BigDecimal → number */
  totalScore: number
  /** 0-固定组卷 1-随机抽题 */
  isRandom: number
  /** A/B/C 卷型 */
  paperType: string
  description: string | null
  createdAt: string
  updatedAt: string
  editable: boolean
}

/** 试卷题目项：question 为嵌套完整题目（含选项） */
export interface PaperQuestionVO {
  id: number
  paperId: number
  questionId: number
  score: number
  sortOrder: number
  paperGroup: string
  section: string | null
  question: QuestionVO
}

/** GET /papers/{id} 详情：含题目列表与分值核对 */
export interface ExamPaperDetailVO extends ExamPaperVO {
  questions: PaperQuestionVO[]
  totalQuestions: number
  /** 各题分值之和（与 totalScore 比对） */
  actualScore: number
}

export interface ExamPaperCreateDTO {
  title: string
  totalScore?: number
  isRandom?: number
  paperType?: string
  description?: string
}

export interface ExamPaperQueryParams {
  keyword?: string
  isRandom?: number
  page?: number
  size?: number
}

/** 批量加题项（对应后端 AddQuestionDTO） */
export interface AddQuestionDTO {
  questionId: number
  score: number
  sortOrder?: number
  section?: string
  paperGroup?: string
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
// 路径/字段均对齐真实后端（QuestionBank/Question/ExamPaper Controller）。
export const examApi = {
  listBanks: (params?: { keyword?: string; isPublic?: number; page?: number; size?: number }): Promise<PageResult<QuestionBankVO>> =>
    http.get('/v1/exam/banks', { params }),

  createBank: (dto: QuestionBankCreateDTO): Promise<QuestionBankVO> =>
    http.post('/v1/exam/banks', dto),

  deleteBank: (bankId: number): Promise<void> =>
    http.delete(`/v1/exam/banks/${bankId}`),

  // 题目按题库过滤：GET /questions?bankId=（非 /banks/{id}/questions）
  listQuestions: (bankId: number, params?: { type?: number; difficulty?: number; keyword?: string; page?: number; size?: number }): Promise<PageResult<QuestionVO>> =>
    http.get('/v1/exam/questions', { params: { bankId, page: 1, size: 20, ...params } }),

  createQuestion: (dto: QuestionCreateDTO): Promise<QuestionVO> =>
    http.post('/v1/exam/questions', dto),

  deleteQuestion: (questionId: number): Promise<void> =>
    http.delete(`/v1/exam/questions/${questionId}`),

  // 试卷归属 creator，列表不按班级过滤
  listPapers: (params?: ExamPaperQueryParams): Promise<PageResult<ExamPaperVO>> =>
    http.get('/v1/exam/papers', { params }),

  createPaper: (dto: ExamPaperCreateDTO): Promise<ExamPaperVO> =>
    http.post('/v1/exam/papers', dto),

  deletePaper: (paperId: number): Promise<void> =>
    http.delete(`/v1/exam/papers/${paperId}`),

  // 试卷题目来自详情接口的 .questions（无独立 list 端点）
  getPaperDetail: (paperId: number): Promise<ExamPaperDetailVO> =>
    http.get(`/v1/exam/papers/${paperId}`),

  // 批量加题（单题也走 batch）→ 返回最新详情
  addQuestions: (paperId: number, questions: AddQuestionDTO[]): Promise<ExamPaperDetailVO> =>
    http.post(`/v1/exam/papers/${paperId}/questions/batch`, { questions }),

  // 按 questionId + 卷组移除
  removeQuestion: (paperId: number, questionId: number, paperGroup = 'A'): Promise<void> =>
    http.delete(`/v1/exam/papers/${paperId}/questions/${questionId}`, { params: { paperGroup } }),
}

// ─── Hooks ───────────────────────────────────────────────────────────────────

export function useQuestionBanks(keyword?: string) {
  return useQuery({
    queryKey: ['exam', 'banks', keyword],
    queryFn: () => examApi.listBanks({ keyword }).then((r) => r.list),
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

export function useExamPapers(params?: ExamPaperQueryParams) {
  return useQuery({
    queryKey: ['exam', 'papers', params ?? null],
    queryFn: () => examApi.listPapers(params).then((r) => r.list),
    staleTime: 30_000,
  })
}

export function usePaperDetail(paperId: number | null) {
  return useQuery({
    queryKey: ['exam', 'paper-detail', paperId],
    queryFn: () => examApi.getPaperDetail(paperId!),
    enabled: !!paperId,
    staleTime: 15_000,
  })
}

export function useCreatePaper() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: ExamPaperCreateDTO) => examApi.createPaper(dto),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['exam', 'papers'] })
    },
  })
}

export function useDeletePaper() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId }: { paperId: number }) => examApi.deletePaper(paperId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['exam', 'papers'] })
    },
  })
}

export function useAddQuestionsToPaper() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId, questions }: { paperId: number; questions: AddQuestionDTO[] }) =>
      examApi.addQuestions(paperId, questions),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'paper-detail', variables.paperId] })
      qc.invalidateQueries({ queryKey: ['exam', 'papers'] })
    },
  })
}

export function useRemovePaperQuestion() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ paperId, questionId, paperGroup }: { paperId: number; questionId: number; paperGroup?: string }) =>
      examApi.removeQuestion(paperId, questionId, paperGroup),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['exam', 'paper-detail', variables.paperId] })
      qc.invalidateQueries({ queryKey: ['exam', 'papers'] })
    },
  })
}
