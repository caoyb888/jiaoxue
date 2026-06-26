import { describe, expect, it } from 'vitest'
import type { ClassDailyStatVO } from '@edu/api'
import { dailyDates, seriesValues, TREND_SERIES } from './chartOptions'

const daily: ClassDailyStatVO[] = [
  {
    statDate: '2026-06-24',
    lessonCount: 2,
    attendCount: 50,
    barrageCount: 30,
    questionCount: 5,
    scoreCount: 3,
    slideCount: 12,
    activeStudentCount: 48,
  },
  {
    statDate: '2026-06-25',
    lessonCount: 1,
    attendCount: 40,
    barrageCount: 10,
    questionCount: 2,
    scoreCount: 1,
    slideCount: 8,
    activeStudentCount: 39,
  },
]

describe('chartOptions helpers', () => {
  it('dailyDates extracts the date axis in order', () => {
    expect(dailyDates(daily)).toEqual(['2026-06-24', '2026-06-25'])
  })

  it('seriesValues extracts a numeric field series', () => {
    expect(seriesValues(daily, 'attendCount')).toEqual([50, 40])
    expect(seriesValues(daily, 'activeStudentCount')).toEqual([48, 39])
  })

  it('seriesValues coerces missing to zero', () => {
    const partial = [{ statDate: '2026-06-26' }] as unknown as ClassDailyStatVO[]
    expect(seriesValues(partial, 'attendCount')).toEqual([0])
  })

  it('TREND_SERIES covers the five interaction buckets', () => {
    expect(TREND_SERIES.map((s) => s.label)).toEqual(['签到', '弹幕', '提问', '加分', '翻页'])
  })
})
