import React, { useCallback } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import { http } from '@edu/api'

interface Props {
  value: string
  onChange: (html: string) => void
  placeholder?: string
  disabled?: boolean
}

export function RichTextEditor({ value, onChange, placeholder = '请输入题干内容...', disabled = false }: Props) {
  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({ inline: false, allowBase64: false }),
      Link.configure({ openOnClick: false }),
      Placeholder.configure({ placeholder }),
    ],
    content: value,
    editable: !disabled,
    onUpdate: ({ editor: ed }) => {
      onChange(ed.getHTML())
    },
  })

  const handleImageUpload = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0]
      if (!file || !editor) return
      const formData = new FormData()
      formData.append('file', file)
      try {
        const res = await http.post<{ data: { url: string } }>('/v1/file/upload/image', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        const url = res.data?.url
        if (url) {
          editor.chain().focus().setImage({ src: url, alt: file.name }).run()
        }
      } catch {
        // upload error handled silently — UI stays intact
      } finally {
        e.target.value = ''
      }
    },
    [editor]
  )

  if (!editor) return null

  const btnClass = (active: boolean) =>
    `rounded px-2 py-1 text-xs transition-colors ${
      active
        ? 'bg-blue-500 text-white'
        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
    }`

  return (
    <div className={`rounded-lg border ${disabled ? 'border-gray-200 bg-gray-50' : 'border-gray-300 bg-white focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500'}`}>
      {/* 工具栏 */}
      {!disabled && (
        <div className="flex flex-wrap items-center gap-1 border-b px-2 py-1.5">
          <button type="button" onClick={() => editor.chain().focus().toggleBold().run()} className={btnClass(editor.isActive('bold'))} title="粗体">
            <strong>B</strong>
          </button>
          <button type="button" onClick={() => editor.chain().focus().toggleItalic().run()} className={btnClass(editor.isActive('italic'))} title="斜体">
            <em>I</em>
          </button>
          <button type="button" onClick={() => editor.chain().focus().toggleStrike().run()} className={btnClass(editor.isActive('strike'))} title="删除线">
            <span className="line-through">S</span>
          </button>
          <div className="h-4 w-px bg-gray-300" />
          <button type="button" onClick={() => editor.chain().focus().toggleBulletList().run()} className={btnClass(editor.isActive('bulletList'))} title="无序列表">
            ≡
          </button>
          <button type="button" onClick={() => editor.chain().focus().toggleOrderedList().run()} className={btnClass(editor.isActive('orderedList'))} title="有序列表">
            1.
          </button>
          <div className="h-4 w-px bg-gray-300" />
          <button
            type="button"
            onClick={() => {
              const url = window.prompt('输入链接 URL')
              if (url) editor.chain().focus().setLink({ href: url }).run()
            }}
            className={btnClass(editor.isActive('link'))}
            title="插入链接"
          >
            🔗
          </button>
          <label className="cursor-pointer rounded px-2 py-1 text-xs bg-gray-100 text-gray-700 hover:bg-gray-200 transition-colors" title="插入图片">
            🖼
            <input type="file" accept="image/*" className="hidden" onChange={handleImageUpload} />
          </label>
          <div className="h-4 w-px bg-gray-300" />
          <button type="button" onClick={() => editor.chain().focus().undo().run()} disabled={!editor.can().undo()} className="rounded px-2 py-1 text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30" title="撤销">
            ↩
          </button>
          <button type="button" onClick={() => editor.chain().focus().redo().run()} disabled={!editor.can().redo()} className="rounded px-2 py-1 text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30" title="重做">
            ↪
          </button>
        </div>
      )}

      {/* 编辑区 */}
      <EditorContent
        editor={editor}
        className="prose prose-sm max-w-none px-3 py-2 text-sm text-gray-800 focus:outline-none min-h-[80px]"
      />
    </div>
  )
}

// ─── 纯展示组件（学生端/预览），不加载编辑器 ─────────────────────────────

export function RichTextView({ html }: { html: string }) {
  return (
    <div
      className="prose prose-sm max-w-none text-sm text-gray-800 leading-relaxed"
      dangerouslySetInnerHTML={{ __html: html }}
    />
  )
}
