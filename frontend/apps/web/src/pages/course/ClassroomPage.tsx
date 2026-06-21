import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useStartLesson, useEndLesson, useLessonDetail, courseApi } from '@edu/api'
import type { LessonStartVO } from '@edu/api'
import { useAuthStore } from '@edu/store'
import { useLessonTopic } from '../../hooks/useLessonTopic'

/** 课堂页：教师开始/结束课堂 + 课件翻页推送（C5：默认 SLIDE_ONLY） */
export default function ClassroomPage() {
  const { classId } = useParams<{ classId: string }>()
  const navigate = useNavigate()
  const { roles } = useAuthStore()
  const isTeacher = roles.includes('ROLE_TEACHER') || roles.includes('ROLE_ADMIN')

  const [lessonId, setLessonId] = useState<number | null>(null)
  const [lessonInfo, setLessonInfo] = useState<LessonStartVO | null>(null)
  const [currentSlide, setCurrentSlide] = useState(1)
  const [elapsedMin, setElapsedMin] = useState(0)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // 课件翻页：订阅 /topic/lesson/{id}/slide，并可向 /app/lesson/{id}/nextSlide 推送
  const sendSlide = useLessonTopic<{ slideIndex: number }>(
    lessonId ?? undefined,
    'slide',
    (msg) => setCurrentSlide(msg.slideIndex),
  )

  const { data: lesson } = useLessonDetail(lessonId)
  const startLesson = useStartLesson()
  const endLesson = useEndLesson()

  const totalSlides = lesson?.material?.pageCount ?? 0
  const slideBaseUrl = lesson?.material?.slideDir
    ? `/minio/edu-slides/${lesson.material.slideDir}`
    : null

  // 课堂计时器
  useEffect(() => {
    if (!lessonInfo) return
    timerRef.current = setInterval(() => setElapsedMin((m) => m + 1), 60_000)
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [lessonInfo])

  const handleStart = async () => {
    if (!classId) return
    const result = await startLesson.mutateAsync({
      classId: Number(classId),
      liveMode: 'SLIDE_ONLY',
    })
    setLessonId(result.lessonId)
    setLessonInfo(result)
    setCurrentSlide(1)
    // STOMP 订阅在 lessonId 变化后由 useLessonTopic 自动建立
  }

  const handleEnd = async () => {
    if (!lessonId) return
    await endLesson.mutateAsync(lessonId)
    if (timerRef.current) clearInterval(timerRef.current)
    setLessonId(null)
    setLessonInfo(null)
    setCurrentSlide(1)
    setElapsedMin(0)
  }

  const handleSlideChange = async (newSlide: number) => {
    if (!lessonId || newSlide < 1 || newSlide > totalSlides) return
    setCurrentSlide(newSlide)
    // 持久化当前页（REST）+ STOMP 广播给学生（notify @MessageMapping nextSlide → /topic/.../slide）
    try {
      await courseApi.updateSlide(lessonId, newSlide)
    } catch {}
    sendSlide('nextSlide', { slideIndex: newSlide })
  }

  const isActive = !!lessonInfo

  return (
    <div className="flex h-screen flex-col bg-gray-900">
      {/* 顶部工具栏 */}
      <header className="flex h-14 shrink-0 items-center justify-between bg-gray-800 px-4 md:px-6">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate(-1)}
            className="rounded p-1 text-gray-400 hover:text-white"
          >
            ←
          </button>
          <span className="text-sm font-medium text-white">
            {isActive ? `课堂进行中 · ${elapsedMin} 分钟` : '待开课'}
          </span>
          {isActive && (
            <span className="flex h-2 w-2 animate-pulse rounded-full bg-red-500" />
          )}
        </div>

        {isTeacher && (
          <div className="flex items-center gap-2">
            {!isActive ? (
              <button
                onClick={handleStart}
                disabled={startLesson.isPending}
                className="rounded-lg bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
              >
                {startLesson.isPending ? '开课中…' : '开始上课'}
              </button>
            ) : (
              <button
                onClick={handleEnd}
                disabled={endLesson.isPending}
                className="rounded-lg bg-red-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
              >
                {endLesson.isPending ? '结课中…' : '结束上课'}
              </button>
            )}
          </div>
        )}
      </header>

      {/* 课堂互动工具栏（开课后显示；新标签打开以保留课堂直播状态） */}
      {isActive && isTeacher && lessonId && (
        <div className="flex h-11 shrink-0 items-center gap-2 border-t border-gray-700 bg-gray-800 px-4 md:px-6">
          <span className="text-xs text-gray-400">课堂互动：</span>
          {[
            { label: '签到', path: 'attendance' },
            { label: '随机点名', path: 'roll-call' },
            { label: '弹幕', path: 'barrage' },
          ].map((tool) => (
            <a
              key={tool.path}
              href={`/lesson/${lessonId}/${tool.path}`}
              target="_blank"
              rel="noreferrer"
              className="rounded-md bg-gray-700 px-3 py-1 text-xs font-medium text-gray-200 hover:bg-gray-600"
            >
              {tool.label}
            </a>
          ))}
        </div>
      )}

      {/* 主内容区：课件展示 */}
      <main className="flex flex-1 overflow-hidden">
        {/* 课件区 */}
        <div className="flex flex-1 flex-col items-center justify-center">
          {slideBaseUrl && totalSlides > 0 ? (
            <img
              src={`${slideBaseUrl}slide_${String(currentSlide).padStart(4, '0')}.png`}
              alt={`第 ${currentSlide} 页`}
              className="max-h-full max-w-full object-contain"
            />
          ) : (
            <div className="flex flex-col items-center gap-3 text-gray-500">
              <svg className="h-16 w-16 opacity-30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p className="text-sm">{isActive ? '暂无课件，可直接讲授' : '开始上课后显示课件'}</p>
            </div>
          )}
        </div>

        {/* 右侧信息面板（md+ 显示） */}
        <aside className="hidden md:flex md:w-56 md:flex-col border-l border-gray-700 bg-gray-800 lg:w-64">
          <div className="p-4">
            <h2 className="text-xs font-medium uppercase tracking-wider text-gray-400">课堂信息</h2>
            <div className="mt-3 space-y-2 text-sm text-gray-300">
              <InfoRow label="状态" value={isActive ? '进行中' : '未开始'} />
              <InfoRow label="模式" value={lessonInfo?.liveMode ?? 'SLIDE_ONLY'} />
              {totalSlides > 0 && (
                <InfoRow label="课件" value={`${currentSlide} / ${totalSlides} 页`} />
              )}
              <InfoRow label="时长" value={`${elapsedMin} 分钟`} />
            </div>
          </div>
        </aside>
      </main>

      {/* 底部翻页控制 */}
      {isActive && totalSlides > 0 && (
        <nav className="flex h-14 shrink-0 items-center justify-center gap-4 bg-gray-800 border-t border-gray-700">
          <button
            onClick={() => handleSlideChange(currentSlide - 1)}
            disabled={currentSlide <= 1}
            className="rounded-lg px-5 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 disabled:opacity-40"
          >
            ← 上一页
          </button>
          <span className="text-sm text-gray-400">
            {currentSlide} / {totalSlides}
          </span>
          <button
            onClick={() => handleSlideChange(currentSlide + 1)}
            disabled={currentSlide >= totalSlides}
            className="rounded-lg px-5 py-2 text-sm font-medium text-gray-300 hover:bg-gray-700 disabled:opacity-40"
          >
            下一页 →
          </button>
        </nav>
      )}
    </div>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between">
      <span className="text-gray-500">{label}</span>
      <span>{value}</span>
    </div>
  )
}
