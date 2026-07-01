import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── Types ───────────────────────────────────────────────────────────────────

export interface StudentGradeVO {
  id: number | null
  classId: number
  studentId: number
  attendScore: number | null
  quizScore: number | null
  interactionScore: number | null
  examScore: number | null
  totalScore: number | null
  offlineScore: number | null
  /** 0-未计算 1-已计算 */
  calcStatus: number | null
  updatedAt: string | null
}

export interface GradeRuleVO {
  id: number
  classId: number
  ruleName: string
  gradeType: number
  gradeTypeName: string
  weight: number
  description: string | null
}

export interface GradeRuleListVO {
  classId: number
  rules: GradeRuleVO[]
  /** 权重合计（满分应为 100） */
  totalWeight: number
  /** 权重是否配置完成（=100） */
  weightComplete: boolean
}

export interface GradeRuleCreateDTO {
  classId: number
  ruleName: string
  gradeType: number
  weight: number
  description?: string
}

export interface OfflineImportResultVO {
  total: number
  successCount: number
  failCount: number
  errors: string[]
}

/** grade_type → 名称（1-期末 2-平时 3-实验 4-项目 5-出勤 6-其他）。 */
export const GRADE_TYPES: { value: number; label: string }[] = [
  { value: 1, label: '期末考试' },
  { value: 2, label: '平时作业' },
  { value: 3, label: '实验报告' },
  { value: 4, label: '项目实践' },
  { value: 5, label: '出勤' },
  { value: 6, label: '其他' },
]

// ─── API functions ────────────────────────────────────────────────────────────

export const gradeApi = {
  listClassGrades: (classId: number) =>
    http.get<void, StudentGradeVO[]>(`/v1/grade/class/${classId}`),

  listRules: (classId: number) =>
    http.get<void, GradeRuleListVO>(`/v1/grade/rules/class/${classId}`),

  createRule: (dto: GradeRuleCreateDTO) =>
    http.post<GradeRuleCreateDTO, GradeRuleVO>('/v1/grade/rules', dto),

  deleteRule: (ruleId: number) =>
    http.delete<void, void>(`/v1/grade/rules/${ruleId}`),

  /** 导出成绩回传 xlsx（二进制，绕过 Result 解包）。 */
  exportGrades: (classId: number, format: 'zhengfang' | 'qiangzhi') =>
    http.get<void, Blob>(`/v1/grade/export/${classId}`, {
      params: { format },
      responseType: 'blob',
    }),

  importOffline: (classId: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<FormData, OfflineImportResultVO>(`/v1/grade/import/offline/${classId}`, form)
  },
}

/** 触发浏览器下载一个 Blob。 */
export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// ─── React Query hooks ────────────────────────────────────────────────────────

export function useClassGrades(classId: number | null) {
  return useQuery({
    queryKey: ['classGrades', classId],
    queryFn: () => gradeApi.listClassGrades(classId!),
    enabled: classId !== null,
    staleTime: 15_000,
  })
}

export function useGradeRules(classId: number | null) {
  return useQuery({
    queryKey: ['gradeRules', classId],
    queryFn: () => gradeApi.listRules(classId!),
    enabled: classId !== null,
    staleTime: 15_000,
  })
}

export function useCreateGradeRule(classId: number | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: GradeRuleCreateDTO) => gradeApi.createRule(dto),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['gradeRules', classId] }),
  })
}

export function useDeleteGradeRule(classId: number | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (ruleId: number) => gradeApi.deleteRule(ruleId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['gradeRules', classId] }),
  })
}

export function useImportOfflineGrades(classId: number | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => gradeApi.importOffline(classId!, file),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['classGrades', classId] }),
  })
}
