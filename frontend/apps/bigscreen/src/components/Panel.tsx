import type { ReactNode } from 'react'

interface PanelProps {
  title: string
  children: ReactNode
  className?: string
}

/** 大屏面板容器（标题 + 内容区）。 */
export function Panel({ title, children, className }: PanelProps) {
  return (
    <section className={`flex flex-col rounded-lg border border-screen-border bg-screen-panel ${className ?? ''}`}>
      <header className="flex items-center gap-2 border-b border-screen-border px-5 py-3">
        <span className="h-4 w-1 rounded bg-screen-accent" />
        <h2 className="text-base font-semibold text-screen-accent2">{title}</h2>
      </header>
      <div className="min-h-0 flex-1 p-4">{children}</div>
    </section>
  )
}
