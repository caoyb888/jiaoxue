import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { interactionApi, type BarrageDTO } from '@edu/api/modules/interaction'

interface BarrageItem {
  id: string
  content: string
  style: 'roll' | 'top' | 'bottom'
  color: string
  track: number
  startX: number
}

const COLORS = ['#3b82f6', '#8b5cf6', '#ec4899', '#10b981', '#f59e0b', '#ef4444']
const ROLL_SPEED = 120 // px/s

/** 弹幕展示层 + 发送区（S3-13，三端适配） */
export default function BarragePage() {
  const { lessonId } = useParams<{ lessonId: string }>()
  const id = Number(lessonId)

  const canvasRef = useRef<HTMLCanvasElement>(null)
  const itemsRef = useRef<BarrageItem[]>([])
  const animFrameRef = useRef<number>()
  const lastTimeRef = useRef<number>(0)

  const [input, setInput] = useState('')
  const [style, setStyle] = useState<BarrageDTO['style']>('roll')
  const [sending, setSending] = useState(false)

  // 绘制弹幕（canvas 渲染）
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')!

    const resize = () => {
      canvas.width = canvas.offsetWidth
      canvas.height = canvas.offsetHeight
    }
    resize()
    window.addEventListener('resize', resize)

    const render = (timestamp: number) => {
      const delta = (timestamp - lastTimeRef.current) / 1000
      lastTimeRef.current = timestamp

      ctx.clearRect(0, 0, canvas.width, canvas.height)
      ctx.font = 'bold 18px sans-serif'

      itemsRef.current = itemsRef.current.filter(item => {
        if (item.style === 'roll') {
          item.startX -= ROLL_SPEED * delta
          ctx.fillStyle = item.color
          ctx.fillText(item.content, item.startX, item.track * 28 + 24)
          return item.startX > -ctx.measureText(item.content).width
        } else {
          // top/bottom 静止显示 3 秒（通过 TTL 控制，此处简化）
          const y = item.style === 'top'
            ? item.track * 28 + 24
            : canvas.height - item.track * 28 - 10
          ctx.fillStyle = item.color
          ctx.fillText(item.content, canvas.width / 2 - ctx.measureText(item.content).width / 2, y)
          return true
        }
      })

      animFrameRef.current = requestAnimationFrame(render)
    }

    animFrameRef.current = requestAnimationFrame(render)
    return () => {
      cancelAnimationFrame(animFrameRef.current!)
      window.removeEventListener('resize', resize)
    }
  }, [])

  const addBarrage = (content: string, style: BarrageDTO['style'] = 'roll') => {
    const canvas = canvasRef.current
    if (!canvas) return
    const tracks = Math.floor(canvas.height / 30)
    itemsRef.current.push({
      id: Math.random().toString(36).slice(2),
      content,
      style: style ?? 'roll',
      color: COLORS[Math.floor(Math.random() * COLORS.length)],
      track: Math.floor(Math.random() * Math.max(1, tracks - 1)) + 1,
      startX: canvas.width + 20,
    })
  }

  const handleSend = async () => {
    if (!input.trim() || sending) return
    setSending(true)
    try {
      await interactionApi.sendBarrage(id, { content: input.trim(), style })
      addBarrage(input.trim(), style)
      setInput('')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="flex flex-col h-screen bg-gray-900">
      {/* 弹幕画布 */}
      <div className="flex-1 relative">
        <canvas ref={canvasRef} className="w-full h-full" />
        <div className="absolute top-2 left-2 text-white/40 text-xs">弹幕互动</div>
      </div>

      {/* 发送区（底部，三端适配） */}
      <div className="bg-gray-800 border-t border-gray-700 p-3 safe-area-bottom">
        {/* 样式选择 */}
        <div className="flex gap-2 mb-2">
          {(['roll', 'top', 'bottom'] as const).map(s => (
            <button
              key={s}
              onClick={() => setStyle(s)}
              className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                style === s ? 'bg-blue-500 text-white' : 'bg-gray-700 text-gray-400'
              }`}
            >
              {s === 'roll' ? '滚动' : s === 'top' ? '顶部' : '底部'}
            </button>
          ))}
        </div>

        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSend()}
            placeholder="发个弹幕吧…"
            maxLength={100}
            className="flex-1 bg-gray-700 text-white placeholder-gray-500 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={handleSend}
            disabled={sending || !input.trim()}
            className="bg-blue-500 hover:bg-blue-600 disabled:opacity-40 text-white px-4 py-2 rounded-xl text-sm font-medium transition-colors"
          >
            发送
          </button>
        </div>
      </div>
    </div>
  )
}
