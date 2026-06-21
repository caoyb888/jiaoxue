import { useEffect, useCallback } from 'react'
import { isWeb } from '@edu/utils'
import { examStudentApi } from '@edu/api'

/**
 * 监考事件监听（S5-12）：
 *   - visibilitychange → TAB_SWITCH 事件上报
 *   - oncopy intercept → COPY 事件上报（allowCopy=0 时拦截）
 *
 * 浏览器截图无法直接检测，通过 visibilitychange 兼容拦截。
 */
export function useExamMonitor(
  publishId: number,
  options: { allowCopy: boolean; enabled: boolean },
) {
  const report = useCallback(
    (eventType: string, detail?: string) => {
      if (!options.enabled) return
      examStudentApi.reportMonitorEvent(publishId, eventType, detail).catch(() => {
        // 网络异常不阻断考试，静默失败
      })
    },
    [publishId, options.enabled],
  )

  // 切屏检测（visibilitychange）
  useEffect(() => {
    if (!isWeb || !options.enabled) return

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        report('TAB_SWITCH', 'visibilityState=hidden')
      }
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    return () => document.removeEventListener('visibilitychange', handleVisibilityChange)
  }, [report, options.enabled])

  // 复制拦截（allowCopy=false 时禁止复制并上报）
  useEffect(() => {
    if (!isWeb || !options.enabled) return

    const handleCopy = (e: ClipboardEvent) => {
      if (!options.allowCopy) {
        e.preventDefault()
        report('COPY', 'clipboard copy intercepted')
      } else {
        report('COPY', 'copy allowed')
      }
    }

    document.addEventListener('copy', handleCopy)
    return () => document.removeEventListener('copy', handleCopy)
  }, [report, options.allowCopy, options.enabled])
}
