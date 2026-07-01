import { describe, expect, it } from 'vitest'
import type { ReviewDimensionVO } from '@edu/api'
import { buildReviewRadarOption } from './reviewRadar'

const dims: ReviewDimensionVO[] = [
  { name: '内容质量', weight: 0.4, maxScore: 40, score: 32, comment: '充实' },
  { name: '逻辑结构', weight: 0.3, maxScore: 30, score: 24, comment: '清晰' },
  { name: '语言表达', weight: 0.2, maxScore: 20, score: 15, comment: '流畅' },
  { name: '时间控制', weight: 0.1, maxScore: 10, score: 8, comment: '得当' },
]

describe('buildReviewRadarOption', () => {
  it('maps each dimension to an indicator with its maxScore', () => {
    const opt = buildReviewRadarOption(dims)
    const radar = opt.radar as { indicator: { name: string; max: number }[] }
    expect(radar.indicator).toHaveLength(4)
    expect(radar.indicator[0]).toEqual({ name: '内容质量', max: 40 })
    expect(radar.indicator[3]).toEqual({ name: '时间控制', max: 10 })
  })

  it('builds a single radar series with per-dimension scores in order', () => {
    const opt = buildReviewRadarOption(dims)
    const series = (opt.series as { type: string; data: { value: number[] }[] }[])[0]
    expect(series.type).toBe('radar')
    expect(series.data[0].value).toEqual([32, 24, 15, 8])
  })

  it('handles empty dimensions', () => {
    const opt = buildReviewRadarOption([])
    const radar = opt.radar as { indicator: unknown[] }
    expect(radar.indicator).toHaveLength(0)
  })
})
