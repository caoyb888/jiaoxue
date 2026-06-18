# Sprint 5 执行进度

> 分支: feature/sprint5  
> 开始时间: 2026-06-18

## 任务状态

| ID | 描述 | 状态 | 完成时间 | commit |
|----|------|------|---------|--------|
| S5-01 | exam_publish 发布配置接口（监考/人脸核验/乱序配置）+ ExamMonitor/ExamSubmitQueue 实体 + 学生端列表 API + XXL-Job状态同步 | ✅ 完成 | 2026-06-18 | 8f1c66c |
| S5-02 | POST /exam/{id}/enter 进入考试（密码验证/人脸核验/分批下题） | ✅ 完成 | 2026-06-18 | a9d4f65 |
| S5-03 | 【C2核心】交卷容灾（Redis幂等+Kafka+XXL-Job落库） | ✅ 完成 | 2026-06-18 | 38e03e4 |
| S5-04 | 【C6合规】人脸核验（百度AI，只存比对结果） | 🔲 待开始 | - | - |
| S5-05 | 监考状态维护（心跳+切屏告警+WebSocket推送） | 🔲 待开始 | - | - |
| S5-06 | 主观题图片附件支持（MinIO路径JSON） | 🔲 待开始 | - | - |
| S5-07 | 教师阅卷接口（review_status状态流转） | 🔲 待开始 | - | - |
| S5-08 | 监考状态大屏接口（session_status实时分布） | 🔲 待开始 | - | - |
| S5-09 | 【C2前端】IndexedDB 15秒草稿自动保存（useExamAutoSave hook） | 🔲 待开始 | - | - |
| S5-10 | 【C2前端】交卷打散（学号末两位取模延迟0~30s） | 🔲 待开始 | - | - |
| S5-11 | 考试进入页+答题页全屏水印 | 🔲 待开始 | - | - |
| S5-12 | 切屏监听+复制拦截+截屏事件上报 | 🔲 待开始 | - | - |
| S5-13 | 教师阅卷页+监考大屏学生状态分布 | 🔲 待开始 | - | - |
| S5-14 | Taro小程序答题页（客观题+小程序锁屏） | 🔲 待开始 | - | - |
| S5-15 | JMeter高并发压测（1000学生30s集中交卷） | 🔲 待开始 | - | - |

## S5-01 变更内容

### 新增文件
- `backend/edu-exam/.../domain/entity/ExamMonitor.java` — 监考状态实体
- `backend/edu-exam/.../domain/entity/ExamSubmitQueue.java` — 交卷队列实体（C2基础）
- `backend/edu-exam/.../repository/ExamMonitorMapper.java` — 含 heartbeat/countByStatus 查询
- `backend/edu-exam/.../repository/ExamSubmitQueueMapper.java` — 含 selectPending 查询
- `backend/edu-exam/.../domain/vo/StudentExamListVO.java` — 学生端考试列表视图
- `backend/edu-exam/.../job/ExamStatusSyncJob.java` — XXL-Job 每分钟同步 exam_publish.status

### 修改文件
- `ExamPublishService.java` — 新增 listForStudent / syncExamStatus 方法
- `ExamPublishServiceImpl.java` — 实现上述两方法（含 N+1 优化）
- `ExamPublishController.java` — 新增 GET /api/v1/exam/publishes/student/list
- `ExamPublishMapper.java` — 新增 selectActiveOrPending 查询
- `ExamPublishServiceTest.java` — 新增 8 条测试（总计 22 条全绿）
