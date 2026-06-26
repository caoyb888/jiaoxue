import { useQueries, useQuery } from '@tanstack/react-query'
import { http } from '../client'

// ─── 统计分析 API（S7 数据大屏 / 历史图表）──────────────────────────────────────

/** 大屏实时概览（最近 5 分钟滑动窗口，S7-03）。 */
export interface RealtimeOverviewVO {
  windowMinutes: number
  activeLessonCount: number
  onlineStudentCount: number
  /** 各事件桶发生量：ATTEND/BARRAGE/QUESTION/SCORE/SLIDE */
  eventVolume: Record<string, number>
}

/** 院系某时间桶统计（S7-05）。 */
export interface DeptPeriodStatVO {
  periodStart: string
  lessonCount: number
  classCount: number
  attendCount: number
  barrageCount: number
  questionCount: number
  scoreCount: number
  slideCount: number
  activeStudentCount: number
}

export interface DeptHistoryVO {
  deptId: number
  period: string
  fromDate: string
  toDate: string
  buckets: DeptPeriodStatVO[]
}

/** 班级某日统计（S7-04）。 */
export interface ClassDailyStatVO {
  statDate: string
  lessonCount: number
  attendCount: number
  barrageCount: number
  questionCount: number
  scoreCount: number
  slideCount: number
  activeStudentCount: number
}

export interface ClassHistoryVO {
  classId: number
  fromDate: string
  toDate: string
  daily: ClassDailyStatVO[]
}

/** 大屏刷新间隔（10s，S7-12）。 */
export const STAT_REFRESH_MS = 10_000

export const statApi = {
  realtimeOverview: (): Promise<RealtimeOverviewVO> =>
    http.get('/v1/stat/realtime/overview'),
  deptHistory: (deptId: number, period = 'day', days = 30): Promise<DeptHistoryVO> =>
    http.get(`/v1/stat/history/dept/${deptId}`, { params: { period, days } }),
  classHistory: (classId: number, days = 30): Promise<ClassHistoryVO> =>
    http.get(`/v1/stat/history/class/${classId}`, { params: { days } }),
}

export function useRealtimeOverview() {
  return useQuery({
    queryKey: ['stat', 'realtime', 'overview'],
    queryFn: statApi.realtimeOverview,
    refetchInterval: STAT_REFRESH_MS,
  })
}

export function useDeptHistory(deptId?: number, period = 'day', days = 30) {
  return useQuery({
    queryKey: ['stat', 'dept', deptId, period, days],
    queryFn: () => statApi.deptHistory(deptId as number, period, days),
    enabled: !!deptId,
  })
}

export function useClassHistory(classId?: number, days = 30) {
  return useQuery({
    queryKey: ['stat', 'class', classId, days],
    queryFn: () => statApi.classHistory(classId as number, days),
    enabled: !!classId,
  })
}

/** 院系活跃排行项（客户端聚合）。 */
export interface DeptRankItem {
  deptId: number
  activeStudentCount: number
  attendCount: number
}

/**
 * 院系活跃排行（无全院系聚合接口，前端对给定院系并行查近 days 天 day 粒度，
 * 按活跃学生数汇总并降序）。10s 自动刷新。院系名映射见调用方。
 */
export function useDeptRanking(deptIds: number[], days = 1) {
  const results = useQueries({
    queries: deptIds.map((id) => ({
      queryKey: ['stat', 'dept', id, 'day', days, 'rank'],
      queryFn: () => statApi.deptHistory(id, 'day', days),
      refetchInterval: STAT_REFRESH_MS,
    })),
  })
  const items: DeptRankItem[] = results.map((r, i) => {
    const buckets = r.data?.buckets ?? []
    return {
      deptId: deptIds[i],
      activeStudentCount: buckets.reduce((s, b) => s + b.activeStudentCount, 0),
      attendCount: buckets.reduce((s, b) => s + b.attendCount, 0),
    }
  })
  items.sort((a, b) => b.activeStudentCount - a.activeStudentCount)
  return { items, isLoading: results.some((r) => r.isLoading) }
}
