import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { useGroupDiscussion, useSummarizeDiscussion, type DiscussionMessageVO } from '@edu/api'
import { useAuthStore } from '@edu/store'
import { useLessonTopic } from '../../hooks/useLessonTopic'

interface DiscussionBroadcast {
  action: 'MESSAGE' | 'END'
  userId?: number | null
  userName?: string
  content?: string
  sentAt?: string | null
}

/**
 * 分组讨论页（S8-12）：学生实时发言经 WebSocket 广播，教师端可切换小组并触发 AI 汇总。
 */
export default function GroupDiscussionPage() {
  const { lessonId: lessonIdParam } = useParams<{ lessonId: string }>()
  const lessonId = lessonIdParam ? Number(lessonIdParam) : null
  const [searchParams] = useSearchParams()
  const { roles, userId } = useAuthStore()
  const isTeacher = roles.includes('ROLE_TEACHER') || roles.includes('ROLE_ADMIN')

  const [groupId, setGroupId] = useState(() => Number(searchParams.get('groupId') ?? 1) || 1)
  const [messages, setMessages] = useState<DiscussionMessageVO[]>([])
  const [ended, setEnded] = useState(false)
  const [input, setInput] = useState('')
  const listEndRef = useRef<HTMLDivElement | null>(null)

  // 切换小组时清空本地实时消息（重新订阅新组主题）
  useEffect(() => {
    setMessages([])
    setEnded(false)
  }, [groupId])

  const publish = useLessonTopic<DiscussionBroadcast>(
    lessonId ?? undefined,
    `group/${groupId}/discussion`,
    (msg) => {
      if (msg.action === 'END') {
        setEnded(true)
        return
      }
      setMessages((prev) => [
        ...prev,
        {
          userId: msg.userId ?? null,
          userName: msg.userName ?? '匿名',
          content: msg.content ?? '',
          sentAt: msg.sentAt ?? null,
        },
      ])
    },
  )

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const { data: summary, isFetching: summaryLoading } = useGroupDiscussion(lessonId, groupId)
  const summarize = useSummarizeDiscussion(lessonId ?? 0, groupId)

  const handleSend = () => {
    const content = input.trim()
    if (!content) return
    publish(`group/${groupId}/discuss`, { content })
    setInput('')
  }

  const handleEnd = () => {
    publish(`group/${groupId}/discuss/end`, {})
    summarize.mutate()
  }

  return (
    <div className="flex h-screen flex-col bg-gray-900 text-gray-100">
      <header className="flex h-14 shrink-0 items-center justify-between bg-gray-800 px-4 md:px-6">
        <span className="text-sm font-medium">分组讨论 · 课堂 {lessonId ?? '-'}</span>
        {isTeacher && (
          <div className="flex items-center gap-2 text-sm">
            <span className="text-gray-400">小组</span>
            <button
              onClick={() => setGroupId((g) => Math.max(1, g - 1))}
              className="rounded bg-gray-700 px-2 py-1 hover:bg-gray-600"
            >
              −
            </button>
            <input
              type="number"
              min={1}
              value={groupId}
              onChange={(e) => setGroupId(Math.max(1, Number(e.target.value) || 1))}
              className="w-14 rounded border border-gray-600 bg-gray-700 px-2 py-1 text-center"
            />
            <button
              onClick={() => setGroupId((g) => g + 1)}
              className="rounded bg-gray-700 px-2 py-1 hover:bg-gray-600"
            >
              +
            </button>
          </div>
        )}
      </header>

      <main className="flex flex-1 flex-col overflow-hidden lg:flex-row">
        {/* 实时讨论区 */}
        <section className="flex flex-1 flex-col overflow-hidden">
          <div className="flex-1 space-y-3 overflow-y-auto p-4">
            {messages.length === 0 ? (
              <p className="mt-8 text-center text-sm text-gray-500">
                {ended ? '本组讨论已结束' : '暂无发言，开始讨论吧'}
              </p>
            ) : (
              messages.map((m, i) => {
                const mine = m.userId != null && m.userId === userId
                return (
                  <div key={i} className={`flex flex-col ${mine ? 'items-end' : 'items-start'}`}>
                    <span className="mb-0.5 text-xs text-gray-500">{m.userName}</span>
                    <span
                      className={`max-w-[80%] rounded-2xl px-3 py-2 text-sm ${
                        mine ? 'bg-blue-600 text-white' : 'bg-gray-700 text-gray-100'
                      }`}
                    >
                      {m.content}
                    </span>
                  </div>
                )
              })
            )}
            <div ref={listEndRef} />
          </div>

          {/* 发言输入 */}
          <div className="flex shrink-0 items-center gap-2 border-t border-gray-700 bg-gray-800 p-3">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSend()}
              placeholder={ended ? '讨论已结束' : '输入你的观点…'}
              disabled={ended}
              className="flex-1 rounded-lg border border-gray-600 bg-gray-700 px-3 py-2 text-sm outline-none focus:border-blue-500 disabled:opacity-50"
            />
            <button
              onClick={handleSend}
              disabled={ended || !input.trim()}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              发送
            </button>
          </div>
        </section>

        {/* 教师端 AI 汇总面板（lg 以上右栏） */}
        {isTeacher && (
          <aside className="shrink-0 border-t border-gray-700 bg-gray-800 lg:w-80 lg:border-l lg:border-t-0 xl:w-96">
            <div className="flex items-center justify-between p-4">
              <h2 className="text-xs font-medium uppercase tracking-wider text-gray-400">AI 汇总</h2>
              <button
                onClick={handleEnd}
                disabled={summarize.isPending || lessonId === null}
                className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
              >
                {summarize.isPending ? '汇总中…' : '结束并汇总'}
              </button>
            </div>
            <div className="space-y-4 px-4 pb-4 text-sm">
              <div className="flex gap-4 text-xs text-gray-400">
                <span>参与 {summary?.participantCount ?? 0} 人</span>
                <span>发言 {summary?.messageCount ?? 0} 条</span>
              </div>
              {summaryLoading && !summary ? (
                <p className="text-gray-500">加载中…</p>
              ) : summary?.summary ? (
                <>
                  <div>
                    <h3 className="mb-1 text-xs font-semibold text-gray-300">讨论概要</h3>
                    <p className="whitespace-pre-wrap text-gray-200">{summary.summary}</p>
                  </div>
                  {summary.keyPoints.length > 0 && (
                    <div>
                      <h3 className="mb-1 text-xs font-semibold text-gray-300">关键观点</h3>
                      <ul className="list-disc space-y-1 pl-4 text-gray-200">
                        {summary.keyPoints.map((kp, i) => (
                          <li key={i}>{kp}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </>
              ) : (
                <p className="text-gray-500">尚无汇总，点击「结束并汇总」生成。</p>
              )}
            </div>
          </aside>
        )}
      </main>
    </div>
  )
}
