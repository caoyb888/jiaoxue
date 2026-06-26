import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useHandleWarn, useWarnList } from '@edu/api'
import type { WarnQueryParams } from '@edu/api'
import {
  targetTypeLabel,
  warnStatusBadgeClass,
  warnStatusLabel,
  warnTypeLabel,
} from './warnLabels'

const TYPE_FILTERS = [
  { value: '', label: '全部类型' },
  { value: 'LOW_ATTEND', label: '低考勤' },
  { value: 'ZERO_ACTIVE', label: '零活跃' },
  { value: 'FREQUENT_ABSENCE', label: '频繁缺席' },
]

const STATUS_FILTERS = [
  { value: '', label: '全部状态' },
  { value: '0', label: '未处理' },
  { value: '1', label: '已处理' },
  { value: '2', label: '已忽略' },
]

const PAGE_SIZE = 20

/** 教学预警列表页（S7-14，管理员）。调 S7-07 API。 */
export default function WarnListPage() {
  const [warnType, setWarnType] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(1)

  const params: WarnQueryParams = {
    page,
    size: PAGE_SIZE,
    ...(warnType ? { warnType } : {}),
    ...(status ? { status: Number(status) } : {}),
  }
  const { data, isLoading, isError } = useWarnList(params)
  const handleWarn = useHandleWarn()

  const rows = data?.list ?? []
  const totalPages = data?.pages ?? 0

  const onFilterChange = (next: () => void) => {
    setPage(1)
    next()
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex items-center justify-between border-b bg-white px-4 py-3 md:px-8">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">教学预警</h1>
          <p className="mt-0.5 text-sm text-gray-500">低考勤 / 零活跃 / 频繁缺席预警，支持一键处理</p>
        </div>
        <Link to="/dashboard" className="text-sm text-blue-600 hover:underline">
          返回首页
        </Link>
      </div>

      <main className="px-4 py-6 md:px-8">
        {/* 筛选栏 */}
        <div className="mb-4 flex flex-wrap items-center gap-3">
          <select
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm"
            value={warnType}
            onChange={(e) => onFilterChange(() => setWarnType(e.target.value))}
          >
            {TYPE_FILTERS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          <select
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm"
            value={status}
            onChange={(e) => onFilterChange(() => setStatus(e.target.value))}
          >
            {STATUS_FILTERS.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
          {typeof data?.total === 'number' && (
            <span className="text-sm text-gray-500">共 {data.total} 条</span>
          )}
        </div>

        {isError && (
          <div className="flex h-64 items-center justify-center text-red-500">加载失败，请刷新重试</div>
        )}

        {!isError && isLoading && (
          <div className="flex h-64 items-center justify-center text-gray-400">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
          </div>
        )}

        {!isError && !isLoading && rows.length === 0 && (
          <div className="flex h-64 items-center justify-center text-gray-400">暂无预警记录</div>
        )}

        {!isError && !isLoading && rows.length > 0 && (
          <div className="overflow-hidden rounded-xl border bg-white shadow-sm">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
                <tr>
                  <th className="px-4 py-3">类型</th>
                  <th className="px-4 py-3">对象</th>
                  <th className="px-4 py-3">统计日期</th>
                  <th className="px-4 py-3">指标 / 阈值</th>
                  <th className="px-4 py-3">详情</th>
                  <th className="px-4 py-3">状态</th>
                  <th className="px-4 py-3 text-right">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((w) => (
                  <tr key={w.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-900">{warnTypeLabel(w.warnType)}</td>
                    <td className="px-4 py-3 text-gray-600">
                      {targetTypeLabel(w.targetType)} #{w.targetId}
                    </td>
                    <td className="px-4 py-3 text-gray-600">{w.statDate}</td>
                    <td className="px-4 py-3 tabular-nums text-gray-600">
                      {w.metricValue} / {w.thresholdValue}
                    </td>
                    <td className="px-4 py-3 text-gray-500">{w.detail}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${warnStatusBadgeClass(w.status)}`}>
                        {warnStatusLabel(w.status)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {w.status === 0 ? (
                        <div className="flex justify-end gap-2">
                          <button
                            disabled={handleWarn.isPending}
                            onClick={() => handleWarn.mutate({ id: w.id, status: 1 })}
                            className="rounded-md border border-green-300 bg-green-50 px-2.5 py-1 text-xs text-green-700 hover:bg-green-100 disabled:opacity-50"
                          >
                            标记处理
                          </button>
                          <button
                            disabled={handleWarn.isPending}
                            onClick={() => handleWarn.mutate({ id: w.id, status: 2 })}
                            className="rounded-md border border-gray-300 bg-white px-2.5 py-1 text-xs text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                          >
                            忽略
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* 分页 */}
        {totalPages > 1 && (
          <div className="mt-4 flex items-center justify-center gap-3 text-sm">
            <button
              disabled={page <= 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              className="rounded-md border border-gray-300 bg-white px-3 py-1 disabled:opacity-40"
            >
              上一页
            </button>
            <span className="text-gray-500">
              第 {page} / {totalPages} 页
            </span>
            <button
              disabled={page >= totalPages}
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              className="rounded-md border border-gray-300 bg-white px-3 py-1 disabled:opacity-40"
            >
              下一页
            </button>
          </div>
        )}
      </main>
    </div>
  )
}
