import Taro, { useRouter } from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { useState } from 'react'
import { examStudentApi } from '@edu/api/modules/exam'
import type { ExamEnterVO } from '@edu/api/modules/exam'

const ENTER_KEY_PREFIX = 'exam_enter'

/**
 * 考试进入页（S5-14 Taro小程序版）
 * 不使用 document.*/window.*/localStorage（C8约束）
 */
export default function ExamIndexPage() {
  const router = useRouter()
  const publishId = Number(router.params.publishId ?? '0')

  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  const handleEnter = async () => {
    if (!publishId) {
      Taro.showToast({ title: '参数错误，缺少publishId', icon: 'error' })
      return
    }
    setLoading(true)
    try {
      const result = await examStudentApi.enterExam(publishId, password ? { password } : undefined)
      const enterData: ExamEnterVO = result.data
      // 存入 Taro Storage（小程序不可用 IndexedDB）
      Taro.setStorageSync(`${ENTER_KEY_PREFIX}_${publishId}`, JSON.stringify(enterData))
      Taro.navigateTo({ url: `/pages/exam/answer?publishId=${publishId}` })
    } catch (e: unknown) {
      const err = e as { response?: { data?: { msg?: string } } }
      const msg = err?.response?.data?.msg ?? '进入考试失败，请检查密码'
      Taro.showToast({ title: msg, icon: 'error', duration: 3000 })
    } finally {
      setLoading(false)
    }
  }

  return (
    <View className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-6">
      <View className="bg-white rounded-3xl p-8 w-full max-w-xs shadow-sm">
        <Text className="text-xl font-bold text-gray-900 block text-center mb-2">进入考试</Text>
        <Text className="text-sm text-gray-500 block text-center mb-8">请输入考试密码（如无密码直接进入）</Text>

        <View className="space-y-4">
          <View className="space-y-2">
            <Text className="text-sm font-medium text-gray-700 block">考试密码</Text>
            <Input
              type="text"
              password
              value={password}
              onInput={(e) => setPassword(e.detail.value)}
              placeholder="请输入密码（可留空）"
              className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm"
            />
          </View>

          <View className="bg-yellow-50 rounded-xl p-4">
            <Text className="text-xs font-semibold text-yellow-700 block mb-1">考试注意事项</Text>
            <Text className="text-xs text-yellow-600 block leading-relaxed">
              · 进入考试后请勿切换应用{'\n'}
              · 系统会记录切换行为{'\n'}
              · 答案每15秒自动保存{'\n'}
              · 网络中断后重进可恢复答案
            </Text>
          </View>

          <Button
            onClick={handleEnter}
            disabled={loading}
            className="w-full bg-blue-600 text-white font-semibold py-3 rounded-2xl text-sm"
          >
            {loading ? '进入中...' : '进入考试'}
          </Button>
        </View>
      </View>
    </View>
  )
}
