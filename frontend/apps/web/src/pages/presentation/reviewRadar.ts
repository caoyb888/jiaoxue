import type { EChartsOption } from 'echarts'
import type { ReviewDimensionVO } from '@edu/api'

/**
 * 由汇报点评维度构建 ECharts 雷达图配置（纯函数，便于单测）。
 * 每个维度一个轴，max=该维度满分；单条系列展示各维度得分。
 */
export function buildReviewRadarOption(dimensions: ReviewDimensionVO[]): EChartsOption {
  return {
    tooltip: { trigger: 'item' },
    radar: {
      indicator: dimensions.map((d) => ({ name: d.name, max: d.maxScore })),
      radius: '65%',
      splitNumber: 4,
      axisName: { color: '#cbd5e1', fontSize: 12 },
      splitLine: { lineStyle: { color: '#334155' } },
      splitArea: { areaStyle: { color: ['#1e293b', '#0f172a'] } },
      axisLine: { lineStyle: { color: '#334155' } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: dimensions.map((d) => d.score),
            name: '得分',
            areaStyle: { color: 'rgba(37, 99, 235, 0.35)' },
            lineStyle: { color: '#2563eb' },
            itemStyle: { color: '#2563eb' },
          },
        ],
      },
    ],
  }
}
