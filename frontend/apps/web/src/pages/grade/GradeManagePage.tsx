import { useMemo, useRef, useState } from 'react'
import {
  useMyClasses,
  useClassGrades,
  useGradeRules,
  useCreateGradeRule,
  useDeleteGradeRule,
  useImportOfflineGrades,
  gradeApi,
  downloadBlob,
  GRADE_TYPES,
  type OfflineImportResultVO,
} from '@edu/api'
import { EChart } from '../../components/EChart'
import { buildGradeDistributionOption } from './gradeCharts'

/** 成绩管理页（S8-14）：权重配置 + 汇总成绩表 + 分布图 + xlsx 导出/线下导入。 */
export default function GradeManagePage() {
  const { data: classes } = useMyClasses()
  const [classId, setClassId] = useState<number | null>(null)
  const effectiveClassId = classId ?? classes?.[0]?.id ?? null

  const { data: grades } = useClassGrades(effectiveClassId)
  const { data: ruleList } = useGradeRules(effectiveClassId)
  const createRule = useCreateGradeRule(effectiveClassId)
  const deleteRule = useDeleteGradeRule(effectiveClassId)
  const importOffline = useImportOfflineGrades(effectiveClassId)
  const fileRef = useRef<HTMLInputElement>(null)
  const [importResult, setImportResult] = useState<OfflineImportResultVO | null>(null)

  const [form, setForm] = useState({ ruleName: '', gradeType: 1, weight: 20 })

  const distOption = useMemo(
    () => buildGradeDistributionOption(grades ?? []),
    [grades],
  )

  const handleAddRule = () => {
    if (!effectiveClassId || !form.ruleName.trim()) return
    createRule.mutate(
      { classId: effectiveClassId, ruleName: form.ruleName.trim(), gradeType: form.gradeType, weight: form.weight },
      { onSuccess: () => setForm({ ruleName: '', gradeType: 1, weight: 20 }) },
    )
  }

  const handleExport = async (format: 'zhengfang' | 'qiangzhi') => {
    if (!effectiveClassId) return
    const blob = await gradeApi.exportGrades(effectiveClassId, format)
    downloadBlob(blob, `grade_${effectiveClassId}_${format}.xlsx`)
  }

  const handleImport = (file: File | undefined) => {
    if (!file || !effectiveClassId) return
    importOffline.mutate(file, { onSuccess: (r) => setImportResult(r) })
    if (fileRef.current) fileRef.current.value = ''
  }

  return (
    <div className="min-h-screen bg-gray-50 p-4 md:p-6">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <h1 className="text-lg font-semibold text-gray-800">成绩管理</h1>
        <select
          value={effectiveClassId ?? ''}
          onChange={(e) => setClassId(e.target.value ? Number(e.target.value) : null)}
          className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-sm"
        >
          {classes?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.className}（{c.courseName}）
            </option>
          ))}
        </select>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        {/* 权重配置 */}
        <section className="rounded-xl bg-white p-4 shadow-sm lg:col-span-1">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-medium text-gray-700">权重配置</h2>
            {ruleList && (
              <span
                className={`rounded-full px-2 py-0.5 text-xs ${
                  ruleList.weightComplete ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'
                }`}
              >
                合计 {ruleList.totalWeight}%
              </span>
            )}
          </div>

          <ul className="mb-3 space-y-1.5">
            {ruleList?.rules.map((r) => (
              <li key={r.id} className="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2 text-sm">
                <span className="text-gray-700">
                  {r.ruleName} <span className="text-xs text-gray-400">· {r.gradeTypeName}</span>
                </span>
                <span className="flex items-center gap-2">
                  <span className="font-medium text-gray-600">{r.weight}%</span>
                  <button
                    onClick={() => deleteRule.mutate(r.id)}
                    className="text-xs text-red-500 hover:text-red-600"
                  >
                    删除
                  </button>
                </span>
              </li>
            ))}
            {ruleList?.rules.length === 0 && (
              <li className="py-2 text-center text-xs text-gray-400">暂无规则，默认权重计算</li>
            )}
          </ul>

          <div className="space-y-2 border-t border-gray-100 pt-3">
            <input
              value={form.ruleName}
              onChange={(e) => setForm((f) => ({ ...f, ruleName: e.target.value }))}
              placeholder="规则名称（如 期末考试）"
              className="w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm"
            />
            <div className="flex gap-2">
              <select
                value={form.gradeType}
                onChange={(e) => setForm((f) => ({ ...f, gradeType: Number(e.target.value) }))}
                className="flex-1 rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
              >
                {GRADE_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
              <input
                type="number"
                min={1}
                max={100}
                value={form.weight}
                onChange={(e) => setForm((f) => ({ ...f, weight: Number(e.target.value) }))}
                className="w-20 rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
              />
              <span className="self-center text-sm text-gray-400">%</span>
            </div>
            <button
              onClick={handleAddRule}
              disabled={createRule.isPending || !form.ruleName.trim()}
              className="w-full rounded-lg bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              添加规则
            </button>
            {createRule.isError && <p className="text-xs text-red-500">添加失败（权重合计不得超过 100）</p>}
          </div>
        </section>

        {/* 成绩分布 + 操作 */}
        <section className="rounded-xl bg-white p-4 shadow-sm lg:col-span-2">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-sm font-medium text-gray-700">成绩分布</h2>
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => handleExport('zhengfang')}
                className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50"
              >
                导出（正方）
              </button>
              <button
                onClick={() => handleExport('qiangzhi')}
                className="rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50"
              >
                导出（强智）
              </button>
              <button
                onClick={() => fileRef.current?.click()}
                disabled={importOffline.isPending}
                className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
              >
                {importOffline.isPending ? '导入中…' : '导入线下成绩'}
              </button>
              <input
                ref={fileRef}
                type="file"
                accept=".xlsx"
                className="hidden"
                onChange={(e) => handleImport(e.target.files?.[0])}
              />
            </div>
          </div>

          <EChart option={distOption} className="h-56 w-full" />

          {importResult && (
            <p className="mt-2 text-xs text-gray-500">
              导入完成：共 {importResult.total} 行，成功 {importResult.successCount}，失败{' '}
              {importResult.failCount}
              {importResult.errors.length > 0 && `（${importResult.errors.slice(0, 3).join('；')}…）`}
            </p>
          )}
        </section>
      </div>

      {/* 汇总成绩表 */}
      <section className="mt-4 overflow-x-auto rounded-xl bg-white p-4 shadow-sm">
        <h2 className="mb-3 text-sm font-medium text-gray-700">汇总成绩</h2>
        <table className="w-full min-w-[640px] text-sm">
          <thead>
            <tr className="border-b border-gray-200 text-left text-xs text-gray-500">
              <th className="py-2 pr-4">学号</th>
              <th className="px-2">考勤</th>
              <th className="px-2">小测</th>
              <th className="px-2">互动</th>
              <th className="px-2">考试</th>
              <th className="px-2">线下</th>
              <th className="px-2 font-semibold text-gray-700">总分</th>
              <th className="px-2">状态</th>
            </tr>
          </thead>
          <tbody>
            {grades?.map((g) => (
              <tr key={g.studentId} className="border-b border-gray-100">
                <td className="py-2 pr-4 text-gray-700">{g.studentId}</td>
                <td className="px-2 text-gray-600">{g.attendScore ?? '-'}</td>
                <td className="px-2 text-gray-600">{g.quizScore ?? '-'}</td>
                <td className="px-2 text-gray-600">{g.interactionScore ?? '-'}</td>
                <td className="px-2 text-gray-600">{g.examScore ?? '-'}</td>
                <td className="px-2 text-gray-600">{g.offlineScore ?? '-'}</td>
                <td className="px-2 font-semibold text-gray-800">{g.totalScore ?? '—'}</td>
                <td className="px-2">
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs ${
                      g.calcStatus === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {g.calcStatus === 1 ? '已计算' : '待计算'}
                  </span>
                </td>
              </tr>
            ))}
            {grades?.length === 0 && (
              <tr>
                <td colSpan={8} className="py-6 text-center text-xs text-gray-400">
                  暂无成绩数据
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  )
}
