import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── Types ───────────────────────────────────────────────────────────────────

export interface DiscussionMessageVO {
  userId: number | null
  userName: string
  content: string
  sentAt: string | null
}

/** 分组讨论 AI 汇总（后端 GroupDiscussionVO，S8-04）。 */
export interface GroupDiscussionVO {
  lessonId: number
  groupId: number
  groupName: string | null
  participantCount: number
  messageCount: number
  summary: string | null
  keyPoints: string[]
  status: string | null
  messages: DiscussionMessageVO[]
}

// ─── API functions ────────────────────────────────────────────────────────────

export const discussionApi = {
  getSummary: (lessonId: number, groupId: number) =>
    http.get<void, GroupDiscussionVO | null>(`/v1/ai/discussion/${lessonId}/group/${groupId}`),

  summarize: (lessonId: number, groupId: number) =>
    http.post<void, GroupDiscussionVO | null>(`/v1/ai/discussion/${lessonId}/group/${groupId}/summarize`),
}

// ─── React Query hooks ────────────────────────────────────────────────────────

export function useGroupDiscussion(lessonId: number | null, groupId: number | null) {
  return useQuery({
    queryKey: ['discussion', lessonId, groupId],
    queryFn: () => discussionApi.getSummary(lessonId!, groupId!),
    enabled: lessonId !== null && groupId !== null,
    staleTime: 10_000,
  })
}

export function useSummarizeDiscussion(lessonId: number, groupId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => discussionApi.summarize(lessonId, groupId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['discussion', lessonId, groupId] }),
  })
}
