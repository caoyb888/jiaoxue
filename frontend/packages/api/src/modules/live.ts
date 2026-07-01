import { useQuery } from '@tanstack/react-query'
import { http } from '../client'

// ─── Types ───────────────────────────────────────────────────────────────────

/** 课堂直播配置（后端 LiveConfigVO，S8-01）。 */
export interface LiveConfigVO {
  lessonId: number
  /** SLIDE_ONLY 线下课堂 / ONLINE_CLASS 线上课堂 */
  liveMode: string
  /** SLIDE_ONLY 恒 false（C5：不发起任何 WebRTC/推拉流） */
  webrtcEnabled: boolean
  rtmpEnabled: boolean
  streamKey: string | null
  pushUrl: string | null
  /** HLS 拉流地址（.m3u8）；SLIDE_ONLY 为 null */
  playUrl: string | null
  /** 0待推流/1推流中/2已结束/3已生成回放；SLIDE_ONLY 为 null */
  status: number | null
}

/** 课堂回放信息（后端 ReplayVO，S8-03）。 */
export interface ReplayVO {
  lessonId: number
  available: boolean
  visible: boolean
  replayUrl: string | null
  durationSec: number | null
}

// ─── API functions ────────────────────────────────────────────────────────────

export const liveApi = {
  getConfig: (lessonId: number) =>
    http.get<void, LiveConfigVO>(`/v1/live/${lessonId}`),

  getReplay: (lessonId: number) =>
    http.get<void, ReplayVO>(`/v1/live/${lessonId}/replay`),
}

// ─── React Query hooks ────────────────────────────────────────────────────────

/** 轮询直播配置（进行中的线上课需感知推流状态变化）。 */
export function useLiveConfig(lessonId: number | null, refetchMs = 15_000) {
  return useQuery({
    queryKey: ['liveConfig', lessonId],
    queryFn: () => liveApi.getConfig(lessonId!),
    enabled: lessonId !== null,
    refetchInterval: refetchMs,
    staleTime: 5_000,
  })
}

export function useReplay(lessonId: number | null) {
  return useQuery({
    queryKey: ['replay', lessonId],
    queryFn: () => liveApi.getReplay(lessonId!),
    enabled: lessonId !== null,
    staleTime: 30_000,
  })
}
