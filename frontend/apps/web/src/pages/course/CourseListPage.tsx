import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCourseList } from '@edu/api'
import type { CourseListItemVO } from '@edu/api'

export default function CourseListPage() {
  const [keyword, setKeyword] = useState('')
  const [semester, setSemester] = useState('')
  const [page, setPage] = useState(1)

  const { data, isLoading, isError } = useCourseList({
    keyword: keyword || undefined,
    semester: semester || undefined,
    page,
    size: 12,
  })

  return (
    <div className="min-h-screen bg-gray-50">
      {/* 顶部搜索栏 */}
      <div className="bg-white border-b px-4 py-3 md:px-8">
        <div className="flex flex-col gap-3 md:flex-row md:items-center">
          <h1 className="text-lg font-semibold text-gray-900">课程管理</h1>
          <div className="flex flex-1 gap-2 md:max-w-xl">
            <input
              type="text"
              value={keyword}
              onChange={(e) => { setKeyword(e.target.value); setPage(1) }}
              placeholder="搜索课程名称..."
              className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
            <input
              type="text"
              value={semester}
              onChange={(e) => { setSemester(e.target.value); setPage(1) }}
              placeholder="学期（如 2025-2026-1）"
              className="w-44 rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
          </div>
        </div>
      </div>

      <main className="px-4 py-6 md:px-8">
        {/* 状态处理 */}
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

        {data && (
          <>
            <p className="mb-4 text-sm text-gray-500">共 {data.total} 门课程</p>

            {/* 课程卡片网格 */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {data.list.map((course) => (
                <CourseCard key={course.id} course={course} />
              ))}
            </div>

            {/* 分页 */}
            {data.pages > 1 && (
              <div className="mt-6 flex items-center justify-center gap-2">
                <button
                  disabled={page <= 1}
                  onClick={() => setPage((p) => p - 1)}
                  className="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-40"
                >
                  上一页
                </button>
                <span className="text-sm text-gray-600">
                  {page} / {data.pages}
                </span>
                <button
                  disabled={page >= data.pages}
                  onClick={() => setPage((p) => p + 1)}
                  className="rounded px-3 py-1 text-sm text-gray-600 hover:bg-gray-100 disabled:opacity-40"
                >
                  下一页
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  )
}

function CourseCard({ course }: { course: CourseListItemVO }) {
  const typeColor: Record<string, string> = {
    REQUIRED: 'bg-blue-100 text-blue-700',
    ELECTIVE: 'bg-green-100 text-green-700',
    PUBLIC: 'bg-purple-100 text-purple-700',
  }

  return (
    <Link
      to={`/course/${course.id}/classes`}
      className="group flex flex-col rounded-xl border border-gray-200 bg-white p-4 shadow-sm transition hover:border-blue-400 hover:shadow-md"
    >
      <div className="mb-3 flex items-start justify-between">
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${typeColor[course.courseType] ?? 'bg-gray-100 text-gray-600'}`}>
          {COURSE_TYPE_LABEL[course.courseType] ?? course.courseType}
        </span>
        <span className="text-xs text-gray-400">{course.credit} 学分</span>
      </div>

      <h3 className="flex-1 text-sm font-semibold text-gray-900 group-hover:text-blue-600">
        {course.courseName}
      </h3>

      <div className="mt-3 space-y-1 text-xs text-gray-500">
        <div className="flex items-center gap-1">
          <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
          </svg>
          {course.deptName}
        </div>
        <div className="flex items-center gap-1">
          <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          {course.semester}
        </div>
        <div className="flex items-center gap-1">
          <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0" />
          </svg>
          {course.classCount} 个教学班
        </div>
      </div>
    </Link>
  )
}

const COURSE_TYPE_LABEL: Record<string, string> = {
  REQUIRED: '必修',
  ELECTIVE: '选修',
  PUBLIC: '公共课',
}
