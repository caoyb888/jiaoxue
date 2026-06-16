// 平台判断（Taro 条件编译规范 §6.5 — 所有平台判断必须从此文件引用）
export const isWeapp = (typeof process !== 'undefined' && process.env?.['TARO_ENV'] === 'weapp')
export const isWeb = !isWeapp

// 手机号脱敏（等保合规：日志中只打印前3位+****）
export function maskPhone(phone: string): string {
  if (!phone || phone.length < 7) return '***'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

// 学号末两位取模（考试交卷打散机制 §8.2）
export function getSubmitDelay(studentNo: string, maxDelayMs = 30000): number {
  const lastTwo = parseInt(studentNo.slice(-2), 10)
  return Math.floor((lastTwo / 100) * maxDelayMs)
}

// 日期格式化
export function formatDate(date: Date | string, fmt = 'YYYY-MM-DD HH:mm:ss'): string {
  const d = typeof date === 'string' ? new Date(date) : date
  const pad = (n: number) => String(n).padStart(2, '0')
  return fmt
    .replace('YYYY', String(d.getFullYear()))
    .replace('MM', pad(d.getMonth() + 1))
    .replace('DD', pad(d.getDate()))
    .replace('HH', pad(d.getHours()))
    .replace('mm', pad(d.getMinutes()))
    .replace('ss', pad(d.getSeconds()))
}
