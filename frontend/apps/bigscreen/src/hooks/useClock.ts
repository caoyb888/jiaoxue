import { useEffect, useState } from 'react'

/** 每秒更新的当前时间字符串（zh-CN）。 */
export function useClock(): string {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 1000)
    return () => window.clearInterval(timer)
  }, [])
  return now.toLocaleString('zh-CN', { hour12: false })
}
