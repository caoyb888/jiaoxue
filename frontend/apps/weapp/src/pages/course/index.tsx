import { View, Text, Input, ScrollView } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useQuery } from '@tanstack/react-query'
import { courseApi } from '@edu/api'
import type { ClassRoomVO } from '@edu/api'
import { useState } from 'react'

// ⚠️ 小程序端严禁使用 document.* / window.* / localStorage
// 数据存储必须使用 Taro.setStorageSync / Taro.getStorageSync

export default function CoursePage() {
  const [keyword, setKeyword] = useState('')

  const { data: classes = [], isLoading } = useQuery({
    queryKey: ['weapp-myClasses'],
    queryFn: () => courseApi.myClasses().then((r) => r.data),
    staleTime: 60_000,
  })

  const filtered = keyword
    ? classes.filter((c) => c.className.includes(keyword) || c.courseName.includes(keyword))
    : classes

  const goToClassroom = (cls: ClassRoomVO) => {
    Taro.navigateTo({ url: `/pages/classroom/index?classId=${cls.id}&className=${cls.className}` })
  }

  return (
    <View className="min-h-screen bg-gray-50">
      {/* 搜索栏 */}
      <View className="bg-white px-4 py-3 border-b border-gray-100">
        <Input
          value={keyword}
          onInput={(e) => setKeyword(e.detail.value)}
          placeholder="搜索课程或班级..."
          className="rounded-lg bg-gray-100 px-3 py-2 text-sm"
        />
      </View>

      {/* 课程列表 */}
      <ScrollView scrollY className="flex-1">
        {isLoading ? (
          <View className="flex h-40 items-center justify-center">
            <Text className="text-gray-400 text-sm">加载中...</Text>
          </View>
        ) : filtered.length === 0 ? (
          <View className="flex h-40 items-center justify-center">
            <Text className="text-gray-400 text-sm">暂无课程</Text>
          </View>
        ) : (
          <View className="p-4 space-y-3">
            {filtered.map((cls) => (
              <ClassCard key={cls.id} cls={cls} onPress={() => goToClassroom(cls)} />
            ))}
          </View>
        )}
      </ScrollView>
    </View>
  )
}

function ClassCard({ cls, onPress }: { cls: ClassRoomVO; onPress: () => void }) {
  const statusColor = cls.status === 1 ? '#16a34a' : '#9ca3af'
  const statusText = cls.status === 1 ? '进行中' : '已结束'

  return (
    <View
      onClick={onPress}
      className="bg-white rounded-xl p-4 shadow-sm border border-gray-100"
    >
      <View className="flex items-start justify-between">
        <Text className="text-base font-semibold text-gray-900">{cls.className}</Text>
        <View
          className="rounded-full px-2 py-0.5 text-xs"
          style={{ backgroundColor: cls.status === 1 ? '#dcfce7' : '#f3f4f6', color: statusColor }}
        >
          <Text style={{ color: statusColor, fontSize: '11px' }}>{statusText}</Text>
        </View>
      </View>
      <Text className="mt-1 text-sm text-gray-500">{cls.courseName}</Text>
      <View className="mt-2 flex items-center gap-3">
        <Text className="text-xs text-gray-400">教师：{cls.teacherName}</Text>
        <Text className="text-xs text-gray-400">学期：{cls.semester}</Text>
        <Text className="text-xs text-gray-400">{cls.studentCount} 人</Text>
      </View>
    </View>
  )
}
