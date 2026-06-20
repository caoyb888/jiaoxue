import { useEffect, useRef } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import { useAuthStore } from '@edu/store'
import { isWeb } from '@edu/utils'

/**
 * 订阅课堂实时 STOMP 主题（edu-notify `/ws`，SimpleBroker `/topic`）。
 *
 * 后端拓扑：业务服务 → Kafka(edu.teaching.events) → edu-notify 消费 → STOMP 广播。
 * 主题约定：`/topic/lesson/{lessonId}/{topic}`，topic ∈ attend | barrage | roll-call | slide …
 *
 * 鉴权：dev 经 Vite 代理直连 notify，无网关注入的 X-User-Id，
 *       故按后端 `JwtHandshakeInterceptor` 的兜底约定用 query 参数 `userId` 传递。
 * 仅 Web 端启用（小程序用 Taro.connectSocket，另行实现）。
 */
export function useLessonTopic<T = unknown>(
  lessonId: number | undefined,
  topic: string,
  onMessage: (data: T) => void,
) {
  const userId = useAuthStore((s) => s.userId)
  // 用 ref 持有最新回调，避免回调变化导致反复重连
  const cbRef = useRef(onMessage)
  cbRef.current = onMessage

  useEffect(() => {
    if (!isWeb || !lessonId || !userId) return

    const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const brokerURL = `${scheme}://${window.location.host}/ws/websocket?userId=${userId}`

    const client = new Client({
      brokerURL,
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/lesson/${lessonId}/${topic}`, (msg: IMessage) => {
          try {
            cbRef.current(JSON.parse(msg.body) as T)
          } catch {
            /* 非 JSON 负载忽略 */
          }
        })
      },
    })

    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [lessonId, topic, userId])
}
