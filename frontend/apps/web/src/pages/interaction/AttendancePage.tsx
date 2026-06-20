import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useGenerateCode, useCurrentCode, useAttendanceList, useRollCall, type RollCallVO } from '@edu/api/modules/interaction'
import { useLessonTopic } from '../../hooks/useLessonTopic'

/** 教师端签到管理页（S3-11） */
export default function AttendancePage() {
  const { lessonId } = useParams<{ lessonId: string }>()
  const id = Number(lessonId)

  const [countdown, setCountdown] = useState(0)
  const [wsCount, setWsCount] = useState<number | null>(null)
  const [rollCallResult, setRollCallResult] = useState<RollCallVO | null>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const generateCode = useGenerateCode(id)
  const { data: codeData, refetch: refetchCode } = useCurrentCode(id)
  const { data: attendanceData, refetch: refetchAttendance } = useAttendanceList(id)
  const rollCall = useRollCall(id)

  // 订阅签到人数实时推送（edu-notify STOMP /topic/lesson/{id}/attend）
  useLessonTopic<{ count: number }>(id, 'attend', (msg) => {
    setWsCount(msg.count)
  })

  // 倒计时
  useEffect(() => {
    if (codeData) {
      setCountdown(codeData.remainSeconds)
    }
  }, [codeData])

  useEffect(() => {
    if (timerRef.current) clearInterval(timerRef.current)
    if (countdown > 0) {
      timerRef.current = setInterval(() => {
        setCountdown(prev => {
          if (prev <= 1) {
            clearInterval(timerRef.current!)
            return 0
          }
          return prev - 1
        })
      }, 1000)
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current) }
  }, [countdown])

  const handleGenerate = async () => {
    await generateCode.mutateAsync()
    await refetchCode()
  }

  const handleRollCall = async () => {
    const result = await rollCall.mutateAsync({ count: 1, excludeAbsent: true, style: 'random' })
    setRollCallResult(result)
  }

  const totalCount = wsCount ?? (attendanceData?.attendedCount ?? 0)

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">课堂签到管理</h1>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* 左：签到码卡片 */}
          <div className="bg-white rounded-2xl shadow-sm p-6 flex flex-col items-center">
            <h2 className="text-lg font-semibold text-gray-700 mb-4">签到码</h2>

            {codeData ? (
              <>
                {/* 二维码占位（实际用 qrcode.react 渲染 qrToken） */}
                <div className="w-48 h-48 bg-gray-100 rounded-xl flex items-center justify-center mb-4 border-2 border-dashed border-gray-300">
                  <div className="text-center">
                    <div className="text-xs text-gray-400 mb-1">二维码</div>
                    <div className="font-mono text-xs text-gray-500 break-all px-2">
                      {codeData.qrToken.slice(0, 16)}…
                    </div>
                  </div>
                </div>

                {/* 口令 */}
                <div className="text-4xl font-mono font-bold tracking-[0.3em] text-blue-600 mb-2">
                  {codeData.code}
                </div>

                {/* 倒计时 */}
                <div className={`text-sm font-medium mb-4 ${countdown <= 30 ? 'text-red-500' : 'text-gray-500'}`}>
                  {countdown > 0 ? `有效期 ${Math.floor(countdown / 60)}:${String(countdown % 60).padStart(2, '0')}` : '已过期'}
                </div>
              </>
            ) : (
              <div className="w-48 h-48 bg-gray-50 rounded-xl flex items-center justify-center mb-4 text-gray-400 text-sm">
                点击生成签到码
              </div>
            )}

            <button
              onClick={handleGenerate}
              disabled={generateCode.isPending}
              className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-medium py-2.5 rounded-xl transition-colors"
            >
              {generateCode.isPending ? '生成中…' : codeData ? '重新生成' : '生成签到码'}
            </button>
          </div>

          {/* 右：签到统计 */}
          <div className="bg-white rounded-2xl shadow-sm p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-gray-700">签到统计</h2>
              <button
                onClick={() => refetchAttendance()}
                className="text-sm text-blue-600 hover:underline"
              >
                刷新
              </button>
            </div>

            {/* 大数字 */}
            <div className="text-center my-6">
              <span className="text-6xl font-bold text-blue-600">{totalCount}</span>
              <span className="text-xl text-gray-400 ml-2">/ {attendanceData?.totalStudents ?? '—'}</span>
              <div className="text-sm text-gray-500 mt-1">已签到人数</div>
            </div>

            {/* 进度条 */}
            {attendanceData && attendanceData.totalStudents > 0 && (
              <div className="w-full bg-gray-100 rounded-full h-3 mb-4">
                <div
                  className="bg-blue-500 h-3 rounded-full transition-all duration-500"
                  style={{ width: `${attendanceData.attendRate}%` }}
                />
              </div>
            )}

            {/* 状态列表（前5条） */}
            <div className="space-y-2 mt-4">
              {attendanceData?.items.slice(0, 5).map(item => (
                <div key={item.studentId} className="flex items-center justify-between text-sm">
                  <span className="text-gray-700">{item.studentName}</span>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                    item.status === 1 ? 'bg-green-100 text-green-700' :
                    item.status === 2 ? 'bg-yellow-100 text-yellow-700' :
                    'bg-red-100 text-red-600'
                  }`}>
                    {item.statusLabel}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* 随机点名 */}
        <div className="mt-6 bg-white rounded-2xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-700">随机点名</h2>
            <button
              onClick={handleRollCall}
              disabled={rollCall.isPending}
              className="bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white px-4 py-2 rounded-xl text-sm font-medium transition-colors"
            >
              {rollCall.isPending ? '抽取中…' : '随机点名'}
            </button>
          </div>

          {rollCallResult && (
            <div className="bg-purple-50 rounded-xl p-4 text-center">
              <div className="text-sm text-purple-600 mb-2">点名结果</div>
              <div className="text-2xl font-bold text-purple-700">
                学生 #{rollCallResult.studentIds[0]}
              </div>
              <div className="text-xs text-purple-400 mt-1">{rollCallResult.message}</div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
