import { http as apiClient } from '../client'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

// ─── Types ───────────────────────────────────────────────────────────────────

export interface AttendCodeVO {
  lessonId: number
  code: string
  qrToken: string
  expireAt: string
  remainSeconds: number
}

export interface AttendResultVO {
  lessonId: number
  studentId: number
  firstAttend: boolean
  totalCount: number
  message: string
}

export interface AttendanceItemVO {
  studentId: number
  studentName: string
  studentNo?: string
  status: 0 | 1 | 2 | 3
  statusLabel: string
  method: string
  attendedAt: string
  isModified: number
}

export interface AttendanceListVO {
  lessonId: number
  totalStudents: number
  attendedCount: number
  absentCount: number
  attendRate: number
  items: AttendanceItemVO[]
}

export interface BarrageDTO {
  content: string
  style?: 'roll' | 'top' | 'bottom'
}

export interface RollCallDTO {
  count?: number
  excludeAbsent?: boolean
  style?: 'random' | 'spotlight' | 'racing'
}

export interface RollCallVO {
  lessonId: number
  studentIds: number[]
  style: string
  message: string
}

export interface SlideFeedbackDTO {
  slidePage: number
  keyword: string
  feedbackType?: 1 | 2 | 3
}

export interface ClassScoreDTO {
  studentId: number
  score: number
  reason?: string
}

// ─── API Functions ────────────────────────────────────────────────────────────

export const interactionApi = {
  generateCode: (lessonId: number) =>
    apiClient.post<AttendCodeVO>(`/interaction/lesson/${lessonId}/attend/code`),

  getCurrentCode: (lessonId: number) =>
    apiClient.get<AttendCodeVO>(`/interaction/lesson/${lessonId}/attend/code`),

  attend: (lessonId: number, data: { code?: string; qrToken?: string }) =>
    apiClient.post<AttendResultVO>(`/interaction/lesson/${lessonId}/attend`, data),

  listAttendance: (lessonId: number) =>
    apiClient.get<AttendanceListVO>(`/interaction/lesson/${lessonId}/attendance`),

  modifyAttendance: (lessonId: number, studentId: number, data: { status: number }) =>
    apiClient.put<void>(`/interaction/lesson/${lessonId}/attendance/${studentId}`, data),

  sendBarrage: (lessonId: number, data: BarrageDTO) =>
    apiClient.post<void>(`/interaction/lesson/${lessonId}/barrage`, data),

  rollCall: (lessonId: number, data: RollCallDTO) =>
    apiClient.post<RollCallVO>(`/interaction/lesson/${lessonId}/roll-call`, data),

  slideFeedback: (lessonId: number, data: SlideFeedbackDTO) =>
    apiClient.post<void>(`/interaction/lesson/${lessonId}/slide-feedback`, data),

  slideFeedbackStats: (lessonId: number) =>
    apiClient.get<Array<{ slide_page: number; count: number }>>(
      `/interaction/lesson/${lessonId}/slide-feedback/stats`
    ),

  addScore: (lessonId: number, classId: number, data: ClassScoreDTO) =>
    apiClient.post<void>(`/interaction/lesson/${lessonId}/score?classId=${classId}`, data),
}

// ─── React Query Hooks ────────────────────────────────────────────────────────

export function useCurrentCode(lessonId: number | undefined) {
  return useQuery({
    queryKey: ['attend-code', lessonId],
    queryFn: () => interactionApi.getCurrentCode(lessonId!),
    enabled: !!lessonId,
    staleTime: 30_000,
  })
}

export function useGenerateCode(lessonId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => interactionApi.generateCode(lessonId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attend-code', lessonId] })
    },
  })
}

export function useAttend(lessonId: number) {
  return useMutation({
    mutationFn: (data: { code?: string; qrToken?: string }) =>
      interactionApi.attend(lessonId, data),
  })
}

export function useAttendanceList(lessonId: number) {
  return useQuery({
    queryKey: ['attendance', lessonId],
    queryFn: () => interactionApi.listAttendance(lessonId),
    staleTime: 10_000,
  })
}

export function useRollCall(lessonId: number) {
  return useMutation({
    mutationFn: (data: RollCallDTO) => interactionApi.rollCall(lessonId, data),
  })
}
