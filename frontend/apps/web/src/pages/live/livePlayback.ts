import type { LiveConfigVO } from '@edu/api'

/** 直播播放状态：C5 线下课无流 / 线上课未推流 / 线上课已结束 / 正在直播。 */
export type LiveState = 'offline' | 'waiting' | 'ended' | 'playing'

export interface LivePlayback {
  state: LiveState
  /** 仅 playing 时有值：HLS 拉流地址（.m3u8）。 */
  playUrl: string | null
  /** 面向用户的中文提示语。 */
  message: string
}

/**
 * 由直播配置推导前端播放策略（纯函数，便于单测）。
 *
 * <p><b>C5 红线</b>：SLIDE_ONLY 或未启用 WebRTC 的课堂一律 offline，前端不加载任何播放器。
 */
export function resolvePlayback(config: LiveConfigVO | undefined | null): LivePlayback {
  if (!config) {
    return { state: 'offline', playUrl: null, message: '暂无直播信息' }
  }
  // 线下课堂（SLIDE_ONLY）或未开启流媒体：仅课件，无视频（C5）
  if (config.liveMode !== 'ONLINE_CLASS' || !config.webrtcEnabled) {
    return { state: 'offline', playUrl: null, message: '本节为线下课堂，无直播视频' }
  }
  // status: 0待推流/1推流中/2已结束/3已生成回放
  if (config.status === 1 && config.playUrl) {
    return { state: 'playing', playUrl: config.playUrl, message: '直播进行中' }
  }
  if (config.status === 2 || config.status === 3) {
    return { state: 'ended', playUrl: null, message: '直播已结束，可稍后查看回放' }
  }
  return { state: 'waiting', playUrl: null, message: '直播尚未开始，请稍候' }
}
