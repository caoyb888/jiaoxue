import { useState } from 'react'
import DOMPurify from 'dompurify'
import {
  useMyNotices,
  useUnreadNoticeCount,
  useMarkNoticeRead,
  usePublishNotice,
  type NoticeItemVO,
  type NoticeScope,
  type NoticeTargetRole,
} from '@edu/api'
import { useAuthStore } from '@edu/store'

const SCOPE_LABEL: Record<NoticeScope, string> = { SCHOOL: '全校', DEPT: '院系', CLASS: '班级' }

/** 通知公告页（S8-15）：学生接收端（未读 Badge）+ 教师发布端。 */
export default function NoticeCenterPage() {
  const { roles } = useAuthStore()
  const isTeacher = roles.includes('ROLE_TEACHER') || roles.includes('ROLE_ADMIN')

  const [onlyUnread, setOnlyUnread] = useState(false)
  const { data: notices } = useMyNotices(onlyUnread)
  const { data: unread } = useUnreadNoticeCount()
  const markRead = useMarkNoticeRead()
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const handleOpen = (n: NoticeItemVO) => {
    setExpandedId((prev) => (prev === n.id ? null : n.id))
    if (!n.read) markRead.mutate(n.id)
  }

  return (
    <div className="min-h-screen bg-gray-50 p-4 md:p-6">
      <div className="mx-auto max-w-3xl">
        <div className="mb-4 flex items-center justify-between">
          <h1 className="flex items-center gap-2 text-lg font-semibold text-gray-800">
            通知公告
            {unread ? (
              <span className="rounded-full bg-red-500 px-2 py-0.5 text-xs font-medium text-white">
                {unread} 未读
              </span>
            ) : null}
          </h1>
          <label className="flex items-center gap-1.5 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={onlyUnread}
              onChange={(e) => setOnlyUnread(e.target.checked)}
            />
            仅看未读
          </label>
        </div>

        {isTeacher && <PublishForm />}

        <ul className="space-y-2">
          {notices?.map((n) => (
            <li key={n.id} className="rounded-xl bg-white p-4 shadow-sm">
              <button
                onClick={() => handleOpen(n)}
                className="flex w-full items-start justify-between text-left"
              >
                <div className="flex items-start gap-2">
                  {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-red-500" />}
                  <div>
                    <h2 className={`text-sm ${n.read ? 'text-gray-700' : 'font-semibold text-gray-900'}`}>
                      {n.title}
                    </h2>
                    <p className="mt-0.5 text-xs text-gray-400">
                      {n.senderName} · {SCOPE_LABEL[n.scope]} ·{' '}
                      {n.publishedAt ? n.publishedAt.slice(0, 16).replace('T', ' ') : ''}
                    </p>
                  </div>
                </div>
                <span className="ml-2 shrink-0 text-xs text-gray-400">
                  {expandedId === n.id ? '收起' : '展开'}
                </span>
              </button>
              {expandedId === n.id && (
                <div
                  className="prose prose-sm mt-3 max-w-none border-t border-gray-100 pt-3 text-sm text-gray-700"
                  dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(n.content) }}
                />
              )}
            </li>
          ))}
          {notices?.length === 0 && (
            <li className="rounded-xl bg-white py-10 text-center text-sm text-gray-400 shadow-sm">
              {onlyUnread ? '没有未读通知' : '暂无通知'}
            </li>
          )}
        </ul>
      </div>
    </div>
  )
}

/** 教师发布端表单。 */
function PublishForm() {
  const publish = usePublishNotice()
  const [open, setOpen] = useState(false)
  const [form, setForm] = useState<{
    title: string
    content: string
    scope: NoticeScope
    deptId: string
    classId: string
    targetRoles: NoticeTargetRole
  }>({ title: '', content: '', scope: 'SCHOOL', deptId: '', classId: '', targetRoles: 'ALL' })

  const canSubmit =
    form.title.trim().length > 0 &&
    form.content.trim().length > 0 &&
    (form.scope !== 'DEPT' || form.deptId.trim() !== '') &&
    (form.scope !== 'CLASS' || form.classId.trim() !== '')

  const handleSubmit = () => {
    if (!canSubmit) return
    publish.mutate(
      {
        title: form.title.trim(),
        content: form.content.trim(),
        scope: form.scope,
        deptId: form.scope === 'DEPT' ? Number(form.deptId) : undefined,
        classId: form.scope === 'CLASS' ? Number(form.classId) : undefined,
        targetRoles: form.targetRoles,
      },
      {
        onSuccess: () =>
          setForm({ title: '', content: '', scope: 'SCHOOL', deptId: '', classId: '', targetRoles: 'ALL' }),
      },
    )
  }

  return (
    <div className="mb-4 rounded-xl bg-white p-4 shadow-sm">
      <button
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between text-sm font-medium text-gray-700"
      >
        发布通知
        <span className="text-gray-400">{open ? '−' : '+'}</span>
      </button>
      {open && (
        <div className="mt-3 space-y-2">
          <input
            value={form.title}
            onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
            placeholder="通知标题"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <textarea
            value={form.content}
            onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
            rows={4}
            placeholder="通知内容（支持 HTML）"
            className="w-full resize-y rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <div className="flex flex-wrap gap-2">
            <select
              value={form.scope}
              onChange={(e) => setForm((f) => ({ ...f, scope: e.target.value as NoticeScope }))}
              className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
            >
              <option value="SCHOOL">全校</option>
              <option value="DEPT">院系</option>
              <option value="CLASS">班级</option>
            </select>
            {form.scope === 'DEPT' && (
              <input
                type="number"
                value={form.deptId}
                onChange={(e) => setForm((f) => ({ ...f, deptId: e.target.value }))}
                placeholder="院系ID"
                className="w-28 rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
              />
            )}
            {form.scope === 'CLASS' && (
              <input
                type="number"
                value={form.classId}
                onChange={(e) => setForm((f) => ({ ...f, classId: e.target.value }))}
                placeholder="班级ID"
                className="w-28 rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
              />
            )}
            <select
              value={form.targetRoles}
              onChange={(e) => setForm((f) => ({ ...f, targetRoles: e.target.value as NoticeTargetRole }))}
              className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
            >
              <option value="ALL">全部角色</option>
              <option value="TEACHER">仅教师</option>
              <option value="STUDENT">仅学生</option>
            </select>
          </div>
          <button
            onClick={handleSubmit}
            disabled={!canSubmit || publish.isPending}
            className="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {publish.isPending ? '发布中…' : '发布'}
          </button>
          {publish.isSuccess && (
            <p className="text-xs text-green-600">
              发布成功，已推送 {publish.data?.sendCount ?? 0} 人。
            </p>
          )}
          {publish.isError && <p className="text-xs text-red-500">发布失败，请检查范围参数</p>}
        </div>
      )}
    </div>
  )
}
