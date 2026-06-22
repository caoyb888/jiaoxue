import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useLessonDialogues, useDialogueHistory } from '@edu/api'

/**
 * 教师端全班对话概览（S6-12）。列出本节课所有学生的对话会话，点击查看完整历史。
 */
export function DialogueOverviewPage() {
  const { lessonId } = useParams<{ lessonId: string }>()
  const { data: sessions = [], isLoading } = useLessonDialogues(lessonId)
  const [activeSession, setActiveSession] = useState<string | null>(null)

  return (
    <div className="max-w-5xl mx-auto py-8 px-4">
      <h1 className="text-xl font-bold text-gray-900 mb-1">全班 AI 对话概览</h1>
      <p className="text-xs text-gray-500 mb-5">课堂 #{lessonId} · 共 {sessions.length} 个会话</p>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="space-y-3">
          {isLoading && <p className="text-gray-400 text-sm">加载中…</p>}
          {!isLoading && sessions.length === 0 && (
            <p className="text-gray-400 text-sm">本节课暂无学生发起 AI 对话。</p>
          )}
          {sessions.map((s) => (
            <button
              key={s.sessionId}
              onClick={() => setActiveSession(s.sessionId)}
              className={`w-full text-left bg-white rounded-xl shadow-sm p-4 transition-colors ${
                activeSession === s.sessionId ? 'ring-2 ring-indigo-500' : 'hover:bg-gray-50'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-gray-800">学生 {s.userId}</span>
                <span className="text-xs text-gray-400">{s.messageCount} 条消息</span>
              </div>
              <p className="text-xs text-gray-500 mt-1 truncate">主题：{s.topic}</p>
              <p className="text-xs text-gray-400 mt-0.5">
                轮次 {s.turnCount}/{s.maxTurns}
              </p>
            </button>
          ))}
        </div>

        <div className="lg:sticky lg:top-6 h-fit">
          {activeSession ? (
            <SessionHistory sessionId={activeSession} />
          ) : (
            <div className="bg-white rounded-xl shadow-sm p-8 text-center text-gray-400 text-sm">
              选择左侧会话查看对话详情
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function SessionHistory({ sessionId }: { sessionId: string }) {
  const { data: messages = [], isLoading } = useDialogueHistory(sessionId)

  return (
    <div className="bg-white rounded-xl shadow-sm p-4 max-h-[70vh] overflow-y-auto space-y-3">
      {isLoading && <p className="text-gray-400 text-sm">加载中…</p>}
      {messages.map((m, i) => (
        <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
          <div
            className={`max-w-[80%] rounded-2xl px-3.5 py-2 text-sm whitespace-pre-wrap ${
              m.role === 'user'
                ? 'bg-indigo-600 text-white rounded-br-sm'
                : 'bg-gray-100 text-gray-800 rounded-bl-sm'
            }`}
          >
            {m.content}
          </div>
        </div>
      ))}
    </div>
  )
}
