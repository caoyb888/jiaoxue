interface KpiCardProps {
  label: string
  value: number | string
  unit?: string
  loading?: boolean
}

/** 大屏 KPI 数字卡。 */
export function KpiCard({ label, value, unit, loading }: KpiCardProps) {
  return (
    <div className="flex flex-col justify-center rounded-lg border border-screen-border bg-screen-panel px-6 py-5">
      <span className="text-sm text-screen-accent2/80">{label}</span>
      <div className="mt-2 flex items-baseline gap-2">
        <span className="text-5xl font-bold tabular-nums text-screen-accent">
          {loading ? '—' : value}
        </span>
        {unit && <span className="text-base text-screen-accent2/70">{unit}</span>}
      </div>
    </div>
  )
}
