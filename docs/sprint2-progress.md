# Sprint 2 开发进度

## 子任务列表

| 子任务 | Story | 内容 | 状态 |
|--------|-------|------|------|
| T1 | S2-01/02/03/06 | edu-course：课程/班级/学生管理/排课 | ✅ 完成 |
| T2 | S2-05/09/10/15 | 课堂 lesson 开始结束 + MinIO/ES/jwxt 基础设施 | ✅ 完成 |
| T3 | S2-04 | 课件上传三步流程 + LibreOffice 异步转图片 | ✅ 完成 |
| T4 | S2-07/08 | edu-ai 骨架 + PromptSecurityFilter（C4约束） | ✅ 完成 |
| T5 | S2-11/12/13/14 | 前端：课程列表/课堂页/Taro小程序/课件管理 | ✅ 完成 |

## T1 完成内容（S2-01/02/03/06）

### 新增文件（edu-course）

**实体层 (7个)**
- `domain/entity/Course.java` — 课程表
- `domain/entity/ClassRoom.java` — 教学班表
- `domain/entity/ClassStudent.java` — 班级学生关联
- `domain/entity/ClassGroup.java` — 分组表
- `domain/entity/ClassGroupStudent.java` — 分组成员
- `domain/entity/Lesson.java` — 课堂记录
- `domain/entity/LessonSchedule.java` — 排课表

**数据访问层 (5个 Mapper + 4个 XML)**
- `repository/CourseMapper.java` + `mapper/CourseMapper.xml`
- `repository/ClassRoomMapper.java` + `mapper/ClassRoomMapper.xml`
- `repository/ClassStudentMapper.java` + `mapper/ClassStudentMapper.xml`
- `repository/LessonMapper.java` + `mapper/LessonMapper.xml`
- `repository/LessonScheduleMapper.java`

**业务层 (3接口 + 3实现)**
- `service/CourseService.java` + `impl/CourseServiceImpl.java`
- `service/ClassRoomService.java` + `impl/ClassRoomServiceImpl.java`
- `service/LessonService.java` + `impl/LessonServiceImpl.java`

**接口层 (3个 Controller)**
- `controller/CourseController.java` — GET /api/v1/course/list, POST /api/v1/course
- `controller/ClassRoomController.java` — GET/POST /api/v1/course/class, /my, /{classId}/students
- `controller/LessonController.java` — GET /api/v1/course/lesson/list, /{lessonId}, /slide, /schedule

**DTO/VO (6个 DTO + 5个 VO)**
- CourseCreateDTO, CourseQueryDTO, ClassRoomCreateDTO, StudentBatchDTO, LessonQueryDTO, LessonScheduleCreateDTO
- CourseListItemVO, ClassRoomVO, ClassRoomDetailVO, LessonDetailVO, LessonScheduleVO

**配置 + 测试**
- `config/MybatisPlusFillHandler.java`
- `CourseApplication.java` — 添加 @MapperScan, @ComponentScan
- `application.yml` — 数据库/Redis/MyBatisPlus/Kafka 配置（已修复重复 spring 键问题）
- `service/ClassRoomServiceTest.java` — 4个单测，全部通过

### 额外修复（edu-common bug fix）
- `edu-common/pom.xml` — 添加 spring-kafka optional 依赖（OperationLogAspect 编译依赖）
- `GlobalExceptionHandler.java` — 移除 Spring Security AccessDeniedException（未引入 Spring Security）
- `edu-ai/pom.xml` — 修复 Spring AI 1.0.0 starter 包名：`spring-ai-openai-spring-boot-starter` → `spring-ai-starter-model-openai`
- `edu-common/result/PageResult.java` — 新增分页结果封装类

### 验收标准对照
- [x] `GET /api/v1/course/list` — 支持 semester/deptId/keyword/page/size 过滤
- [x] `POST /api/v1/course` — 创建课程，@OperationLog 记录
- [x] `GET /api/v1/course/class/my` — 教师/学生双视角
- [x] `POST /api/v1/course/class/{classId}/students` — 批量添加，去重处理
- [x] `DELETE /api/v1/course/class/{classId}/students/{studentId}` — 移除学生
- [x] `GET /api/v1/course/lesson/list` / `/{lessonId}` — 课堂列表/详情（含课件信息）
- [x] `POST /api/v1/course/lesson/{lessonId}/slide` — currentSlide 更新
- [x] `POST /api/v1/course/lesson/schedule` — 排课创建
- [x] 单测 4 个，BUILD SUCCESS

---

## T2 完成内容（S2-05/09/10/15）

### S2-05：课堂开始/结束接口

**新增 DTO/VO**
- `edu-course/domain/dto/LessonStartDTO.java` — classId + materialId + title + chapter + liveMode(默认SLIDE_ONLY)
- `edu-course/domain/vo/LessonStartVO.java` — lessonId + status + liveMode + wsEndpoint + wsTopicPrefix
- `edu-course/domain/vo/LessonEndVO.java` — lessonId + status + durationMin + aiTaskTriggered + message

**LessonServiceImpl 新增**
- `startLesson(Long teacherId, LessonStartDTO dto)` — 检查教学班归属 → 创建 Lesson（SLIDE_ONLY 时清空 livePushUrl/livePlayUrl，C5约束）
- `endLesson(Long lessonId, Long teacherId)` — 更新状态 → 计算时长 → Kafka 发布 `edu.ai.tasks`（edu-common AiTaskEvent）

**LessonController 新增**
- `POST /api/v1/course/lesson/start` + `POST /api/v1/course/lesson/{lessonId}/end`（均加 @OperationLog）

**edu-common 新增**
- `event/AiTaskEvent.java` — taskId/lessonId/teacherId/classId/taskType/triggerTime

### S2-09：MinIO 基础设施（edu-file）

- `config/MinioProperties.java` — @ConfigurationProperties(prefix="minio")，5个 bucket 列表 + presignExpireMinutes
- `config/MinioConfig.java` — MinioClient @Bean
- `util/MinioUtil.java` — generatePresignedPutUrl / generatePresignedGetUrl / createBucket / deleteObject / objectExists
- `init/BucketInitializer.java` — ApplicationRunner，启动时建 bucket + 设置生命周期（edu-exam-attach 90天过期，edu-live-replay 180天过期）
- `application.yml` — 补充 datasource/redis/minio 配置

### S2-10：Elasticsearch 索引初始化

- `docs/es/create_indices.sh` — 一键初始化脚本，执行方式：`bash docs/es/create_indices.sh [ES_HOST]`
- `docs/es/edu_question_mapping.json` — 题库索引：content/options/analysis 字段用 ik_max_word，questionType/tags/difficulty 用 keyword
- `docs/es/edu_courseware_mapping.json` — 课件索引：title/description/extractedText 用 ik_max_word，支持全文检索

### S2-15：edu-jwxt 基础设施

- `domain/entity/JwxtIdMapping.java` — @TableName("jwxt_id_mapping")，双向唯一索引
- `domain/entity/JwxtSyncLog.java` — @TableName("jwxt_sync_log")，ETL 执行记录
- `repository/JwxtIdMappingMapper.java` — selectLocalIdByJwxtId / selectJwxtIdByLocalId / batchUpsert
- `repository/JwxtSyncLogMapper.java` — 基础 CRUD
- `resources/mapper/JwxtIdMappingMapper.xml` — selectLocalIdByJwxtId / selectJwxtIdByLocalId / batchUpsert（INSERT ON DUPLICATE KEY UPDATE）
- `config/MybatisPlusFillHandler.java` — 自动填充 createdAt/updatedAt
- `JwxtApplication.java` — 添加 @MapperScan + @ComponentScan
- `application.yml` — 补充完整配置（datasource/redis/mybatis-plus）

### 编译结果
- edu-course: `BUILD SUCCESS`（含 edu-common 依赖）
- edu-file: `BUILD SUCCESS`
- edu-jwxt: `BUILD SUCCESS`
- 单测: 4/4 通过（edu-course ClassRoomServiceTest）

### 关键约束落地
- [x] C3：endLesson 发 Kafka edu.ai.tasks，Kafka 失败只 warn 不影响主流程
- [x] C5：SLIDE_ONLY 模式强制清空 livePushUrl/livePlayUrl
- [x] MinIO 预签名 URL 流程（3步：presign → 前端直传 → complete），绕过后端带宽瓶颈
- [x] jwxt_id_mapping batchUpsert 用 ON DUPLICATE KEY UPDATE，幂等写入
- [x] ES 索引中文分词：写入 ik_max_word，检索 ik_smart

---

## T3 完成内容（S2-04）

### 课件上传三步流程（edu-course）

**新增实体/Mapper**
- `domain/entity/CourseMaterial.java` — @TableName("course_material"), fileType/status/slideDir/pageCount/@TableLogic
- `repository/CourseMaterialMapper.java` — selectMaterialPage, updateConvertResult
- `resources/mapper/CourseMaterialMapper.xml` — 分页查询 + 转换结果更新

**DTO/VO (5个)**
- `domain/dto/MaterialUploadDTO.java` — fileName, fileType @Pattern(regexp="pptx|pdf|docx|mp4"), fileSizeKb
- `domain/dto/MaterialCompleteDTO.java` — uploadId, title
- `domain/vo/MaterialUploadVO.java` — presignedUrl + uploadId + objectPath
- `domain/vo/MaterialCompleteVO.java` — materialId + title + status + message
- `domain/vo/MaterialListItemVO.java` — 列表展示用，含转换状态

**业务层**
- `service/MaterialService.java` + `impl/MaterialServiceImpl.java`:
  - Step 1：`applyUpload` — 生成 UUID uploadId，objectPath = "materials/teacher-{id}/{date}/{uploadId}.{ext}"，Redis ticket（TTL 60min），返回预签名 PUT URL
  - Step 2：前端直接 PUT 到 MinIO（绕过后端带宽）
  - Step 3：`completeUpload` — 读 Redis ticket 校验 teacherId → 插入 CourseMaterial → 非 mp4 则发 Kafka edu.material.convert → 清除 ticket

**Kafka 消费者（LibreOffice 转图片）**
- `service/MaterialConvertConsumer.java` — @KafkaListener(concurrency="2"), 从 MinIO 下载文件，`soffice --headless --convert-to png:impress_png_Export --outdir tmpDir file`（5min 超时），PNG 上传至 `slides/{materialId}/slide_0001.png` 等，try/finally 清理临时目录

**接口层**
- `controller/MaterialController.java` — POST /upload, POST /upload/complete(@OperationLog), GET /list

**edu-common 新增**
- `constant/KafkaTopic.java` — 追加 `MATERIAL_CONVERT = "edu.material.convert"`

### 关键约束落地
- [x] 禁止后端转发文件，预签名 URL 让前端直传 MinIO，节省后端带宽
- [x] LibreOffice 转换完全异步（Kafka consumer），不阻塞上传接口
- [x] Redis upload ticket TTL=60min，防止僵尸上传占用命名空间
- [x] fileType 用正则 @Pattern 校验，防止任意文件类型绕过

---

## T4 完成内容（S2-07/08）

### edu-ai 骨架 + PromptSecurityFilter（C4约束）

**安全层（C4强制约束）**
- `security/PromptSecurityException.java` — code=200702
- `security/PromptSecurityFilter.java`:
  - `SYSTEM_GUARD` 常量注入教育场景前置约束 System Prompt
  - `OVERRIDE_PATTERN` 正则：检测"ignore/forget/override + rule/instruction/above"
  - `INJECTION_PATTERN` 正则：检测"you are now/pretend to be/从现在开始"
  - 敏感词表从 Nacos 配置（`@Value("${ai.security.sensitive-words:}")`）
  - `filter(AiRequest)` + `filterOutput(String)` + `sanitizeOverride()` 三重防护

**多模型路由（Spring AI 1.0.0 builder 模式）**
- `domain/model/ModelType.java` — ANALYSIS/GENERATION/REVIEW
- `domain/model/AiRequest.java` — userPrompt/systemPrompt/modelType/lessonId/userId
- `config/AiModelConfig.java`:
  - `@Bean("analysisChatClient")` / `@Bean("generationChatClient")` / `@Bean("reviewChatClient")`
  - 全部使用 `OpenAiApi.builder().baseUrl().apiKey().build()` + `OpenAiChatModel.builder()` + `ChatClient.builder(model).build()`
  - 关键修复：Spring AI 1.0.0 去掉了 `new OpenAiApi(url,key)` 构造器，必须用 builder

**AI 网关服务**
- `service/AiGatewayService.java` — `chat(AiRequest)` → Flux<String>（SSE流式），`chatSync(AiRequest)` → String；调用前必须过 `securityFilter.filter()`，输出过 `filterOutput()`；`mockMode=true` 时绕过真实 LLM（dev/CI 安全）

**Kafka 消费者（AI 任务处理）**
- `consumer/AiTaskConsumer.java` — @KafkaListener(topics="edu.ai.tasks", concurrency="3")（C3约束：不可随意调大），switch taskType SUMMARY→生成摘要+思维导图，MINDMAP→独立生成；Redis 去重键 TTL=24h；失败时 markFailed + rethrow 触发 Kafka 重试

**Repository 层**
- `domain/entity/LessonReport.java` — genStatus: 0-PENDING/1-GENERATING/2-DONE/3-FAILED
- `repository/LessonReportMapper.java` + `mapper/LessonReportMapper.xml`
- `service/LessonReportService.java` — initReport/saveAiContent/markFailed

**接口层**
- `controller/DialogueController.java` — POST /task（创建会话返回 sessionId），POST /{sessionId}/message（SseEmitter + Virtual Threads + Flux订阅），GET /{sessionId}/history
- `controller/MindmapController.java` — GET /{lessonId}（查 LessonReport genStatus，返回 PENDING/GENERATING/DONE/FAILED + mindmap JSON）
- `config/AiExceptionHandler.java` — @RestControllerAdvice 处理 PromptSecurityException

**测试**
- `test/PromptSecurityFilterTest.java` — 5个单测：正常输入注入SYSTEM_GUARD、敏感词拦截、覆盖指令拦截、System Prompt注入净化、filterOutput屏蔽敏感词

### 关键约束落地
- [x] C4：所有 LLM 调用路径必须经过 PromptSecurityFilter.filter()，不可绕过
- [x] C3：AiTaskConsumer concurrency 固定为 "3"，防止 GPU/API 并发过载
- [x] Spring AI 1.0.0 builder API：通过 `javap` 验证后才写配置，避免编译错误
- [x] mockMode 默认 true，CI 环境不需要真实 API Key

---

## T5 完成内容（S2-11/12/13/14）

### S2-11：Web 课程列表页

- `frontend/packages/api/src/modules/course.ts` — 完整类型定义 + API 函数 + React Query hooks
  - 类型：CourseListItemVO, ClassRoomVO, LessonDetailVO, MaterialVO, LessonStartVO/EndVO, MaterialUploadVO/CompleteVO/ListItemVO, ApiPageResult<T>
  - API：courseApi（list/myClasses/startLesson/endLesson/getLessonDetail/updateSlide），materialApi（applyUpload/completeUpload/list）
  - Hooks：useCourseList/useMyClasses/useLessonDetail/useStartLesson/useEndLesson/useMaterialList
- `frontend/packages/api/src/index.ts` — 更新导出，含所有 course/material 类型和 hooks
- `frontend/apps/web/src/pages/course/CourseListPage.tsx` — keyword/semester 搜索，分页卡片网格，CourseCard 组件（类型徽章/院系/学期/班级数），Link to 班级详情

### S2-12：Web 课堂页（教师视角）

- `frontend/apps/web/src/pages/course/ClassroomPage.tsx`:
  - `useStartLesson`/`useEndLesson` mutation，开始/结束课堂
  - WebSocket 仅在 `isWeb` 判断后才使用 `new WebSocket()`（Taro DOM API 隔离）
  - 翻页：`courseApi.updateSlide`，课件图片 `<img>` 展示
  - 状态：elapsed 计时器（setInterval），课堂状态徽章
- `frontend/apps/web/src/router/index.tsx` — 新增路由：`/courses`, `/course/:classId/classroom`, `/materials`

### S2-13：Taro 小程序（课程列表 + 课堂页）

- `frontend/apps/weapp/package.json` — Taro 3.6.34 依赖声明
- `frontend/apps/weapp/src/app.config.ts` — pages + tabBar + window 导航栏配置
- `frontend/apps/weapp/src/app.tsx` — QueryClientProvider 根组件
- `frontend/apps/weapp/src/app.scss` — 全局页面样式
- `frontend/apps/weapp/src/pages/course/index.tsx` — 课程列表：
  - 纯 Taro 组件（View/Text/Input/ScrollView），无 DOM API
  - useQuery 加载 `courseApi.myClasses()`，本地关键词过滤
  - `Taro.navigateTo` 跳转课堂页（含 classId/className 参数）
  - ClassCard 组件：状态徽章/教师/学期/学生数
- `frontend/apps/weapp/src/pages/classroom/index.tsx` — 课堂页（教师视角）：
  - **`Taro.connectSocket` 替代 `new WebSocket`（小程序 DOM 隔离约束）**
  - `useRouter` 解析 classId/className 参数
  - useQuery 每5秒轮询 `courseApi.getLessonDetail` 获取课件状态
  - 开始/结束课堂按钮，`Taro.showModal` 确认对话框
  - Image 组件展示幻灯片，翻页按钮调用 `courseApi.updateSlide`
  - `useEffect` cleanup 关闭 WebSocket 连接

### S2-14：Web 课件管理页

- `frontend/apps/web/src/pages/course/MaterialManagePage.tsx`:
  - 3步上传：`materialApi.applyUpload` → `fetch(presignedUrl, {method:'PUT'})` (isWeb 保护) → `materialApi.completeUpload`
  - UploadProgress 步骤指示器（可复用组件）
  - 课件列表表格：文件类型/大小/状态（转换中/完成/失败）徽章
  - 状态颜色映射：status 0=灰色(处理中), 1=绿色(可用), 2=红色(失败)

### 关键约束落地
- [x] §6.5：Taro 页面无 DOM API，`new WebSocket` 用 `Taro.connectSocket` 替代
- [x] §6.4：所有服务端状态用 React Query，无 useEffect+fetch 直接调用
- [x] §6.1：函数组件 + 具名导出（pages/ 目录页面组件使用 default export）
- [x] §6.2：TypeScript 严格模式，无 any，可选链代替非空断言
- [x] §6.3：Tailwind CSS 移动优先，无内联 style（Taro 页面因框架限制允许 style 对象）
- [x] §6.6：MaterialManagePage presigned URL 直传，前端 PUT to MinIO

---

## Sprint 2 总结

**完成时间：** 2026-06-17  
**总计新增文件：** ~80个（后端 ~60 + 前端 ~20）  
**编译状态：** edu-course / edu-file / edu-jwxt / edu-ai BUILD SUCCESS  
**测试：** 单测 4+5=9个全部通过

**已落地的架构约束：**
- C3（AI全异步）：lessonEnd → Kafka edu.ai.tasks → AiTaskConsumer 处理，不阻塞主流程
- C4（Prompt安全）：PromptSecurityFilter 三重防护，AI网关强制过滤
- C5（线下禁WebRTC）：SLIDE_ONLY 模式清空 livePushUrl/livePlayUrl
- §8.1签到预备：Redis 队列设计模式已在 edu-course lesson 流程中预留
- §8.6人脸识别：FaceVerifyResult 无 rawPhoto 字段（T4 LessonReport 参照此规范）
