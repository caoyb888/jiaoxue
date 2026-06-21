import { useEffect, useRef, useCallback } from 'react'
import { isWeb } from '@edu/utils'
import type { AnswerItemDTO } from '@edu/api'

// CLAUDE.md §6.6: 15s 自动保存，不得修改此间隔
const SAVE_INTERVAL_MS = 15_000
const DB_NAME = 'edu_exam_drafts'
const STORE_NAME = 'drafts'

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1)
    req.onupgradeneeded = () => {
      req.result.createObjectStore(STORE_NAME)
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error)
  })
}

function dbKey(publishId: number, userId: number) {
  return `exam_draft_${publishId}_${userId}`
}

export async function saveExamDraft(publishId: number, userId: number, answers: AnswerItemDTO[]) {
  if (!isWeb) return
  const db = await openDb()
  return new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).put({ answers, savedAt: new Date().toISOString() }, dbKey(publishId, userId))
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

export async function loadExamDraft(publishId: number, userId: number): Promise<AnswerItemDTO[] | null> {
  if (!isWeb) return null
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const req = tx.objectStore(STORE_NAME).get(dbKey(publishId, userId))
    req.onsuccess = () => resolve(req.result?.answers ?? null)
    req.onerror = () => reject(req.error)
  })
}

export async function clearExamDraft(publishId: number, userId: number) {
  if (!isWeb) return
  const db = await openDb()
  return new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    tx.objectStore(STORE_NAME).delete(dbKey(publishId, userId))
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

/**
 * 每15秒自动将当前答案保存到 IndexedDB（断网容灾，C2约束）。
 * 提交成功后必须调用 clearDraft()，否则下次进入会恢复旧草稿。
 *
 * @param publishId 考试发布 ID
 * @param userId 当前学生 ID
 * @param getAnswers 返回最新答案的函数（避免 stale closure）
 * @param enabled 是否启用自动保存（交卷后应关闭）
 */
export function useExamAutoSave(
  publishId: number,
  userId: number,
  getAnswers: () => AnswerItemDTO[],
  enabled: boolean,
) {
  const getAnswersRef = useRef(getAnswers)
  getAnswersRef.current = getAnswers

  const clearDraft = useCallback(() => clearExamDraft(publishId, userId), [publishId, userId])

  useEffect(() => {
    if (!enabled || !isWeb) return

    const timer = setInterval(async () => {
      try {
        const answers = getAnswersRef.current()
        await saveExamDraft(publishId, userId, answers)
      } catch (err) {
        console.warn('自动保存草稿失败:', err)
      }
    }, SAVE_INTERVAL_MS)

    return () => clearInterval(timer)
  }, [publishId, userId, enabled])

  return { clearDraft }
}
