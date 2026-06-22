import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  useCreateDialogue,
  streamDialogueMessage,
  type DialogueTaskVO,
} from '@edu/api'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

/**
 * 学生端 AI 对话页（S6-12）。SSE 流式对话气泡 + typing 动画。
 */
export function DialoguePage() {
  const { lessonId } = useParams<{ lessonId: string }>()
  const createDialogue = useCreateDialogue()

  const [session, setSession] = useState<DialogueTaskVO | null>(null)
  const [topic, setTopic] = useState('课堂答疑互动')
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const userTurns = messages.filter((m) => m.role === 'user').length
  const reachedLimit = session ? userTurns >= session.maxTurns : false

  const start = () => {
    createDialogue.mutate(
      { lessonId: Number(lessonId), topic },
      {
        onSuccess: (vo) => {
          setSession(vo)
          setMessages([{ role: 'assistant', content: vo.opening }])
        },
      },
    )
  }

  const appendToLastAssistant = (chunk: string) => {
    setMessages((prev) => {
      const next = [...prev]
      const last = next[next.length - 1]
      if (last && last.role === 'assistant') {
        next[next.length - 1] = { ...last, content: last.content + chunk }
      }
      return next
    })
  }

  const send = async () => {
    if (!session || !input.trim() || sending || reachedLimit) return
    const content = input.trim()
    setInput('')
    setSending(true)
    setMessages((prev) => [
      ...prev,
      { role: 'user', content },
      { role: 'assistant', content: '', streaming: true },
    ])

    await streamDialogueMessage(session.sessionId, content, {
      onChunk: appendToLastAssistant,
      onError: (code, message) => appendToLastAssistant(`[已拦截 ${code}] ${message}`),
    })

    setMessages((prev) => {
      const next = [...prev]
      const last = next[next.length - 1]
      if (last && last.role === 'assistant') {
        next[next.length - 1] = { ...last, streaming: false }
      }
      return next
    })
    setSending(false)
  }

  if (!session) {
    return (
      <div className="max-w-md mx-auto py-16 px-4">
        <div className="bg-white rounded-2xl shadow-sm p-6 space-y-4">
          <h1 className="text-lg font-bold text-gray-900">开始 AI 对话答疑</h1>
          <div className="space-y-1">
            <label className="text-xs font-medium text-gray-600">讨论主题</label>
            <input
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <button
            onClick={start}
            disabled={createDialogue.isPending || !topic.trim()}
            className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-300 text-white text-sm font-semibold py-2.5 rounded-lg transition-colors"
          >
            {createDialogue.isPending ? '创建中…' : '开始对话'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto h-[calc(100vh-2rem)] flex flex-col py-4 px-4">
      <header className="pb-3 border-b">
        <h1 className="text-base font-bold text-gray-900">{session.topic}</h1>
        <p className="text-xs text-gray-400">
          剩余轮次 {Math.max(0, session.maxTurns - userTurns)} / {session.maxTurns}
        </p>
      </header>

      <MessageList messages={messages} />

      <div className="pt-3 flex items-end gap-2">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              void send()
            }
          }}
          rows={1}
          disabled={reachedLimit}
          placeholder={reachedLimit ? '已达最大对话轮次' : '输入你的问题，Enter 发送…'}
          className="flex-1 border border-gray-300 rounded-xl px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-gray-100"
        />
        <button
          onClick={() => void send()}
          disabled={sending || !input.trim() || reachedLimit}
          className="bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-300 text-white text-sm font-semibold px-4 py-2.5 rounded-xl transition-colors"
        >
          发送
        </button>
      </div>
    </div>
  )
}

function MessageList({ messages }: { messages: ChatMessage[] }) {
  const endRef = useRef<HTMLDivElement>(null)
  const lastContent = messages[messages.length - 1]?.content
  // 新消息或流式追加时滚到底
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages.length, lastContent])

  return (
    <div className="flex-1 overflow-y-auto py-4 space-y-3">
      {messages.map((m, i) => (
        <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
          <div
            className={`max-w-[78%] rounded-2xl px-4 py-2.5 text-sm whitespace-pre-wrap ${
              m.role === 'user'
                ? 'bg-indigo-600 text-white rounded-br-sm'
                : 'bg-white text-gray-800 shadow-sm rounded-bl-sm'
            }`}
          >
            {m.content || (m.streaming && <TypingDots />)}
          </div>
        </div>
      ))}
      <div ref={endRef} />
    </div>
  )
}

function TypingDots() {
  return (
    <span className="inline-flex gap-1 py-1">
      <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:-0.3s]" />
      <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce [animation-delay:-0.15s]" />
      <span className="w-1.5 h-1.5 bg-gray-400 rounded-full animate-bounce" />
    </span>
  )
}
