import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userApi } from '@edu/api'
import type { UserVO, DeptTreeNode, RoleVO } from '@edu/api'
import { Button, Modal, useToast } from '@edu/ui'

// ---------- DeptTree ----------

function DeptTreeItem({
  node,
  selectedId,
  onSelect,
  depth,
}: {
  node: DeptTreeNode
  selectedId: number | null
  onSelect: (id: number | null) => void
  depth: number
}) {
  const [expanded, setExpanded] = useState(depth === 0)
  const hasChildren = node.children && node.children.length > 0

  return (
    <div>
      <button
        type="button"
        onClick={() => onSelect(selectedId === node.id ? null : node.id)}
        className={`flex w-full items-center gap-1 rounded-lg px-3 py-2 text-left text-sm transition-colors ${
          selectedId === node.id
            ? 'bg-blue-50 font-medium text-blue-700'
            : 'text-gray-700 hover:bg-gray-100'
        }`}
        style={{ paddingLeft: `${0.75 + depth * 1.25}rem` }}
      >
        {hasChildren && (
          <button
            type="button"
            onClick={(e) => { e.stopPropagation(); setExpanded((v) => !v) }}
            className="mr-1 flex-shrink-0 text-gray-400"
          >
            <svg
              className={`h-3 w-3 transition-transform ${expanded ? 'rotate-90' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        )}
        {!hasChildren && <span className="mr-1 w-3 flex-shrink-0" />}
        <svg className="mr-1.5 h-4 w-4 flex-shrink-0 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" />
        </svg>
        <span className="truncate">{node.deptName}</span>
      </button>

      {hasChildren && expanded && (
        <div>
          {node.children!.map((child) => (
            <DeptTreeItem key={child.id} node={child} selectedId={selectedId} onSelect={onSelect} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  )
}

// ---------- RoleAssignModal ----------

function RoleAssignModal({
  user,
  roles,
  open,
  onClose,
}: {
  user: UserVO | null
  roles: RoleVO[]
  open: boolean
  onClose: () => void
}) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<number[]>([])

  React.useEffect(() => {
    if (user && open) {
      const userRoleCodes = user.roles
      const matched = roles.filter((r) => userRoleCodes.includes(r.roleCode)).map((r) => r.id)
      setSelected(matched)
    }
  }, [user, open, roles])

  const mutation = useMutation({
    mutationFn: () => userApi.assignRoles(user!.id, selected),
    onSuccess: () => {
      toast.success('角色分配成功')
      queryClient.invalidateQueries({ queryKey: ['users'] })
      onClose()
    },
    onError: () => toast.error('角色分配失败，请重试'),
  })

  const toggle = (id: number) =>
    setSelected((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={`分配角色 — ${user?.realName ?? ''}`}
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>取消</Button>
          <Button loading={mutation.isPending} onClick={() => mutation.mutate()}>确认</Button>
        </>
      }
    >
      <div className="flex flex-col gap-2">
        {roles.map((role) => (
          <label key={role.id} className="flex cursor-pointer items-center gap-3 rounded-lg border border-gray-100 px-4 py-3 transition hover:bg-gray-50">
            <input
              type="checkbox"
              className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
              checked={selected.includes(role.id)}
              onChange={() => toggle(role.id)}
            />
            <div>
              <p className="text-sm font-medium text-gray-900">{role.roleName}</p>
              <p className="text-xs text-gray-400">{role.roleCode}</p>
            </div>
          </label>
        ))}
        {roles.length === 0 && <p className="py-4 text-center text-sm text-gray-400">暂无角色</p>}
      </div>
    </Modal>
  )
}

// ---------- UserRow ----------

const USER_TYPE_LABELS: Record<number, string> = { 0: '管理员', 1: '教师', 2: '学生' }
const STATUS_LABELS: Record<number, { label: string; cls: string }> = {
  1: { label: '正常', cls: 'bg-green-100 text-green-700' },
  0: { label: '停用', cls: 'bg-red-100 text-red-600' },
}

function UserRow({
  user,
  onAssignRole,
}: {
  user: UserVO
  onAssignRole: (user: UserVO) => void
}) {
  const toast = useToast()
  const queryClient = useQueryClient()

  const statusMutation = useMutation({
    mutationFn: (newStatus: number) => userApi.updateStatus(user.id, newStatus),
    onSuccess: () => {
      toast.success('状态更新成功')
      queryClient.invalidateQueries({ queryKey: ['users'] })
    },
    onError: () => toast.error('状态更新失败'),
  })

  const st = STATUS_LABELS[user.status] ?? STATUS_LABELS[1]

  return (
    <tr className="border-t border-gray-100 hover:bg-gray-50">
      <td className="px-4 py-3">
        <div className="flex items-center gap-3">
          {user.avatarUrl ? (
            <img src={user.avatarUrl} className="h-8 w-8 rounded-full object-cover" alt="" />
          ) : (
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-sm font-medium text-blue-700">
              {user.realName.charAt(0)}
            </div>
          )}
          <div>
            <p className="text-sm font-medium text-gray-900">{user.realName}</p>
            <p className="text-xs text-gray-400">{user.username}</p>
          </div>
        </div>
      </td>
      <td className="px-4 py-3 text-sm text-gray-600">{user.deptName ?? '—'}</td>
      <td className="px-4 py-3 text-sm text-gray-600">{USER_TYPE_LABELS[user.userType] ?? '未知'}</td>
      <td className="px-4 py-3">
        <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${st.cls}`}>
          {st.label}
        </span>
      </td>
      <td className="px-4 py-3 text-sm text-gray-600">{user.lastLoginAt ? user.lastLoginAt.substring(0, 10) : '从未登录'}</td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <Button size="sm" variant="ghost" onClick={() => onAssignRole(user)}>
            分配角色
          </Button>
          <Button
            size="sm"
            variant={user.status === 1 ? 'secondary' : 'ghost'}
            loading={statusMutation.isPending}
            onClick={() => statusMutation.mutate(user.status === 1 ? 0 : 1)}
          >
            {user.status === 1 ? '停用' : '启用'}
          </Button>
        </div>
      </td>
    </tr>
  )
}

// ---------- Page ----------

export default function UserManagePage() {
  const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null)
  const [keyword, setKeyword] = useState('')
  const [userType, setUserType] = useState<number | undefined>()
  const [pageNum, setPageNum] = useState(1)
  const pageSize = 15

  const [roleModalUser, setRoleModalUser] = useState<UserVO | null>(null)

  const { data: deptTree = [], isLoading: deptLoading } = useQuery({
    queryKey: ['dept-tree'],
    queryFn: () => userApi.getDeptTree(),
    staleTime: 300_000,
  })

  const { data: roles = [] } = useQuery({
    queryKey: ['roles'],
    queryFn: () => userApi.getRoles(),
    staleTime: 300_000,
  })

  const { data: usersPage, isLoading: usersLoading } = useQuery({
    queryKey: ['users', { keyword, userType, deptId: selectedDeptId, pageNum }],
    queryFn: () =>
      userApi.getUsers({ keyword: keyword || undefined, userType, deptId: selectedDeptId ?? undefined, pageNum, pageSize }),
  })

  const totalPages = usersPage?.pages ?? 1

  return (
    <div className="flex h-screen flex-col bg-gray-50">
      {/* Top bar */}
      <header className="flex h-14 items-center justify-between border-b border-gray-200 bg-white px-6">
        <h1 className="text-base font-semibold text-gray-900">用户管理</h1>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* Dept sidebar */}
        <aside className="hidden w-56 flex-shrink-0 overflow-y-auto border-r border-gray-200 bg-white py-3 md:flex md:flex-col">
          <p className="mb-2 px-4 text-xs font-semibold uppercase tracking-wider text-gray-400">院系</p>
          <button
            type="button"
            onClick={() => { setSelectedDeptId(null); setPageNum(1) }}
            className={`flex w-full items-center gap-2 px-4 py-2 text-sm transition-colors ${
              selectedDeptId === null ? 'bg-blue-50 font-medium text-blue-700' : 'text-gray-700 hover:bg-gray-100'
            }`}
          >
            全部
          </button>
          {deptLoading && <p className="px-4 py-2 text-xs text-gray-400">加载中…</p>}
          {deptTree.map((node) => (
            <DeptTreeItem
              key={node.id}
              node={node}
              selectedId={selectedDeptId}
              onSelect={(id) => { setSelectedDeptId(id); setPageNum(1) }}
              depth={0}
            />
          ))}
        </aside>

        {/* Main content */}
        <main className="flex flex-1 flex-col overflow-hidden">
          {/* Filters */}
          <div className="flex flex-wrap items-center gap-3 border-b border-gray-200 bg-white px-6 py-3">
            <input
              type="text"
              placeholder="搜索姓名 / 用户名 / 手机号"
              value={keyword}
              onChange={(e) => { setKeyword(e.target.value); setPageNum(1) }}
              className="h-9 w-64 rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm text-gray-700 focus:border-blue-400 focus:outline-none focus:ring-1 focus:ring-blue-400"
            />
            <select
              value={userType ?? ''}
              onChange={(e) => { setUserType(e.target.value === '' ? undefined : Number(e.target.value)); setPageNum(1) }}
              className="h-9 rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm text-gray-700 focus:border-blue-400 focus:outline-none focus:ring-1 focus:ring-blue-400"
            >
              <option value="">全部类型</option>
              <option value="0">管理员</option>
              <option value="1">教师</option>
              <option value="2">学生</option>
            </select>
            <span className="ml-auto text-sm text-gray-400">
              共 {usersPage?.total ?? 0} 条
            </span>
          </div>

          {/* Table */}
          <div className="flex-1 overflow-auto">
            <table className="min-w-full">
              <thead className="sticky top-0 bg-gray-50">
                <tr>
                  {['用户', '院系', '类型', '状态', '最后登录', '操作'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50 bg-white">
                {usersLoading ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-sm text-gray-400">加载中…</td>
                  </tr>
                ) : (usersPage?.records ?? []).length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-12 text-center text-sm text-gray-400">暂无数据</td>
                  </tr>
                ) : (
                  (usersPage?.records ?? []).map((user) => (
                    <UserRow key={user.id} user={user} onAssignRole={setRoleModalUser} />
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 border-t border-gray-200 bg-white py-3">
              <Button size="sm" variant="secondary" disabled={pageNum <= 1} onClick={() => setPageNum((p) => p - 1)}>
                上一页
              </Button>
              <span className="text-sm text-gray-600">
                {pageNum} / {totalPages}
              </span>
              <Button size="sm" variant="secondary" disabled={pageNum >= totalPages} onClick={() => setPageNum((p) => p + 1)}>
                下一页
              </Button>
            </div>
          )}
        </main>
      </div>

      {/* Role Modal */}
      <RoleAssignModal
        user={roleModalUser}
        roles={roles}
        open={roleModalUser !== null}
        onClose={() => setRoleModalUser(null)}
      />
    </div>
  )
}
