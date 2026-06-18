import React, { useState } from 'react'
import {
  useQuestionBanks,
  useQuestions,
  useCreateBank,
  useCreateQuestion,
  useDeleteQuestion,
} from '@edu/api'
import type { QuestionBankVO, QuestionVO, QuestionCreateDTO } from '@edu/api'
import { QUESTION_TYPES } from '@edu/api'
import { QuestionForm } from './components/QuestionForm'

export default function QuestionBankPage() {
  const [selectedBankId, setSelectedBankId] = useState<number | null>(null)
  const [keyword, setKeyword] = useState('')
  const [showCreateBank, setShowCreateBank] = useState(false)
  const [showCreateQuestion, setShowCreateQuestion] = useState(false)
  const [bankFormName, setBankFormName] = useState('')

  const { data: banks = [], isLoading: banksLoading } = useQuestionBanks()
  const { data: questionPage, isLoading: questionsLoading } = useQuestions(
    selectedBankId,
    keyword || undefined
  )

  const createBank = useCreateBank()
  const createQuestion = useCreateQuestion()
  const deleteQuestion = useDeleteQuestion()

  function handleCreateBank(e: React.FormEvent) {
    e.preventDefault()
    if (!bankFormName.trim()) return
    createBank.mutate(
      { bankName: bankFormName.trim(), isPublic: 0 },
      {
        onSuccess: (res) => {
          setShowCreateBank(false)
          setBankFormName('')
          setSelectedBankId(res.data?.id ?? null)
        },
      }
    )
  }

  function handleCreateQuestion(dto: QuestionCreateDTO) {
    createQuestion.mutate(dto, {
      onSuccess: () => setShowCreateQuestion(false),
    })
  }

  function handleDeleteQuestion(q: QuestionVO) {
    if (!confirm(`确认删除题目？\n${q.content.slice(0, 50)}`)) return
    deleteQuestion.mutate({ questionId: q.id, bankId: q.bankId })
  }

  const selectedBank = banks.find((b) => b.id === selectedBankId)
  const questions = questionPage?.list ?? []

  return (
    <div className="flex h-screen bg-gray-50">
      {/* ── 左侧题库列表 ── */}
      <aside className="flex w-56 flex-col border-r bg-white lg:w-64">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <span className="text-sm font-semibold text-gray-900">我的题库</span>
          <button
            onClick={() => setShowCreateBank(true)}
            className="rounded-full bg-blue-500 px-2 py-0.5 text-xs text-white hover:bg-blue-600"
          >
            + 新建
          </button>
        </div>

        <div className="flex-1 overflow-y-auto py-2">
          {banksLoading && (
            <div className="flex h-16 items-center justify-center text-gray-400 text-xs">
              加载中...
            </div>
          )}
          {banks.map((bank) => (
            <BankItem
              key={bank.id}
              bank={bank}
              active={bank.id === selectedBankId}
              onClick={() => {
                setSelectedBankId(bank.id)
                setKeyword('')
              }}
            />
          ))}
          {!banksLoading && banks.length === 0 && (
            <p className="px-4 py-3 text-xs text-gray-400">暂无题库，点击上方「新建」</p>
          )}
        </div>
      </aside>

      {/* ── 右侧题目列表 ── */}
      <main className="flex flex-1 flex-col overflow-hidden">
        {/* 顶栏 */}
        <div className="flex items-center justify-between border-b bg-white px-6 py-3">
          <div>
            <h1 className="text-base font-semibold text-gray-900">
              {selectedBank ? selectedBank.bankName : '请选择题库'}
            </h1>
            {selectedBank && (
              <p className="text-xs text-gray-400">
                {selectedBank.isPublic === 1 ? '院系共享' : '私有'} ·{' '}
                共 {questionPage?.total ?? 0} 题
              </p>
            )}
          </div>
          {selectedBankId && (
            <div className="flex items-center gap-3">
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="搜索题目..."
                className="w-48 rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
              <button
                onClick={() => setShowCreateQuestion(true)}
                className="rounded-lg bg-blue-500 px-4 py-1.5 text-sm text-white hover:bg-blue-600"
              >
                + 新建题目
              </button>
            </div>
          )}
        </div>

        {/* 题目列表 */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          {!selectedBankId && (
            <div className="flex h-64 items-center justify-center text-gray-400">
              请在左侧选择或新建题库
            </div>
          )}

          {selectedBankId && questionsLoading && (
            <div className="flex h-48 items-center justify-center">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
            </div>
          )}

          {selectedBankId && !questionsLoading && questions.length === 0 && (
            <div className="flex h-48 flex-col items-center justify-center gap-3 text-gray-400">
              <span>题库暂无题目</span>
              <button
                onClick={() => setShowCreateQuestion(true)}
                className="rounded-lg bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600"
              >
                创建第一道题目
              </button>
            </div>
          )}

          <div className="space-y-3">
            {questions.map((q) => (
              <QuestionCard
                key={q.id}
                question={q}
                onDelete={() => handleDeleteQuestion(q)}
              />
            ))}
          </div>
        </div>
      </main>

      {/* ── 新建题库对话框 ── */}
      {showCreateBank && (
        <Modal title="新建题库" onClose={() => setShowCreateBank(false)}>
          <form onSubmit={handleCreateBank} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">题库名称 *</label>
              <input
                autoFocus
                required
                value={bankFormName}
                onChange={(e) => setBankFormName(e.target.value)}
                placeholder="如：Java程序设计题库"
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setShowCreateBank(false)}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="submit"
                disabled={createBank.isPending}
                className="rounded-lg bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600 disabled:opacity-60"
              >
                {createBank.isPending ? '创建中...' : '创建'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── 新建题目对话框 ── */}
      {showCreateQuestion && selectedBankId && (
        <Modal title="新建题目" onClose={() => setShowCreateQuestion(false)} wide>
          <QuestionForm
            bankId={selectedBankId}
            onSubmit={handleCreateQuestion}
            onCancel={() => setShowCreateQuestion(false)}
            isSubmitting={createQuestion.isPending}
          />
        </Modal>
      )}
    </div>
  )
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function BankItem({ bank, active, onClick }: {
  bank: QuestionBankVO
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className={`w-full px-4 py-2.5 text-left text-sm transition-colors ${
        active
          ? 'bg-blue-50 font-medium text-blue-700'
          : 'text-gray-700 hover:bg-gray-50'
      }`}
    >
      <div className="truncate">{bank.bankName}</div>
      {bank.isPublic === 1 && (
        <div className="mt-0.5 text-xs text-blue-400">院系共享</div>
      )}
    </button>
  )
}

function QuestionCard({ question, onDelete }: {
  question: QuestionVO
  onDelete: () => void
}) {
  const typeName = QUESTION_TYPES[question.type] ?? '未知'
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
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-2">
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${typeColor[question.type] ?? 'bg-gray-100 text-gray-700'}`}>
              {typeName}
            </span>
            <span className="text-xs text-gray-400">{question.score} 分</span>
          </div>
          <p className="text-sm text-gray-800 leading-relaxed line-clamp-3">{question.content}</p>

          {question.options.length > 0 && (
            <div className="mt-2 grid grid-cols-2 gap-1 sm:grid-cols-4">
              {question.options.map((opt) => (
                <div
                  key={opt.id}
                  className={`rounded px-2 py-0.5 text-xs ${
                    opt.isCorrect === 1
                      ? 'bg-green-50 text-green-700 font-medium'
                      : 'text-gray-500'
                  }`}
                >
                  {opt.optionLabel}. {opt.content}
                </div>
              ))}
            </div>
          )}

          {question.answer && question.options.length === 0 && (
            <p className="mt-1.5 text-xs text-gray-400">
              答案：{question.answer.slice(0, 60)}{question.answer.length > 60 ? '...' : ''}
            </p>
          )}
        </div>
        <button
          onClick={onDelete}
          className="flex-shrink-0 text-gray-300 hover:text-red-400 transition-colors"
          title="删除题目"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </div>
    </div>
  )
}

function Modal({ title, onClose, children, wide = false }: {
  title: string
  onClose: () => void
  children: React.ReactNode
  wide?: boolean
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40">
      <div
        className={`w-full rounded-2xl bg-white shadow-2xl ${wide ? 'max-w-2xl' : 'max-w-md'}`}
      >
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-base font-semibold text-gray-900">{title}</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>
        <div className="max-h-[80vh] overflow-y-auto px-6 py-4">{children}</div>
      </div>
    </div>
  )
}
