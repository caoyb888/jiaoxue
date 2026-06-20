import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { examStudentApi } from '@edu/api'
import type { ExamEnterVO } from '@edu/api'

/**
 * 考试进入页。
 * 流程：输入密码（可选）→ enter 接口 → 若 sessionStatus=VERIFYING 跳转人脸核验 → 进入答题页。
 */
export function ExamEnterPage() {
  const { publishId } = useParams<{ publishId: string }>()
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  const enterMutation = useMutation({
    mutationFn: () => examStudentApi.enterExam(Number(publishId), { password: password || undefined }),
    onSuccess: (res) => {
      const vo: ExamEnterVO = res.data
      if (vo.sessionStatus === 'VERIFYING') {
        navigate(`/exam/${publishId}/face-verify`, { state: { enterData: vo } })
      } else {
        navigate(`/exam/${publishId}/answer`, { state: { enterData: vo } })
      }
    },
    onError: (err: { response?: { data?: { msg?: string } } }) => {
      setError(err.response?.data?.msg ?? '进入失败，请稍后重试')
    },
  })

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 p-4">
      <div className="bg-white rounded-xl shadow-md w-full max-w-md p-8 space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900">进入考试</h1>
          <p className="text-sm text-gray-500 mt-1">请仔细阅读注意事项后进入</p>
        </div>

        {/* 注意事项 */}
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 text-sm text-amber-800 space-y-1">
          <p className="font-semibold">考试须知</p>
          <ul className="list-disc list-inside space-y-1">
            <li>请保持网络连接稳定，断网不会丢失已保存的答案</li>
            <li>切换标签页将被记录，超过3次将自动标记异常</li>
            <li>交卷后不可修改，请确认答题完整后再提交</li>
            <li>禁止截图、复制题目内容</li>
          </ul>
        </div>

        {/* 密码输入（有密码才显示） */}
        <div className="space-y-2">
          <label className="block text-sm font-medium text-gray-700">考试密码（如有）</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="请输入考试密码"
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 rounded px-3 py-2">{error}</p>
        )}

        <button
          onClick={() => enterMutation.mutate()}
          disabled={enterMutation.isPending}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-semibold rounded-lg py-3 transition-colors"
        >
          {enterMutation.isPending ? '进入中...' : '进入考试'}
        </button>
      </div>
    </div>
  )
}
