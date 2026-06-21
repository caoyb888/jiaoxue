import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { useAttend } from '@edu/api/modules/interaction'

type Mode = 'select' | 'qr' | 'code' | 'success' | 'already'

/** 学生签到页（S3-12）：支持口令签到和二维码扫描 */
export default function StudentAttendPage() {
  const { lessonId } = useParams<{ lessonId: string }>()
  const id = Number(lessonId)
  const [searchParams] = useSearchParams()
  const scannedToken = searchParams.get('qrToken')

  const [mode, setMode] = useState<Mode>('select')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [totalCount, setTotalCount] = useState<number | null>(null)

  const attend = useAttend(id)

  // 扫码深链进入（?qrToken=...）：自动用 qrToken 签到，无需手动操作
  const autoTriedRef = useRef(false)
  useEffect(() => {
    if (!scannedToken || autoTriedRef.current || !id) return
    autoTriedRef.current = true
    attend
      .mutateAsync({ qrToken: scannedToken })
      .then((result) => {
        setTotalCount(result.totalCount)
        setMode(result.firstAttend ? 'success' : 'already')
      })
      .catch(() => {
        setError('二维码无效或已过期，请改用口令签到')
        setMode('code')
      })
  }, [scannedToken, id]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleCodeSubmit = async () => {
    if (!code.trim()) {
      setError('请输入签到口令')
      return
    }
    setError(null)
    try {
      const result = await attend.mutateAsync({ code: code.trim().toUpperCase() })
      setTotalCount(result.totalCount)
      setMode(result.firstAttend ? 'success' : 'already')
    } catch (e: unknown) {
      setError('签到码无效或已过期，请重新输入')
    }
  }

  // 摄像头扫码（Web 端用 getUserMedia，小程序端用 wx.scanCode）
  const handleScanQr = async () => {
    // Web 端：提示用户使用摄像头（实际需集成 jsQR 或 zxing-js）
    // 此处演示：直接跳转到口令模式作为降级
    setMode('code')
  }

  if (mode === 'success') {
    return (
      <div className="min-h-screen bg-green-50 flex flex-col items-center justify-center p-6">
        <div className="bg-white rounded-3xl shadow-lg p-10 text-center max-w-sm w-full">
          {/* 成功动画（checkmark）*/}
          <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6 animate-bounce">
            <svg className="w-10 h-10 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">签到成功！</h2>
          {totalCount !== null && (
            <p className="text-gray-500 text-sm">当前已有 <span className="font-semibold text-green-600">{totalCount}</span> 人签到</p>
          )}
        </div>
      </div>
    )
  }

  if (mode === 'already') {
    return (
      <div className="min-h-screen bg-yellow-50 flex flex-col items-center justify-center p-6">
        <div className="bg-white rounded-3xl shadow-lg p-10 text-center max-w-sm w-full">
          <div className="w-20 h-20 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg className="w-10 h-10 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01M21 12A9 9 0 113 12a9 9 0 0118 0z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">您已签到</h2>
          <p className="text-gray-500 text-sm">请勿重复操作</p>
          <button
            onClick={() => setMode('select')}
            className="mt-6 text-sm text-blue-600 hover:underline"
          >
            返回
          </button>
        </div>
      </div>
    )
  }

  if (mode === 'select') {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-6">
        <div className="bg-white rounded-3xl shadow-lg p-8 max-w-sm w-full">
          <h1 className="text-xl font-bold text-gray-900 text-center mb-8">课堂签到</h1>
          <div className="space-y-4">
            <button
              onClick={handleScanQr}
              className="w-full flex items-center gap-4 p-4 rounded-2xl border-2 border-blue-100 hover:border-blue-300 hover:bg-blue-50 transition-colors"
            >
              <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center flex-shrink-0">
                <svg className="w-6 h-6 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 3.5A9.5 9.5 0 0112 21a9.5 9.5 0 01-9.5-9.5A9.5 9.5 0 0112 2.5" />
                </svg>
              </div>
              <div className="text-left">
                <div className="font-semibold text-gray-900">扫码签到</div>
                <div className="text-sm text-gray-500">扫描教师屏幕二维码</div>
              </div>
            </button>

            <button
              onClick={() => setMode('code')}
              className="w-full flex items-center gap-4 p-4 rounded-2xl border-2 border-purple-100 hover:border-purple-300 hover:bg-purple-50 transition-colors"
            >
              <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center flex-shrink-0">
                <svg className="w-6 h-6 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M7 20l4-16m2 16l4-16M6 9h14M4 15h14" />
                </svg>
              </div>
              <div className="text-left">
                <div className="font-semibold text-gray-900">口令签到</div>
                <div className="text-sm text-gray-500">输入6位签到口令</div>
              </div>
            </button>
          </div>
        </div>
      </div>
    )
  }

  // 口令输入界面
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-6">
      <div className="bg-white rounded-3xl shadow-lg p-8 max-w-sm w-full">
        <button
          onClick={() => { setMode('select'); setError(null); setCode('') }}
          className="text-gray-400 hover:text-gray-600 mb-6 flex items-center gap-1 text-sm"
        >
          ← 返回
        </button>

        <h2 className="text-xl font-bold text-gray-900 mb-6">输入签到口令</h2>

        <input
          type="text"
          value={code}
          onChange={e => setCode(e.target.value.toUpperCase())}
          onKeyDown={e => e.key === 'Enter' && handleCodeSubmit()}
          placeholder="请输入6位口令"
          maxLength={8}
          className="w-full text-center text-3xl font-mono tracking-[0.3em] border-2 border-gray-200 rounded-2xl py-4 px-4 focus:outline-none focus:border-blue-400 uppercase"
          autoFocus
        />

        {error && (
          <p className="text-red-500 text-sm mt-3 text-center">{error}</p>
        )}

        <button
          onClick={handleCodeSubmit}
          disabled={attend.isPending || !code.trim()}
          className="mt-6 w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white font-medium py-3 rounded-2xl transition-colors text-lg"
        >
          {attend.isPending ? '签到中…' : '确认签到'}
        </button>
      </div>
    </div>
  )
}
