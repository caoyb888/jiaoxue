import React, { useRef, useState } from 'react'
import { useMaterialList, materialApi } from '@edu/api'
import type { MaterialListItemVO } from '@edu/api'
import { useQueryClient } from '@tanstack/react-query'
import { isWeb } from '@edu/utils'

const FILE_TYPE_MAP: Record<string, string> = {
  pptx: 'PPT',
  pdf: 'PDF',
  docx: 'Word',
  mp4: '视频',
}

const STATUS_LABEL: Record<number, { text: string; cls: string }> = {
  0: { text: '转换中', cls: 'text-yellow-600 bg-yellow-50' },
  1: { text: '可用', cls: 'text-green-600 bg-green-50' },
  2: { text: '失败', cls: 'text-red-600 bg-red-50' },
}

/** 课件管理页：三步上传 + 列表 */
export default function MaterialManagePage() {
  const [keyword, setKeyword] = useState('')
  const [page] = useState(1)
  const { data, isLoading } = useMaterialList({ keyword: keyword || undefined, page })
  const qc = useQueryClient()

  // 上传状态机
  const [uploadState, setUploadState] = useState<'idle' | 'pending' | 'uploading' | 'completing' | 'done'>('idle')
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [titleInput, setTitleInput] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0]
    if (!file || !titleInput.trim()) {
      setUploadError('请选择文件并填写标题')
      return
    }

    const ext = file.name.split('.').pop()?.toLowerCase() ?? ''
    const allowed = ['pptx', 'pdf', 'docx', 'mp4']
    if (!allowed.includes(ext)) {
      setUploadError('仅支持 pptx / pdf / docx / mp4')
      return
    }

    setUploadError(null)
    setUploadState('pending')

    try {
      // Step 1: 申请预签名上传 URL
      const { data: ticket } = await materialApi.applyUpload({
        fileName: file.name,
        fileType: ext,
        fileSizeKb: Math.ceil(file.size / 1024),
      })

      setUploadState('uploading')

      // Step 2: 前端直接 PUT MinIO（绕过后端带宽）
      // 小程序环境用 Taro.uploadFile，Web 用 fetch PUT
      if (isWeb) {
        const res = await fetch(ticket.presignedUrl, {
          method: 'PUT',
          body: file,
          headers: { 'Content-Type': file.type || 'application/octet-stream' },
        })
        if (!res.ok) throw new Error(`上传失败: HTTP ${res.status}`)
      }

      setUploadState('completing')

      // Step 3: 通知后端上传完成
      await materialApi.completeUpload({
        uploadId: ticket.uploadId,
        title: titleInput.trim(),
      })

      setUploadState('done')
      qc.invalidateQueries({ queryKey: ['materials'] })

      // 重置表单
      setTimeout(() => {
        setUploadState('idle')
        setTitleInput('')
        if (fileRef.current) fileRef.current.value = ''
      }, 2000)
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : '上传失败'
      setUploadError(msg)
      setUploadState('idle')
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-white border-b px-4 py-3 md:px-8">
        <h1 className="text-lg font-semibold text-gray-900">课件管理</h1>
      </div>

      <div className="px-4 py-6 md:px-8 space-y-6">
        {/* 上传区域 */}
        <section className="rounded-xl border border-dashed border-gray-300 bg-white p-6">
          <h2 className="mb-4 text-sm font-medium text-gray-700">上传课件</h2>

          <div className="space-y-3">
            <div>
              <label className="block text-xs text-gray-500 mb-1">课件标题</label>
              <input
                type="text"
                value={titleInput}
                onChange={(e) => setTitleInput(e.target.value)}
                placeholder="请输入课件标题"
                disabled={uploadState !== 'idle'}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 disabled:bg-gray-50"
              />
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">选择文件（支持 pptx / pdf / docx / mp4）</label>
              <input
                ref={fileRef}
                type="file"
                accept=".pptx,.pdf,.docx,.mp4"
                disabled={uploadState !== 'idle'}
                className="block w-full text-sm text-gray-500 file:mr-3 file:rounded file:border-0 file:bg-blue-50 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-blue-700 hover:file:bg-blue-100 disabled:opacity-60"
              />
            </div>

            {uploadError && (
              <p className="text-xs text-red-500">{uploadError}</p>
            )}

            <UploadProgress state={uploadState} />

            <button
              onClick={handleUpload}
              disabled={uploadState !== 'idle'}
              className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
            >
              {uploadState === 'idle' ? '开始上传' : UPLOAD_STATE_LABEL[uploadState]}
            </button>
          </div>
        </section>

        {/* 课件列表 */}
        <section>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-medium text-gray-700">课件库</h2>
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="搜索课件..."
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-blue-500"
            />
          </div>

          {isLoading ? (
            <div className="flex h-32 items-center justify-center text-gray-400">
              <div className="h-6 w-6 animate-spin rounded-full border-4 border-blue-500 border-t-transparent" />
            </div>
          ) : (
            <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 text-xs text-gray-500">
                  <tr>
                    <th className="px-4 py-3 text-left">标题</th>
                    <th className="px-4 py-3 text-left hidden md:table-cell">类型</th>
                    <th className="px-4 py-3 text-left hidden md:table-cell">页数</th>
                    <th className="px-4 py-3 text-left">状态</th>
                    <th className="px-4 py-3 text-left hidden lg:table-cell">上传时间</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {data?.list.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-12 text-center text-gray-400">暂无课件</td>
                    </tr>
                  )}
                  {data?.list.map((m) => (
                    <MaterialRow key={m.id} material={m} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

function MaterialRow({ material }: { material: MaterialListItemVO }) {
  const status = STATUS_LABEL[material.status] ?? { text: '未知', cls: 'text-gray-500 bg-gray-50' }
  return (
    <tr className="hover:bg-gray-50">
      <td className="px-4 py-3 font-medium text-gray-900">{material.title}</td>
      <td className="px-4 py-3 text-gray-500 hidden md:table-cell">
        {FILE_TYPE_MAP[material.fileType] ?? material.fileType}
      </td>
      <td className="px-4 py-3 text-gray-500 hidden md:table-cell">
        {material.pageCount > 0 ? `${material.pageCount} 页` : '—'}
      </td>
      <td className="px-4 py-3">
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${status.cls}`}>
          {status.text}
        </span>
      </td>
      <td className="px-4 py-3 text-gray-500 hidden lg:table-cell">
        {material.createdAt.slice(0, 10)}
      </td>
    </tr>
  )
}

function UploadProgress({ state }: { state: string }) {
  if (state === 'idle' || state === 'done') return null
  const steps = [
    { key: 'pending', label: '申请上传凭证' },
    { key: 'uploading', label: '上传文件至存储' },
    { key: 'completing', label: '通知服务端完成' },
  ]
  return (
    <div className="flex items-center gap-3">
      {steps.map((s, i) => {
        const idx = steps.findIndex((x) => x.key === state)
        const done = i < idx
        const active = s.key === state
        return (
          <React.Fragment key={s.key}>
            <div className="flex items-center gap-1.5">
              <div className={`h-2 w-2 rounded-full ${done ? 'bg-green-500' : active ? 'bg-blue-500 animate-pulse' : 'bg-gray-300'}`} />
              <span className={`text-xs ${active ? 'text-blue-600' : done ? 'text-green-600' : 'text-gray-400'}`}>
                {s.label}
              </span>
            </div>
            {i < steps.length - 1 && <span className="text-gray-300">→</span>}
          </React.Fragment>
        )
      })}
    </div>
  )
}

const UPLOAD_STATE_LABEL: Record<string, string> = {
  pending: '申请中…',
  uploading: '上传中…',
  completing: '完成中…',
  done: '上传成功 ✓',
}
