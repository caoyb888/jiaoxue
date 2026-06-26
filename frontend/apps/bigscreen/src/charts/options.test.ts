import { describe, expect, it } from 'vitest'
import { deptName, eventBarData } from './options'

describe('eventBarData', () => {
  it('orders buckets and fills missing with zero', () => {
    const data = eventBarData({ BARRAGE: 15, ATTEND: 42 })
    expect(data.map((d) => d.key)).toEqual(['ATTEND', 'BARRAGE', 'QUESTION', 'SCORE', 'SLIDE'])
    expect(data[0]).toEqual({ key: 'ATTEND', label: '签到', value: 42 })
    expect(data[1].value).toBe(15)
    expect(data[2].value).toBe(0) // QUESTION 缺失补零
  })

  it('treats undefined eventVolume as all zero', () => {
    expect(eventBarData(undefined).every((d) => d.value === 0)).toBe(true)
  })
})

describe('deptName', () => {
  it('maps id to configured name', () => {
    expect(deptName(1, { 1: '信息工程学院' })).toBe('信息工程学院')
  })

  it('falls back when name absent', () => {
    expect(deptName(9, {})).toBe('院系9')
  })
})
