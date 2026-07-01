import type { EChartsOption } from 'echarts'
import type { StudentGradeVO } from '@edu/api'

/** 成绩分段（五档）标签。 */
export const SCORE_BUCKETS = ['<60', '60–69', '70–79', '80–89', '90–100'] as const

/**
 * 将学生总分分桶为五档计数（纯函数）。null 总分（未计算）不计入。
 */
export function bucketTotalScores(grades: StudentGradeVO[]): number[] {
  const counts = [0, 0, 0, 0, 0]
  for (const g of grades) {
    if (g.totalScore == null) continue
    const s = Number(g.totalScore)
    if (s < 60) counts[0]++
    else if (s < 70) counts[1]++
    else if (s < 80) counts[2]++
    else if (s < 90) counts[3]++
    else counts[4]++
  }
  return counts
}

const BUCKET_COLORS = ['#ef4444', '#f59e0b', '#eab308', '#3b82f6', '#16a34a']

/** 成绩分布直方图配置。 */
export function buildGradeDistributionOption(grades: StudentGradeVO[]): EChartsOption {
  const counts = bucketTotalScores(grades)
  return {
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: [...SCORE_BUCKETS] },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        data: counts.map((v, i) => ({ value: v, itemStyle: { color: BUCKET_COLORS[i] } })),
        barMaxWidth: 48,
        label: { show: true, position: 'top', color: '#cbd5e1' },
      },
    ],
  }
}
