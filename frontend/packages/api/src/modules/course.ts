import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { http } from '../client'

// ─── Types ───────────────────────────────────────────────────────────────────

export interface CourseListItemVO {
  id: number
  courseCode: string
  courseName: string
  credit: number
  courseType: string
  semester: string
  deptName: string
  classCount: number
  createdAt: string
}

export interface ClassRoomVO {
  id: number
  classCode: string
  className: string
  teacherId: number
  teacherName: string
  courseId: number
  courseName: string
  semester: string
  studentCount: number
  status: number
}

export interface LessonDetailVO {
  id: number
  classId: number
  title: string
  chapter: string
  status: number
  liveMode: string
  startTime: string | null
  endTime: string | null
  durationMin: number
  currentSlide: number
  materialId: number | null
  material: MaterialVO | null
}

export interface MaterialVO {
  id: number
  title: string
  fileType: string
  pageCount: number
  slideDir: string
  status: number
}

export interface LessonStartDTO {
  classId: number
  materialId?: number
  title?: string
  chapter?: string
  liveMode?: string
}

export interface LessonStartVO {
  lessonId: number
  status: number
  liveMode: string
  wsEndpoint: string
  wsTopicPrefix: string
}

export interface LessonEndVO {
  lessonId: number
  status: number
  durationMin: number
  aiTaskTriggered: boolean
  message: string
}

export interface MaterialUploadVO {
  uploadId: string
  presignedUrl: string
  expiresIn: number
  objectPath: string
}

export interface MaterialCompleteVO {
  materialId: number
  status: number
  message: string
}

export interface MaterialListItemVO {
  id: number
  title: string
  fileType: string
  pageCount: number
  status: number
  fileSizeKb: number
  createdAt: string
}

export interface CourseQueryParams {
  semester?: string
  deptId?: number
  keyword?: string
  page?: number
  size?: number
}

export interface ApiPageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  pages: number
}

// ─── API functions ────────────────────────────────────────────────────────────

export const courseApi = {
  list: (params: CourseQueryParams) =>
    http.get<CourseQueryParams, ApiPageResult<CourseListItemVO>>('/v1/course/list', { params }),

  myClasses: (params?: { semester?: string; status?: number }) =>
    http.get<typeof params, ClassRoomVO[]>('/v1/course/class/my', { params }),

  startLesson: (dto: LessonStartDTO) =>
    http.post<LessonStartDTO, LessonStartVO>('/v1/course/lesson/start', dto),

  endLesson: (lessonId: number) =>
    http.post<void, LessonEndVO>(`/v1/course/lesson/${lessonId}/end`),

  getLessonDetail: (lessonId: number) =>
    http.get<void, LessonDetailVO>(`/v1/course/lesson/${lessonId}`),

  // 课堂列表（按班级/状态过滤）。status: 0-未开始 1-进行中 2-已结束
  listLessons: (params: { classId: number; status?: number; page?: number; size?: number }) =>
    http.get<typeof params, ApiPageResult<LessonDetailVO>>('/v1/course/lesson/list', { params }),

  updateSlide: (lessonId: number, slideNo: number) =>
    http.post<{ slideNo: number }, void>(`/v1/course/lesson/${lessonId}/slide`, { slideNo }),
}

export const materialApi = {
  applyUpload: (dto: { fileName: string; fileType: string; fileSizeKb: number }) =>
    http.post<typeof dto, MaterialUploadVO>('/v1/course/material/upload', dto),

  completeUpload: (dto: { uploadId: string; title: string }) =>
    http.post<typeof dto, MaterialCompleteVO>('/v1/course/material/upload/complete', dto),

  list: (params?: { keyword?: string; page?: number; size?: number }) =>
    http.get<typeof params, ApiPageResult<MaterialListItemVO>>('/v1/course/material/list', { params }),
}

// ─── React Query hooks ────────────────────────────────────────────────────────

export function useCourseList(params: CourseQueryParams) {
  return useQuery({
    queryKey: ['courses', params],
    queryFn: () => courseApi.list(params),
    staleTime: 30_000,
  })
}

export function useMyClasses(params?: { semester?: string; status?: number }) {
  return useQuery({
    queryKey: ['myClasses', params],
    queryFn: () => courseApi.myClasses(params),
    staleTime: 60_000,
  })
}

export function useLessonDetail(lessonId: number | null) {
  return useQuery({
    queryKey: ['lesson', lessonId],
    queryFn: () => courseApi.getLessonDetail(lessonId!),
    enabled: lessonId !== null,
    staleTime: 5_000,
  })
}

/** 取某班级当前进行中的课堂（status=1），无则返回 null。供学生进入签到/答题。 */
export function useActiveLesson(classId: number | null) {
  return useQuery({
    queryKey: ['activeLesson', classId],
    queryFn: () => courseApi.listLessons({ classId: classId!, status: 1, size: 1 }),
    enabled: classId !== null,
    select: (page): LessonDetailVO | null => page.list[0] ?? null,
    staleTime: 10_000,
  })
}

export function useStartLesson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (dto: LessonStartDTO) => courseApi.startLesson(dto),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['myClasses'] }),
  })
}

export function useEndLesson() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (lessonId: number) => courseApi.endLesson(lessonId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['myClasses'] }),
  })
}

export function useMaterialList(params?: { keyword?: string; page?: number }) {
  return useQuery({
    queryKey: ['materials', params],
    queryFn: () => materialApi.list(params),
    staleTime: 30_000,
  })
}
