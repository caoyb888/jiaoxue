import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useLessonDetail, useLiveConfig } from '@edu/api'
import { useLessonTopic } from '../../hooks/useLessonTopic'
import { useHlsPlayer } from '../../hooks/useHlsPlayer'
import { resolvePlayback } from './livePlayback'

/**
 * 线上直播页（S8-11）：课件 + 视频并列（lg 以上左右布局，lg 以下上下堆叠）。
 *
 * <p>C5：仅 ONLINE_CLASS 且已推流时加载 HLS 播放器；SLIDE_ONLY 线下课不出现视频区。
 */
export default function LiveClassPage() {
  const { lessonId: lessonIdParam } = useParams<{ lessonId: string }>()
  const lessonId = lessonIdParam ? Number(lessonIdParam) : null
  const navigate = useNavigate()

  const { data: lesson } = useLessonDetail(lessonId)
  const { data: liveConfig } = useLiveConfig(lessonId)
  const playback = resolvePlayback(liveConfig)
  const videoRef = useHlsPlayer(playback.playUrl)

  const [currentSlide, setCurrentSlide] = useState(1)
  // 学生端跟随教师翻页（订阅 /topic/lesson/{id}/slide）
  useLessonTopic<{ slideIndex: number }>(
    lessonId ?? undefined,
    'slide',
    (msg) => setCurrentSlide(msg.slideIndex),
  )

  const totalSlides = lesson?.material?.pageCount ?? 0
  const slideBaseUrl = lesson?.material?.slideDir
    ? `/minio/edu-slides/${lesson.material.slideDir}`
    : null

  return (
    <div className="flex h-screen flex-col bg-gray-900">
      <header className="flex h-14 shrink-0 items-center justify-between bg-gray-800 px-4 md:px-6">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate(-1)} className="rounded p-1 text-gray-400 hover:text-white">
            ←
          </button>
          <span className="text-sm font-medium text-white">{lesson?.title ?? '线上直播'}</span>
          {playback.state === 'playing' && (
            <span className="flex items-center gap-1.5 text-xs font-medium text-red-400">
              <span className="flex h-2 w-2 animate-pulse rounded-full bg-red-500" />
              直播中
            </span>
          )}
        </div>
        {totalSlides > 0 && (
          <span className="text-xs text-gray-400">
            课件 {currentSlide} / {totalSlides}
          </span>
        )}
      </header>

      {/* 课件 + 视频并列：lg 以下上下堆叠（视频在上），lg 以上左右并列（课件左、视频右） */}
      <main className="flex flex-1 flex-col-reverse overflow-hidden lg:flex-row">
        {/* 课件区 */}
        <section className="flex flex-1 items-center justify-center overflow-hidden bg-black p-2">
          {slideBaseUrl && totalSlides > 0 ? (
            <img
              src={`${slideBaseUrl}slide_${String(currentSlide).padStart(4, '0')}.png`}
              alt={`第 ${currentSlide} 页`}
              className="max-h-full max-w-full object-contain"
            />
          ) : (
            <p className="text-sm text-gray-500">暂无课件</p>
          )}
        </section>

        {/* 视频区（lg 以上固定宽度右栏，lg 以下顶部 16:9） */}
        <aside className="shrink-0 border-b border-gray-700 bg-gray-800 lg:w-[28rem] lg:border-b-0 lg:border-l xl:w-[32rem]">
          <div className="aspect-video w-full bg-black">
            {playback.state === 'playing' ? (
              <video
                ref={videoRef}
                controls
                autoPlay
                playsInline
                className="h-full w-full bg-black"
              />
            ) : (
              <div className="flex h-full flex-col items-center justify-center gap-2 text-gray-400">
                <svg className="h-12 w-12 opacity-30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
                <p className="text-sm">{playback.message}</p>
              </div>
            )}
          </div>
          <div className="p-4 text-sm text-gray-300">
            <h2 className="text-xs font-medium uppercase tracking-wider text-gray-400">直播信息</h2>
            <div className="mt-2 space-y-1.5">
              <InfoRow label="模式" value={liveConfig?.liveMode === 'ONLINE_CLASS' ? '线上课堂' : '线下课堂'} />
              <InfoRow label="状态" value={playback.message} />
            </div>
          </div>
        </aside>
      </main>
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
