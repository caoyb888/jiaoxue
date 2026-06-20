import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { http } from '@edu/api'

interface MonitorItemVO {
  studentId: number
  sessionStatus: string
  lastHeartbeatAt: string | null
  tabSwitchCount: number
  screenshotCount: number
  copyCount: number
  abnormalFlag: number
}

interface MonitorDashboardVO {
  publishId: number
  totalStudents: number
  statusDistribution: Record<string, number>
  students: MonitorItemVO[]
}

const STATUS_LABEL: Record<string, string> = {
  ANSWERING: '作答中',
  VERIFYING: '核验中',
  SUBMITTED: '已交卷',
  OFFLINE:   '离线',
  ABNORMAL:  '异常',
}

const STATUS_COLOR: Record<string, string> = {
  ANSWERING: 'bg-green-100 text-green-700',
  VERIFYING: 'bg-yellow-100 text-yellow-700',
  SUBMITTED: 'bg-blue-100 text-blue-700',
  OFFLINE:   'bg-gray-100 text-gray-500',
  ABNORMAL:  'bg-red-100 text-red-700',
}

/**
 * 监考状态大屏（S5-13）。
 * 实时展示当前考试各学生状态分布，10s 轮询。
 */
export function MonitorDashboardPage() {
  const [searchParams] = useSearchParams()
  const publishId = searchParams.get('publishId')
  const [filterStatus, setFilterStatus] = useState<string>('ALL')

  const { data, isLoading } = useQuery({
    queryKey: ['monitor', 'dashboard', publishId],
    queryFn: (): Promise<MonitorDashboardVO> =>
      // http 拦截器已解包 Result→data，直接返回业务数据
      http.get(`/v1/exam/monitor/list`, { params: { publishId } }),
    enabled: !!publishId,
    refetchInterval: 10_000,
    staleTime: 9_000,
  })

  const students = data?.students ?? []
  const filtered = filterStatus === 'ALL'
    ? students
    : students.filter((s) => s.sessionStatus === filterStatus)

  if (!publishId) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-400">请指定 publishId 参数</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">监考大屏</h1>
        <span className="text-sm text-gray-400">每10秒自动刷新</span>
      </div>

      {/* 状态分布统计卡 */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {Object.entries(STATUS_LABEL).map(([status, label]) => {
          const count = data?.statusDistribution[status] ?? 0
          return (
            <button
              key={status}
              onClick={() => setFilterStatus(filterStatus === status ? 'ALL' : status)}
              className={`bg-gray-800 hover:bg-gray-700 rounded-xl p-4 text-center transition-all border-2 ${
                filterStatus === status ? 'border-blue-500' : 'border-transparent'
              }`}
            >
              <div className="text-3xl font-bold text-white">{count}</div>
              <div className="text-xs text-gray-400 mt-1">{label}</div>
            </button>
          )
        })}
      </div>

      {/* 进度条 */}
      {data && data.totalStudents > 0 && (
        <div className="bg-gray-800 rounded-xl p-4">
          <div className="flex items-center justify-between text-sm mb-2">
            <span className="text-gray-400">交卷进度</span>
            <span className="text-white">
              {data.statusDistribution['SUBMITTED'] ?? 0} / {data.totalStudents}
            </span>
          </div>
          <div className="w-full bg-gray-700 rounded-full h-2">
            <div
              className="bg-blue-500 h-2 rounded-full transition-all"
              style={{ width: `${((data.statusDistribution['SUBMITTED'] ?? 0) / data.totalStudents) * 100}%` }}
            />
          </div>
        </div>
      )}

      {/* 学生列表 */}
      <div className="bg-gray-800 rounded-xl overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-700 flex items-center justify-between">
          <span className="text-sm font-medium">
            {filterStatus === 'ALL' ? `全部学生（${students.length}人）` : `${STATUS_LABEL[filterStatus]}（${filtered.length}人）`}
          </span>
          {isLoading && <span className="text-xs text-gray-500">刷新中...</span>}
        </div>
        <div className="divide-y divide-gray-700 max-h-[50vh] overflow-y-auto">
          {filtered.map((s) => (
            <div key={s.studentId} className="px-4 py-3 flex items-center gap-4">
              <span className="text-sm text-gray-300 w-20 shrink-0">{s.studentId}</span>
              <span className={`text-xs px-2 py-0.5 rounded-full ${STATUS_COLOR[s.sessionStatus] ?? 'bg-gray-600 text-gray-300'}`}>
                {STATUS_LABEL[s.sessionStatus] ?? s.sessionStatus}
              </span>
              <div className="flex gap-3 text-xs text-gray-500 ml-auto">
                {s.tabSwitchCount > 0 && <span className="text-yellow-400">切屏×{s.tabSwitchCount}</span>}
                {s.screenshotCount > 0 && <span className="text-orange-400">截图×{s.screenshotCount}</span>}
                {s.copyCount > 0 && <span className="text-red-400">复制×{s.copyCount}</span>}
                {s.abnormalFlag === 1 && <span className="text-red-500 font-semibold">⚠ 异常</span>}
              </div>
              {s.sessionStatus === 'SUBMITTED' ? (
                <Link
                  to={`/exam/${publishId}/review/${s.studentId}`}
                  className="shrink-0 rounded-md bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700"
                >
                  阅卷
                </Link>
              ) : (
                <span className="shrink-0 px-3 py-1 text-xs text-gray-600">未交卷</span>
              )}
            </div>
          ))}
          {filtered.length === 0 && (
            <div className="px-4 py-8 text-center text-gray-500 text-sm">暂无数据</div>
          )}
        </div>
      </div>
    </div>
  )
}
