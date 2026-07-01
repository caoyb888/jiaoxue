import { useEffect, useRef } from 'react'
import Hls from 'hls.js'

/**
 * 将 HLS（.m3u8）拉流地址挂载到 <video>。
 *
 * <p>优先用浏览器原生 HLS（Safari/iOS）；否则用 hls.js（Chrome/Firefox 等）。
 * playUrl 为 null 时不加载任何流（配合 C5：线下课堂不出现视频）。
 *
 * @returns 需绑定到 <video> 的 ref
 */
export function useHlsPlayer(playUrl: string | null) {
  const videoRef = useRef<HTMLVideoElement | null>(null)

  useEffect(() => {
    const video = videoRef.current
    if (!video || !playUrl) return

    // 原生 HLS（Safari）直接置 src
    if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = playUrl
      return
    }

    if (Hls.isSupported()) {
      const hls = new Hls({ lowLatencyMode: true, enableWorker: true })
      hls.loadSource(playUrl)
      hls.attachMedia(video)
      return () => hls.destroy()
    }

    // 兜底：既不支持原生也不支持 hls.js
    video.src = playUrl
  }, [playUrl])

  return videoRef
}
