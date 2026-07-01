import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── Types ───────────────────────────────────────────────────────────────────

export type NoticeScope = 'SCHOOL' | 'DEPT' | 'CLASS'
export type NoticeTargetRole = 'ALL' | 'TEACHER' | 'STUDENT'

/** 接收端通知列表项（后端 NoticeItemVO，S8-15）。 */
export interface NoticeItemVO {
  id: number
  title: string
  content: string
  senderName: string
  scope: NoticeScope
  publishedAt: string | null
  read: boolean
}

/** 发布响应（后端 NoticeVO，S8-10）。 */
export interface NoticeVO {
  id: number
  senderName: string
  title: string
  scope: NoticeScope
  sendCount: number
  status: number
  publishedAt: string | null
}

export interface NoticePublishDTO {
  title: string
  content: string
  scope: NoticeScope
  deptId?: number
  classId?: number
  targetRoles?: NoticeTargetRole
  senderName?: string
}

// ─── API functions ────────────────────────────────────────────────────────────

export const notifyApi = {
  publish: (dto: NoticePublishDTO) =>
    http.post<NoticePublishDTO, NoticeVO>('/v1/notice/publish', dto),

  myNotices: (onlyUnread = false, limit = 50) =>
    http.get<void, NoticeItemVO[]>('/v1/notice/my', { params: { onlyUnread, limit } }),

  unreadCount: () => http.get<void, number>('/v1/notice/my/unread-count'),

  markRead: (noticeId: number) =>
    http.post<void, void>(`/v1/notice/${noticeId}/read`),
}

// ─── React Query hooks ────────────────────────────────────────────────────────

export function useMyNotices(onlyUnread = false, limit = 50) {
  return useQuery({
    queryKey: ['myNotices', onlyUnread, limit],
    queryFn: () => notifyApi.myNotices(onlyUnread, limit),
    staleTime: 15_000,
  })
}

export function useUnreadNoticeCount() {
  return useQuery({
    queryKey: ['noticeUnreadCount'],
    queryFn: () => notifyApi.unreadCount(),
    staleTime: 15_000,
    refetchInterval: 60_000,
  })
}

export function useMarkNoticeRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (noticeId: number) => notifyApi.markRead(noticeId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['myNotices'] })
      qc.invalidateQueries({ queryKey: ['noticeUnreadCount'] })
    },
  })
}

export function usePublishNotice() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: NoticePublishDTO) => notifyApi.publish(dto),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['myNotices'] }),
  })
}
