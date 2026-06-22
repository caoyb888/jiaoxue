import { useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  useMindmap,
  useRegenerateMindmap,
  useSetMindmapVisible,
  useSaveMindmapContent,
} from '@edu/api'
import { MindmapView } from './MindmapView'

const STATUS_LABEL: Record<string, { text: string; cls: string }> = {
  PENDING: { text: '待生成', cls: 'bg-gray-100 text-gray-500' },
  GENERATING: { text: '生成中', cls: 'bg-blue-100 text-blue-700' },
  DONE: { text: '已生成', cls: 'bg-green-100 text-green-700' },
  FAILED: { text: '生成失败', cls: 'bg-rose-100 text-rose-700' },
}

/**
 * 课堂思维导图页（S6-11）。
 * Markmap.js 渲染、教师编辑 JSON、学生可见性切换、重新生成。
 */
export function MindmapPage() {
  const { lessonId } = useParams<{ lessonId: string }>()
  const { data, isLoading } = useMindmap(lessonId)
  const regenerate = useRegenerateMindmap(lessonId ?? '')
  const setVisible = useSetMindmapVisible(lessonId ?? '')
  const saveContent = useSaveMindmapContent(lessonId ?? '')

  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [draftError, setDraftError] = useState<string | null>(null)

  const status = data ? STATUS_LABEL[data.genStatus] ?? STATUS_LABEL.PENDING : STATUS_LABEL.PENDING

  const openEditor = () => {
    setDraft(data?.markmapJson ? JSON.stringify(data.markmapJson, null, 2) : '')
    setDraftError(null)
    setEditing(true)
  }

  const handleSave = () => {
    try {
      JSON.parse(draft)
    } catch {
      setDraftError('JSON 格式有误，请检查后再保存')
      return
    }
    saveContent.mutate(draft, { onSuccess: () => setEditing(false) })
  }

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 space-y-5">
      <header className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
            <span className="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-emerald-100 text-emerald-700 text-sm font-black">
              脑
            </span>
            课堂思维导图
          </h1>
          <p className="text-xs text-gray-500 mt-1">课堂 #{lessonId}</p>
        </div>
        <span className={`self-start text-xs px-2.5 py-1 rounded-full ${status.cls}`}>{status.text}</span>
      </header>

      {/* 工具栏 */}
      <div className="flex flex-wrap items-center gap-2">
        <button
          onClick={() => regenerate.mutate()}
          disabled={regenerate.isPending}
          className="bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-300 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          {regenerate.isPending ? '已提交…' : '重新生成'}
        </button>
        <button
          onClick={editing ? () => setEditing(false) : openEditor}
          className="border border-gray-300 hover:bg-gray-50 text-gray-700 text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
        >
          {editing ? '取消编辑' : '编辑'}
        </button>
        <label className="ml-auto flex items-center gap-2 text-sm text-gray-700 select-none">
          <input
            type="checkbox"
            checked={data?.studentVisible ?? false}
            onChange={(e) => setVisible.mutate(e.target.checked)}
            disabled={setVisible.isPending}
            className="w-4 h-4 accent-emerald-600"
          />
          对学生可见
        </label>
      </div>

      {regenerate.isSuccess && (
        <p className="text-xs text-emerald-600 bg-emerald-50 rounded-lg px-3 py-2">
          已提交重新生成任务，完成后请刷新查看。
        </p>
      )}

      {/* 编辑器 */}
      {editing && (
        <div className="bg-white rounded-xl shadow-sm p-4 space-y-3">
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={12}
            spellCheck={false}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-xs font-mono resize-y focus:outline-none focus:ring-2 focus:ring-emerald-500"
            placeholder='{"title":"主题","children":[{"content":"子节点","children":[]}]}'
          />
          {draftError && <p className="text-xs text-rose-600">{draftError}</p>}
          <button
            onClick={handleSave}
            disabled={saveContent.isPending || !draft.trim()}
            className="bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-300 text-white text-sm font-semibold px-4 py-2 rounded-lg transition-colors"
          >
            {saveContent.isPending ? '保存中…' : '保存'}
          </button>
        </div>
      )}

      {/* 渲染区 */}
      <div className="bg-white rounded-xl shadow-sm p-4">
        {isLoading && <p className="text-gray-400 text-sm">加载中…</p>}
        {!isLoading && data?.markmapJson ? (
          <MindmapView data={data.markmapJson} />
        ) : (
          !isLoading && (
            <p className="text-gray-400 text-sm py-12 text-center">
              暂无思维导图。课堂结束后将自动生成，或点击「重新生成」。
            </p>
          )
        )}
      </div>
    </div>
  )
}
