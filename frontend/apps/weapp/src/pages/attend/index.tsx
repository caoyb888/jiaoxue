import Taro, { useRouter } from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { useState } from 'react'
import { interactionApi } from '@edu/api/modules/interaction'

type Mode = 'select' | 'code' | 'success' | 'already'

/**
 * 学生签到页 - Taro 小程序版（S3-12）
 *
 * 【C8 约束】：
 * - 不使用 document.*、window.*、localStorage
 * - 摄像头用 Taro.scanCode 替代 getUserMedia
 * - 存储用 Taro.setStorageSync 替代 localStorage
 */
export default function AttendPage() {
  const router = useRouter()
  const lessonId = Number(router.params.lessonId ?? '0')

  const [mode, setMode] = useState<Mode>('select')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [totalCount, setTotalCount] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)

  const handleScanQr = () => {
    // 【C8】小程序扫码用 Taro.scanCode，不用 getUserMedia
    Taro.scanCode({
      onlyFromCamera: true,
      scanType: ['qrCode'],
      success: async (res) => {
        const qrToken = res.result
        setLoading(true)
        try {
          const result = await interactionApi.attend(lessonId, { qrToken })
          setTotalCount(result.totalCount)
          setMode(result.firstAttend ? 'success' : 'already')
        } catch {
          Taro.showToast({ title: '签到码无效或已过期', icon: 'error' })
        } finally {
          setLoading(false)
        }
      },
      fail: () => {
        Taro.showToast({ title: '扫码取消', icon: 'none' })
      },
    })
  }

  const handleCodeSubmit = async () => {
    if (!code.trim()) {
      setError('请输入签到口令')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const result = await interactionApi.attend(lessonId, { code: code.trim().toUpperCase() })
      setTotalCount(result.totalCount)
      setMode(result.firstAttend ? 'success' : 'already')
    } catch {
      setError('签到码无效或已过期')
    } finally {
      setLoading(false)
    }
  }

  if (mode === 'success') {
    return (
      <View className="flex flex-col items-center justify-center min-h-screen bg-green-50 p-6">
        <View className="bg-white rounded-3xl p-10 text-center w-full max-w-xs">
          <View className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <Text className="text-5xl">✅</Text>
          </View>
          <Text className="text-2xl font-bold text-gray-900 block mb-2">签到成功！</Text>
          {totalCount !== null && (
            <Text className="text-gray-500 text-sm">当前已有 {totalCount} 人签到</Text>
          )}
        </View>
      </View>
    )
  }

  if (mode === 'already') {
    return (
      <View className="flex flex-col items-center justify-center min-h-screen bg-yellow-50 p-6">
        <View className="bg-white rounded-3xl p-10 text-center w-full max-w-xs">
          <Text className="text-5xl block mb-4">⚠️</Text>
          <Text className="text-2xl font-bold text-gray-900 block mb-2">您已签到</Text>
          <Text className="text-gray-500 text-sm block mb-6">请勿重复操作</Text>
          <Button onClick={() => setMode('select')} className="text-sm text-blue-600">
            返回
          </Button>
        </View>
      </View>
    )
  }

  if (mode === 'code') {
    return (
      <View className="flex flex-col items-center justify-center min-h-screen bg-gray-50 p-6">
        <View className="bg-white rounded-3xl p-8 w-full max-w-xs">
          <Button
            onClick={() => { setMode('select'); setError(null); setCode('') }}
            className="text-gray-400 text-sm mb-6 text-left"
          >
            ← 返回
          </Button>

          <Text className="text-xl font-bold text-gray-900 block mb-6">输入签到口令</Text>

          <Input
            type="text"
            value={code}
            onInput={e => setCode(e.detail.value.toUpperCase())}
            placeholder="请输入6位口令"
            maxlength={8}
            className="text-center text-3xl font-mono tracking-widest border-2 border-gray-200 rounded-2xl py-4 px-4 w-full mb-2"
          />

          {error && (
            <Text className="text-red-500 text-sm text-center block mb-4">{error}</Text>
          )}

          <Button
            onClick={handleCodeSubmit}
            disabled={loading || !code.trim()}
            className="mt-4 w-full bg-blue-600 text-white font-medium py-3 rounded-2xl"
          >
            {loading ? '签到中…' : '确认签到'}
          </Button>
        </View>
      </View>
    )
  }

  return (
    <View className="flex flex-col items-center justify-center min-h-screen bg-gray-50 p-6">
      <View className="bg-white rounded-3xl p-8 w-full max-w-xs">
        <Text className="text-xl font-bold text-gray-900 text-center block mb-8">课堂签到</Text>

        <View className="space-y-4">
          <Button
            onClick={handleScanQr}
            className="w-full flex items-center gap-4 p-4 rounded-2xl border border-blue-100 bg-blue-50 text-left"
          >
            <Text className="text-2xl">📷</Text>
            <View>
              <Text className="font-semibold text-gray-900 block">扫码签到</Text>
              <Text className="text-sm text-gray-500">扫描教师屏幕二维码</Text>
            </View>
          </Button>

          <Button
            onClick={() => setMode('code')}
            className="w-full flex items-center gap-4 p-4 rounded-2xl border border-purple-100 bg-purple-50 text-left"
          >
            <Text className="text-2xl">🔤</Text>
            <View>
              <Text className="font-semibold text-gray-900 block">口令签到</Text>
              <Text className="text-sm text-gray-500">输入6位签到口令</Text>
            </View>
          </Button>
        </View>
      </View>
    </View>
  )
}
