import { View, Text, Image, Button } from '@tarojs/components'
import Taro, { useRouter } from '@tarojs/taro'
import { useQuery } from '@tanstack/react-query'
import { courseApi } from '@edu/api'
import { useState, useEffect, useRef } from 'react'

// ⚠️ 小程序端 WebSocket 必须用 Taro.connectSocket，不能用 new WebSocket（DOM API）
// ⚠️ isWeb/isWeapp 从 @edu/utils 引入，勿内联判断

export default function ClassroomPage() {
  const router = useRouter()
  const classId = router.params.classId ? Number(router.params.classId) : null
  const className = router.params.className ?? ''

  const [lessonId, setLessonId] = useState<number | null>(null)
  const [currentSlide, setCurrentSlide] = useState(1)
  const [isStarting, setIsStarting] = useState(false)
  const socketTask = useRef<Taro.SocketTask | null>(null)

  const { data: lesson } = useQuery({
    queryKey: ['weapp-lesson', lessonId],
    queryFn: () => courseApi.getLessonDetail(lessonId!).then((r) => r.data),
    enabled: lessonId !== null,
    refetchInterval: 5_000,
  })

  const totalSlides = lesson?.material?.pageCount ?? 0
  const slideDir = lesson?.material?.slideDir ?? ''
  const slideUrl = slideDir
    ? `https://api.smu.edu.cn/minio/edu-slides/${slideDir}slide_${String(currentSlide).padStart(4, '0')}.png`
    : ''

  // WebSocket 连接（Taro.connectSocket — 小程序兼容）
  const connectWs = (wsEndpoint: string, lessonId: number) => {
    const task = Taro.connectSocket({ url: wsEndpoint })
    task.onMessage((msg) => {
      try {
        const data = JSON.parse(msg.data as string)
        if (data.type === 'SLIDE_CHANGE') {
          setCurrentSlide(data.slideNo)
        }
      } catch {}
    })
    task.onError(() => Taro.showToast({ title: '连接中断，将自动重连', icon: 'none' }))
    socketTask.current = task
  }

  useEffect(() => {
    return () => {
      socketTask.current?.close({})
    }
  }, [])

  const handleStartLesson = async () => {
    if (!classId) return
    setIsStarting(true)
    try {
      const result = await courseApi.startLesson({ classId, liveMode: 'SLIDE_ONLY' })
      setLessonId(result.data.lessonId)
      setCurrentSlide(1)
      connectWs(result.data.wsEndpoint, result.data.lessonId)
      Taro.showToast({ title: '课堂已开始', icon: 'success' })
    } catch {
      Taro.showToast({ title: '开课失败', icon: 'error' })
    } finally {
      setIsStarting(false)
    }
  }

  const handleEndLesson = async () => {
    if (!lessonId) return
    const confirmed = await Taro.showModal({ title: '确认结课', content: '确定结束本次课堂吗？' })
    if (!confirmed.confirm) return
    try {
      await courseApi.endLesson(lessonId)
      socketTask.current?.close({})
      setLessonId(null)
      Taro.showToast({ title: 'AI报告生成中，稍后通知您', icon: 'none', duration: 3000 })
    } catch {
      Taro.showToast({ title: '结课失败', icon: 'error' })
    }
  }

  const handlePrevSlide = async () => {
    if (!lessonId || currentSlide <= 1) return
    const next = currentSlide - 1
    setCurrentSlide(next)
    await courseApi.updateSlide(lessonId, next)
  }

  const handleNextSlide = async () => {
    if (!lessonId || currentSlide >= totalSlides) return
    const next = currentSlide + 1
    setCurrentSlide(next)
    await courseApi.updateSlide(lessonId, next)
  }

  const isActive = !!lessonId

  return (
    <View className="flex min-h-screen flex-col bg-gray-900">
      {/* 标题栏 */}
      <View className="flex items-center justify-between bg-gray-800 px-4 py-3">
        <Text className="text-sm font-medium text-white">{className}</Text>
        {isActive ? (
          <Button
            size="mini"
            style={{ backgroundColor: '#dc2626', color: '#fff', borderRadius: '6px' }}
            onClick={handleEndLesson}
          >
            结束课堂
          </Button>
        ) : (
          <Button
            size="mini"
            loading={isStarting}
            style={{ backgroundColor: '#2563eb', color: '#fff', borderRadius: '6px' }}
            onClick={handleStartLesson}
          >
            开始上课
          </Button>
        )}
      </View>

      {/* 课件展示区 */}
      <View className="flex flex-1 items-center justify-center bg-gray-900">
        {slideUrl ? (
          <Image
            src={slideUrl}
            mode="aspectFit"
            className="w-full"
            style={{ maxHeight: '65vh' }}
          />
        ) : (
          <View className="flex flex-col items-center gap-2">
            <Text className="text-gray-500 text-sm">
              {isActive ? '暂无课件' : '开始上课后显示课件'}
            </Text>
          </View>
        )}
      </View>

      {/* 翻页控制 */}
      {isActive && totalSlides > 0 && (
        <View className="flex items-center justify-between bg-gray-800 px-4 py-3">
          <Button
            disabled={currentSlide <= 1}
            style={{ backgroundColor: '#374151', color: '#d1d5db', borderRadius: '6px' }}
            onClick={handlePrevSlide}
          >
            ← 上一页
          </Button>
          <Text className="text-gray-400 text-sm">
            {currentSlide} / {totalSlides}
          </Text>
          <Button
            disabled={currentSlide >= totalSlides}
            style={{ backgroundColor: '#374151', color: '#d1d5db', borderRadius: '6px' }}
            onClick={handleNextSlide}
          >
            下一页 →
          </Button>
        </View>
      )}
    </View>
  )
}
