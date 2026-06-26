import { useAuthStore } from '@edu/store'
import { authApi } from '@edu/api'
import { useNavigate, Link } from 'react-router-dom'

interface NavItem {
  label: string
  href: string
  icon: string
  description: string
  roles: string[]
  color: string
}

const NAV_ITEMS: NavItem[] = [
  {
    label: '课程管理',
    href: '/courses',
    icon: '📚',
    description: '查看和管理课程、班级、排课信息',
    roles: ['ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT'],
    color: 'bg-blue-50 border-blue-200 hover:bg-blue-100',
  },
  {
    label: '互动教学',
    href: '/interaction',
    icon: '🎯',
    description: '课堂签到、随机点名、弹幕互动',
    roles: ['ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT'],
    color: 'bg-green-50 border-green-200 hover:bg-green-100',
  },
  {
    label: '题库管理',
    href: '/exam/question-banks',
    icon: '📝',
    description: '创建和管理题目，支持单选、多选、判断、简答',
    roles: ['ROLE_ADMIN', 'ROLE_TEACHER'],
    color: 'bg-purple-50 border-purple-200 hover:bg-purple-100',
  },
  {
    label: '试卷管理',
    href: '/exam/papers',
    icon: '📋',
    description: '组卷、发布考试、查看批改结果',
    roles: ['ROLE_ADMIN', 'ROLE_TEACHER'],
    color: 'bg-orange-50 border-orange-200 hover:bg-orange-100',
  },
  {
    label: '在线考试',
    href: '/exam/list',
    icon: '🖥️',
    description: '教师监考已发布考试，学生进入考试作答',
    roles: ['ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT'],
    color: 'bg-cyan-50 border-cyan-200 hover:bg-cyan-100',
  },
  {
    label: '课件管理',
    href: '/materials',
    icon: '📁',
    description: '上传和管理课件、教学资料',
    roles: ['ROLE_ADMIN', 'ROLE_TEACHER'],
    color: 'bg-yellow-50 border-yellow-200 hover:bg-yellow-100',
  },
  {
    label: '教学统计',
    href: '/stat/class-history',
    icon: '📊',
    description: '查看教学班签到、弹幕、提问等互动趋势',
    roles: ['ROLE_ADMIN', 'ROLE_TEACHER'],
    color: 'bg-teal-50 border-teal-200 hover:bg-teal-100',
  },
  {
    label: '用户管理',
    href: '/admin/users',
    icon: '👥',
    description: '管理系统用户、角色分配、院系设置',
    roles: ['ROLE_ADMIN'],
    color: 'bg-red-50 border-red-200 hover:bg-red-100',
  },
]

function getRoleLabel(role: string): string {
  const map: Record<string, string> = {
    ROLE_ADMIN: '系统管理员',
    ROLE_TEACHER: '教师',
    ROLE_STUDENT: '学生',
  }
  return map[role] ?? role
}

function getRoleColor(role: string): string {
  const map: Record<string, string> = {
    ROLE_ADMIN: 'bg-red-100 text-red-700',
    ROLE_TEACHER: 'bg-blue-100 text-blue-700',
    ROLE_STUDENT: 'bg-green-100 text-green-700',
  }
  return map[role] ?? 'bg-gray-100 text-gray-700'
}

export default function DashboardPage() {
  const { realName, roles, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try {
      await authApi.logout()
    } finally {
      logout()
      navigate('/login', { replace: true })
    }
  }

  const visibleItems = NAV_ITEMS.filter(
    (item) => item.roles.some((r) => roles.includes(r))
  )

  const greeting = () => {
    const h = new Date().getHours()
    if (h < 12) return '早上好'
    if (h < 18) return '下午好'
    return '晚上好'
  }

  return (
    <div className="flex min-h-screen flex-col bg-gray-50">
      {/* Header */}
      <header className="flex h-14 items-center justify-between border-b bg-white px-6 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-blue-600 text-sm font-bold text-white">
            智
          </div>
          <span className="font-semibold text-gray-900">山东管理学院智慧教学平台</span>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            {roles.map((r) => (
              <span key={r} className={`rounded-full px-2 py-0.5 text-xs font-medium ${getRoleColor(r)}`}>
                {getRoleLabel(r)}
              </span>
            ))}
          </div>
          <span className="text-sm text-gray-600">{realName}</span>
          <button
            onClick={handleLogout}
            className="rounded-md px-3 py-1.5 text-sm text-gray-500 hover:bg-gray-100 hover:text-gray-700"
          >
            退出登录
          </button>
        </div>
      </header>

      {/* Main */}
      <main className="flex-1 px-6 py-8">
        <div className="mx-auto max-w-5xl">
          {/* Welcome */}
          <div className="mb-8">
            <h1 className="text-2xl font-semibold text-gray-900">
              {greeting()}，{realName}
            </h1>
            <p className="mt-1 text-sm text-gray-500">欢迎使用山东管理学院智慧教学系统</p>
          </div>

          {/* Quick Stats */}
          <div className="mb-8 grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div className="rounded-xl border bg-white p-4 shadow-sm">
              <p className="text-xs text-gray-500">今日课程</p>
              <p className="mt-1 text-2xl font-semibold text-gray-900">3</p>
            </div>
            <div className="rounded-xl border bg-white p-4 shadow-sm">
              <p className="text-xs text-gray-500">进行中课堂</p>
              <p className="mt-1 text-2xl font-semibold text-blue-600">1</p>
            </div>
            <div className="rounded-xl border bg-white p-4 shadow-sm">
              <p className="text-xs text-gray-500">待批改试卷</p>
              <p className="mt-1 text-2xl font-semibold text-orange-500">0</p>
            </div>
            <div className="rounded-xl border bg-white p-4 shadow-sm">
              <p className="text-xs text-gray-500">系统通知</p>
              <p className="mt-1 text-2xl font-semibold text-gray-900">0</p>
            </div>
          </div>

          {/* Module Cards */}
          <div>
            <h2 className="mb-4 text-sm font-medium text-gray-500 uppercase tracking-wide">功能模块</h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {visibleItems.map((item) => (
                <Link
                  key={item.href + item.label}
                  to={item.href}
                  className={`flex items-start gap-4 rounded-xl border p-5 transition-colors ${item.color}`}
                >
                  <span className="text-3xl">{item.icon}</span>
                  <div>
                    <h3 className="font-semibold text-gray-900">{item.label}</h3>
                    <p className="mt-1 text-sm text-gray-600">{item.description}</p>
                  </div>
                </Link>
              ))}
            </div>
          </div>

          {/* Quick Links for Teachers */}
          {(roles.includes('ROLE_TEACHER') || roles.includes('ROLE_ADMIN')) && (
            <div className="mt-8">
              <h2 className="mb-4 text-sm font-medium text-gray-500 uppercase tracking-wide">快速操作</h2>
              <div className="flex flex-wrap gap-3">
                <Link
                  to="/interaction"
                  className="rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
                >
                  进入课堂
                </Link>
                <Link
                  to="/exam/question-banks"
                  className="rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
                >
                  管理题库
                </Link>
                <Link
                  to="/exam/papers"
                  className="rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
                >
                  出卷 / 发布考试
                </Link>
                <Link
                  to="/materials"
                  className="rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
                >
                  上传课件
                </Link>
              </div>
            </div>
          )}

          {/* Quick Links for Students */}
          {roles.includes('ROLE_STUDENT') && !roles.includes('ROLE_TEACHER') && (
            <div className="mt-8">
              <h2 className="mb-4 text-sm font-medium text-gray-500 uppercase tracking-wide">快速操作</h2>
              <div className="flex flex-wrap gap-3">
                <Link
                  to="/courses"
                  className="rounded-lg border bg-white px-4 py-2 text-sm font-medium text-gray-700 shadow-sm hover:bg-gray-50"
                >
                  我的课程
                </Link>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t bg-white px-6 py-4 text-center text-xs text-gray-400">
        山东管理学院智慧教学系统 © 2026 | 技术支持：信息技术中心
      </footer>
    </div>
  )
}
