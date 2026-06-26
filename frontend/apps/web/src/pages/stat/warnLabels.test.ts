import { describe, expect, it } from 'vitest'
import { targetTypeLabel, warnStatusBadgeClass, warnStatusLabel, warnTypeLabel } from './warnLabels'

describe('warnLabels', () => {
  it('maps known warn types', () => {
    expect(warnTypeLabel('LOW_ATTEND')).toBe('低考勤')
    expect(warnTypeLabel('ZERO_ACTIVE')).toBe('零活跃')
    expect(warnTypeLabel('FREQUENT_ABSENCE')).toBe('频繁缺席')
  })

  it('falls back to raw value for unknown type', () => {
    expect(warnTypeLabel('FOO')).toBe('FOO')
  })

  it('maps target type and status', () => {
    expect(targetTypeLabel('STUDENT')).toBe('学生')
    expect(warnStatusLabel(0)).toBe('未处理')
    expect(warnStatusLabel(1)).toBe('已处理')
    expect(warnStatusLabel(2)).toBe('已忽略')
  })

  it('returns distinct badge classes by status', () => {
    expect(warnStatusBadgeClass(0)).toContain('amber')
    expect(warnStatusBadgeClass(1)).toContain('green')
    expect(warnStatusBadgeClass(2)).toContain('gray')
  })
})
