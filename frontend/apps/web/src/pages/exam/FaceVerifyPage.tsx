import { useState, useRef, useEffect, useCallback } from 'react'
import { useParams, useLocation, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { examStudentApi } from '@edu/api'
import type { ExamEnterVO, FaceVerifyResultVO } from '@edu/api'

/**
 * 人脸核验页（S5-04 / C6）。
 * 流程：进入考试若 sessionStatus=VERIFYING → 本页采集现场照片 → 比对通过 → 进入答题页。
 *
 * C6 合规：现场照片仅以 base64 提交给后端内存比对，前端不留存、不写库；
 *          响应只含 passed/score，无原始照片。
 */
export function FaceVerifyPage() {
  const { publishId } = useParams<{ publishId: string }>()
  const { state } = useLocation()
  const navigate = useNavigate()
  const enterData = state?.enterData as ExamEnterVO | undefined

  const videoRef = useRef<HTMLVideoElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const streamRef = useRef<MediaStream | null>(null)

  const [cameraError, setCameraError] = useState<string | null>(null)
  const [result, setResult] = useState<FaceVerifyResultVO | null>(null)

  const pid = Number(publishId)

  // 启动摄像头
  const startCamera = useCallback(async () => {
    setCameraError(null)
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false })
      streamRef.current = stream
      if (videoRef.current) {
        videoRef.current.srcObject = stream
        await videoRef.current.play()
      }
    } catch {
      setCameraError('无法访问摄像头，请检查浏览器权限后重试')
    }
  }, [])

  const stopCamera = useCallback(() => {
    streamRef.current?.getTracks().forEach((t) => t.stop())
    streamRef.current = null
  }, [])

  useEffect(() => {
    startCamera()
    return () => stopCamera()
  }, [startCamera, stopCamera])

  const verifyMutation = useMutation({
    mutationFn: (base64: string) => examStudentApi.faceVerify(pid, base64),
    onSuccess: (res: FaceVerifyResultVO) => {
      setResult(res)
      if (res.passed) {
        stopCamera()
        // 通过后进入答题页，沿用 enterData（含首批题目/配置）
        setTimeout(() => {
          navigate(`/exam/${publishId}/answer`, {
            state: { enterData: enterData ? { ...enterData, sessionStatus: 'ANSWERING' } : undefined },
          })
        }, 800)
      }
    },
    onError: (err: Error) => {
      setResult({ passed: false, score: 0, sessionStatus: 'VERIFYING', message: err.message || '核验失败，请重试' })
    },
  })

  // 拍照 → base64（去掉 dataURL 前缀）
  const capture = useCallback(() => {
    const video = videoRef.current
    const canvas = canvasRef.current
    if (!video || !canvas) return
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)
    const dataUrl = canvas.toDataURL('image/jpeg', 0.8)
    const base64 = dataUrl.split(',')[1]
    if (base64) verifyMutation.mutate(base64)
  }, [verifyMutation])

  // 缺少 enterData（直接 URL 进入，未先调 enter）
  if (!enterData) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
        <div className="bg-white rounded-xl shadow-md w-full max-w-md p-8 text-center space-y-4">
          <p className="text-gray-600">请先从考试列表进入考试后再进行人脸核验。</p>
          <button
            onClick={() => navigate(`/exam/${publishId}/enter`)}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
          >
            前往进入考试
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className="bg-white rounded-xl shadow-md w-full max-w-md p-6 space-y-5">
        <div className="text-center">
          <h1 className="text-xl font-bold text-gray-900">人脸核验</h1>
          <p className="mt-1 text-sm text-gray-500">请将面部置于取景框中央，光线充足</p>
        </div>

        <div className="relative aspect-[3/4] w-full overflow-hidden rounded-xl bg-gray-900">
          {cameraError ? (
            <div className="flex h-full flex-col items-center justify-center gap-3 px-6 text-center">
              <p className="text-sm text-gray-300">{cameraError}</p>
              <button onClick={startCamera} className="rounded-lg bg-blue-600 px-4 py-1.5 text-sm text-white hover:bg-blue-700">
                重试
              </button>
            </div>
          ) : (
            <>
              <video ref={videoRef} playsInline muted className="h-full w-full object-cover" />
              <div className="pointer-events-none absolute inset-0 m-auto h-48 w-48 rounded-full border-2 border-white/70" />
            </>
          )}
        </div>
        <canvas ref={canvasRef} className="hidden" />

        {result && (
          <div
            className={`rounded-lg px-3 py-2 text-sm ${
              result.passed ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-600'
            }`}
          >
            {result.passed
              ? `核验通过（相似度 ${Math.round(result.score)}），正在进入考试…`
              : `${result.message || '核验未通过'}（相似度 ${Math.round(result.score)}），请调整后重试`}
          </div>
        )}

        <button
          onClick={capture}
          disabled={verifyMutation.isPending || !!cameraError || (result?.passed ?? false)}
          className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition-colors hover:bg-blue-700 disabled:bg-blue-300"
        >
          {verifyMutation.isPending ? '核验中…' : result?.passed ? '核验通过' : '拍照核验'}
        </button>

        <p className="text-center text-xs text-gray-400">
          照片仅用于本次身份核验，系统不会留存您的现场照片
        </p>
      </div>
    </div>
  )
}
