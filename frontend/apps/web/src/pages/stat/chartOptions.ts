import type { EChartsOption } from 'echarts'
import type { ClassDailyStatVO } from '@edu/api'

/** ClassDailyStatVO 中的数值字段（排除日期）。 */
export type DailyNumericKey = Exclude<keyof ClassDailyStatVO, 'statDate'>

/** 趋势折线的事件系列定义。 */
export const TREND_SERIES: { key: DailyNumericKey; label: string }[] = [
  { key: 'attendCount', label: '签到' },
  { key: 'barrageCount', label: '弹幕' },
  { key: 'questionCount', label: '提问' },
  { key: 'scoreCount', label: '加分' },
  { key: 'slideCount', label: '翻页' },
]

/** 提取日期轴（纯函数）。 */
export function dailyDates(daily: ClassDailyStatVO[]): string[] {
  return daily.map((d) => d.statDate)
}

/** 提取某数值字段的序列（缺失按 0，纯函数）。 */
export function seriesValues(daily: ClassDailyStatVO[], key: DailyNumericKey): number[] {
  return daily.map((d) => Number(d[key] ?? 0))
}

const COLORS = ['#2563eb', '#16a34a', '#f59e0b', '#db2777', '#0891b2']

export function buildTrendLineOption(daily: ClassDailyStatVO[]): EChartsOption {
  const dates = dailyDates(daily)
  return {
    grid: { left: 48, right: 24, top: 40, bottom: 40 },
    tooltip: { trigger: 'axis' },
    legend: { data: TREND_SERIES.map((s) => s.label), top: 8 },
    xAxis: { type: 'category', boundaryGap: false, data: dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: TREND_SERIES.map((s, i) => ({
      name: s.label,
      type: 'line',
      smooth: true,
      showSymbol: dates.length <= 31,
      data: seriesValues(daily, s.key),
      itemStyle: { color: COLORS[i % COLORS.length] },
    })),
  }
}

export function buildActiveBarOption(daily: ClassDailyStatVO[]): EChartsOption {
  const dates = dailyDates(daily)
  return {
    grid: { left: 48, right: 24, top: 40, bottom: 40 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['活跃学生数', '开课数'], top: 8 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '活跃学生数',
        type: 'bar',
        data: seriesValues(daily, 'activeStudentCount'),
        itemStyle: { color: '#2563eb', borderRadius: [3, 3, 0, 0] },
      },
      {
        name: '开课数',
        type: 'bar',
        data: seriesValues(daily, 'lessonCount'),
        itemStyle: { color: '#93c5fd', borderRadius: [3, 3, 0, 0] },
      },
    ],
  }
}
