import { describe, expect, it } from 'vitest'
import type { LiveConfigVO } from '@edu/api'
import { resolvePlayback } from './livePlayback'

function config(over: Partial<LiveConfigVO>): LiveConfigVO {
  return {
    lessonId: 1,
    liveMode: 'ONLINE_CLASS',
    webrtcEnabled: true,
    rtmpEnabled: true,
    streamKey: 'k',
    pushUrl: 'rtmp://x',
    playUrl: 'https://cdn/x.m3u8',
    status: 1,
    ...over,
  }
}

describe('resolvePlayback', () => {
  it('offline when config missing', () => {
    expect(resolvePlayback(undefined).state).toBe('offline')
    expect(resolvePlayback(null).state).toBe('offline')
  })

  it('offline for SLIDE_ONLY (C5)', () => {
    const p = resolvePlayback(config({ liveMode: 'SLIDE_ONLY', webrtcEnabled: false, playUrl: null, status: null }))
    expect(p.state).toBe('offline')
    expect(p.playUrl).toBeNull()
  })

  it('offline when webrtc disabled even if ONLINE_CLASS', () => {
    expect(resolvePlayback(config({ webrtcEnabled: false })).state).toBe('offline')
  })

  it('playing when status=1 and playUrl present', () => {
    const p = resolvePlayback(config({ status: 1 }))
    expect(p.state).toBe('playing')
    expect(p.playUrl).toBe('https://cdn/x.m3u8')
  })

  it('waiting when status=0 (not yet pushing)', () => {
    expect(resolvePlayback(config({ status: 0, playUrl: null })).state).toBe('waiting')
  })

  it('waiting when status=1 but no playUrl yet', () => {
    expect(resolvePlayback(config({ status: 1, playUrl: null })).state).toBe('waiting')
  })

  it('ended when status=2 or 3', () => {
    expect(resolvePlayback(config({ status: 2 })).state).toBe('ended')
    expect(resolvePlayback(config({ status: 3 })).state).toBe('ended')
  })
})
