import type { EChartsOption } from 'echarts'
import type { DeptRankItem, RealtimeOverviewVO } from '@edu/api'

/** 事件桶 → 中文标签。 */
export const EVENT_LABELS: Record<string, string> = {
  ATTEND: '签到',
  BARRAGE: '弹幕',
  QUESTION: '提问',
  SCORE: '加分',
  SLIDE: '翻页',
}

/** 事件桶展示顺序。 */
export const EVENT_ORDER = ['ATTEND', 'BARRAGE', 'QUESTION', 'SCORE', 'SLIDE']

export interface EventBar {
  key: string
  label: string
  value: number
}

/** 把 eventVolume 归一为有序、补零的柱状数据（纯函数，便于测试）。 */
export function eventBarData(eventVolume: RealtimeOverviewVO['eventVolume'] | undefined): EventBar[] {
  return EVENT_ORDER.map((key) => ({
    key,
    label: EVENT_LABELS[key] ?? key,
    value: eventVolume?.[key] ?? 0,
  }))
}

const AXIS_COLOR = '#7dd3fc'
const SPLIT_COLOR = 'rgba(125,211,252,0.12)'

export function buildEventBarOption(eventVolume: RealtimeOverviewVO['eventVolume'] | undefined): EChartsOption {
  const data = eventBarData(eventVolume)
  return {
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: data.map((d) => d.label),
      axisLine: { lineStyle: { color: AXIS_COLOR } },
      axisLabel: { color: AXIS_COLOR },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: AXIS_COLOR },
      splitLine: { lineStyle: { color: SPLIT_COLOR } },
    },
    series: [
      {
        type: 'bar',
        data: data.map((d) => d.value),
        barWidth: '46%',
        itemStyle: { color: '#22d3ee', borderRadius: [4, 4, 0, 0] },
      },
    ],
  }
}

/** 院系 ID → 展示名（无映射回退 “院系{id}”）。 */
export function deptName(deptId: number, names: Record<number, string>): string {
  return names[deptId] ?? `院系${deptId}`
}

export function buildDeptRankingOption(
  items: DeptRankItem[],
  names: Record<number, string>,
): EChartsOption {
  // 横向条形图：值大的排上方 → ECharts category 轴自下而上，故倒序
  const ordered = [...items].reverse()
  return {
    grid: { left: 90, right: 30, top: 10, bottom: 20 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: AXIS_COLOR },
      splitLine: { lineStyle: { color: SPLIT_COLOR } },
    },
    yAxis: {
      type: 'category',
      data: ordered.map((d) => deptName(d.deptId, names)),
      axisLine: { lineStyle: { color: AXIS_COLOR } },
      axisLabel: { color: AXIS_COLOR },
    },
    series: [
      {
        type: 'bar',
        data: ordered.map((d) => d.activeStudentCount),
        barWidth: '52%',
        itemStyle: { color: '#38bdf8', borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: '#e0f2fe' },
      },
    ],
  }
}
