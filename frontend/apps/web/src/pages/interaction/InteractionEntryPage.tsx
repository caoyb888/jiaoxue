import React from 'react'
import { Link } from 'react-router-dom'
import { useMyClasses } from '@edu/api'
import type { ClassRoomVO } from '@edu/api'

/** 互动教学入口：列出我的教学班，进入对应课堂开展签到/点名/弹幕 */
export default function InteractionEntryPage() {
  const { data: classes = [], isLoading, isError } = useMyClasses()

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="border-b bg-white px-4 py-3 md:px-8">
        <h1 className="text-lg font-semibold text-gray-900">互动教学</h1>
        <p className="mt-0.5 text-sm text-gray-500">
          选择教学班进入课堂，开展课堂签到、随机点名、弹幕互动
        </p>
      </div>

      <main className="px-4 py-6 md:px-8">
        {isLoading && (
          <div className="flex h-48 items-center justify-center text-gray-400">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
          </div>
        )}

        {isError && (
          <div className="flex h-48 items-center justify-center text-red-500">
            加载失败，请刷新重试
          </div>
        )}

        {!isLoading && !isError && classes.length === 0 && (
          <div className="flex h-48 flex-col items-center justify-center gap-2 text-gray-400">
            <p className="text-sm">暂无可上课的教学班</p>
            <p className="text-xs">请先在「课程管理」中确认你已分配教学班</p>
          </div>
        )}

        {classes.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {classes.map((cls) => (
              <ClassCard key={cls.id} cls={cls} />
            ))}
          </div>
        )}
      </main>
    </div>
  )
}

function ClassCard({ cls }: { cls: ClassRoomVO }) {
  return (
    <div className="flex flex-col rounded-xl border bg-white p-5 shadow-sm transition-shadow hover:shadow-md">
      <h3 className="font-semibold text-gray-900">{cls.className}</h3>
      <p className="mt-1 text-sm text-gray-600">{cls.courseName}</p>
      <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500">
        <span>{cls.classCode}</span>
        <span>·</span>
        <span>{cls.studentCount} 名学生</span>
        <span>·</span>
        <span>{cls.semester}</span>
      </div>
      <Link
        to={`/course/${cls.id}/classroom`}
        className="mt-4 inline-flex items-center justify-center rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
      >
        进入课堂 →
      </Link>
    </div>
  )
}
