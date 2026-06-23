# CLAUDE.md — 山东管理学院智慧教学系统

> 本文件是 Claude Code 的项目规范文件。AI 编码助手在本项目中的所有操作均须遵循此文档的约定。  
> 对应技术方案版本：V2.0（2026-06-16）

---

## 目录

1. [项目概述](#1-项目概述)
2. [仓库结构](#2-仓库结构)
3. [开发环境快速启动](#3-开发环境快速启动)
4. [技术栈与版本锁定](#4-技术栈与版本锁定)
5. [后端开发规范](#5-后端开发规范)
6. [前端开发规范](#6-前端开发规范)
7. [数据库规范](#7-数据库规范)
8. [关键架构约束（必须遵守）](#8-关键架构约束必须遵守)
9. [安全与合规红线](#9-安全与合规红线)
10. [测试规范](#10-测试规范)
11. [Git 工作流](#11-git-工作流)
12. [CI/CD 流水线](#12-cicd-流水线)
13. [常用命令速查](#13-常用命令速查)

---

## 1. 项目概述

**项目名称：** 山东管理学院智慧教学系统（smart-edu-platform）  
**建设目标：** 互动教学、AI课堂赋能、在线测试、教学数据分析、教务管理一体化数字应用  
**服务对象：** 山东管理学院全体师生及教务管理人员  
**合规要求：** 网络安全等级保护（等保2.0）三级

### 五大功能模块

| 模块 | 服务 | 说明 |
|------|------|------|
| 模块1 互动教学 | `edu-interaction` | 签到、弹幕、随机点名、课堂报告 |
| 模块2 AI辅助教学 | `edu-ai` | 大模型对话、ASR转写、智能批改 |
| 模块3 在线测试 | `edu-exam` | 出卷、监考、阅卷、防泄题 |
| 模块4 数据统计分析 | `edu-stat` | 大屏监控、预警引擎、ClickHouse |
| 模块5 教务管理 | `edu-user` + `edu-jwxt` | 用户管理、教务系统ETL对接 |

---

## 2. 仓库结构

```
smart-edu-platform/
│
├── CLAUDE.md                     ← 本文件
│
├── backend/                      ← 后端（Java 21 / Maven 多模块）
│   ├── edu-gateway/              # 8080（内网机映射 18080）— Spring Cloud Gateway
│   ├── edu-auth/                 # 8081 — JWT + OAuth2 + 微信登录
│   ├── edu-user/                 # 8082 — 用户/角色/院系/通知
│   ├── edu-course/               # 8083 — 课程/班级/课件/排课
│   ├── edu-interaction/          # 8084 — 签到/弹幕/点名/互动
│   ├── edu-exam/                 # 8085 — 试卷/监考/阅卷/批改
│   ├── edu-grade/                # 8086 — 成绩规则/成绩单/导出
│   ├── edu-ai/                   # 8087 — LLM/ASR/AI批改（全异步）
│   ├── edu-stat/                 # 8088（内网机映射 18088）— ClickHouse统计分析
│   ├── edu-file/                 # 8089 — MinIO/CDN/生命周期
│   ├── edu-notify/               # 8090 — 微信推送/WebSocket
│   ├── edu-live/                 # 8091 — WebRTC/RTMP/直播回放
│   ├── edu-admin/                # 8092 — 管理后台BFF聚合层
│   ├── edu-jwxt/                 # 8093 — 教务系统ETL对接
│   └── edu-common/               # 公共模块：Result/异常/工具类/常量
│
├── frontend/                     ← 前端（React 18 + TypeScript）
│   ├── apps/
│   │   ├── web/                  # PC + 平板 + 手机 PWA（Vite）
│   │   ├── weapp/                # 微信小程序（Taro 3）
│   │   └── bigscreen/            # 数据大屏（xl 专用）
│   ├── packages/
│   │   ├── ui/                   # 共享 UI 组件库（Tailwind）
│   │   ├── api/                  # API 调用层（React Query hooks）
│   │   ├── store/                # Zustand 全局状态
│   │   └── utils/                # 工具函数（含 idb、env 判断）
│   └── package.json              # pnpm workspace 根
│
├── infra/                        ← 基础设施配置
│   ├── k8s/                      # Kubernetes 部署 YAML
│   ├── docker-compose.dev.yml    # 本地开发环境
│   ├── nginx/                    # Nginx 配置
│   └── scripts/                  # 运维脚本
│
└── docs/                         ← 技术文档
    ├── 技术方案V2.md
    ├── api/                      # OpenAPI 规范（.yaml）
    └── db/                       # 数据库设计文档
```

---

## 3. 开发环境快速启动

### 3.0 双机开发环境说明

本项目采用**本机编写代码、内网机运行程序**的分离模式：

| 角色 | 说明 |
|------|------|
| **本机（开发）** | 编写代码、运行 IDE、git 操作、前端热更新调试 |
| **内网机（运行）** | 运行 Docker 中间件、部署后端微服务、运行前端构建产物 |

**内网机信息：**

| 项目 | 值 |
|------|----|
| IP | `100.84.68.115` |
| 主机名 | `onlyserver` |
| 用户 | `xintong` |
| OS | Ubuntu 24.04.4 LTS |
| CPU | Intel Core2 Duo T7700 @ 2.40GHz（8线程） |
| 内存 | 62 GB |
| 磁盘 | 116 GB（已用 57 GB，剩余 55 GB） |
| SSH 别名 | `ssh onlyserver`（本机已配置免密） |

**内网机已安装环境：**

| 软件 | 版本 | 状态 |
|------|------|------|
| Java（Temurin） | 21.0.10 LTS | ✅ 已安装 |
| Node.js | 20.20.0 | ✅ 已安装 |
| pnpm | 10.33.0 | ✅ 已安装 |
| Docker | 29.2.1 | ✅ 已安装 |
| kubectl | 1.35.1 | ✅ 已安装 |
| Maven | — | ❌ 待安装 |

**⚠️ 端口占用警告（内网机已有 Nextcloud 服务）：**

| 被占用端口 | 占用容器 | 处理方式 |
|-----------|---------|---------|
| `3306` | nextcloud-db (MySQL) | Docker Compose 中 edu MySQL 改用 `3307` |
| `6379` | nextcloud-redis | Docker Compose 中 edu Redis 改用 `6380` |
| `8080` | onlyoffice | edu-gateway 端口不变（8080 由内网机 onlyoffice 占用，考虑改为 `18080`）|
| `8088` | nextcloud-app | edu-stat 端口改用 `18088` |

> **端口映射约定（Docker Compose dev 环境）：**  
> edu 项目所有中间件端口在原端口前加 `1` 前缀（如 `13306`、`16379`），避免与宿主机已有服务冲突。  
> 微服务端口 8081–8093 不冲突，保持原编号。

**代码同步到内网机（Git）：**

> 本机为代码仓库主体，内网机通过 `git push` 自动同步工作目录（`receive.denyCurrentBranch = updateInstead`）。

> **⚠️ 关键约束（曾踩坑）：`updateInstead` 只会更新内网机“当前检出（checked-out）的那个分支”的工作目录文件。**
> 若内网机检出在 `main`，而你 `git push onlyserver develop`，则**只更新 develop 的 ref、不会更新磁盘文件** —— 内网机源码仍停留在旧分支，`git log` 看似落后、重新构建会编译到旧代码。
>
> 因此推送前务必确认内网机检出分支与推送分支一致：
> ```bash
> # 查看内网机当前检出分支
> ssh onlyserver 'cd ~/smart-edu && git rev-parse --abbrev-ref HEAD'
> # 如不一致，先在内网机切到目标分支（要求工作树干净），之后 push 才会自动更新文件
> ssh onlyserver 'cd ~/smart-edu && git checkout develop'
> ```
> 当前约定：内网机长期检出 **`develop`**（集成分支），日常 `git push onlyserver develop` 即自动同步文件。

```bash
# 首次建立同步关系（已完成，无需重复执行）
# git remote add onlyserver onlyserver:~/smart-edu
# ssh onlyserver 'cd ~/smart-edu && git init && git config receive.denyCurrentBranch updateInstead && git checkout develop'

# 日常同步：commit 后推送到内网机（内网机须检出 develop，工作目录才会自动更新）
git push onlyserver develop

# 推送后核对内网机已更新到最新提交
ssh onlyserver 'cd ~/smart-edu && git log --oneline -1'
```

> **同步代码 ≠ 重新部署。** `git push` 只更新内网机源码文件，**不会重建 jar / 重启服务**。代码改动要在内网机生效，必须重新构建并重启对应服务（见下方“重建并重启微服务”）。

**部署到内网机常用命令：**

```bash
# SSH 免密登录
ssh onlyserver

# 将编译好的 jar 推送到内网机（示例：edu-auth）
scp backend/edu-auth/target/edu-auth.jar onlyserver:~/apps/

# 在内网机启动 Docker Compose 中间件
ssh onlyserver 'cd ~/smart-edu && docker compose -f infra/docker-compose.dev.yml up -d'

# 查看内网机服务日志
ssh onlyserver 'docker logs -f edu-auth --tail 100'
```

**在内网机重建并重启微服务（代码改动生效）：**

> 内网机已装 Maven（`/opt/maven`，不在默认 PATH，需显式导出）。微服务以 `java -jar` 方式运行（非容器），日志在 `~/.edu-dev/logs/`、pidfile 在 `~/.edu-dev/pids/`。

```bash
# 1) 在内网机用最新源码重建（单个服务，-am 连带 edu-common）
ssh onlyserver 'export PATH=/opt/maven/bin:$PATH && cd ~/smart-edu/backend \
  && mvn -q -pl edu-ai -am package -DskipTests --no-transfer-progress'
# 全量重建（老 CPU 较慢，可并行）：mvn -q -T 1C clean package -DskipTests

# 2) 停旧进程（按 jar 名 pkill，避免 pidfile 过期）
ssh onlyserver "pkill -f 'edu-ai-1.0.0-SNAPSHOT.jar'"

# 3) 起新进程（setsid 让 ssh 不被后台进程挂住 channel；标准启动参数）
ssh onlyserver 'cd ~/smart-edu/backend && setsid java -Xms256m -Xmx512m \
  -Dspring.profiles.active=dev -Dspring.cloud.nacos.config.import-check.enabled=false \
  -jar edu-ai/target/edu-ai-1.0.0-SNAPSHOT.jar \
  > ~/.edu-dev/logs/edu-ai.log 2>&1 </dev/null &'

# 4) 验证（约 30~60s 启动）
ssh onlyserver 'curl -s http://localhost:8087/actuator/health'

# 一键全量重建+重启（含中间件检查，跳过 seed/前端）
ssh onlyserver 'bash ~/smart-edu/infra/scripts/start-all.sh --backend-only --skip-seed'
```

> **dev 登录拿 token（验证联调）：** 测试账号手机号存 `sys_user.phone_cipher`（dev 为明文）；验证码写 Redis `sms:code:{phone}`（默认 `123456`，见 `infra/scripts/reset-sms-codes.sh`）；调用 `POST /api/v1/auth/login/phone`（body `{phone, code}`，**非** `/auth/login`）取 `accessToken`。

### 3.1 前置依赖

```bash
# 内网机需补装 Maven（其余已就绪）
ssh onlyserver 'sudo apt-get install -y maven && mvn --version'

# 本机（开发机）需安装
git
IDE（IntelliJ IDEA / VSCode）
```

### 3.2 启动中间件（内网机 Docker）

```bash
# 在内网机上启动所有中间件（端口已规避 Nextcloud 冲突）
ssh onlyserver 'cd ~/smart-edu && docker compose -f infra/docker-compose.dev.yml up -d'

# 包含（均运行在内网机 100.84.68.115）：
#   MySQL 8.0       → 100.84.68.115:13306  (root/edu_dev_2026)
#   Redis 7         → 100.84.68.115:16379
#   Kafka           → 100.84.68.115:19092
#   Nacos           → http://100.84.68.115:18848  (nacos/nacos)
#   MongoDB         → 100.84.68.115:17017
#   MinIO           → http://100.84.68.115:19000  (minioadmin/minioadmin)
#   Elasticsearch   → 100.84.68.115:19200
#   ClickHouse      → 100.84.68.115:18123
#   XXL-Job         → http://100.84.68.115:18160  (admin/123456)
#
# 微服务（运行在内网机）：
#   edu-gateway     → 100.84.68.115:18080
#   edu-auth        → 100.84.68.115:8081
#   edu-user        → 100.84.68.115:8082
#   edu-course      → 100.84.68.115:8083
#   edu-interaction → 100.84.68.115:8084
#   edu-exam        → 100.84.68.115:8085
#   edu-grade       → 100.84.68.115:8086
#   edu-ai          → 100.84.68.115:8087
#   edu-stat        → 100.84.68.115:18088
#   edu-file        → 100.84.68.115:8089
#   edu-notify      → 100.84.68.115:8090
#   edu-live        → 100.84.68.115:8091
#   edu-admin       → 100.84.68.115:8092
#   edu-jwxt        → 100.84.68.115:8093
```

### 3.3 启动后端服务

```bash
cd backend

# 编译公共模块（首次或 edu-common 变更后必须执行）
mvn install -pl edu-common -am -q

# 启动单个服务（以 edu-interaction 为例）
mvn spring-boot:run -pl edu-interaction \
  -Dspring-boot.run.profiles=dev

# 启动网关（需先启动目标服务）
mvn spring-boot:run -pl edu-gateway \
  -Dspring-boot.run.profiles=dev

# 批量启动核心服务（开发常用组合）
./infra/scripts/start-dev.sh interaction course auth gateway
```

### 3.4 启动前端

```bash
cd frontend

# 安装依赖（首次或 package.json 变更后）
pnpm install

# 启动 Web 应用（含热更新）
pnpm --filter web dev               # http://localhost:5173

# 启动大屏
pnpm --filter bigscreen dev         # http://localhost:5174

# 启动微信小程序开发（需微信开发者工具）
pnpm --filter weapp dev             # 输出到 weapp/dist，用开发者工具打开
```

### 3.5 初始化数据库

```bash
# 执行 DDL（全量建表，仅用于新环境）
mysql -h localhost -u root -pedu_dev_2026 < docs/db/schema_all.sql

# 插入测试数据（含测试账号）
mysql -h localhost -u root -pedu_dev_2026 edu_db < docs/db/seed_dev.sql

# ClickHouse 初始化
clickhouse-client < docs/db/clickhouse_schema.sql

# 测试账号
# 管理员：admin / edu2026@admin
# 教师：  teacher01 / edu2026@test
# 学生：  student01 / edu2026@test
```

---

## 4. 技术栈与版本锁定

### 4.1 后端版本

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | 禁止使用 17 及以下 |
| Spring Boot | 3.2.x | 禁止升级到 4.x（未评估） |
| Spring Cloud | 2023.0.x | 与 Boot 3.2 配套 |
| Spring AI | 1.0.x | 统一 LLM 调用，禁止直接调用第三方 SDK |
| MyBatis-Plus | 3.5.x | 禁止裸写 JDBC |
| Nacos | 2.3.x | 配置中心 + 服务注册 |
| Sentinel | 1.8.x | 限流熔断 |
| XXL-Job | 2.4.x | 定时任务 |
| Kafka Client | 3.6.x | 与服务端版本对齐 |
| MapStruct | 1.5.x | DTO ↔ Entity 转换，禁止手写 setter 赋值 |

### 4.2 前端版本

| 组件 | 版本 | 说明 |
|------|------|------|
| React | 18.x | 必须使用函数组件 + Hooks |
| TypeScript | 5.x | 严格模式，禁止 `any` |
| Tailwind CSS | 3.x | 禁止写内联 style |
| Vite | 5.x | 构建工具 |
| Taro | 3.6.x | 小程序跨端 |
| React Query | 5.x（TanStack） | 服务端状态管理 |
| Zustand | 4.x | 客户端全局状态 |
| ECharts | 5.x | 图表库 |
| Tiptap | 2.x | 富文本编辑器 |

### 4.3 中间件版本（本地开发 Docker 镜像）

| 中间件 | 版本 |
|--------|------|
| MySQL | 8.0 |
| Redis | 7.2 |
| Kafka | 3.6 |
| MongoDB | 7.0 |
| Elasticsearch | 8.11 |
| ClickHouse | 23.x |
| MinIO | RELEASE.2024-01 |

---

## 5. 后端开发规范

### 5.1 包结构（每个微服务统一）

```
edu-{service}/
└── src/main/java/cn/smu/edu/{service}/
    ├── controller/      # REST 接口，只做参数校验和结果封装
    ├── service/         # 业务逻辑（接口 + 实现分离）
    ├── repository/      # 数据访问层（MyBatis-Plus Mapper）
    ├── domain/
    │   ├── entity/      # 数据库实体（@TableName）
    │   ├── dto/         # 请求 DTO（@Valid 校验注解）
    │   └── vo/          # 响应 VO
    ├── event/           # Kafka 事件对象
    ├── config/          # Bean 配置、安全配置
    ├── exception/       # 业务异常类
    └── {Service}Application.java
```

### 5.2 统一响应结构

**所有接口必须使用 `edu-common` 中的 `Result<T>` 封装：**

```java
// 正确 ✅
@GetMapping("/lesson/{id}")
public Result<LessonVO> getLesson(@PathVariable Long id) {
    return Result.ok(lessonService.findById(id));
}

// 错误 ❌ — 禁止直接返回实体或裸 Map
@GetMapping("/lesson/{id}")
public Lesson getLesson(@PathVariable Long id) { ... }
```

```java
// Result.java（edu-common）
public record Result<T>(int code, String msg, T data) {
    public static <T> Result<T> ok(T data)       { return new Result<>(200, "ok", data); }
    public static <T> Result<T> ok()             { return new Result<>(200, "ok", null); }
    public static <T> Result<T> fail(String msg) { return new Result<>(500, msg, null); }
    public static <T> Result<T> fail(int code, String msg) { return new Result<>(code, msg, null); }
}
```

### 5.3 异常处理

```java
// 业务异常 — 使用 BizException，不要抛原生 RuntimeException
throw new BizException(ErrorCode.LESSON_NOT_FOUND);  // ✅
throw new RuntimeException("课堂不存在");              // ❌

// GlobalExceptionHandler 统一捕获（edu-common 已提供，各服务引入即可）
// 包括：BizException、BindException（参数校验）、AccessDeniedException
```

### 5.4 数据访问层规范

```java
// ✅ 使用 MyBatis-Plus，优先使用 LambdaQueryWrapper
lessonMapper.selectOne(new LambdaQueryWrapper<Lesson>()
    .eq(Lesson::getClassId, classId)
    .eq(Lesson::getStatus, LessonStatus.ACTIVE)
    .orderByDesc(Lesson::getStartTime));

// ✅ 批量操作用 MyBatis-Plus 的 saveBatch 或自定义 XML 的批量 INSERT
lessonService.saveBatch(lessons, 200);  // 每批200条

// ❌ 禁止在循环中单条 INSERT/UPDATE（N+1 问题）
for (Lesson l : lessons) lessonMapper.insert(l);

// ❌ 禁止在 Service 层直接写 SQL 字符串
String sql = "SELECT * FROM lesson WHERE class_id = " + classId;
```

### 5.5 DTO ↔ Entity 转换

```java
// ✅ 必须使用 MapStruct，禁止手写 setter 链
@Mapper(componentModel = "spring")
public interface LessonMapper {
    LessonVO toVO(Lesson lesson);
    Lesson toEntity(LessonCreateDTO dto);
}

// ❌ 禁止
LessonVO vo = new LessonVO();
vo.setId(lesson.getId());
vo.setTitle(lesson.getTitle());
// ... 10行 setter
```

### 5.6 Kafka 消息规范

```java
// Topic 命名：edu.{domain}.{action}（全小写，点分隔）
// 示例：edu.lesson.ended / edu.exam.submitted / edu.ai.tasks / edu.notice

// ✅ 发送消息（异步，不阻塞主流程）
kafkaTemplate.send("edu.ai.tasks", new AiTaskEvent(lessonId, teacherId, TaskType.SUMMARY));

// ✅ 消费者必须做幂等处理（Redis 去重键）
@KafkaListener(topics = "edu.ai.tasks", concurrency = "3")
public void consume(AiTaskEvent event) {
    String dedupeKey = "ai:task:done:" + event.getTaskId();
    if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofHours(24)))) {
        processTask(event);
    }
}

// Kafka 消费者 concurrency 上限：
//   edu.ai.tasks       → concurrency = "3"   （GPU/算力限制，不可随意调大）
//   edu.exam.submit    → concurrency = "10"
//   edu.teaching.events → concurrency = "5"
```

### 5.7 Redis 键命名规范

```
{service}:{domain}:{identifier}[:{field}]

示例：
  attend:queue:{lessonId}          签到队列（List）
  attend:bloom:{lessonId}          签到布隆过滤器
  attend:count:{lessonId}          签到人数计数器（String/incr）
  exam:submit:{examId}:{userId}    交卷幂等键（String，TTL 30min）
  exam:answer:{examId}:{userId}    交卷 Redis 暂存（String，TTL 2h）
  ai:task:done:{taskId}           AI任务去重键（String，TTL 24h）
  session:{token}                  用户 Session（Hash，TTL 2h）
  sms:code:{phone}                 短信验证码（String，TTL 5min）
```

### 5.8 日志规范

```java
// ✅ 使用 SLF4J + 参数化日志
log.info("课堂签到完成: lessonId={}, studentId={}, count={}", lessonId, studentId, count);
log.error("AI任务处理失败: taskId={}", event.getTaskId(), e);

// ❌ 禁止字符串拼接
log.info("课堂签到完成: lessonId=" + lessonId);

// 日志级别规范：
//   ERROR  — 需要立即处理的错误（发告警）
//   WARN   — 可能有问题的情况（不发告警）
//   INFO   — 关键业务操作节点（签到成功、AI任务完成、教务同步完成等）
//   DEBUG  — 开发调试，生产环境关闭
```

### 5.9 操作日志（等保要求）

```java
// 所有写操作接口必须加 @OperationLog 注解（AOP 自动记录，留存 180 天）
@OperationLog(module = "exam", operation = "发布试卷")
@PostMapping("/exam/publish")
public Result<Void> publishExam(@RequestBody ExamPublishDTO dto) { ... }

// 以下操作必须加注解（不得遗漏）：
// - 用户增删改、角色分配
// - 试卷发布/修改/删除
// - 成绩修改、线下成绩导入
// - 教务数据同步操作
// - 系统配置变更
```

---

## 6. 前端开发规范

### 6.1 组件规范

```tsx
// ✅ 函数组件 + 具名导出（方便 tree-shaking 和调试）
export function AttendanceCard({ lessonId, count }: AttendanceCardProps) {
  // ...
}

// ❌ 禁止 class 组件（新代码）
class AttendanceCard extends React.Component { ... }

// ❌ 禁止默认导出（除页面组件 pages/ 目录）
export default function AttendanceCard() { ... }
```

### 6.2 TypeScript 严格规范

```typescript
// tsconfig.json 关键配置（不得降低）
{
  "strict": true,
  "noImplicitAny": true,
  "strictNullChecks": true,
  "noUnusedLocals": true
}

// ❌ 禁止使用 any
const data: any = response;          // ❌
const data: LessonVO = response;     // ✅

// ❌ 禁止非空断言（!）用于可能为 null 的值（请用可选链）
lesson!.title                        // ❌
lesson?.title ?? '未命名'            // ✅
```

### 6.3 Tailwind 响应式规范（三端适配核心）

```
移动优先原则：base 样式写手机端，通过前缀逐步增强
  sm:  ≥640px  小屏平板
  md:  ≥768px  标准平板
  lg:  ≥1024px 桌面端
  xl:  ≥1280px 宽屏/大屏
```

```tsx
// ✅ 正确的响应式写法
<div className="flex flex-col md:flex-row">           {/* 手机列，平板以上行 */}
<aside className="hidden md:flex md:w-56 lg:w-64">   {/* 手机隐藏，平板显示 */}
<nav className="md:hidden fixed bottom-0 ...">       {/* 手机底Tab，平板隐藏 */}

// ❌ 禁止写内联 style（破坏响应式一致性）
<div style={{ display: 'flex', flexDirection: 'row' }}>

// ❌ 禁止随意使用媒体查询 @media（统一用 Tailwind 前缀）
// 例外：大屏特殊动画效果可在 bigscreen/styles 目录写自定义 CSS
```

### 6.4 API 调用规范（React Query）

```tsx
// ✅ 所有服务端状态用 React Query，不要手写 useEffect + fetch
export function useLessonDetail(lessonId: string) {
  return useQuery({
    queryKey: ['lesson', lessonId],
    queryFn: () => api.course.getLesson(lessonId),
    staleTime: 30_000,
    enabled: !!lessonId,
  });
}

// ✅ 写操作用 useMutation
export function useAttend(lessonId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.interaction.attend(lessonId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendance', lessonId] });
    },
  });
}

// ❌ 禁止在组件内直接 fetch/axios（应通过 api/ 层）
useEffect(() => {
  fetch('/api/lesson/' + id).then(...);  // ❌
}, []);
```

### 6.5 Taro 小程序条件编译规范（强制）

```typescript
// env.ts — 必须引用此文件判断平台，禁止内联判断
export const isWeapp = process.env.TARO_ENV === 'weapp';
export const isWeb   = process.env.TARO_ENV === 'webapp' || !process.env.TARO_ENV;
```

```tsx
// ✅ DOM API 必须隔离（CI lint 强制检查）
import { isWeb } from '@edu/utils/env';

if (isWeb) {
  DOMPurify.sanitize(html);   // Web 端 XSS 过滤
}
// 小程序端用 Taro.parseXml + 白名单替代

// ❌ 禁止直接调用 DOM API（小程序沙箱中会崩溃）
document.querySelector('.slide');       // ❌
window.localStorage.getItem('token');   // ❌
```

**不可在小程序端使用的功能（必须用 Web Only 隔离）：**

- `document.*` / `window.*` 相关 API
- `qrcode.js`、`DOMPurify`、`Tiptap`（富文本编辑器）
- ECharts（大屏图表）→ 替换为 `echarts-for-weixin`
- `localStorage` / `IndexedDB` → 替换为 `Taro.setStorageSync`
- Browser Push / Notification API → 替换为微信订阅消息

### 6.6 考试草稿暂存规范（IndexedDB，不得修改）

```typescript
// useExamAutoSave.ts — 每15秒自动保存，断网容灾
// 键名格式：exam_draft_{examId}_{userId}
// 不得更改保存间隔（<15s 过于频繁，>30s 风险过高）
// 提交成功后必须调用 idb.delete(DB_KEY) 清除草稿
```

### 6.7 状态管理规范

```typescript
// 全局状态（Zustand）：仅用于跨页面/跨组件共享的用户信息、配置
// 服务端状态（React Query）：所有来自接口的数据
// 本地 UI 状态（useState）：仅影响当前组件的临时状态

// ❌ 禁止用 Zustand 缓存接口数据（与 React Query 职责重叠）
// ❌ 禁止用 React Query 管理纯 UI 状态（如 modal 开关）
```

---

## 7. 数据库规范

### 7.1 MySQL 命名规范

```sql
-- 表名：snake_case，含义前缀
--   业务表无前缀，系统表 sys_ 前缀，日志表 log_ 前缀，教务对接表 jwxt_ 前缀
--   示例：lesson / attendance / sys_user / log_operation / jwxt_sync_log

-- 字段名：snake_case
-- 主键：id BIGINT AUTO_INCREMENT（统一，禁止 UUID 做主键）
-- 时间字段：created_at / updated_at（DATETIME，由 MyBatis-Plus 自动填充）
-- 软删除：is_deleted TINYINT DEFAULT 0（启用 MyBatis-Plus 逻辑删除）
-- 状态字段：status TINYINT（注释必须写明每个值的含义）

-- 必须建索引的场景：
--   外键关联字段（class_id, teacher_id, student_id）
--   查询过滤常用字段（status, created_at）
--   全文检索字段（content）用 FULLTEXT INDEX

-- 禁止的操作：
--   禁止 SELECT *（显式列出字段）
--   禁止在 WHERE 中对字段做函数运算（破坏索引）
--   禁止在生产环境直接 UPDATE/DELETE 不带 WHERE（CI 脚本会检查 migration SQL）
```

### 7.2 数据库迁移规范

```bash
# 所有 DDL 变更必须通过 Flyway migration 文件，禁止手动在生产库执行 SQL

# 文件命名：V{版本号}__{描述}.sql
# 示例：
docs/db/migrations/
  V1.0.0__init_schema.sql
  V1.1.0__add_jwxt_sync_log.sql
  V1.2.0__add_ai_task_status_to_lesson.sql

# 规则：
# - 已提交的 migration 文件禁止修改（Flyway checksum 会失败）
# - 新需求写新版本文件
# - 本地开发：mvn flyway:migrate -pl edu-{service}
```

### 7.3 Redis 操作规范

```java
// ✅ 所有 Key 必须设置 TTL，禁止永不过期的 Key
redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));  // ✅
redisTemplate.opsForValue().set(key, value);  // ❌ 无 TTL，禁止

// ✅ 批量操作用 Pipeline
redisTemplate.executePipelined((RedisCallback<Object>) conn -> {
    // 批量操作
    return null;
});

// Key TTL 参考：
//   签到码       5 min
//   短信验证码   5 min
//   用户 Session 2 h（Access Token 有效期）
//   交卷幂等键   30 min
//   答案暂存     2 h
//   AI任务去重   24 h
//   签到布隆过滤 课程结束后 24 h（手动清理）
```

### 7.4 ClickHouse 使用规范

```sql
-- ClickHouse 只用于统计分析（OLAP），禁止做事务性写入
-- 写入方式：Kafka → Consumer 批量 INSERT（每批 ≥ 1000 条）
-- 禁止频繁单条 INSERT（ClickHouse 不适合 OLTP 场景）

-- 查询规范：
-- WHERE 子句必须包含分区键（如 stat_date）避免全表扫描
-- 复杂聚合查询必须在 dev 环境测试执行时间，超过 5s 需优化
```

---

## 8. 关键架构约束（必须遵守）

以下规则是本项目最高优先级的技术约束，**任何 PR 违反以下条目将被直接拒绝**。

### 8.1 签到高并发：禁止直连 MySQL 写入

```
❌ 禁止：学生签到请求 → 直接 INSERT attendance 表
✅ 必须：学生签到 → Redis 布隆去重 → Redis List 队列 → 批量异步落库

理由：高校雷击效应，全校数千学生集中在课前5分钟签到，直连 MySQL 会导致连接池耗尽
```

### 8.2 考试交卷：必须有 IndexedDB 草稿 + 打散机制

```
❌ 禁止：倒计时归零时，全员同时同步提交大体积答卷
✅ 必须：
  - 前端每15秒将当前答案写入 IndexedDB（断网容灾）
  - 自动交卷前30秒内，按学号末两位取模，分散0~30秒提交
  - 后端接口做幂等处理（Redis 幂等键，TTL 30min）
  - 后端先写 Redis 暂存（毫秒响应），再 Kafka 异步落库

理由：3000人同时交卷（含图片附件）会导致接口超时雪崩
```

### 8.3 AI 任务：全部异步化，禁止同步等待

```
❌ 禁止：课堂结束 → 同步调用 LLM → 等待结果 → 返回
✅ 必须：课堂结束 → 发 Kafka 消息（edu.ai.tasks）→ 立即返回"处理中"
         → Consumer 异步处理 → WebSocket 通知教师"已完成"

AI 任务 Kafka 消费者并发度上限：
  edu-ai 服务 concurrency = "3"（防止 GPU/API 并发过载，不得随意调大）

理由：ASR转写+LLM摘要+思维导图生成是 CPU/GPU 密集型，下课高峰同时触发会拖垮服务
```

### 8.4 Prompt 安全：所有 LLM 调用必须过安全层

```
❌ 禁止：直接将教师输入的提示词传给 LLM
✅ 必须：通过 PromptSecurityFilter.filter() 处理后再调用

安全层职责：
  1. 前置强约束 System Prompt（不可被用户内容覆盖）
  2. 敏感词检测（政治/宗教/违规词库，存 Nacos 可配置）
  3. 越权指令过滤（"忽略上面规则"等注入模式）
  4. 输出二次过滤（响应中的敏感内容替换）

理由：教师非技术用户，提示词调试权限易被滥用或间接被学生利用注入
```

### 8.5 直播：线下课堂禁止默认开启 WebRTC

```
❌ 禁止：普通线下课堂默认开启 WebRTC 音视频流
✅ 必须：
  - 线下课（SLIDE_ONLY 模式）：仅 WebSocket，WebRTC = false，RTMP = false
  - 线上网课（ONLINE_CLASS 模式）：开启 WebRTC + RTMP
  - 教师主动开启"录播"功能才启用 RTMP

理由：WebRTC+CDN 带宽成本极高，普通教室内无需流媒体，节省95%带宽开销
```

### 8.6 人脸识别：禁止存储原始人脸图片

```
❌ 禁止：将学生现场采集的人脸图片存入系统数据库或 MinIO
✅ 必须：
  - 调用第三方 API（百度/旷视）比对，只存比对结果 + 置信度分值
  - 档案照（用于比对的标准照）AES-256 加密后存 MinIO，双重鉴权访问
  - FaceVerifyResult 中明确不设 rawPhoto 字段

理由：《个人信息保护法》对生物特征信息的严格限制，等保三级合规要求
```

### 8.7 操作日志：180天留存，禁止物理删除

```
✅ 操作日志写入独立库（edu_audit_db），不与业务库混用
✅ 保留期不少于 180 天（等保三级硬性要求）
✅ 到期数据归档至冷存储，严禁 DELETE
❌ 禁止任何代码对 log_operation 表执行 DELETE

理由：等保2.0三级标准强制要求，违规将影响合规评测结果
```

### 8.8 Taro 小程序：DOM API 必须隔离

```
❌ 禁止：在 Taro 共享代码中直接调用 document.* / window.* / localStorage
✅ 必须：用 isWeb 判断隔离，或 #ifdef WEB 条件编译块包裹

CI 检查：所有 PR 会自动 lint 检查 weapp/ 目录下的 DOM API 调用
违规 PR 不予合并
```

---

## 9. 安全与合规红线

### 9.1 等保三级强制要求

| 要求 | 实现位置 | 负责角色 |
|------|----------|----------|
| 操作日志 ≥ 180 天 | `edu_audit_db` + XXL-Job 归档 | 后端 + 运维 |
| 三员分立 | 系统管理员/安全管理员/审计管理员分开 | 运维配置 |
| 最小权限 | 各服务独立 DB 账号，禁止共用 root | 运维 |
| 网络隔离 | 数据库位于内网，禁止公网直连 | 运维 |
| 备份验证 | 每季度备份恢复演练 | 运维 |
| WAF | 部署 Web 应用防火墙 | 运维 |
| 漏洞扫描 | OWASP Dependency-Check，每次 release | CI/CD |

### 9.2 数据安全红线

```
1. 禁止在日志中打印以下信息：
   - 手机号（只打印前3位+****）
   - 身份证号（只打印前6位+****）
   - JWT Token
   - 密码（任何情况）
   - 人脸图片 base64

2. 禁止在 Git 仓库中提交：
   - 任何环境的密码、密钥、Token
   - 生产环境配置文件（application-prod.yml）
   - 含真实学生数据的 SQL 文件

3. 敏感配置统一存 Nacos（加密存储），通过环境变量注入
```

### 9.3 接口安全规范

```java
// 所有写操作接口必须鉴权（@PreAuthorize 或网关 JWT 过滤）
// 数据权限用 @DataScope 注解（防越权读取其他院系数据）
// 签到、交卷、提交答案等高频接口必须配置 Sentinel 限流规则

// 禁止的写法：
String sql = "SELECT * FROM sys_user WHERE id = " + userId;  // SQL注入 ❌
log.info("用户登录：手机号={}", phone);                       // 泄露隐私 ❌
System.out.println("JWT: " + token);                         // 泄露凭证 ❌
```

---

## 10. 测试规范

### 10.1 单元测试

```java
// 每个 Service 类必须有对应的单元测试，覆盖率 ≥ 70%
// 测试类位置：src/test/java/...（镜像 main 目录结构）
// 命名：{ClassName}Test.java

// 使用 JUnit 5 + Mockito
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    @Mock AttendanceRepository attendanceRepo;
    @Mock RedisTemplate<String, String> redisTemplate;
    @InjectMocks AttendanceServiceImpl attendanceService;

    @Test
    void attend_shouldReturnAlreadyAttended_whenBloomFilterHit() { ... }
    @Test
    void attend_shouldPushToQueue_whenFirstAttend() { ... }
}
```

### 10.2 集成测试

```java
// 关键场景必须有集成测试（使用 @SpringBootTest + Testcontainers）
// 必测场景：
//   - 签到接口幂等性（同一学生重复请求只签到一次）
//   - 交卷接口幂等性（网络重试不重复提交）
//   - AI Prompt 安全层过滤（敏感词拦截）
//   - 教务 ETL 增量同步（新增/修改/无变化三种情况）
```

### 10.3 前端测试

```typescript
// 组件单元测试：Vitest + React Testing Library
// 关键 Hook 测试：useExamAutoSave（验证 IndexedDB 写入）
// E2E 测试（S8 阶段引入）：Playwright

// 测试文件位置：{component}.test.tsx（与组件同目录）
```

### 10.4 性能压测（S9 阶段）

```
必须压测的场景（使用 JMeter / k6）：

1. 签到接口 TPS 目标：单节点 ≥ 1500/s
   场景：3000 学生在 2 分钟内随机签到

2. 交卷接口并发目标：≥ 500 并发（含图片附件）
   场景：1000 学生在 30 秒内集中交卷（测试打散机制效果）

3. WebSocket 广播目标：≥ 10000 条/s
   场景：教师发题，3000 学生 WebSocket 同时收到

4. AI 任务队列：100 节课同时结束，验证任务不积压超过 5 分钟

压测报告需存档至 docs/perf-test-reports/
```

---

## 11. Git 工作流

### 11.1 分支策略

```
main          — 生产分支，只接受来自 release 的 PR，受保护
develop       — 集成分支，Sprint 内所有 feature 合并至此
release/vX.X  — 发版分支（如 release/v1.2.0），从 develop 创建
feature/{ticket}-{description}  — 功能分支（从 develop 拉）
fix/{ticket}-{description}      — 修复分支（从 develop 或 main 拉）
hotfix/{ticket}-{description}   — 紧急修复（从 main 拉，合并回 main + develop）

示例：
  feature/EDU-142-attendance-bloom-filter
  fix/EDU-156-ai-prompt-security-bypass
  hotfix/EDU-201-exam-submit-null-pointer
```

### 11.2 Commit 规范（Conventional Commits）

```
格式：<type>(<scope>): <subject>

type：
  feat     新功能
  fix      Bug修复
  refactor 重构（不影响功能）
  perf     性能优化
  test     测试相关
  docs     文档
  chore    构建/配置/依赖变更
  security 安全修复（等保相关标记为此类型）

scope：服务名（interaction/exam/ai/stat/jwxt 等）或 frontend/infra

示例：
  feat(interaction): 签到接口引入 Redis 队列削峰，防雷击效应
  fix(exam): 修复交卷接口缺少幂等检查导致重复提交
  security(ai): 添加 Prompt 安全过滤层，防止提示词注入
  perf(stat): ClickHouse 签到统计查询添加分区键过滤，查询降至 200ms
  feat(jwxt): 实现教务系统增量同步 ETL，支持正方/强智接口
```

### 11.3 PR 规范

```
PR 标题格式：[{Sprint}][{服务}] {简要描述}
示例：[S3][interaction] 签到高并发削峰：Redis 队列 + 布隆过滤

PR 模板（.github/pull_request_template.md）：
  ## 变更说明
  ## 涉及模块
  ## 测试情况
  ## 是否涉及数据库变更（含 migration 文件）
  ## 是否涉及等保/安全要点
  ## Checklist
    - [ ] 单元测试通过
    - [ ] 无 any 类型（前端）
    - [ ] 无未设置 TTL 的 Redis Key
    - [ ] 日志无敏感信息
    - [ ] 操作日志注解已添加（写操作）
```

---

## 12. CI/CD 流水线

### 12.1 后端 CI（每次 Push / PR 触发）

```yaml
# .gitlab-ci.yml 关键阶段
stages:
  - lint        # Checkstyle + PMD 代码规范检查
  - test        # 单元测试 + 覆盖率（≥70% 才通过）
  - security    # OWASP Dependency-Check 依赖漏洞扫描
  - build       # Maven 打包
  - docker      # 构建 Docker 镜像，推送至 registry.smu.edu.cn
  - deploy-dev  # 自动部署到开发环境（仅 develop 分支）
```

### 12.2 前端 CI

```yaml
stages:
  - lint        # ESLint（含 no-dom-in-weapp 自定义规则）
  - typecheck   # tsc --noEmit（TypeScript 严格检查）
  - test        # Vitest 单元测试
  - build       # Vite 构建 + Taro 构建小程序
  - deploy-dev  # 部署到开发 CDN
```

### 12.3 自定义 lint 规则（前端 CI 强制）

```
no-dom-in-weapp：检测 weapp/ 目录下对 document.*/window.*/localStorage 的直接调用
no-any-type：检测 TypeScript any 类型使用
no-inline-style：检测内联 style 属性（强制用 Tailwind）
require-result-wrapper：检测后端 Controller 是否所有方法都返回 Result<T>
```

---

## 13. 常用命令速查

### 后端

```bash
# 编译全部模块
cd backend && mvn clean install -DskipTests -q

# 运行单服务单元测试
mvn test -pl edu-interaction

# 查看 Kafka 消息（调试）
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic edu.ai.tasks --from-beginning

# 清空签到队列（测试用）
redis-cli DEL "attend:queue:*"

# 执行数据库 migration
mvn flyway:migrate -pl edu-interaction -Dflyway.url=jdbc:mysql://localhost:3306/edu_db

# 触发教务同步（手动，调试用）
curl -X POST http://localhost:8093/api/v1/jwxt/sync/full \
  -H "Authorization: Bearer {admin-token}"
```

### 前端

```bash
# 类型检查（不构建）
pnpm --filter web typecheck

# 运行所有测试
pnpm test

# 构建生产包
pnpm --filter web build

# 分析包体积
pnpm --filter web build --analyze

# 更新 Service Worker 缓存版本（发布后必须执行）
sed -i 's/edu-static-v[0-9]*/edu-static-v{新版本}/' apps/web/public/service-worker.js
```

### 基础设施

```bash
# 查看所有服务健康状态
./infra/scripts/health-check.sh

# 重置开发测试短信验证码（dev 登录用，默认所有测试账号=123456，24h）
# 在内网机执行；前端登录直接输入验证码、勿点“发送验证码”
ssh onlyserver 'bash ~/smart-edu/infra/scripts/reset-sms-codes.sh'

# 手动触发文件生命周期清理（测试用）
curl -X POST "http://localhost:8160/xxl-job-admin/api/trigger" \
  -d "jobId=fileLifecycleCheck"

# 查看 ClickHouse 慢查询
clickhouse-client --query "SELECT query, query_duration_ms FROM system.query_log WHERE query_duration_ms > 3000 ORDER BY query_duration_ms DESC LIMIT 20"

# 检查 MinIO 存储使用量
mc du local/edu-files/
```

---

*CLAUDE.md 版本：1.0 | 对应技术方案：V2.0 | 维护人：项目技术负责人*  
*如发现规范描述与实际代码冲突，以本文件为准，并通过 PR 更新实际代码。*
