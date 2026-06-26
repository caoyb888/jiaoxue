import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useClassHistory, useMyClasses } from '@edu/api'
import { EChart } from '../../components/EChart'
import { buildActiveBarOption, buildTrendLineOption } from './chartOptions'

const DAY_OPTIONS = [
  { value: 7, label: '近 7 天' },
  { value: 30, label: '近 30 天' },
  { value: 90, label: '近 90 天' },
]

/** 班级历史教学统计图表页（S7-13，教师端）。调 S7-04 班级历史 API。 */
export default function ClassHistoryPage() {
  const { data: classes = [], isLoading: classesLoading } = useMyClasses()
  const [classId, setClassId] = useState<number | undefined>()
  const [days, setDays] = useState(30)

  // 班级加载完成后默认选中第一个
  useEffect(() => {
    if (classId === undefined && classes.length > 0) {
      setClassId(classes[0].id)
    }
  }, [classes, classId])

  const { data, isLoading, isError } = useClassHistory(classId, days)
  const daily = data?.daily ?? []

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex items-center justify-between border-b bg-white px-4 py-3 md:px-8">
        <div>
          <h1 className="text-lg font-semibold text-gray-900">班级历史统计</h1>
          <p className="mt-0.5 text-sm text-gray-500">查看教学班各维度互动趋势</p>
        </div>
        <Link to="/dashboard" className="text-sm text-blue-600 hover:underline">
          返回首页
        </Link>
      </div>

      <main className="px-4 py-6 md:px-8">
        {/* 筛选栏 */}
        <div className="mb-6 flex flex-wrap items-center gap-3">
          <label className="text-sm text-gray-600">教学班</label>
          <select
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm"
            value={classId ?? ''}
            disabled={classesLoading || classes.length === 0}
            onChange={(e) => setClassId(Number(e.target.value))}
          >
            {classes.length === 0 && <option value="">暂无教学班</option>}
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.className}（{c.courseName}）
              </option>
            ))}
          </select>

          <div className="ml-2 flex gap-1">
            {DAY_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                onClick={() => setDays(opt.value)}
                className={`rounded-md px-3 py-1.5 text-sm ${
                  days === opt.value
                    ? 'bg-blue-600 text-white'
                    : 'border border-gray-300 bg-white text-gray-600 hover:bg-gray-50'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {isError && (
          <div className="flex h-64 items-center justify-center text-red-500">加载失败，请刷新重试</div>
        )}

        {!isError && isLoading && (
          <div className="flex h-64 items-center justify-center text-gray-400">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
          </div>
        )}

        {!isError && !isLoading && daily.length === 0 && (
          <div className="flex h-64 flex-col items-center justify-center gap-1 text-gray-400">
            <p className="text-sm">该时间范围内暂无统计数据</p>
            <p className="text-xs">开展课堂签到、弹幕、提问等互动后将在此汇总</p>
          </div>
        )}

        {!isError && !isLoading && daily.length > 0 && (
          <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
            <section className="rounded-xl border bg-white p-4 shadow-sm">
              <h2 className="mb-2 text-sm font-medium text-gray-700">互动事件趋势</h2>
              <EChart option={buildTrendLineOption(daily)} className="h-72 w-full" />
            </section>
            <section className="rounded-xl border bg-white p-4 shadow-sm">
              <h2 className="mb-2 text-sm font-medium text-gray-700">每日活跃学生与开课数</h2>
              <EChart option={buildActiveBarOption(daily)} className="h-72 w-full" />
            </section>
          </div>
        )}
      </main>
    </div>
  )
}
