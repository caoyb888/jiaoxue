import React, { useState } from 'react'
import type { QuestionCreateDTO, QuestionOptionDTO } from '@edu/api'
import { QUESTION_TYPES, OPTION_TYPES } from '@edu/api'

interface Props {
  bankId: number
  onSubmit: (dto: QuestionCreateDTO) => void
  onCancel: () => void
  isSubmitting: boolean
}

const OPTION_LABELS = ['A', 'B', 'C', 'D', 'E']
const DEFAULT_OPTION_COUNT = 4

function defaultOptions(count = DEFAULT_OPTION_COUNT): QuestionOptionDTO[] {
  return OPTION_LABELS.slice(0, count).map((label) => ({
    optionLabel: label,
    content: '',
    isCorrect: 0,
  }))
}

export function QuestionForm({ bankId, onSubmit, onCancel, isSubmitting }: Props) {
  const [type, setType] = useState(1)
  const [content, setContent] = useState('')
  const [answer, setAnswer] = useState('')
  const [analysis, setAnalysis] = useState('')
  const [score, setScore] = useState('2.00')
  const [options, setOptions] = useState<QuestionOptionDTO[]>(defaultOptions())

  const hasOptions = OPTION_TYPES.has(type)
  const isTrueFalse = type === 3
  const isMultiChoice = type === 2

  function handleTypeChange(newType: number) {
    setType(newType)
    setAnswer('')
    if (OPTION_TYPES.has(newType)) {
      setOptions(defaultOptions())
    }
  }

  function handleOptionContentChange(idx: number, value: string) {
    setOptions((prev) =>
      prev.map((o, i) => (i === idx ? { ...o, content: value } : o))
    )
  }

  function handleSingleCorrect(idx: number) {
    setOptions((prev) =>
      prev.map((o, i) => ({ ...o, isCorrect: i === idx ? 1 : 0 }))
    )
    setAnswer(OPTION_LABELS[idx])
  }

  function handleMultiCorrectToggle(idx: number) {
    const next = options.map((o, i) =>
      i === idx ? { ...o, isCorrect: o.isCorrect === 1 ? 0 : 1 } : o
    )
    setOptions(next)
    setAnswer(
      next
        .filter((o) => o.isCorrect === 1)
        .map((o) => o.optionLabel)
        .join(',')
    )
  }

  function addOption() {
    if (options.length >= 5) return
    setOptions((prev) => [
      ...prev,
      { optionLabel: OPTION_LABELS[prev.length], content: '', isCorrect: 0 },
    ])
  }

  function removeOption(idx: number) {
    if (options.length <= 2) return
    setOptions((prev) => {
      const next = prev.filter((_, i) => i !== idx)
      return next.map((o, i) => ({ ...o, optionLabel: OPTION_LABELS[i] }))
    })
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const dto: QuestionCreateDTO = {
      bankId,
      type,
      content: content.trim(),
      answer: answer.trim() || undefined,
      analysis: analysis.trim() || undefined,
      score,
      options: hasOptions ? options : undefined,
    }
    onSubmit(dto)
  }

  const inputClass =
    'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500'
  const labelClass = 'block text-sm font-medium text-gray-700 mb-1'

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {/* 题型选择 */}
      <div>
        <label className={labelClass}>题型</label>
        <div className="flex flex-wrap gap-2">
          {Object.entries(QUESTION_TYPES).map(([code, name]) => (
            <button
              key={code}
              type="button"
              onClick={() => handleTypeChange(Number(code))}
              className={`rounded-full px-3 py-1 text-sm transition-colors ${
                type === Number(code)
                  ? 'bg-blue-500 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {name}
            </button>
          ))}
        </div>
      </div>

      {/* 题干 */}
      <div>
        <label className={labelClass}>题干 *</label>
        <textarea
          required
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={3}
          placeholder="请输入题目内容..."
          className={inputClass}
        />
      </div>

      {/* 选项（单选/多选/投票） */}
      {hasOptions && (
        <div>
          <label className={labelClass}>
            选项
            {isMultiChoice && (
              <span className="ml-2 text-xs text-blue-500">（多选：勾选所有正确选项）</span>
            )}
            {type === 1 && (
              <span className="ml-2 text-xs text-blue-500">（单选：点击选中正确答案）</span>
            )}
            {type === 6 && (
              <span className="ml-2 text-xs text-gray-400">（投票题无正确答案）</span>
            )}
          </label>
          <div className="space-y-2">
            {options.map((opt, idx) => (
              <div key={opt.optionLabel} className="flex items-center gap-2">
                {type === 1 && (
                  <button
                    type="button"
                    onClick={() => handleSingleCorrect(idx)}
                    className={`flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full border-2 text-xs font-bold transition-colors ${
                      opt.isCorrect === 1
                        ? 'border-green-500 bg-green-500 text-white'
                        : 'border-gray-300 text-gray-500'
                    }`}
                  >
                    {opt.optionLabel}
                  </button>
                )}
                {isMultiChoice && (
                  <button
                    type="button"
                    onClick={() => handleMultiCorrectToggle(idx)}
                    className={`flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-md border-2 text-xs font-bold transition-colors ${
                      opt.isCorrect === 1
                        ? 'border-green-500 bg-green-500 text-white'
                        : 'border-gray-300 text-gray-500'
                    }`}
                  >
                    {opt.optionLabel}
                  </button>
                )}
                {type === 6 && (
                  <span className="flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full bg-gray-100 text-xs font-bold text-gray-500">
                    {opt.optionLabel}
                  </span>
                )}
                <input
                  required
                  value={opt.content}
                  onChange={(e) => handleOptionContentChange(idx, e.target.value)}
                  placeholder={`选项 ${opt.optionLabel} 内容`}
                  className="flex-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                />
                <button
                  type="button"
                  onClick={() => removeOption(idx)}
                  disabled={options.length <= 2}
                  className="text-gray-400 hover:text-red-500 disabled:opacity-30"
                >
                  ✕
                </button>
              </div>
            ))}
            {options.length < 5 && (
              <button
                type="button"
                onClick={addOption}
                className="mt-1 text-sm text-blue-500 hover:underline"
              >
                + 添加选项
              </button>
            )}
          </div>
        </div>
      )}

      {/* 判断题答案 */}
      {isTrueFalse && (
        <div>
          <label className={labelClass}>正确答案 *</label>
          <div className="flex gap-3">
            {['正确', '错误'].map((val) => (
              <button
                key={val}
                type="button"
                onClick={() => setAnswer(val === '正确' ? 'true' : 'false')}
                className={`rounded-full px-4 py-1.5 text-sm transition-colors ${
                  (val === '正确' && answer === 'true') ||
                  (val === '错误' && answer === 'false')
                    ? 'bg-green-500 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {val}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 填空题/主观题参考答案 */}
      {(type === 4 || type === 5) && (
        <div>
          <label className={labelClass}>
            {type === 4 ? '参考答案 *' : '参考答案（可选）'}
          </label>
          <textarea
            required={type === 4}
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            rows={2}
            placeholder={type === 4 ? '填空题标准答案' : '主观题参考答案（供教师批改参考）'}
            className={inputClass}
          />
        </div>
      )}

      {/* 解析 */}
      <div>
        <label className={labelClass}>解析（可选）</label>
        <textarea
          value={analysis}
          onChange={(e) => setAnalysis(e.target.value)}
          rows={2}
          placeholder="题目解析，学生答题后可见..."
          className={inputClass}
        />
      </div>

      {/* 默认分值 */}
      <div className="flex items-center gap-4">
        <div className="w-32">
          <label className={labelClass}>默认分值</label>
          <input
            type="number"
            min="0.5"
            step="0.5"
            value={score}
            onChange={(e) => setScore(e.target.value)}
            className={inputClass}
          />
        </div>
      </div>

      {/* 按钮 */}
      <div className="flex justify-end gap-3 border-t pt-4">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
        >
          取消
        </button>
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-lg bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600 disabled:opacity-60"
        >
          {isSubmitting ? '保存中...' : '保存题目'}
        </button>
      </div>
    </form>
  )
}
