import { useEffect, useRef } from 'react'
import { getSubmitDelay } from '@edu/utils'

const AUTO_SUBMIT_TRIGGER_BEFORE_END_MS = 30_000 // 倒计时30s内触发打散提交

/**
 * C2约束：交卷打散机制。
 * 倒计时30s开始时，按学号末两位取模延迟0~30s自动提交，防止全员同时交卷雪崩。
 *
 * @param endTime 考试结束时间（ISO string）
 * @param studentNo 学号（末两位用于取模计算延迟）
 * @param onAutoSubmit 自动提交回调（调用者负责实际提交逻辑）
 * @param enabled 是否启用（已手动提交后应设为 false）
 */
export function useAutoSubmit(
  endTime: string | null,
  studentNo: string,
  onAutoSubmit: (submitType: 'AUTO') => void,
  enabled: boolean,
) {
  const onSubmitRef = useRef(onAutoSubmit)
  onSubmitRef.current = onAutoSubmit

  useEffect(() => {
    if (!enabled || !endTime) return

    const endMs = new Date(endTime).getTime()
    const triggerAt = endMs - AUTO_SUBMIT_TRIGGER_BEFORE_END_MS
    const delay = getSubmitDelay(studentNo) // 0~30000ms

    // 距离触发点还有多少毫秒
    const msUntilTrigger = triggerAt - Date.now()
    if (msUntilTrigger < 0) {
      // 已过触发点（如断网重连后），立即用打散延迟提交
      const fallbackTimer = setTimeout(() => {
        onSubmitRef.current('AUTO')
      }, delay)
      return () => clearTimeout(fallbackTimer)
    }

    // 等到触发点，再等打散延迟
    const outerTimer = setTimeout(() => {
      const innerTimer = setTimeout(() => {
        onSubmitRef.current('AUTO')
      }, delay)
      return () => clearTimeout(innerTimer)
    }, msUntilTrigger)

    return () => clearTimeout(outerTimer)
  }, [endTime, studentNo, enabled])
}
