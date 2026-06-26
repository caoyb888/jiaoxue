import { useDeptRanking, useRealtimeOverview } from '@edu/api'
import { EChart } from './components/EChart'
import { KpiCard } from './components/KpiCard'
import { Panel } from './components/Panel'
import { useClock } from './hooks/useClock'
import { buildDeptRankingOption, buildEventBarOption } from './charts/options'
import { DEPT_NAMES, RANK_DEPT_IDS } from './config'

export function App() {
  const clock = useClock()
  const overview = useRealtimeOverview()
  const ranking = useDeptRanking(RANK_DEPT_IDS, 1)

  const data = overview.data

  return (
    <div className="flex h-screen flex-col gap-4 bg-screen-bg p-5 text-slate-100">
      {/* 顶部标题栏 */}
      <header className="flex items-center justify-between rounded-lg border border-screen-border bg-screen-panel px-6 py-3">
        <h1 className="text-2xl font-bold tracking-wide text-screen-accent">
          山东管理学院 · 智慧教学数据大屏
        </h1>
        <div className="flex items-center gap-4 text-sm text-screen-accent2/80">
          <span>近 {data?.windowMinutes ?? 5} 分钟实时 · 每 10 秒刷新</span>
          <span className="tabular-nums">{clock}</span>
        </div>
      </header>

      {/* KPI 行 */}
      <div className="grid grid-cols-4 gap-4">
        <KpiCard label="实时开课数" value={data?.activeLessonCount ?? 0} unit="节" loading={overview.isLoading} />
        <KpiCard label="在线学生数" value={data?.onlineStudentCount ?? 0} unit="人" loading={overview.isLoading} />
        <KpiCard
          label="实时签到"
          value={data?.eventVolume?.ATTEND ?? 0}
          unit="次"
          loading={overview.isLoading}
        />
        <KpiCard
          label="实时互动(弹幕+提问)"
          value={(data?.eventVolume?.BARRAGE ?? 0) + (data?.eventVolume?.QUESTION ?? 0)}
          unit="次"
          loading={overview.isLoading}
        />
      </div>

      {/* 图表行 */}
      <div className="grid min-h-0 flex-1 grid-cols-2 gap-4">
        <Panel title="实时事件分布（近5分钟）">
          <EChart option={buildEventBarOption(data?.eventVolume)} className="h-full w-full" />
        </Panel>
        <Panel title="院系活跃排行（今日）">
          {ranking.isLoading ? (
            <div className="flex h-full items-center justify-center text-screen-accent2/60">加载中…</div>
          ) : (
            <EChart option={buildDeptRankingOption(ranking.items, DEPT_NAMES)} className="h-full w-full" />
          )}
        </Panel>
      </div>

      {overview.isError && (
        <div className="rounded border border-red-500/40 bg-red-500/10 px-4 py-2 text-sm text-red-300">
          实时数据加载失败，将在下次刷新重试
        </div>
      )}
    </div>
  )
}
