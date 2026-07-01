import { describe, expect, it } from 'vitest'
import type { StudentGradeVO } from '@edu/api'
import { bucketTotalScores, buildGradeDistributionOption } from './gradeCharts'

function grade(totalScore: number | null): StudentGradeVO {
  return {
    id: 1, classId: 1, studentId: 1,
    attendScore: null, quizScore: null, interactionScore: null, examScore: null,
    totalScore, offlineScore: null, calcStatus: 1, updatedAt: null,
  }
}

describe('bucketTotalScores', () => {
  it('buckets scores into five bands with correct boundaries', () => {
    const grades = [grade(59), grade(60), grade(69), grade(70), grade(89), grade(90), grade(100)]
    // <60:1  60-69:2  70-79:1  80-89:1  90-100:2
    expect(bucketTotalScores(grades)).toEqual([1, 2, 1, 1, 2])
  })

  it('ignores null (uncalculated) totals', () => {
    expect(bucketTotalScores([grade(null), grade(85), grade(null)])).toEqual([0, 0, 0, 1, 0])
  })

  it('empty input yields all zeros', () => {
    expect(bucketTotalScores([])).toEqual([0, 0, 0, 0, 0])
  })
})

describe('buildGradeDistributionOption', () => {
  it('produces a bar series whose data matches the buckets', () => {
    const opt = buildGradeDistributionOption([grade(95), grade(95), grade(55)])
    const series = (opt.series as { type: string; data: { value: number }[] }[])[0]
    expect(series.type).toBe('bar')
    expect(series.data.map((d) => d.value)).toEqual([1, 0, 0, 0, 2])
  })
})
