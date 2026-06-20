import React, { useState, useRef } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { http } from '@edu/api'

// ─── Types ────────────────────────────────────────────────────────────────

interface ParsedQuestionRow {
  lineNo: number
  questionType: number
  typeName: string
  content: string
  answer: string | null
  optionCount: number
  error: string | null
}

interface ImportResult {
  successCount: number
  failCount: number
  rows: ParsedQuestionRow[]
}

// ─── API ──────────────────────────────────────────────────────────────────

// 注意：http 拦截器已解包 Result→data，故方法直接 resolve 业务数据。
const docxApi = {
  preview: (bankId: number, file: File): Promise<ImportResult> => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post(`/v1/exam/banks/${bankId}/import/preview`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  confirm: (bankId: number, file: File): Promise<{ importedCount: number }> => {
    const formData = new FormData()
    formData.append('file', file)
    return http.post(`/v1/exam/banks/${bankId}/import/confirm`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

// ─── Component ────────────────────────────────────────────────────────────

interface Props {
  bankId: number
  bankName: string
  onClose: () => void
}

type Step = 'select' | 'preview' | 'done'

export function DocxImportPanel({ bankId, bankName, onClose }: Props) {
  const qc = useQueryClient()
  const fileRef = useRef<HTMLInputElement>(null)
  const [step, setStep] = useState<Step>('select')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewResult, setPreviewResult] = useState<ImportResult | null>(null)
  const [importedCount, setImportedCount] = useState(0)
  const [filterError, setFilterError] = useState(false)

  const previewMutation = useMutation({
    mutationFn: (file: File) => docxApi.preview(bankId, file),
    onSuccess: (res) => {
      setPreviewResult(res ?? null)
      setStep('preview')
    },
  })

  const confirmMutation = useMutation({
    mutationFn: (file: File) => docxApi.confirm(bankId, file),
    onSuccess: (res) => {
      setImportedCount(res.importedCount ?? 0)
      qc.invalidateQueries({ queryKey: ['exam', 'questions', bankId] })
      setStep('done')
    },
  })

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    if (!file.name.endsWith('.docx')) {
      alert('请选择 .docx 格式文件')
      return
    }
    setSelectedFile(file)
    setPreviewResult(null)
    setStep('select')
  }

  function handlePreview() {
    if (!selectedFile) return
    previewMutation.mutate(selectedFile)
  }

  function handleConfirm() {
    if (!selectedFile) return
    confirmMutation.mutate(selectedFile)
  }

  function handleReset() {
    setStep('select')
    setSelectedFile(null)
    setPreviewResult(null)
    setFilterError(false)
    if (fileRef.current) fileRef.current.value = ''
  }

  const errorRows = previewResult?.rows.filter((r) => r.error) ?? []
  const validRows = previewResult?.rows.filter((r) => !r.error) ?? []
  const displayRows = filterError
    ? errorRows
    : (previewResult?.rows ?? [])

  return (
    <div className="flex flex-col gap-5">
      {/* 步骤说明 */}
      <div className="flex items-center gap-2 text-xs text-gray-500">
        <StepDot n={1} active={step === 'select'} done={step !== 'select'}>选择文件</StepDot>
        <div className="h-px flex-1 bg-gray-200" />
        <StepDot n={2} active={step === 'preview'} done={step === 'done'}>预览解析</StepDot>
        <div className="h-px flex-1 bg-gray-200" />
        <StepDot n={3} active={step === 'done'} done={false}>导入完成</StepDot>
      </div>

      {/* ── Step 1: 选择文件 ── */}
      {step === 'select' && (
        <div className="space-y-4">
          <div
            className={`flex flex-col items-center justify-center rounded-xl border-2 border-dashed py-10 transition-colors ${
              selectedFile ? 'border-blue-400 bg-blue-50' : 'border-gray-300 hover:border-blue-400 hover:bg-blue-50'
            }`}
            onClick={() => fileRef.current?.click()}
          >
            <input
              ref={fileRef}
              type="file"
              accept=".docx"
              className="hidden"
              onChange={handleFileChange}
            />
            <svg className="h-10 w-10 text-gray-400 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            {selectedFile ? (
              <p className="text-sm font-medium text-blue-600">{selectedFile.name}</p>
            ) : (
              <>
                <p className="text-sm font-medium text-gray-700">点击选择 .docx 文件</p>
                <p className="mt-1 text-xs text-gray-400">仅支持 Word 格式（.docx）</p>
              </>
            )}
          </div>

          <FormatHint />

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
            >
              取消
            </button>
            <button
              type="button"
              disabled={!selectedFile || previewMutation.isPending}
              onClick={handlePreview}
              className="rounded-lg bg-blue-500 px-4 py-2 text-sm text-white hover:bg-blue-600 disabled:opacity-50"
            >
              {previewMutation.isPending ? '解析中...' : '解析预览'}
            </button>
          </div>
        </div>
      )}

      {/* ── Step 2: 预览解析结果 ── */}
      {step === 'preview' && previewResult && (
        <div className="space-y-4">
          {/* 统计 */}
          <div className="flex gap-4">
            <Stat label="解析成功" value={validRows.length} color="text-green-600" />
            <Stat label="解析失败" value={errorRows.length} color="text-red-600" />
            <Stat label="合计" value={previewResult.rows.length} color="text-gray-700" />
          </div>

          {/* 筛选 */}
          {errorRows.length > 0 && (
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setFilterError((v) => !v)}
                className={`rounded-full px-3 py-1 text-xs transition-colors ${
                  filterError ? 'bg-red-500 text-white' : 'border border-red-300 text-red-600 hover:bg-red-50'
                }`}
              >
                {filterError ? '显示全部' : `仅看错误行 (${errorRows.length})`}
              </button>
              <p className="text-xs text-gray-400">错误行将跳过，不会导入</p>
            </div>
          )}

          {/* 题目列表 */}
          <div className="max-h-80 overflow-y-auto space-y-2 rounded-lg border border-gray-200 p-2">
            {displayRows.map((row, idx) => (
              <ParsedRowCard key={idx} row={row} />
            ))}
          </div>

          <div className="flex items-center justify-between">
            <button
              type="button"
              onClick={handleReset}
              className="text-sm text-gray-500 hover:underline"
            >
              ← 重新选择文件
            </button>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
              >
                取消
              </button>
              <button
                type="button"
                disabled={validRows.length === 0 || confirmMutation.isPending}
                onClick={handleConfirm}
                className="rounded-lg bg-green-500 px-4 py-2 text-sm text-white hover:bg-green-600 disabled:opacity-50"
              >
                {confirmMutation.isPending
                  ? '导入中...'
                  : `确认导入 ${validRows.length} 道题目`}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Step 3: 导入完成 ── */}
      {step === 'done' && (
        <div className="flex flex-col items-center gap-4 py-6">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
            <svg className="h-8 w-8 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <p className="text-base font-semibold text-gray-900">
            成功导入 {importedCount} 道题目
          </p>
          <p className="text-sm text-gray-500">题目已添加至「{bankName}」</p>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg bg-blue-500 px-6 py-2 text-sm text-white hover:bg-blue-600"
          >
            完成
          </button>
        </div>
      )}
    </div>
  )
}

// ─── Sub-components ────────────────────────────────────────────────────────

function StepDot({ n, active, done, children }: {
  n: number
  active: boolean
  done: boolean
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div className={`flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold transition-colors ${
        done
          ? 'bg-green-500 text-white'
          : active
          ? 'bg-blue-500 text-white'
          : 'bg-gray-200 text-gray-500'
      }`}>
        {done ? '✓' : n}
      </div>
      <span className={`text-xs ${active ? 'font-medium text-blue-600' : 'text-gray-400'}`}>
        {children}
      </span>
    </div>
  )
}

function Stat({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="rounded-lg bg-gray-50 px-4 py-2 text-center">
      <p className={`text-2xl font-bold ${color}`}>{value}</p>
      <p className="text-xs text-gray-500">{label}</p>
    </div>
  )
}

function ParsedRowCard({ row }: { row: ParsedQuestionRow }) {
  const typeColors: Record<number, string> = {
    1: 'bg-blue-100 text-blue-700',
    2: 'bg-purple-100 text-purple-700',
    3: 'bg-green-100 text-green-700',
    4: 'bg-yellow-100 text-yellow-700',
    5: 'bg-orange-100 text-orange-700',
    6: 'bg-pink-100 text-pink-700',
  }
  const hasError = !!row.error

  return (
    <div className={`rounded-lg border p-2.5 ${hasError ? 'border-red-300 bg-red-50' : 'border-gray-100 bg-white'}`}>
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-xs text-gray-400">第 {row.lineNo} 行</span>
          {!hasError && (
            <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${typeColors[row.questionType] ?? 'bg-gray-100 text-gray-700'}`}>
              {row.typeName}
            </span>
          )}
          {row.optionCount > 0 && (
            <span className="text-xs text-gray-400">{row.optionCount} 个选项</span>
          )}
        </div>
        {hasError ? (
          <span className="flex-shrink-0 rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-600 font-medium">错误</span>
        ) : (
          <span className="flex-shrink-0 rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-600 font-medium">✓ 正常</span>
        )}
      </div>

      {hasError ? (
        <p className="mt-1.5 text-xs text-red-600">{row.error}</p>
      ) : (
        <p className="mt-1 text-xs text-gray-700 line-clamp-2">{row.content}</p>
      )}
    </div>
  )
}

function FormatHint() {
  return (
    <details className="rounded-lg bg-amber-50 border border-amber-200 px-4 py-3 text-xs">
      <summary className="cursor-pointer font-medium text-amber-700">Word 文件格式说明</summary>
      <div className="mt-2 space-y-1 text-amber-800 leading-relaxed">
        <p>每道题由以下行组成（顺序不可变）：</p>
        <pre className="mt-1 rounded bg-white/60 p-2 font-mono text-xs overflow-x-auto">{`题型：单选题
题目：下列哪项是Java的基本数据类型？
选项A：int
选项B：String
选项C：List
答案：A
解析：int是Java的8种基本类型之一（可选行）

题型：判断题
题目：Java是面向对象语言。
答案：正确`}</pre>
        <p className="mt-1 text-amber-600">支持题型：单选题、多选题、判断题、填空题、主观题、投票题</p>
      </div>
    </details>
  )
}
