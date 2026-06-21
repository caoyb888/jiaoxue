import Taro, { useRouter } from '@tarojs/taro'
import { View, Text, Button, RadioGroup, Radio, CheckboxGroup, Checkbox } from '@tarojs/components'
import { useState, useEffect, useRef, useCallback } from 'react'
import { examStudentApi } from '@edu/api/modules/exam'
import type { ExamEnterVO, PaperQuestionDetailVO, AnswerItemDTO } from '@edu/api/modules/exam'

const DRAFT_KEY_PREFIX = 'exam_draft'
const ENTER_KEY_PREFIX = 'exam_enter'
const SAVE_INTERVAL_MS = 15_000  // CLAUDE.md §6.6 禁止修改此值

/**
 * 考试答题页 Taro小程序版（S5-14）
 *
 * 【C8约束】：
 * - 不使用 document.*、window.*、localStorage
 * - 草稿用 Taro.setStorageSync（替代 IndexedDB）
 * - 锁屏防切换用 Taro.setKeepScreenOn + Taro.onAppHide
 * - 只支持客观题（单选/多选/判断），主观题在小程序端不展示
 */
export default function ExamAnswerPage() {
  const router = useRouter()
  const publishId = Number(router.params.publishId ?? '0')

  const [enterData, setEnterData] = useState<ExamEnterVO | null>(null)
  const [questions, setQuestions] = useState<PaperQuestionDetailVO[]>([])
  const [answers, setAnswers] = useState<Record<number, string>>({})
  const [currentPage, setCurrentPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [submitted, setSubmitted] = useState(false)
  const [loadingPage, setLoadingPage] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const userIdRef = useRef<number>(0)

  // 初始化：加载考试数据 + 恢复草稿
  useEffect(() => {
    if (!publishId) return

    const stored = Taro.getStorageSync(`${ENTER_KEY_PREFIX}_${publishId}`)
    if (!stored) {
      Taro.showToast({ title: '考试数据丢失，请重新进入', icon: 'error' })
      Taro.navigateBack()
      return
    }
    const data: ExamEnterVO = JSON.parse(stored as string)
    setEnterData(data)
    setQuestions(data.questions)
    setTotalPages(data.totalPages)
    setCurrentPage(data.currentPage ?? 1)

    // 读取 userId（登录时由 edu-auth 存入 storage）
    const storedUserId = Taro.getStorageSync('userId')
    userIdRef.current = storedUserId ? Number(storedUserId) : 0

    // 恢复本地草稿
    const draftKey = `${DRAFT_KEY_PREFIX}_${publishId}_${userIdRef.current}`
    const draft = Taro.getStorageSync(draftKey)
    if (draft) {
      try {
        const items: AnswerItemDTO[] = JSON.parse(draft as string)
        const restored: Record<number, string> = {}
        items.forEach((a) => { restored[a.questionId] = a.answerContent })
        setAnswers(restored)
      } catch (_) {}
    }

    // 锁屏防切换：考试期间保持屏幕常亮
    Taro.setKeepScreenOn({ keepScreenOn: true })

    return () => {
      Taro.setKeepScreenOn({ keepScreenOn: false })
    }
  }, [publishId])

  const getAnswerItems = useCallback((): AnswerItemDTO[] =>
    Object.entries(answers).map(([qId, content]) => ({
      questionId: Number(qId),
      answerContent: content,
    })), [answers])

  const saveDraft = useCallback(() => {
    if (submitted || !publishId) return
    const draftKey = `${DRAFT_KEY_PREFIX}_${publishId}_${userIdRef.current}`
    try {
      Taro.setStorageSync(draftKey, JSON.stringify(getAnswerItems()))
    } catch (_) {}
  }, [publishId, submitted, getAnswerItems])

  // 每15秒自动保存草稿（C2约束，小程序版）
  useEffect(() => {
    if (submitted) return
    const timer = setInterval(saveDraft, SAVE_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [saveDraft, submitted])

  // 心跳（30秒）
  useEffect(() => {
    if (submitted || !publishId) return
    const timer = setInterval(() => {
      examStudentApi.heartbeat(publishId).catch(() => {})
    }, 30_000)
    return () => clearInterval(timer)
  }, [publishId, submitted])

  // 小程序切换防作弊：监听 App-Hide / App-Show 事件
  useEffect(() => {
    if (submitted || !publishId) return

    const handleHide = () => {
      saveDraft()  // 离开前保存草稿
      examStudentApi.reportMonitorEvent(publishId, 'APP_HIDE').catch(() => {})
    }
    const handleShow = () => {
      examStudentApi.reportMonitorEvent(publishId, 'APP_SHOW').catch(() => {})
    }

    Taro.onAppHide(handleHide)
    Taro.onAppShow(handleShow)
    return () => {
      Taro.offAppHide(handleHide)
      Taro.offAppShow(handleShow)
    }
  }, [publishId, submitted, saveDraft])

  const doSubmit = async (submitType: 'MANUAL' | 'AUTO') => {
    if (submitting || submitted) return
    setSubmitting(true)
    try {
      await examStudentApi.submitExam(publishId, {
        answers: getAnswerItems(),
        submitType,
        clientSubmitAt: new Date().toISOString(),
      })
      setSubmitted(true)
      Taro.removeStorageSync(`${DRAFT_KEY_PREFIX}_${publishId}_${userIdRef.current}`)
      Taro.showToast({ title: '交卷成功！', icon: 'success', duration: 2000 })
    } catch (e: unknown) {
      const err = e as { response?: { data?: { msg?: string } } }
      const msg = err?.response?.data?.msg ?? '交卷失败，请重试'
      Taro.showToast({ title: msg, icon: 'error' })
    } finally {
      setSubmitting(false)
    }
  }

  const confirmSubmit = () => {
    saveDraft()
    Taro.showModal({
      title: '确认交卷',
      content: '交卷后无法修改答案，确认提交？',
      confirmText: '确认交卷',
      cancelText: '继续作答',
      success: (res) => {
        if (res.confirm) doSubmit('MANUAL')
      },
    })
  }

  const loadPage = async (page: number) => {
    setLoadingPage(true)
    try {
      const result = await examStudentApi.getQuestionsPage(publishId, page)
      setQuestions(result.data.questions)
      setCurrentPage(result.data.currentPage)
      Taro.pageScrollTo({ scrollTop: 0, duration: 200 })
    } catch (_) {
      Taro.showToast({ title: '加载题目失败', icon: 'error' })
    } finally {
      setLoadingPage(false)
    }
  }

  const setAnswer = (questionId: number, value: string) => {
    setAnswers((prev) => ({ ...prev, [questionId]: value }))
  }

  if (!enterData) {
    return (
      <View className="min-h-screen flex items-center justify-center bg-gray-50">
        <Text className="text-gray-400 text-sm">加载考试数据中...</Text>
      </View>
    )
  }

  if (submitted) {
    return (
      <View className="min-h-screen flex flex-col items-center justify-center bg-green-50 p-6">
        <View className="bg-white rounded-3xl p-10 text-center w-full max-w-xs shadow-sm">
          <Text className="text-5xl block mb-4">✅</Text>
          <Text className="text-xl font-bold text-gray-900 block mb-2">交卷成功</Text>
          <Text className="text-sm text-gray-500 block">成绩将在阅卷完成后公布</Text>
        </View>
      </View>
    )
  }

  return (
    <View className="min-h-screen bg-gray-50 pb-32">
      {/* 顶栏 */}
      <View className="sticky top-0 z-10 bg-white px-4 py-3 flex items-center justify-between border-b border-gray-100">
        <Text className="text-sm text-gray-600">
          第{currentPage}/{totalPages}页
        </Text>
        <Text className="text-xs text-blue-600 font-medium">
          {enterData.endTime
            ? `${new Date(enterData.endTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })} 结束`
            : ''}
        </Text>
      </View>

      {/* 题目列表（客观题：单选/多选/判断） */}
      <View className="px-4 py-4 space-y-4">
        {questions.map((pq, idx) => {
          const q = pq.question
          const isObjective = [1, 2, 3].includes(q.type)
          if (!isObjective) return null  // 主观题在小程序端跳过

          const globalIdx = idx + 1 + (currentPage - 1) * 10
          const currentValue = answers[pq.questionId] ?? ''

          return (
            <View key={pq.id} className="bg-white rounded-2xl p-5 shadow-sm">
              <View className="flex items-start gap-2 mb-4">
                <Text className="text-xs font-semibold text-blue-600 shrink-0 mt-0.5">第{globalIdx}题</Text>
                <Text className="text-sm text-gray-800 leading-relaxed flex-1">{q.content}</Text>
                <Text className="text-xs text-gray-400 shrink-0">{pq.score}分</Text>
              </View>

              {/* 单选题 */}
              {q.type === 1 && (
                <RadioGroup onChange={(e) => setAnswer(pq.questionId, e.detail.value)}>
                  <View className="space-y-2">
                    {q.options.map((opt) => (
                      <View key={opt.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50">
                        <Radio value={opt.optionLabel} checked={currentValue === opt.optionLabel} color="#2563eb" />
                        <Text className="text-sm text-gray-700">
                          <Text className="font-medium">{opt.optionLabel}.</Text> {opt.content}
                        </Text>
                      </View>
                    ))}
                  </View>
                </RadioGroup>
              )}

              {/* 多选题 */}
              {q.type === 2 && (
                <CheckboxGroup onChange={(e) => setAnswer(pq.questionId, [...e.detail.value].sort().join(''))}>
                  <View className="space-y-2">
                    {q.options.map((opt) => (
                      <View key={opt.id} className="flex items-center gap-3 p-3 rounded-xl bg-gray-50">
                        <Checkbox
                          value={opt.optionLabel}
                          checked={currentValue.includes(opt.optionLabel)}
                          color="#2563eb"
                        />
                        <Text className="text-sm text-gray-700">
                          <Text className="font-medium">{opt.optionLabel}.</Text> {opt.content}
                        </Text>
                      </View>
                    ))}
                  </View>
                </CheckboxGroup>
              )}

              {/* 判断题 */}
              {q.type === 3 && (
                <RadioGroup onChange={(e) => setAnswer(pq.questionId, e.detail.value)}>
                  <View className="flex gap-3">
                    {[{ label: '正确', value: 'T' }, { label: '错误', value: 'F' }].map((opt) => (
                      <View key={opt.value} className="flex items-center gap-2 p-3 rounded-xl bg-gray-50 flex-1">
                        <Radio value={opt.value} checked={currentValue === opt.value} color="#2563eb" />
                        <Text className="text-sm text-gray-700">{opt.label}</Text>
                      </View>
                    ))}
                  </View>
                </RadioGroup>
              )}
            </View>
          )
        })}
      </View>

      {/* 底部操作栏 */}
      <View className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-100 px-4 py-3 flex items-center justify-between z-10">
        <View className="flex gap-2">
          <Button
            onClick={() => loadPage(currentPage - 1)}
            disabled={currentPage <= 1 || loadingPage}
            className="px-4 py-2 text-sm border border-gray-200 rounded-xl text-gray-600 disabled:opacity-40"
          >
            上一页
          </Button>
          <Button
            onClick={() => loadPage(currentPage + 1)}
            disabled={currentPage >= totalPages || loadingPage}
            className="px-4 py-2 text-sm border border-gray-200 rounded-xl text-gray-600 disabled:opacity-40"
          >
            下一页
          </Button>
        </View>
        <Button
          onClick={confirmSubmit}
          disabled={submitting}
          className="bg-blue-600 text-white text-sm font-semibold px-5 py-2 rounded-xl"
        >
          {submitting ? '提交中...' : '交卷'}
        </Button>
      </View>
    </View>
  )
}
