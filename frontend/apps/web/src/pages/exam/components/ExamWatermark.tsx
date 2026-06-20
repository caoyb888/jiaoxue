
interface ExamWatermarkProps {
  name: string
  studentId: string
}

/**
 * 全屏防作弊水印（CSS overlay）。
 * pointer-events:none 不影响答题操作；user-select:none 防止框选。
 * Tailwind rotate-[-30deg] 斜向展示，降低截图可读性。
 */
export function ExamWatermark({ name, studentId }: ExamWatermarkProps) {
  const label = `${name}  ${studentId}`
  // 50 个水印块覆盖全屏
  const items = Array.from({ length: 50 })

  return (
    <div
      className="fixed inset-0 z-40 overflow-hidden pointer-events-none select-none"
      aria-hidden="true"
    >
      <div className="grid grid-cols-4 gap-x-16 gap-y-12 rotate-[-30deg] scale-150 -m-24 opacity-[0.08]">
        {items.map((_, i) => (
          <span key={i} className="text-xs text-gray-800 whitespace-nowrap font-medium tracking-wide">
            {label}
          </span>
        ))}
      </div>
    </div>
  )
}
