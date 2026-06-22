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

// ─── AI 思维导图（S6-06/11）────────────────────────────────────────────────────

/** Markmap 树节点：{title|content, children} */
export interface MindmapNode {
  title?: string
  content?: string
  children?: MindmapNode[]
}

export interface MindmapVO {
  lessonId: number
  /** PENDING / GENERATING / DONE / FAILED */
  genStatus: string
  markmapJson: MindmapNode | null
  studentVisible: boolean
}

export const mindmapApi = {
  get: (lessonId: number | string): Promise<MindmapVO> =>
    http.get(`/v1/ai/mindmap/${lessonId}`),
  regenerate: (lessonId: number | string): Promise<string> =>
    http.post(`/v1/ai/mindmap/${lessonId}/regenerate`),
  setVisible: (lessonId: number | string, studentVisible: boolean): Promise<void> =>
    http.put(`/v1/ai/mindmap/${lessonId}`, { studentVisible }),
  saveContent: (lessonId: number | string, markmapJson: string): Promise<void> =>
    http.put(`/v1/ai/mindmap/${lessonId}/content`, { markmapJson }),
}

export function useMindmap(lessonId?: number | string) {
  return useQuery({
    queryKey: ['ai', 'mindmap', lessonId],
    queryFn: () => mindmapApi.get(lessonId as number | string),
    enabled: !!lessonId,
    staleTime: 10_000,
  })
}

export function useRegenerateMindmap(lessonId: number | string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => mindmapApi.regenerate(lessonId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'mindmap', lessonId] }),
  })
}

export function useSetMindmapVisible(lessonId: number | string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (studentVisible: boolean) => mindmapApi.setVisible(lessonId, studentVisible),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'mindmap', lessonId] }),
  })
}

export function useSaveMindmapContent(lessonId: number | string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (markmapJson: string) => mindmapApi.saveContent(lessonId, markmapJson),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'mindmap', lessonId] }),
  })
}

// ─── AI 对话任务（S6-08/12）────────────────────────────────────────────────────

export interface DialogueTaskDTO {
  lessonId: number
  topic: string
  opening?: string
  maxTurns?: number
}

export interface DialogueTaskVO {
  sessionId: string
  topic: string
  opening: string
  maxTurns: number
}

export interface DialogueMessageVO {
  role: 'user' | 'assistant'
  content: string
  seq: number
  createdAt: string | null
}

export interface DialogueSessionSummaryVO {
  sessionId: string
  userId: number
  topic: string
  turnCount: number
  maxTurns: number
  status: string
  messageCount: number
  updatedAt: string | null
}

/** SSE 流事件回调 */
export interface DialogueStreamHandlers {
  onChunk: (text: string) => void
  onDone?: () => void
  onError?: (code: number, message: string) => void
}

export const dialogueApi = {
  createTask: (dto: DialogueTaskDTO): Promise<DialogueTaskVO> =>
    http.post('/v1/ai/dialogue/task', dto),
  history: (sessionId: string): Promise<DialogueMessageVO[]> =>
    http.get(`/v1/ai/dialogue/${sessionId}/history`),
  lessonSessions: (lessonId: number | string): Promise<DialogueSessionSummaryVO[]> =>
    http.get(`/v1/ai/dialogue/lesson/${lessonId}/sessions`),
}

/**
 * 发送对话消息并以 SSE 流式接收回复（axios 不支持流，改用 fetch + ReadableStream）。
 * 事件帧：data:{"type":"chunk|done|error","content"?,"code"?,"message"?}
 */
export async function streamDialogueMessage(
  sessionId: string,
  content: string,
  handlers: DialogueStreamHandlers,
): Promise<void> {
  const token =
    typeof window !== 'undefined' ? sessionStorage.getItem('edu_at') : null
  const res = await fetch(`/api/v1/ai/dialogue/${sessionId}/message`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ content }),
  })
  if (!res.ok || !res.body) {
    handlers.onError?.(res.status, `HTTP ${res.status}`)
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split('\n\n')
    buffer = frames.pop() ?? ''
    for (const frame of frames) {
      const dataLine = frame.split('\n').find((l) => l.startsWith('data:'))
      if (!dataLine) continue
      const payload = dataLine.slice(5).trim()
      if (!payload) continue
      try {
        const evt = JSON.parse(payload) as {
          type: string
          content?: string
          code?: number
          message?: string
        }
        if (evt.type === 'chunk' && evt.content) handlers.onChunk(evt.content)
        else if (evt.type === 'done') handlers.onDone?.()
        else if (evt.type === 'error') handlers.onError?.(evt.code ?? 500, evt.message ?? '对话出错')
      } catch {
        // 忽略不完整帧
      }
    }
  }
  handlers.onDone?.()
}

export function useCreateDialogue() {
  return useMutation({
    mutationFn: (dto: DialogueTaskDTO) => dialogueApi.createTask(dto),
  })
}

export function useDialogueHistory(sessionId?: string) {
  return useQuery({
    queryKey: ['ai', 'dialogue', 'history', sessionId],
    queryFn: () => dialogueApi.history(sessionId as string),
    enabled: !!sessionId,
  })
}

export function useLessonDialogues(lessonId?: number | string) {
  return useQuery({
    queryKey: ['ai', 'dialogue', 'lesson', lessonId],
    queryFn: () => dialogueApi.lessonSessions(lessonId as number | string),
    enabled: !!lessonId,
    staleTime: 10_000,
  })
}
