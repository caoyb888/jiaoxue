import React, { useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import {
  useQuestionBanks,
  useQuestions,
  useExamPapers,
  usePaperQuestions,
  useCreatePaper,
  useDeletePaper,
  useAddQuestionToPaper,
  useUpdatePaperQuestion,
  useRemovePaperQuestion,
  useReorderPaperQuestions,
  QUESTION_TYPES,
  OPTION_TYPES,
} from '@edu/api'
import type { ExamPaperVO, PaperQuestionVO, QuestionVO } from '@edu/api'

// ─── Top-level page ───────────────────────────────────────────────────────────

export default function ExamPaperPage() {
  const [classId] = useState<number>(1) // TODO: get from route params / course selector
  const [selectedPaperId, setSelectedPaperId] = useState<number | null>(null)
  const [selectedBankId, setSelectedBankId] = useState<number | null>(null)
  const [typeFilter, setTypeFilter] = useState<number | null>(null)
  const [previewMode, setPreviewMode] = useState(false)
  const [showCreatePaper, setShowCreatePaper] = useState(false)
  const [newPaperName, setNewPaperName] = useState('')
  const [newPaperDesc, setNewPaperDesc] = useState('')

  const { data: papers = [], isLoading: papersLoading } = useExamPapers(classId)
  const { data: banks = [] } = useQuestionBanks()
  const { data: questionPage } = useQuestions(selectedBankId, undefined)
  const { data: paperQuestions = [], isLoading: pqLoading } = usePaperQuestions(selectedPaperId)

  const createPaper = useCreatePaper()
  const deletePaper = useDeletePaper()
  const addQuestion = useAddQuestionToPaper()
  const removeQuestion = useRemovePaperQuestion()
  const reorder = useReorderPaperQuestions()

  const questions = questionPage?.list ?? []
  const filteredQuestions = typeFilter
    ? questions.filter((q) => q.type === typeFilter)
    : questions

  const selectedPaper = papers.find((p) => p.id === selectedPaperId)

  const paperQuestionIds = useMemo(
    () => new Set(paperQuestions.map((pq) => pq.questionId)),
    [paperQuestions]
  )

  const totalScore = useMemo(
    () => paperQuestions.reduce((sum, pq) => sum + parseFloat(pq.score || '0'), 0),
    [paperQuestions]
  )

  function handleCreatePaper(e: React.FormEvent) {
    e.preventDefault()
    if (!newPaperName.trim()) return
    createPaper.mutate(
      { classId, paperName: newPaperName.trim(), description: newPaperDesc.trim() || undefined },
      {
        onSuccess: (res) => {
          setShowCreatePaper(false)
          setNewPaperName('')
          setNewPaperDesc('')
          setSelectedPaperId(res.id ?? null)
        },
      }
    )
  }

  function handleDeletePaper(paper: ExamPaperVO) {
    if (!confirm(`确认删除试卷「${paper.paperName}」？此操作不可恢复。`)) return
    deletePaper.mutate({ paperId: paper.id, classId })
    if (selectedPaperId === paper.id) setSelectedPaperId(null)
  }

  function handleAddQuestion(q: QuestionVO) {
    if (!selectedPaperId) return
    addQuestion.mutate({ paperId: selectedPaperId, dto: { questionId: q.id, score: q.score } })
  }

  function handleRemoveQuestion(pq: PaperQuestionVO) {
    if (!selectedPaperId) return
    removeQuestion.mutate({ paperId: selectedPaperId, pqId: pq.id })
  }

  function handleMoveUp(idx: number) {
    if (!selectedPaperId || idx === 0) return
    const ids = paperQuestions.map((pq) => pq.id)
    ;[ids[idx - 1], ids[idx]] = [ids[idx], ids[idx - 1]]
    reorder.mutate({ paperId: selectedPaperId, orderedIds: ids })
  }

  function handleMoveDown(idx: number) {
    if (!selectedPaperId || idx === paperQuestions.length - 1) return
    const ids = paperQuestions.map((pq) => pq.id)
    ;[ids[idx], ids[idx + 1]] = [ids[idx + 1], ids[idx]]
    reorder.mutate({ paperId: selectedPaperId, orderedIds: ids })
  }

  return (
    <div className="flex h-screen bg-gray-50">
      {/* ── 左栏：试卷列表 ── */}
      <aside className="flex w-52 flex-col border-r bg-white">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <span className="text-sm font-semibold text-gray-900">试卷列表</span>
          <button
            onClick={() => setShowCreatePaper(true)}
            className="rounded-full bg-blue-500 px-2 py-0.5 text-xs text-white hover:bg-blue-600"
          >
            + 新建
          </button>
        </div>
        <div className="flex-1 overflow-y-auto py-2">
          {papersLoading && (
            <div className="flex h-16 items-center justify-center text-xs text-gray-400">加载中...</div>
          )}
          {papers.map((paper) => (
            <PaperItem
              key={paper.id}
              paper={paper}
              active={paper.id === selectedPaperId}
              onClick={() => {
                setSelectedPaperId(paper.id)
                setPreviewMode(false)
              }}
              onDelete={() => handleDeletePaper(paper)}
            />
          ))}
          {!papersLoading && papers.length === 0 && (
            <p className="px-4 py-3 text-xs text-gray-400">暂无试卷，点击上方「新建」</p>
          )}
        </div>
      </aside>

      {/* ── 中栏：题库 + 题目选择 ── */}
      <section className="flex w-80 flex-col border-r bg-white">
        <div className="border-b px-4 py-3">
          <p className="mb-2 text-xs font-semibold text-gray-500 uppercase tracking-wide">题目选择</p>
          <select
            value={selectedBankId ?? ''}
            onChange={(e) => setSelectedBankId(e.target.value ? Number(e.target.value) : null)}
            className="w-full rounded-lg border border-gray-300 px-2 py-1.5 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
          >
            <option value="">-- 选择题库 --</option>
            {banks.map((b) => (
              <option key={b.id} value={b.id}>{b.bankName}</option>
            ))}
          </select>
          {selectedBankId && (
            <div className="mt-2 flex flex-wrap gap-1">
              <TypeFilterButton active={typeFilter === null} onClick={() => setTypeFilter(null)}>全部</TypeFilterButton>
              {Object.entries(QUESTION_TYPES).map(([code, name]) => (
                <TypeFilterButton
                  key={code}
                  active={typeFilter === Number(code)}
                  onClick={() => setTypeFilter(typeFilter === Number(code) ? null : Number(code))}
                >
                  {name}
                </TypeFilterButton>
              ))}
            </div>
          )}
        </div>

        <div className="flex-1 overflow-y-auto p-3 space-y-2">
          {!selectedBankId && (
            <p className="mt-8 text-center text-xs text-gray-400">请先选择题库</p>
          )}
          {filteredQuestions.map((q) => (
            <BankQuestionItem
              key={q.id}
              question={q}
              inPaper={paperQuestionIds.has(q.id)}
              paperSelected={!!selectedPaperId}
              onAdd={() => handleAddQuestion(q)}
              isAdding={addQuestion.isPending}
            />
          ))}
          {selectedBankId && filteredQuestions.length === 0 && (
            <p className="mt-8 text-center text-xs text-gray-400">该题库暂无{typeFilter ? QUESTION_TYPES[typeFilter] : ''}题目</p>
          )}
        </div>
      </section>

      {/* ── 右栏：试卷编辑 / 预览 ── */}
      <main className="flex flex-1 flex-col overflow-hidden">
        {/* 顶栏 */}
        <div className="flex items-center justify-between border-b bg-white px-6 py-3">
          <div>
            {selectedPaper ? (
              <>
                <h1 className="text-base font-semibold text-gray-900">{selectedPaper.paperName}</h1>
                <p className="text-xs text-gray-400">
                  共 {paperQuestions.length} 题 · 总分 {totalScore.toFixed(1)} 分
                  {selectedPaper.description && ` · ${selectedPaper.description}`}
                </p>
              </>
            ) : (
              <h1 className="text-base font-semibold text-gray-400">请在左侧选择试卷</h1>
            )}
          </div>
          {selectedPaper && (
            <div className="flex items-center gap-2">
              <button
                onClick={() => setPreviewMode((v) => !v)}
                className={`rounded-lg px-4 py-1.5 text-sm transition-colors ${
                  previewMode
                    ? 'bg-gray-800 text-white'
                    : 'border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                {previewMode ? '退出预览' : '预览试卷'}
              </button>
              <Link
                to={`/exam/publish?paperId=${selectedPaper.id}&classId=${classId}`}
                className="rounded-lg bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
              >
                发布考试
              </Link>
            </div>
          )}
        </div>

        {/* 试卷题目列表 */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          {!selectedPaperId && (
            <EmptyHint text="请在左侧选择试卷，从中栏选题组卷" />
          )}

          {selectedPaperId && pqLoading && (
            <div className="flex h-48 items-center justify-center">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
            </div>
          )}

          {selectedPaperId && !pqLoading && paperQuestions.length === 0 && (
            <EmptyHint text="试卷暂无题目，从中栏点击「加入」添加题目" />
          )}

          {previewMode ? (
            <PaperPreview questions={paperQuestions} />
          ) : (
            <div className="space-y-3">
              {paperQuestions.map((pq, idx) => (
                <PaperQuestionCard
                  key={pq.id}
                  pq={pq}
                  idx={idx}
                  total={paperQuestions.length}
                  paperId={selectedPaperId!}
                  onMoveUp={() => handleMoveUp(idx)}
                  onMoveDown={() => handleMoveDown(idx)}
                  onRemove={() => handleRemoveQuestion(pq)}
                />
              ))}
            </div>
          )}
        </div>

        {/* 底部统计栏 */}
        {selectedPaperId && !previewMode && (
          <div className="border-t bg-white px-6 py-3 flex items-center gap-6 text-sm text-gray-600">
            <span>题目数：<strong className="text-gray-900">{paperQuestions.length}</strong></span>
            <span>总分：<strong className="text-blue-600">{totalScore.toFixed(1)} 分</strong></span>
            <span className="text-xs text-gray-400">调整分值请点击题目卡片右侧的分值输入框</span>
          </div>
        )}
      </main>

      {/* ── 新建试卷对话框 ── */}
      {showCreatePaper && (
        <Modal title="新建试卷" onClose={() => setShowCreatePaper(false)}>
          <form onSubmit={handleCreatePaper} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">试卷名称 *</label>
              <input
                autoFocus
                required
                value={newPaperName}
                onChange={(e) => setNewPaperName(e.target.value)}
                placeholder="如：第一章单元测试"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">描述（可选）</label>
              <input
                value={newPaperDesc}
                onChange={(e) => setNewPaperDesc(e.target.value)}
                placeholder="试卷说明..."
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setShowCreatePaper(false)}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="submit"
                disabled={createPaper.isPending}
                className="rounded-lg bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600 disabled:opacity-60"
              >
                {createPaper.isPending ? '创建中...' : '创建'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function PaperItem({ paper, active, onClick, onDelete }: {
  paper: ExamPaperVO
  active: boolean
  onClick: () => void
  onDelete: () => void
}) {
  return (
    <div
      className={`group flex cursor-pointer items-center justify-between px-4 py-2.5 transition-colors ${
        active ? 'bg-blue-50' : 'hover:bg-gray-50'
      }`}
      onClick={onClick}
    >
      <div className="min-w-0 flex-1">
        <p className={`truncate text-sm ${active ? 'font-medium text-blue-700' : 'text-gray-700'}`}>
          {paper.paperName}
        </p>
        <p className="text-xs text-gray-400">{paper.questionCount} 题 · {paper.totalScore} 分</p>
      </div>
      <button
        onClick={(e) => { e.stopPropagation(); onDelete() }}
        className="ml-2 flex-shrink-0 text-gray-300 opacity-0 group-hover:opacity-100 hover:text-red-400 transition-all"
        title="删除试卷"
      >
        <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
  )
}

function TypeFilterButton({ children, active, onClick }: {
  children: React.ReactNode
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full px-2 py-0.5 text-xs transition-colors ${
        active ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
      }`}
    >
      {children}
    </button>
  )
}

function BankQuestionItem({ question, inPaper, paperSelected, onAdd, isAdding }: {
  question: QuestionVO
  inPaper: boolean
  paperSelected: boolean
  onAdd: () => void
  isAdding: boolean
}) {
  const typeColor: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-purple-100 text-purple-700',
    3: 'bg-green-100 text-green-700',
    4: 'bg-yellow-100 text-yellow-700',
    5: 'bg-orange-100 text-orange-700',
    6: 'bg-pink-100 text-pink-700',
  }
  return (
    <div className={`rounded-lg border p-2.5 text-sm transition-colors ${inPaper ? 'border-green-200 bg-green-50' : 'border-gray-200 bg-white hover:border-blue-300'}`}>
      <div className="flex items-start gap-2">
        <span className={`mt-0.5 flex-shrink-0 rounded-full px-1.5 py-0.5 text-xs font-medium ${typeColor[question.type] ?? 'bg-gray-100 text-gray-700'}`}>
          {QUESTION_TYPES[question.type]}
        </span>
        <p className="flex-1 line-clamp-2 text-xs leading-relaxed text-gray-700">{question.content}</p>
      </div>
      <div className="mt-2 flex items-center justify-between">
        <span className="text-xs text-gray-400">{question.score} 分</span>
        {inPaper ? (
          <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-600">已加入</span>
        ) : (
          <button
            disabled={!paperSelected || isAdding}
            onClick={onAdd}
            className="rounded-full bg-blue-500 px-2 py-0.5 text-xs text-white hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {paperSelected ? '加入' : '先选试卷'}
          </button>
        )}
      </div>
    </div>
  )
}

function PaperQuestionCard({ pq, idx, total, paperId, onMoveUp, onMoveDown, onRemove }: {
  pq: PaperQuestionVO
  idx: number
  total: number
  paperId: number
  onMoveUp: () => void
  onMoveDown: () => void
  onRemove: () => void
}) {
  const updatePQ = useUpdatePaperQuestion()
  const [editScore, setEditScore] = useState(pq.score)

  function commitScore() {
    const parsed = parseFloat(editScore)
    if (isNaN(parsed) || parsed <= 0 || editScore === pq.score) {
      setEditScore(pq.score)
      return
    }
    updatePQ.mutate({ paperId, pqId: pq.id, dto: { score: parsed.toFixed(2) } })
  }

  const typeColor: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-purple-100 text-purple-700',
    3: 'bg-green-100 text-green-700',
    4: 'bg-yellow-100 text-yellow-700',
    5: 'bg-orange-100 text-orange-700',
    6: 'bg-pink-100 text-pink-700',
  }

  return (
    <div className="rounded-xl bg-white p-4 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-start gap-3">
        {/* 排序序号 */}
        <div className="flex flex-col items-center gap-0.5">
          <span className="flex h-6 w-6 items-center justify-center rounded-full bg-gray-100 text-xs font-bold text-gray-500">
            {idx + 1}
          </span>
          <button
            disabled={idx === 0}
            onClick={onMoveUp}
            className="text-gray-300 hover:text-blue-500 disabled:opacity-20"
            title="上移"
          >
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
            </svg>
          </button>
          <button
            disabled={idx === total - 1}
            onClick={onMoveDown}
            className="text-gray-300 hover:text-blue-500 disabled:opacity-20"
            title="下移"
          >
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>

        {/* 题目内容 */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1.5">
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${typeColor[pq.questionType] ?? 'bg-gray-100 text-gray-700'}`}>
              {QUESTION_TYPES[pq.questionType] ?? '未知'}
            </span>
          </div>
          <p className="text-sm text-gray-800 leading-relaxed line-clamp-3">{pq.content}</p>

          {OPTION_TYPES.has(pq.questionType) && pq.options.length > 0 && (
            <div className="mt-2 grid grid-cols-2 gap-1">
              {pq.options.map((opt) => (
                <div key={opt.id} className="text-xs text-gray-500">
                  {opt.optionLabel}. {opt.content}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 右侧操作 */}
        <div className="flex flex-col items-end gap-2 flex-shrink-0">
          <button onClick={onRemove} className="text-gray-300 hover:text-red-400 transition-colors" title="移出试卷">
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          <div className="flex items-center gap-1">
            <input
              type="number"
              min="0.5"
              step="0.5"
              value={editScore}
              onChange={(e) => setEditScore(e.target.value)}
              onBlur={commitScore}
              onKeyDown={(e) => { if (e.key === 'Enter') commitScore() }}
              className="w-14 rounded border border-gray-200 px-1.5 py-0.5 text-right text-sm text-blue-600 font-medium outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-400"
            />
            <span className="text-xs text-gray-400">分</span>
          </div>
        </div>
      </div>
    </div>
  )
}

function PaperPreview({ questions }: { questions: PaperQuestionVO[] }) {
  if (questions.length === 0) return null
  return (
    <div className="mx-auto max-w-2xl space-y-6 py-4">
      {questions.map((pq, idx) => (
        <div key={pq.id} className="rounded-xl bg-white p-5 shadow-sm">
          <p className="mb-3 text-sm font-medium text-gray-800">
            {idx + 1}. {pq.content}
            <span className="ml-2 text-xs text-gray-400">（{pq.score} 分）</span>
          </p>

          {OPTION_TYPES.has(pq.questionType) && pq.options.length > 0 && (
            <div className="space-y-2">
              {pq.options.map((opt) => (
                <label key={opt.id} className="flex cursor-pointer items-center gap-3">
                  <span className={`flex h-5 w-5 items-center justify-center rounded-full border-2 border-gray-300 text-xs ${
                    pq.questionType === 2 ? 'rounded-md' : 'rounded-full'
                  }`} />
                  <span className="text-sm text-gray-700">{opt.optionLabel}. {opt.content}</span>
                </label>
              ))}
            </div>
          )}

          {pq.questionType === 3 && (
            <div className="flex gap-4">
              {['正确', '错误'].map((label) => (
                <label key={label} className="flex cursor-pointer items-center gap-2">
                  <span className="flex h-5 w-5 items-center justify-center rounded-full border-2 border-gray-300" />
                  <span className="text-sm text-gray-700">{label}</span>
                </label>
              ))}
            </div>
          )}

          {pq.questionType === 4 && (
            <div className="mt-2 rounded-lg border border-dashed border-gray-300 px-4 py-3 text-sm text-gray-400">填写答案...</div>
          )}

          {pq.questionType === 5 && (
            <div className="mt-2 rounded-lg border border-dashed border-gray-300 px-4 py-6 text-sm text-gray-400">作答区域...</div>
          )}
        </div>
      ))}
    </div>
  )
}

function EmptyHint({ text }: { text: string }) {
  return (
    <div className="flex h-48 items-center justify-center text-gray-400 text-sm">{text}</div>
  )
}

function Modal({ title, onClose, children }: {
  title: string
  onClose: () => void
  children: React.ReactNode
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40">
      <div className="w-full max-w-md rounded-2xl bg-white shadow-2xl">
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-base font-semibold text-gray-900">{title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">✕</button>
        </div>
        <div className="px-6 py-4">{children}</div>
      </div>
    </div>
  )
}
