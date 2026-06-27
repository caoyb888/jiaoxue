/** 预警类型 → 中文。 */
export const WARN_TYPE_LABELS: Record<string, string> = {
  LOW_ATTEND: '低考勤',
  ZERO_ACTIVE: '零活跃',
  FREQUENT_ABSENCE: '频繁缺席',
}

/** 预警对象类型 → 中文。 */
export const TARGET_TYPE_LABELS: Record<string, string> = {
  LESSON: '课堂',
  STUDENT: '学生',
}

/** 处理状态 → 中文。 */
export const WARN_STATUS_LABELS: Record<number, string> = {
  0: '未处理',
  1: '已处理',
  2: '已忽略',
}

export function warnTypeLabel(type: string): string {
  return WARN_TYPE_LABELS[type] ?? type
}

export function targetTypeLabel(type: string): string {
  return TARGET_TYPE_LABELS[type] ?? type
}

export function warnStatusLabel(status: number): string {
  return WARN_STATUS_LABELS[status] ?? `状态${status}`
}

/** 状态对应的徽标样式（Tailwind class）。 */
export function warnStatusBadgeClass(status: number): string {
  switch (status) {
    case 1:
      return 'bg-green-100 text-green-700'
    case 2:
      return 'bg-gray-100 text-gray-500'
    default:
      return 'bg-amber-100 text-amber-700'
  }
}
