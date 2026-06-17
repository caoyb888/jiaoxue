# Sprint 3 开发进度

## 子任务列表

| 子任务 | Story | 内容 | 状态 |
|--------|-------|------|------|
| S3-01 | 签到码生成（QR+口令，Redis TTL 5min） | edu-interaction：AttendanceCode实体/Service/Controller | ✅ 完成 |
| S3-02 | 【C1核心】签到高并发削峰 | BloomFilter去重→Redis队列→批量落库（500ms/50条） | ✅ 完成 |
| S3-03 | 签到查询与手动修改 | 教师端签到列表、手动补签/修改 | ✅ 完成 |
| S3-04 | edu-notify WebSocket配置 | STOMP+SockJS，JWT握手拦截器，Topic注册 | ✅ 完成 |
| S3-05 | 课件翻页WebSocket同步 | /app/lesson/{id}/nextSlide → /topic/lesson/{id}/slide | ✅ 完成 |
| S3-06 | 弹幕：REST+WebSocket广播 | 后台实名/前台匿名，教师屏蔽，Kafka广播 | ✅ 完成 |
| S3-07 | 随机点名 | Fisher-Yates取样，支持排除缺勤，3种样式广播 | ✅ 完成 |
| S3-08 | 课件反馈 | POST slide-feedback，热点页面统计API | ✅ 完成 |
| S3-09 | 课堂积分 | POST /score，汇总至class_score表 | ✅ 完成 |
| S3-10 | 课堂报告框架 | lesson_report生成框架，genStatus状态机 | ✅ 完成 |
| S3-11 | 前端：教师签到二维码展示 | AttendancePage（倒计时+WebSocket实时人数+随机点名） | ✅ 完成 |
| S3-12 | 前端：学生签到页（Web+小程序） | StudentAttendPage + Taro weapp attend/index.tsx | ✅ 完成 |
| S3-13 | 前端：弹幕展示+发送区 | BarragePage（canvas roll/top/bottom，三端适配） | ✅ 完成 |
| S3-14 | 前端：随机点名动效 | RollCallPage（3种样式：random/spotlight/racing） | ✅ 完成 |
| S3-15 | 【C8】CI lint规则上线 | no-dom-in-weapp ESLint规则+.gitlab-ci.yml CI配置 | ✅ 完成 |
| S3-16 | 签到压测脚本 | JMeter .jmx 3000并发+README验收说明 | ✅ 完成 |

## 编译与测试状态

- edu-interaction: BUILD SUCCESS
- edu-notify: BUILD SUCCESS
- 单元测试: 4/4 通过（AttendanceServiceTest）

## 关键架构约束落地

### C1 - 签到禁止直连MySQL（✅ 完成）
- 签到请求 → Redis BloomFilter去重（Redisson RBloomFilter，误判率0.1%）
- → Redis List队列（`attend:queue:{lessonId}`）入队
- → `AttendanceFlushScheduler` 每500ms批量落库（最多50条/批）
- 计数器：Redis `attend:count:{lessonId}` 原子自增
- 布隆过滤器TTL=24h，课堂结束后自动清理

### C8 - Taro小程序DOM API隔离（✅ 完成）
- ESLint规则 `@edu/eslint-rules/no-dom-in-weapp` 实现
- weapp/attend/index.tsx: 用 `Taro.scanCode` 替代 `getUserMedia`
- .gitlab-ci.yml: PR触发lint检查，违规红灯拦截

## 新增文件清单

### edu-interaction 后端
- `domain/entity/AttendanceCode.java`
- `domain/entity/Attendance.java`
- `domain/entity/Barrage.java`
- `domain/entity/RandomCall.java`
- `domain/entity/SlideFeedback.java`
- `domain/entity/ClassScore.java`
- `domain/entity/LessonReport.java`
- `domain/dto/AttendDTO.java` / `AttendModifyDTO.java` / `BarrageDTO.java`
- `domain/dto/RollCallDTO.java` / `SlideFeedbackDTO.java` / `ClassScoreDTO.java`
- `domain/vo/AttendCodeVO.java` / `AttendResultVO.java` / `AttendanceItemVO.java`
- `domain/vo/AttendanceListVO.java` / `RollCallVO.java`
- `event/AttendQueueItem.java`
- `repository/AttendanceCodeMapper.java` / `AttendanceMapper.java`
- `repository/BarrageMapper.java` / `RandomCallMapper.java`
- `repository/SlideFeedbackMapper.java` / `ClassScoreMapper.java`
- `repository/LessonReportMapper.java`
- `service/AttendanceCodeService.java` + `impl/`
- `service/AttendanceService.java` + `impl/`
- `service/AttendanceQueryService.java` + `impl/`
- `service/AttendanceFlushScheduler.java`
- `service/InteractionService.java` + `impl/`
- `service/LessonReportService.java` + `impl/`
- `controller/AttendanceController.java`
- `controller/InteractionController.java`
- `config/MybatisPlusFillHandler.java`
- `test/AttendanceServiceTest.java`（4个单测）

### edu-notify 后端
- `config/WebSocketConfig.java`（STOMP+SockJS+WebSocketMessageBroker）
- `handler/JwtHandshakeInterceptor.java`（JWT握手鉴权）
- `controller/LessonMessageController.java`（@MessageMapping翻页/人数）
- `service/LessonBroadcastService.java` + `impl/`

### 前端
- `packages/api/src/modules/interaction.ts`（完整类型+API+Hooks）
- `apps/web/src/pages/interaction/AttendancePage.tsx`（教师签到管理）
- `apps/web/src/pages/interaction/StudentAttendPage.tsx`（学生签到）
- `apps/web/src/pages/interaction/BarragePage.tsx`（弹幕canvas）
- `apps/web/src/pages/interaction/RollCallPage.tsx`（点名动效）
- `apps/weapp/src/pages/attend/index.tsx`（小程序学生签到，C8合规）
- `packages/eslint-rules/src/rules/no-dom-in-weapp.js`

### CI/基础设施
- `.gitlab-ci.yml`（含C8 lint检查、后端单测、构建、部署）
- `docs/perf-test/S3-16-attend-stress-test.jmx`
- `docs/perf-test/S3-16-README.md`
