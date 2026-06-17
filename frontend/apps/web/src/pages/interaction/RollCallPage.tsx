import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useRollCall, type RollCallVO } from '@edu/api/modules/interaction'

type StyleOption = 'random' | 'spotlight' | 'racing'

/** 随机点名动效页（S3-14）：三种样式 spotlight/racing/random */
export default function RollCallPage() {
  const { lessonId } = useParams<{ lessonId: string }>()
  const id = Number(lessonId)

  const [style, setStyle] = useState<StyleOption>('random')
  const [result, setResult] = useState<RollCallVO | null>(null)
  const [spinning, setSpinning] = useState(false)

  const rollCall = useRollCall(id)

  const handleRollCall = async () => {
    setSpinning(true)
    setResult(null)
    // 模拟抽奖动效时长
    await new Promise(res => setTimeout(res, style === 'racing' ? 2000 : 1200))
    const res = await rollCall.mutateAsync({ count: 1, excludeAbsent: true, style })
    setResult(res)
    setSpinning(false)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-900 via-blue-900 to-indigo-900 flex flex-col items-center justify-center p-6">
      <h1 className="text-white text-3xl font-bold mb-8 tracking-wide">随机点名</h1>

      {/* 样式选择 */}
      <div className="flex gap-3 mb-10">
        {([
          { key: 'random', label: '随机' },
          { key: 'spotlight', label: '聚光灯' },
          { key: 'racing', label: '赛马' },
        ] as { key: StyleOption; label: string }[]).map(opt => (
          <button
            key={opt.key}
            onClick={() => setStyle(opt.key)}
            className={`px-4 py-2 rounded-full font-medium text-sm transition-all ${
              style === opt.key
                ? 'bg-white text-purple-900 shadow-lg scale-105'
                : 'bg-white/20 text-white hover:bg-white/30'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {/* 结果展示区 */}
      <div className="w-64 h-64 flex items-center justify-center mb-10">
        {spinning ? (
          <SpinningAnimation style={style} />
        ) : result ? (
          <div className="text-center animate-bounce">
            <div className="text-6xl mb-4">🎯</div>
            <div className="text-white text-2xl font-bold">
              学生 #{result.studentIds[0]}
            </div>
            <div className="text-white/60 text-sm mt-2">{result.message}</div>
          </div>
        ) : (
          <div className="text-white/40 text-center">
            <div className="text-5xl mb-4">🎲</div>
            <div className="text-sm">点击开始随机点名</div>
          </div>
        )}
      </div>

      <button
        onClick={handleRollCall}
        disabled={spinning || rollCall.isPending}
        className="bg-white text-purple-900 font-bold px-10 py-4 rounded-full text-xl shadow-2xl hover:shadow-purple-500/50 hover:scale-105 active:scale-95 transition-all disabled:opacity-50 disabled:scale-100"
      >
        {spinning ? '抽取中…' : '开始点名'}
      </button>
    </div>
  )
}

function SpinningAnimation({ style }: { style: StyleOption }) {
  if (style === 'spotlight') {
    return (
      <div className="relative w-48 h-48">
        <div className="absolute inset-0 rounded-full bg-yellow-400/20 animate-ping" />
        <div className="absolute inset-4 rounded-full bg-yellow-400/40 animate-ping [animation-delay:0.3s]" />
        <div className="absolute inset-8 rounded-full bg-yellow-400/60 animate-pulse" />
        <div className="absolute inset-0 flex items-center justify-center text-4xl">🔦</div>
      </div>
    )
  }

  if (style === 'racing') {
    return (
      <div className="text-center">
        <div className="text-4xl animate-bounce">🏇</div>
        <div className="mt-4 flex gap-1">
          {[1, 2, 3, 4, 5].map(i => (
            <div
              key={i}
              className="w-2 bg-white/60 rounded-full animate-pulse"
              style={{
                height: `${Math.random() * 40 + 20}px`,
                animationDelay: `${i * 0.1}s`,
              }}
            />
          ))}
        </div>
      </div>
    )
  }

  // random
  return (
    <div className="text-center">
      <div className="text-6xl animate-spin">[🎲]</div>
      <div className="mt-4 text-white/60 text-sm animate-pulse">随机抽取中…</div>
    </div>
  )
}
