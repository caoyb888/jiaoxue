import { useState, useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { examPublishApi, useMyClasses, useExamPapers } from '@edu/api'
import type { ExamPublishCreateDTO } from '@edu/api'

/**
 * 考试发布配置页（S5-01）。
 * 教师选择班级 + 试卷，配置时间/时长/密码/监考/人脸核验/乱序，发布为一场正式考试。
 * 入口：试卷管理页「发布考试」按钮，或在线考试列表页「发布考试」。
 */
export function PublishConfigPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()

  const { data: classes } = useMyClasses()
  const [classId, setClassId] = useState<number | null>(
    params.get('classId') ? Number(params.get('classId')) : null,
  )
  const effectiveClassId = classId ?? classes?.[0]?.id ?? null

  const { data: papers } = useExamPapers(effectiveClassId ?? 0)
  const [paperId, setPaperId] = useState<number | null>(
    params.get('paperId') ? Number(params.get('paperId')) : null,
  )
  const effectivePaperId = paperId ?? papers?.[0]?.id ?? null

  // 配置项
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [durationMin, setDurationMin] = useState(60)
  const [password, setPassword] = useState('')
  const [enableMonitor, setEnableMonitor] = useState(0)
  const [faceVerifyType, setFaceVerifyType] = useState(0)
  const [allowCopy, setAllowCopy] = useState(0)
  const [shuffleQuestion, setShuffleQuestion] = useState(0)
  const [shuffleOption, setShuffleOption] = useState(0)

  const [error, setError] = useState<string | null>(null)

  const publishMutation = useMutation({
    mutationFn: (dto: ExamPublishCreateDTO) => examPublishApi.publish(dto),
    onSuccess: () => navigate('/exam/list'),
    onError: (err: Error) => setError(err.message || '发布失败，请重试'),
  })

  // datetime-local 给出的值可能缺秒，补足为 ISO_LOCAL_DATE_TIME
  const toIso = (v: string) => (v.length === 16 ? `${v}:00` : v)

  const valid = useMemo(
    () => !!effectiveClassId && !!effectivePaperId && !!startTime && !!endTime && endTime > startTime && durationMin >= 1,
    [effectiveClassId, effectivePaperId, startTime, endTime, durationMin],
  )

  function handleSubmit() {
    setError(null)
    if (!valid || !effectiveClassId || !effectivePaperId) {
      setError('请完整填写班级、试卷与有效的考试时间窗口（截止须晚于开始）')
      return
    }
    publishMutation.mutate({
      paperId: effectivePaperId,
      classId: effectiveClassId,
      startTime: toIso(startTime),
      endTime: toIso(endTime),
      durationMin,
      password: password.trim() || undefined,
      enableMonitor,
      faceVerifyType,
      allowCopy,
      shuffleQuestion,
      shuffleOption,
    })
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b bg-white px-6 py-4">
        <div className="mx-auto flex max-w-2xl items-center justify-between">
          <h1 className="text-lg font-semibold text-gray-900">发布考试</h1>
          <Link to="/exam/list" className="text-sm text-gray-500 hover:text-gray-700">返回考试列表</Link>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-6 py-8">
        <div className="space-y-6 rounded-xl border bg-white p-6 shadow-sm">
          {/* 班级 + 试卷 */}
          <Field label="班级">
            <select
              value={effectiveClassId ?? ''}
              onChange={(e) => { setClassId(Number(e.target.value)); setPaperId(null) }}
              className={selectCls}
            >
              {(classes ?? []).length === 0 && <option value="">无可选班级</option>}
              {classes?.map((c) => <option key={c.id} value={c.id}>{c.className}（{c.courseName}）</option>)}
            </select>
          </Field>

          <Field label="试卷">
            <select value={effectivePaperId ?? ''} onChange={(e) => setPaperId(Number(e.target.value))} className={selectCls}>
              {(papers ?? []).length === 0 && <option value="">该班级暂无试卷，请先到「试卷管理」组卷</option>}
              {papers?.map((p) => <option key={p.id} value={p.id}>{p.paperName}（{p.questionCount}题/{p.totalScore}分）</option>)}
            </select>
          </Field>

          {/* 时间窗口 */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="开始时间">
              <input type="datetime-local" value={startTime} onChange={(e) => setStartTime(e.target.value)} className={inputCls} />
            </Field>
            <Field label="截止时间">
              <input type="datetime-local" value={endTime} onChange={(e) => setEndTime(e.target.value)} className={inputCls} />
            </Field>
          </div>

          <Field label="考试时长（分钟）" hint="1–600 分钟，到时自动交卷">
            <input
              type="number" min={1} max={600} value={durationMin}
              onChange={(e) => setDurationMin(Number(e.target.value))}
              className={inputCls}
            />
          </Field>

          <Field label="考试密码" hint="留空表示不设密码">
            <input
              type="text" value={password} onChange={(e) => setPassword(e.target.value)}
              placeholder="可选" className={inputCls}
            />
          </Field>

          {/* 监考与防作弊 */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="在线监考">
              <select value={enableMonitor} onChange={(e) => setEnableMonitor(Number(e.target.value))} className={selectCls}>
                <option value={0}>关闭</option>
                <option value={1}>开启（切屏/复制监测）</option>
              </select>
            </Field>
            <Field label="人脸核验">
              <select value={faceVerifyType} onChange={(e) => setFaceVerifyType(Number(e.target.value))} className={selectCls}>
                <option value={0}>不核验</option>
                <option value={1}>证件照比对</option>
                <option value={2}>现场拍照核验</option>
              </select>
            </Field>
            <Field label="复制粘贴">
              <select value={allowCopy} onChange={(e) => setAllowCopy(Number(e.target.value))} className={selectCls}>
                <option value={0}>禁止复制</option>
                <option value={1}>允许复制</option>
              </select>
            </Field>
            <Field label="题目乱序">
              <select value={shuffleQuestion} onChange={(e) => setShuffleQuestion(Number(e.target.value))} className={selectCls}>
                <option value={0}>固定顺序</option>
                <option value={1}>乱序出题</option>
              </select>
            </Field>
            <Field label="选项乱序">
              <select value={shuffleOption} onChange={(e) => setShuffleOption(Number(e.target.value))} className={selectCls}>
                <option value={0}>固定顺序</option>
                <option value={1}>乱序选项</option>
              </select>
            </Field>
          </div>

          {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

          <div className="flex justify-end gap-3 border-t pt-4">
            <Link to="/exam/list" className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-600 hover:bg-gray-50">取消</Link>
            <button
              onClick={handleSubmit}
              disabled={!valid || publishMutation.isPending}
              className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:bg-blue-300"
            >
              {publishMutation.isPending ? '发布中…' : '发布考试'}
            </button>
          </div>
        </div>
      </main>
    </div>
  )
}

const inputCls = 'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500'
const selectCls = inputCls

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label className="block text-sm font-medium text-gray-700">{label}</label>
      {children}
      {hint && <p className="text-xs text-gray-400">{hint}</p>}
    </div>
  )
}
