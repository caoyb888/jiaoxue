# API 接口规范文档

**项目名称：** 山东管理学院智慧教学系统（smart-edu-platform）  
**规范版本：** V1.0  
**OpenAPI 版本：** 3.0.3  
**编制日期：** 2026-06-16  
**对应技术方案：** V2.0 | 对应数据库设计：V2.0

---

## 目录

1. [总体说明](#1-总体说明)
2. [鉴权规范](#2-鉴权规范)
3. [统一请求/响应规范](#3-统一请求响应规范)
4. [错误码规范](#4-错误码规范)
5. [共享 Schema 定义](#5-共享-schema-定义)
6. [认证模块 — /api/v1/auth](#6-认证模块--apiv1auth)
7. [用户与组织模块 — /api/v1/user](#7-用户与组织模块--apiv1user)
8. [课程与课堂模块 — /api/v1/course](#8-课程与课堂模块--apiv1course)
9. [互动教学模块 — /api/v1/interaction](#9-互动教学模块--apiv1interaction)
10. [题库模块 — /api/v1/question](#10-题库模块--apiv1question)
11. [在线测试模块 — /api/v1/exam](#11-在线测试模块--apiv1exam)
12. [成绩管理模块 — /api/v1/grade](#12-成绩管理模块--apiv1grade)
13. [AI 能力模块 — /api/v1/ai](#13-ai-能力模块--apiv1ai)
14. [统计分析模块 — /api/v1/stat](#14-统计分析模块--apiv1stat)
15. [文件存储模块 — /api/v1/file](#15-文件存储模块--apiv1file)
16. [直播推流模块 — /api/v1/live](#16-直播推流模块--apiv1live)
17. [消息通知模块 — /api/v1/notify](#17-消息通知模块--apiv1notify)
18. [教务对接模块 — /api/v1/jwxt](#18-教务对接模块--apiv1jwxt)
19. [WebSocket 事件规范](#19-websocket-事件规范)
20. [接口变更日志](#20-接口变更日志)

---

## 1. 总体说明

### 1.1 服务路由总览

所有请求统一经过 **Spring Cloud Gateway（:8080）** 路由，客户端只需访问网关地址。

| 路径前缀 | 下游服务 | 端口 | 主要职责 |
|----------|----------|------|----------|
| `/api/v1/auth/**` | edu-auth | 8081 | 登录、Token 刷新、微信 OAuth |
| `/api/v1/user/**` | edu-user | 8082 | 用户、院系、角色、通知 |
| `/api/v1/course/**` | edu-course | 8083 | 课程、教学班、课件、排课、课堂 |
| `/api/v1/interaction/**` | edu-interaction | 8084 | 签到、弹幕、点名、课堂积分 |
| `/api/v1/exam/**` | edu-exam | 8085 | 试卷、发布、作答、监考、批改 |
| `/api/v1/grade/**` | edu-grade | 8086 | 成绩规则、汇总、导出 |
| `/api/v1/ai/**` | edu-ai | 8087 | LLM对话、ASR、思维导图（SSE） |
| `/api/v1/stat/**` | edu-stat | 8088 | 实时大屏、历史统计、预警 |
| `/api/v1/file/**` | edu-file | 8089 | 上传、下载、生命周期 |
| `/api/v1/live/**` | edu-live | 8091 | 直播创建、推流配置、回放 |
| `/api/v1/notify/**` | edu-notify | 8090 | 公告、消息推送 |
| `/api/v1/jwxt/**` | edu-jwxt | 8093 | 教务同步、成绩回传（管理员） |
| `/ws/**` | edu-notify | 8090 | WebSocket STOMP 端点 |

### 1.2 基础 URL

```
生产环境：https://api.smu.edu.cn
测试环境：https://api-test.smu.edu.cn
本地开发：http://localhost:8080
```

### 1.3 接口版本策略

- 当前版本：`v1`，路径前缀 `/api/v1/`
- 不兼容变更时升级为 `/api/v2/`，`v1` 保留 6 个月过渡期
- 兼容性变更（新增字段）在当前版本内滚动发布

---

## 2. 鉴权规范

### 2.1 JWT Token 机制

系统采用 **JWT（RS256 签名）** 作为无状态鉴权凭证。

```
Access Token   有效期：2 小时
Refresh Token  有效期：7 天（仅用于换取新 Access Token）
```

**请求头格式：**

```http
Authorization: Bearer <access_token>
```

**Token 载荷（Payload）：**

```json
{
  "sub": "12345",
  "uid": 12345,
  "username": "zhangsan",
  "roles": ["TEACHER"],
  "deptId": 3,
  "userType": 2,
  "iat": 1718500000,
  "exp": 1718507200
}
```

### 2.2 角色与权限矩阵

| 角色编码 | 说明 | 数据范围 |
|----------|------|----------|
| `SUPER_ADMIN` | 校级超级管理员 | 全平台所有数据 |
| `DEPT_ADMIN` | 院系管理员 | 本院系数据 |
| `TEACHER` | 教师 | 本人创建/主讲的课程数据 |
| `STUDENT` | 学生 | 个人学习数据 + 加入的班级数据 |

**接口权限标注约定（本文档使用）：**

```
🔓 Public      无需登录
🔑 Logged      任意已登录用户
👨‍🏫 Teacher     需要 TEACHER 或更高角色
👨‍💼 DeptAdmin   需要 DEPT_ADMIN 或更高角色
🔐 SuperAdmin  需要 SUPER_ADMIN 角色
```

### 2.3 Token 刷新流程

```
客户端检测 Access Token 即将过期（剩余 <5 分钟）
  → POST /api/v1/auth/token/refresh
  → Body: { "refreshToken": "xxx" }
  → 返回新的 accessToken（原 refreshToken 保持不变直至过期）
```

### 2.4 网关限流规则

| 接口路径 | 限流策略 | 说明 |
|----------|----------|------|
| `POST /api/v1/interaction/attend` | 1500 req/s（令牌桶） | 签到高并发 |
| `POST /api/v1/exam/*/submit` | 500 req/s per publish | 交卷打散 |
| `POST /api/v1/auth/sms/send` | 1 req/min per phone | 防短信轰炸 |
| `GET /api/v1/ai/*/stream` | 100 concurrent connections | SSE 连接数 |
| 其他接口 | 200 req/s per user | 通用限流 |

---

## 3. 统一请求/响应规范

### 3.1 统一响应结构

**所有接口统一返回以下结构（HTTP Status 均为 200，业务错误在 code 中体现）：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": { },
  "timestamp": 1718500000000,
  "traceId": "3f9a1c2d-8b4e-4f6a-a1b2-c3d4e5f60001"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | integer | 业务状态码（见 §4） |
| `msg` | string | 状态描述（成功为 "ok"，失败为具体原因） |
| `data` | any | 响应数据体，失败时为 null |
| `timestamp` | integer | 服务端响应时间戳（毫秒） |
| `traceId` | string | 链路追踪 ID（SkyWalking），用于问题排查 |

### 3.2 分页请求规范

**分页请求参数（Query String）：**

```
page    integer  页码，从 1 开始，默认 1
size    integer  每页条数，默认 20，最大 100
sortBy  string   排序字段（可选）
sortDir string   排序方向：asc / desc，默认 desc
```

**分页响应结构（data 字段）：**

```json
{
  "list": [...],
  "total": 156,
  "page": 1,
  "size": 20,
  "pages": 8
}
```

### 3.3 日期时间格式

- 所有时间字段统一使用 **ISO 8601** 格式：`2026-06-16T14:30:00+08:00`
- 时区：**Asia/Shanghai（UTC+8）**
- 时间戳字段（如 `timestamp`）使用毫秒级 Unix 时间戳

### 3.4 SSE 流式响应规范

AI 接口使用 **Server-Sent Events（SSE）** 流式输出：

```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

data: {"type":"chunk","content":"这是第一个"}

data: {"type":"chunk","content":"文字片段"}

data: {"type":"done","content":"","totalTokens":256}
```

**SSE 事件类型：**

| type | 说明 |
|------|------|
| `chunk` | 文字片段（流式输出） |
| `done` | 输出完成，含 totalTokens |
| `error` | 生成出错，含 message |
| `filtered` | 内容被安全层过滤 |

---

## 4. 错误码规范

### 4.1 HTTP 层面状态码

| HTTP 状态码 | 使用场景 |
|-------------|----------|
| `200` | 所有正常响应（业务错误也用 200，通过 code 区分） |
| `400` | 请求参数格式错误（非业务错误，如 JSON 解析失败） |
| `401` | Token 无效或已过期（网关层拦截） |
| `403` | 权限不足（角色校验失败） |
| `429` | 触发限流 |
| `503` | 服务不可用（熔断降级） |

### 4.2 业务错误码规范

错误码格式：**`{模块码}{序号}`**，6 位数字。

```
200    通用成功
2000xx 通用错误（参数/系统）
2001xx 认证模块
2002xx 用户/组织模块
2003xx 课程/课堂模块
2004xx 互动教学模块
2005xx 在线测试模块
2006xx 成绩管理模块
2007xx AI 能力模块
2008xx 统计分析模块
2009xx 文件存储模块
2010xx 通知/推送模块
2011xx 教务对接模块
```

### 4.3 通用错误码表

| code | msg | 场景 |
|------|-----|------|
| `200` | ok | 成功 |
| `200001` | 参数校验失败 | @Valid 校验不通过，msg 含具体字段 |
| `200002` | 数据不存在 | 查询结果为空 |
| `200003` | 数据已存在 | 唯一键冲突 |
| `200004` | 操作不允许 | 业务状态不满足（如课堂未开始） |
| `200005` | 系统繁忙，请稍后重试 | 服务内部异常 |
| `200006` | 请求过于频繁 | 触发接口限流 |

### 4.4 认证模块错误码（2001xx）

| code | msg | 场景 |
|------|-----|------|
| `200101` | 手机号格式错误 | |
| `200102` | 验证码错误或已过期 | Redis TTL 失效 |
| `200103` | 账号已被禁用 | status = 0 |
| `200104` | Token 已过期，请重新登录 | JWT exp |
| `200105` | Token 无效 | 签名验证失败 |
| `200106` | 微信授权失败 | code 无效或过期 |
| `200107` | Refresh Token 已过期 | 需重新登录 |
| `200108` | 短信发送频繁，请 1 分钟后重试 | |

### 4.5 互动模块错误码（2004xx）

| code | msg | 场景 |
|------|-----|------|
| `200401` | 签到码已过期 | Redis TTL |
| `200402` | 签到码不正确 | |
| `200403` | 课堂未开始，无法签到 | lesson.status ≠ 1 |
| `200404` | 您已签到，请勿重复操作 | BloomFilter 命中 |

### 4.6 在线测试模块错误码（2005xx）

| code | msg | 场景 |
|------|-----|------|
| `200501` | 考试未开始或已结束 | |
| `200502` | 考试密码错误 | |
| `200503` | 答卷已提交，请勿重复操作 | 幂等键命中 |
| `200504` | 人脸核验未通过，请联系监考教师 | |
| `200505` | 切屏次数超限，本次考试已被标记 | |
| `200506` | 题目不在本试卷中 | |

### 4.7 AI 模块错误码（2007xx）

| code | msg | 场景 |
|------|-----|------|
| `200701` | AI 服务暂时不可用，请稍后重试 | LLM 接口超时/熔断 |
| `200702` | 输入内容包含不允许的词汇 | PromptSecurityFilter 拦截 |
| `200703` | AI 任务正在队列中处理，请等待通知 | Kafka 异步任务 |
| `200704` | 批改规则格式错误 | review_rule 解析失败 |

---

## 5. 共享 Schema 定义

> 以下 Schema 在多个接口中复用，采用 OpenAPI 3.0 `$ref` 引用风格。

### 5.1 PageQuery — 分页查询参数

```yaml
# components/parameters/PageQuery
- name: page
  in: query
  schema: { type: integer, default: 1, minimum: 1 }
- name: size
  in: query
  schema: { type: integer, default: 20, minimum: 1, maximum: 100 }
- name: sortBy
  in: query
  schema: { type: string }
- name: sortDir
  in: query
  schema: { type: string, enum: [asc, desc], default: desc }
```

### 5.2 PageResult — 分页响应包装

```yaml
# components/schemas/PageResult
type: object
properties:
  list:
    type: array
    items: {}          # 具体类型由各接口覆盖
  total:
    type: integer
    description: 总记录数
  page:
    type: integer
  size:
    type: integer
  pages:
    type: integer
    description: 总页数
```

### 5.3 UserBrief — 用户简要信息

```yaml
# components/schemas/UserBrief
type: object
properties:
  id:
    type: integer
    format: int64
  username:
    type: string
  realName:
    type: string
  userType:
    type: integer
    description: "1-学生 2-教师 3-院系管理员 4-校级管理员"
  avatarUrl:
    type: string
    nullable: true
  studentNo:
    type: string
    nullable: true
    description: 学生专有
```

### 5.4 LessonBrief — 课堂简要信息

```yaml
# components/schemas/LessonBrief
type: object
properties:
  id:
    type: integer
    format: int64
  title:
    type: string
    nullable: true
  status:
    type: integer
    description: "0-未开始 1-进行中 2-已结束"
  startTime:
    type: string
    format: date-time
    nullable: true
  className:
    type: string
  teacherName:
    type: string
  chapter:
    type: string
    nullable: true
```

### 5.5 QuestionBrief — 题目简要信息

```yaml
# components/schemas/QuestionBrief
type: object
properties:
  id:
    type: integer
    format: int64
  type:
    type: integer
    description: "1-单选 2-多选 3-判断 4-填空 5-主观 6-投票"
  content:
    type: string
    description: 题干（富文本HTML）
  score:
    type: number
    format: double
  difficulty:
    type: integer
    description: "1-极易 2-易 3-中 4-难 5-极难"
  options:
    type: array
    nullable: true
    items:
      $ref: '#/components/schemas/QuestionOption'

# components/schemas/QuestionOption
type: object
properties:
  id:
    type: integer
    format: int64
  optionLabel:
    type: string
    description: "A/B/C/D"
  content:
    type: string
  isCorrect:
    type: boolean
    description: 仅教师/批改时返回
```

---

## 6. 认证模块 — /api/v1/auth

> 下游服务：edu-auth（:8081）  
> 网关限流：短信发送 1次/min/手机号

### 6.1 发送短信验证码

**`POST /api/v1/auth/sms/send`** 🔓

发送登录/注册短信验证码，有效期 5 分钟，同一手机号 1 分钟内只能发送一次。

**Request Body：**

```yaml
content:
  application/json:
    schema:
      type: object
      required: [phone]
      properties:
        phone:
          type: string
          description: 手机号（11位）
          example: "13812345678"
        scene:
          type: string
          enum: [LOGIN, BIND]
          default: LOGIN
          description: "发送场景：LOGIN-登录 BIND-绑定手机号"
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "expireSeconds": 300
  }
}
```

**错误码：** `200101`（手机号格式错误）、`200108`（发送频繁）

---

### 6.2 手机号验证码登录

**`POST /api/v1/auth/login/phone`** 🔓

**Request Body：**

```yaml
content:
  application/json:
    schema:
      type: object
      required: [phone, code]
      properties:
        phone:
          type: string
          example: "13812345678"
        code:
          type: string
          description: 6位短信验证码
          example: "123456"
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
    "expiresIn": 7200,
    "userInfo": {
      "id": 12345,
      "username": "zhangsan",
      "realName": "张三",
      "userType": 2,
      "avatarUrl": "https://cdn.smu.edu.cn/avatar/12345.jpg",
      "roles": ["TEACHER"],
      "deptId": 3,
      "deptName": "计算机学院"
    }
  }
}
```

**错误码：** `200102`（验证码错误）、`200103`（账号禁用）

---

### 6.3 微信扫码登录

**`GET /api/v1/auth/login/wechat/callback`** 🔓

微信 OAuth2 回调，由微信服务器调用，返回登录凭证（前端通过轮询或 WebSocket 接收结果）。

**Query Parameters：**

```
code    string  required  微信授权 code
state   string  required  前端生成的随机 state，用于防 CSRF
```

**Response 200（与 6.2 相同结构）**

**错误码：** `200106`（微信授权失败）

---

### 6.4 刷新 Access Token

**`POST /api/v1/auth/token/refresh`** 🔓

**Request Body：**

```yaml
type: object
required: [refreshToken]
properties:
  refreshToken:
    type: string
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
    "expiresIn": 7200
  }
}
```

**错误码：** `200107`（Refresh Token 过期）

---

### 6.5 退出登录

**`POST /api/v1/auth/logout`** 🔑

将当前 Access Token 加入黑名单（Redis，TTL = Token 剩余有效期）。

**Response 200：**

```json
{ "code": 200, "msg": "ok", "data": null }
```

---

### 6.6 获取当前用户信息

**`GET /api/v1/auth/me`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "id": 12345,
    "username": "zhangsan",
    "realName": "张三",
    "userType": 2,
    "phone": "138****5678",
    "email": "zhangsan@smu.edu.cn",
    "avatarUrl": "https://cdn.smu.edu.cn/avatar/12345.jpg",
    "roles": ["TEACHER"],
    "deptId": 3,
    "deptName": "计算机学院",
    "wechatBound": true,
    "studentNo": null
  }
}
```

> **安全说明**：`phone` 字段应用层脱敏（显示 `138****5678`），`phone_cipher` 密文不出接口。

---

## 7. 用户与组织模块 — /api/v1/user

> 下游服务：edu-user（:8082）

### 7.1 查询用户列表

**`GET /api/v1/user/list`** 👨‍💼

**Query Parameters：**

```
keyword   string   模糊搜索（realName / username / studentNo，走 ES）
deptId    integer  按院系过滤（含子院系）
userType  integer  按用户类型过滤（1-学生 2-教师）
status    integer  账号状态（0-禁用 1-正常）
page      integer  默认 1
size      integer  默认 20
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "list": [
      {
        "id": 12345,
        "username": "zhangsan",
        "realName": "张三",
        "userType": 2,
        "phone": "138****5678",
        "deptId": 3,
        "deptName": "计算机学院",
        "status": 1,
        "lastLoginAt": "2026-06-15T09:30:00+08:00"
      }
    ],
    "total": 1580,
    "page": 1,
    "size": 20,
    "pages": 79
  }
}
```

---

### 7.2 获取用户详情

**`GET /api/v1/user/{userId}`** 🔑

**Path Parameters：** `userId` integer(int64)

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "id": 12345,
    "username": "zhangsan",
    "realName": "张三",
    "userType": 2,
    "phone": "138****5678",
    "email": "zhangsan@smu.edu.cn",
    "avatarUrl": "...",
    "studentNo": null,
    "deptId": 3,
    "deptName": "计算机学院",
    "roles": ["TEACHER"],
    "status": 1,
    "createdAt": "2024-09-01T00:00:00+08:00"
  }
}
```

---

### 7.3 创建用户（手动录入）

**`POST /api/v1/user`** 👨‍💼

> 通常用于手动补录；批量同步通过 edu-jwxt 教务对接完成。

**Request Body：**

```yaml
type: object
required: [username, realName, userType, deptId]
properties:
  username:
    type: string
    description: 用户名（唯一）
    minLength: 3
    maxLength: 50
  realName:
    type: string
    maxLength: 50
  phone:
    type: string
    description: 手机号（11位，应用层 AES-256 加密后存储）
  userType:
    type: integer
    description: "1-学生 2-教师 3-院系管理员"
  deptId:
    type: integer
    format: int64
  studentNo:
    type: string
    nullable: true
    description: 学号（学生必填）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": { "id": 12346 }
}
```

---

### 7.4 修改用户信息

**`PUT /api/v1/user/{userId}`** 🔑

> 普通用户只能修改自身非敏感字段（realName/email/avatarUrl）；管理员可修改角色/状态。

**Request Body（允许部分字段）：**

```yaml
type: object
properties:
  realName:  { type: string }
  email:     { type: string, format: email }
  avatarUrl: { type: string }
  status:
    type: integer
    description: "仅管理员可修改：0-禁用 1-正常"
  deptId:
    type: integer
    format: int64
    description: 仅管理员可修改
```

**Response 200：** `{ "code": 200, "msg": "ok", "data": null }`

---

### 7.5 分配用户角色

**`POST /api/v1/user/{userId}/roles`** 👨‍💼

**Request Body：**

```yaml
type: object
required: [roles]
properties:
  roles:
    type: array
    items:
      type: object
      properties:
        roleCode:
          type: string
          enum: [SUPER_ADMIN, DEPT_ADMIN, TEACHER, STUDENT]
        deptId:
          type: integer
          format: int64
          description: 院系管理员角色时必填
```

**Response 200：** `{ "code": 200, "msg": "ok", "data": null }`

---

### 7.6 查询院系树

**`GET /api/v1/user/dept/tree`** 🔑

返回完整的院系层级树（学校→学院→专业→行政班）。

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    {
      "id": 1,
      "deptCode": "SMU",
      "deptName": "山东管理学院",
      "deptType": 1,
      "level": 1,
      "children": [
        {
          "id": 2,
          "deptCode": "CS",
          "deptName": "计算机学院",
          "deptType": 2,
          "level": 2,
          "children": [...]
        }
      ]
    }
  ]
}
```

---

### 7.7 修改个人密码

**`PUT /api/v1/user/me/password`** 🔑

**Request Body：**

```yaml
type: object
required: [oldPassword, newPassword]
properties:
  oldPassword:
    type: string
    description: 原密码
  newPassword:
    type: string
    description: 新密码（8~20位，含数字+字母）
    minLength: 8
    maxLength: 20
```

---

### 7.8 绑定微信

**`POST /api/v1/user/me/wechat/bind`** 🔑

**Request Body：**

```yaml
type: object
required: [code]
properties:
  code:
    type: string
    description: 微信授权 code
```

**Response 200：** `{ "code": 200, "msg": "ok", "data": null }`

---

## 8. 课程与课堂模块 — /api/v1/course

> 下游服务：edu-course（:8083）

### 8.1 获取课程列表

**`GET /api/v1/course/list`** 🔑

**Query Parameters：**

```
semester  string   学期（如 2025-2026-1），默认当前学期
deptId    integer  按院系过滤
keyword   string   课程名称/编码模糊搜索
page      integer  默认 1
size      integer  默认 20
```

**Response 200（data 为 PageResult）：**

```json
{
  "list": [
    {
      "id": 101,
      "courseCode": "CS301",
      "courseName": "数据结构与算法",
      "deptName": "计算机学院",
      "credit": 3.0,
      "courseType": 1,
      "semester": "2025-2026-1",
      "classCount": 3
    }
  ],
  "total": 45,
  "page": 1,
  "size": 20,
  "pages": 3
}
```

---

### 8.2 创建课程

**`POST /api/v1/course`** 👨‍💼

**Request Body：**

```yaml
type: object
required: [courseCode, courseName, deptId, credit, semester]
properties:
  courseCode:   { type: string, maxLength: 30 }
  courseName:   { type: string, maxLength: 100 }
  deptId:       { type: integer, format: int64 }
  credit:       { type: number, format: double }
  courseType:   { type: integer, description: "1-必修 2-选修 3-实验 4-实践", default: 1 }
  semester:     { type: string, example: "2025-2026-1" }
  description:  { type: string, nullable: true }
```

**Response 200：** `{ "code": 200, "msg": "ok", "data": { "id": 101 } }`

---

### 8.3 获取我的教学班列表

**`GET /api/v1/course/class/my`** 🔑

教师获取自己主讲的教学班；学生获取已加入的教学班。

**Query Parameters：**

```
semester  string  学期，默认当前学期
status    integer 班级状态（0-已归档 1-进行中）
```

**Response 200（data 为数组）：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    {
      "id": 501,
      "courseName": "数据结构与算法",
      "courseCode": "CS301",
      "className": "CS301-01班",
      "classCode": "CS301-01-2526-1",
      "teacherName": "张三",
      "studentCount": 45,
      "semester": "2025-2026-1",
      "status": 1,
      "deptName": "计算机学院"
    }
  ]
}
```

---

### 8.4 创建教学班

**`POST /api/v1/course/class`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [courseId, className, classCode, semester]
properties:
  courseId:    { type: integer, format: int64 }
  className:   { type: string, maxLength: 100 }
  classCode:   { type: string, maxLength: 30 }
  semester:    { type: string }
  deptId:      { type: integer, format: int64 }
```

---

### 8.5 获取教学班详情

**`GET /api/v1/course/class/{classId}`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "id": 501,
    "courseName": "数据结构与算法",
    "className": "CS301-01班",
    "teacher": { "id": 12345, "realName": "张三", "avatarUrl": "..." },
    "studentCount": 45,
    "semester": "2025-2026-1",
    "status": 1,
    "students": [
      { "id": 20001, "realName": "李明", "studentNo": "2024001", "groupName": null }
    ]
  }
}
```

---

### 8.6 班级学生管理

**`POST /api/v1/course/class/{classId}/students`** 👨‍🏫

批量添加学生（导入名单）。

**Request Body：**

```yaml
type: object
required: [studentIds]
properties:
  studentIds:
    type: array
    items: { type: integer, format: int64 }
    description: 学生ID列表
  studentNos:
    type: array
    items: { type: string }
    description: 学号列表（与 studentIds 二选一）
```

**`DELETE /api/v1/course/class/{classId}/students/{studentId}`** 👨‍🏫

移除单个学生。

---

### 8.7 课堂管理

#### 8.7.1 开始课堂

**`POST /api/v1/course/lesson/start`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [classId]
properties:
  classId:
    type: integer
    format: int64
  materialId:
    type: integer
    format: int64
    nullable: true
    description: 课件ID（可选）
  title:
    type: string
    nullable: true
  chapter:
    type: string
    nullable: true
  liveMode:
    type: string
    enum: [SLIDE_ONLY, ONLINE_CLASS]
    default: SLIDE_ONLY
    description: "直播模式：SLIDE_ONLY-仅课件（默认，节省带宽） ONLINE_CLASS-线上直播"
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "status": 1,
    "liveMode": "SLIDE_ONLY",
    "wsEndpoint": "wss://api.smu.edu.cn/ws",
    "wsTopicPrefix": "/topic/lesson/8001"
  }
}
```

#### 8.7.2 结束课堂

**`POST /api/v1/course/lesson/{lessonId}/end`** 👨‍🏫

结束课堂并触发 AI 异步任务（报告、思维导图生成）。

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "status": 2,
    "durationMin": 45,
    "aiTaskTriggered": true,
    "message": "课堂已结束，AI报告正在生成中，完成后将通过通知推送给您"
  }
}
```

#### 8.7.3 获取课堂列表

**`GET /api/v1/course/lesson/list`** 🔑

**Query Parameters：**

```
classId   integer  required  教学班ID
status    integer  课堂状态（0-未开始 1-进行中 2-已结束）
page      integer
size      integer
```

**Response 200（data 为 PageResult<LessonBrief>）**

#### 8.7.4 获取课堂详情

**`GET /api/v1/course/lesson/{lessonId}`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "id": 8001,
    "classId": 501,
    "className": "CS301-01班",
    "teacherId": 12345,
    "teacherName": "张三",
    "title": "第3讲：树与二叉树",
    "chapter": "第三章",
    "status": 2,
    "liveMode": "SLIDE_ONLY",
    "startTime": "2026-06-16T08:00:00+08:00",
    "endTime":   "2026-06-16T09:45:00+08:00",
    "durationMin": 105,
    "material": {
      "id": 201,
      "title": "数据结构第三章课件",
      "pageCount": 32,
      "slideDir": "https://cdn.smu.edu.cn/slides/201/"
    },
    "currentSlide": 32,
    "replayUrl": "https://cdn.smu.edu.cn/replay/8001.m3u8",
    "replayVisible": true
  }
}
```

#### 8.7.5 切换课件页面

**`POST /api/v1/course/lesson/{lessonId}/slide`** 👨‍🏫

实时同步到所有学生端（通过 WebSocket 广播）。

**Request Body：**

```yaml
type: object
required: [pageNum]
properties:
  pageNum:
    type: integer
    minimum: 1
```

**Response 200：** `{ "code": 200, "msg": "ok", "data": { "pageNum": 5 } }`

---

### 8.8 课件管理

#### 8.8.1 上传课件

**`POST /api/v1/course/material/upload`** 👨‍🏫

采用预签名 URL 分步上传流程。

**Step 1：申请上传凭证**

**Request Body：**

```yaml
type: object
required: [fileName, fileType, fileSizeKb]
properties:
  fileName:
    type: string
    description: 原始文件名
  fileType:
    type: string
    enum: [pptx, pdf, docx, mp4]
  fileSizeKb:
    type: integer
    description: 文件大小（KB）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "uploadId": "upload-uuid-xxxx",
    "presignedUrl": "https://minio.smu.edu.cn/edu-files/xxx?X-Amz-Signature=...",
    "expiresIn": 3600,
    "objectPath": "materials/teacher-12345/20260616/filename.pptx"
  }
}
```

**Step 2：前端直接 PUT 到 presignedUrl（不经过后端）**

**Step 3：通知后端上传完成**

**`POST /api/v1/course/material/upload/complete`** 👨‍🏫

```yaml
type: object
required: [uploadId, title]
properties:
  uploadId:    { type: string }
  title:       { type: string }
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "materialId": 201,
    "status": 0,
    "message": "课件上传成功，正在转换为图片序列，请等待约2~5分钟"
  }
}
```

#### 8.8.2 获取课件列表

**`GET /api/v1/course/material/list`** 🔑

**Query Parameters：** `keyword` / `page` / `size`

---

## 9. 互动教学模块 — /api/v1/interaction

> 下游服务：edu-interaction（:8084）  
> **签到接口限流：1500 req/s（令牌桶），超出返回 HTTP 429**

### 9.1 签到相关

#### 9.1.1 教师创建签到

**`POST /api/v1/interaction/attendance/create`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [lessonId]
properties:
  lessonId:
    type: integer
    format: int64
  expireSeconds:
    type: integer
    default: 300
    description: 签到码有效期（秒），默认5分钟
  method:
    type: string
    enum: [QR, CODE, BOTH]
    default: BOTH
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "attendanceCodeId": 3001,
    "code": "XK8B",
    "qrToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "qrContent": "https://api.smu.edu.cn/api/v1/interaction/attend/qr?token=a1b2c3d4...",
    "expireAt": "2026-06-16T08:10:00+08:00",
    "expireSeconds": 300
  }
}
```

#### 9.1.2 学生提交签到

**`POST /api/v1/interaction/attend`** 🔑

> **高并发接口**：先写 Redis 布隆过滤器去重 + List 队列，异步批量落库。响应毫秒级。

**Request Body：**

```yaml
type: object
properties:
  lessonId:
    type: integer
    format: int64
    description: 课堂ID（扫码时从 QR Content 解析，口令时手动关联）
  code:
    type: string
    nullable: true
    description: 口令（CODE/BOTH方式）
  qrToken:
    type: string
    nullable: true
    description: 二维码 Token（QR/BOTH方式）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "status": "SUCCESS",
    "message": "签到成功",
    "attendedAt": "2026-06-16T08:03:15+08:00"
  }
}
```

**错误码：** `200401`（签到码过期）、`200402`（签到码错误）、`200403`（课堂未开始）、`200404`（已签到）

#### 9.1.3 获取签到状态列表（教师端）

**`GET /api/v1/interaction/attendance/list`** 👨‍🏫

**Query Parameters：**

```
lessonId  integer  required
status    integer  考勤状态过滤（0-缺勤 1-正常 2-迟到 3-请假）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "totalStudents": 45,
    "attendedCount": 42,
    "absentCount": 3,
    "attendRate": 93.33,
    "list": [
      {
        "studentId": 20001,
        "realName": "李明",
        "studentNo": "2024001",
        "status": 1,
        "attendedAt": "2026-06-16T08:03:15+08:00",
        "method": "QR",
        "isModified": false
      }
    ]
  }
}
```

#### 9.1.4 教师修改考勤

**`PUT /api/v1/interaction/attendance/{attendanceId}`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [status]
properties:
  status:
    type: integer
    description: "0-缺勤 1-正常 2-迟到 3-请假"
```

---

### 9.2 弹幕相关

#### 9.2.1 发送弹幕

**`POST /api/v1/interaction/barrage`** 🔑

> 弹幕同时通过 WebSocket 广播（延迟更低），REST 接口用于记录存档。

**Request Body：**

```yaml
type: object
required: [lessonId, content]
properties:
  lessonId:
    type: integer
    format: int64
  content:
    type: string
    maxLength: 200
  style:
    type: string
    enum: [roll, top, bottom]
    default: roll
```

**Response 200：** `{ "code": 200, "msg": "ok", "data": { "barrageId": 50001 } }`

#### 9.2.2 获取弹幕列表

**`GET /api/v1/interaction/barrage/list`** 👨‍🏫

**Query Parameters：** `lessonId` (required) / `page` / `size`

---

### 9.3 随机点名

**`POST /api/v1/interaction/random-call`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [lessonId]
properties:
  lessonId:
    type: integer
    format: int64
  count:
    type: integer
    default: 1
    minimum: 1
    maximum: 10
    description: 点名人数
  style:
    type: string
    enum: [random, spotlight, racing]
    default: random
    description: 展示样式（WebSocket广播到全班）
  excludeAbsent:
    type: boolean
    default: true
    description: 是否排除未签到学生
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "callId": 7001,
    "selected": [
      { "id": 20003, "realName": "王芳", "studentNo": "2024003", "avatarUrl": "..." }
    ],
    "style": "spotlight",
    "broadcastSent": true
  }
}
```

---

### 9.4 课件反馈

**`POST /api/v1/interaction/slide-feedback`** 🔑

**Request Body：**

```yaml
type: object
required: [lessonId, slidePage, keyword, feedbackType]
properties:
  lessonId:     { type: integer, format: int64 }
  slidePage:    { type: integer, minimum: 1 }
  keyword:      { type: string, maxLength: 100 }
  feedbackType: { type: integer, description: "1-疑问 2-关键词 3-重点" }
```

**`GET /api/v1/interaction/slide-feedback/stat`** 👨‍🏫

按页码统计反馈数量，用于教师大屏展示热点页面。

**Query Parameters：** `lessonId` (required)

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    { "slidePage": 5, "totalCount": 12, "questionCount": 8, "keywordCount": 4 },
    { "slidePage": 12, "totalCount": 7, "questionCount": 5, "keywordCount": 2 }
  ]
}
```

---

### 9.5 课堂积分

**`POST /api/v1/interaction/score`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [lessonId, studentId, score]
properties:
  lessonId:   { type: integer, format: int64 }
  studentId:  { type: integer, format: int64 }
  score:      { type: number, format: double, minimum: 0, maximum: 100 }
  reason:     { type: string, maxLength: 200, nullable: true }
```

---

### 9.6 课堂报告

**`GET /api/v1/interaction/lesson/{lessonId}/report`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "attendCount": 42,
    "absentCount": 3,
    "attendRate": 93.33,
    "interactCount": 156,
    "quizCount": 3,
    "durationMin": 105,
    "slideCount": 32,
    "genStatus": 2,
    "aiSummary": "本次课堂主要讲解了树的基本概念、二叉树的定义及遍历算法...",
    "aiMindmapJson": { "title": "树与二叉树", "children": [...] },
    "mindmapVisible": true,
    "summaryVisible": true
  }
}
```

**`PUT /api/v1/interaction/lesson/{lessonId}/report/visibility`** 👨‍🏫

控制 AI 报告内容对学生的可见性。

**Request Body：**

```yaml
type: object
properties:
  mindmapVisible: { type: boolean }
  summaryVisible: { type: boolean }
```

---

## 10. 题库模块 — /api/v1/question

> 下游服务：edu-exam（:8085，题库与测试共用服务）

### 10.1 题库管理

#### 10.1.1 获取题库列表

**`GET /api/v1/question/bank/list`** 👨‍🏫

**Query Parameters：** `keyword` / `isPublic` (boolean) / `deptId` / `page` / `size`

**Response 200（data 为 PageResult）：**

```json
{
  "list": [
    {
      "id": 101,
      "bankName": "数据结构题库",
      "description": "树、图、排序算法",
      "questionCount": 256,
      "isPublic": false,
      "creatorName": "张三",
      "createdAt": "2025-09-01T00:00:00+08:00"
    }
  ],
  "total": 8
}
```

#### 10.1.2 创建题库

**`POST /api/v1/question/bank`** 👨‍🏫

```yaml
type: object
required: [bankName]
properties:
  bankName:    { type: string, maxLength: 100 }
  description: { type: string, maxLength: 500, nullable: true }
  isPublic:    { type: boolean, default: false }
  deptId:      { type: integer, format: int64, nullable: true }
```

---

### 10.2 题目管理

#### 10.2.1 获取题目列表

**`GET /api/v1/question/list`** 👨‍🏫

**Query Parameters：**

```
bankId      integer   题库ID
type        integer   题型（1-6）
difficulty  integer   难度（1-5）
keyword     string    全文检索（走 ES，支持题干+解析）
page        integer
size        integer
```

**Response 200（data 为 PageResult<QuestionBrief>）**

#### 10.2.2 创建题目

**`POST /api/v1/question`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [bankId, type, content]
properties:
  bankId:
    type: integer
    format: int64
  type:
    type: integer
    description: "1-单选 2-多选 3-判断 4-填空 5-主观 6-投票"
  content:
    type: string
    description: 题干（富文本HTML）
  answer:
    type: string
    nullable: true
    description: "标准答案（客观题如 'A,C'；主观题参考答案）"
  analysis:
    type: string
    nullable: true
    description: 答案解析（富文本HTML）
  score:
    type: number
    format: double
    default: 0
  difficulty:
    type: integer
    minimum: 1
    maximum: 5
    default: 3
  reviewRule:
    type: string
    nullable: true
    description: AI批改规则（主观题，将经过 PromptSecurityFilter 处理）
  options:
    type: array
    nullable: true
    description: 选项列表（单选/多选/投票题必填）
    items:
      type: object
      required: [optionLabel, content]
      properties:
        optionLabel: { type: string, description: "A/B/C/D" }
        content:     { type: string }
        isCorrect:   { type: boolean, default: false }
```

**Response 200：** `{ "code": 200, "msg": "ok", "data": { "id": 5001 } }`

#### 10.2.3 修改题目

**`PUT /api/v1/question/{questionId}`** 👨‍🏫

（字段与创建相同，允许部分更新）

#### 10.2.4 删除题目（软删除）

**`DELETE /api/v1/question/{questionId}`** 👨‍🏫

#### 10.2.5 批量导入题目（docx）

**`POST /api/v1/question/import`** 👨‍🏫

**Request Body（multipart/form-data）：**

```
bankId    integer    required  目标题库ID
file      file       required  .docx 文件（≤10MB，Apache POI 解析）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "total": 50,
    "success": 48,
    "failed": 2,
    "failedReasons": ["第12题：选项格式错误", "第31题：未识别题型"]
  }
}
```

#### 10.2.6 一键 AI 出题

**`POST /api/v1/question/ai-generate`** 👨‍🏫

基于课件内容或指定主题让 AI 自动生成题目（异步，走 Kafka）。

**Request Body：**

```yaml
type: object
required: [bankId, topic]
properties:
  bankId:
    type: integer
    format: int64
  materialId:
    type: integer
    format: int64
    nullable: true
    description: 课件ID（若提供则基于课件内容出题）
  topic:
    type: string
    description: 出题主题描述
  count:
    type: integer
    default: 10
    minimum: 1
    maximum: 30
  typeDistribution:
    type: object
    description: 题型分布
    properties:
      singleChoice: { type: integer, default: 5 }
      multiChoice:  { type: integer, default: 3 }
      subjective:   { type: integer, default: 2 }
  difficulty:
    type: integer
    minimum: 1
    maximum: 5
    default: 3
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "taskId": "ai-gen-uuid-xxxx",
    "status": "QUEUED",
    "message": "AI出题任务已提交，完成后通过通知推送给您（预计1~3分钟）"
  }
}
```

**错误码：** `200703`（任务正在队列中）

---

## 11. 在线测试模块 — /api/v1/exam

> 下游服务：edu-exam（:8085）  
> **交卷接口限流：500 req/s per publish，超出 HTTP 429**

### 11.1 试卷管理

#### 11.1.1 创建试卷

**`POST /api/v1/exam/paper`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [title]
properties:
  title:
    type: string
    maxLength: 200
  totalScore:
    type: number
    format: double
    default: 100
  isRandom:
    type: boolean
    default: false
    description: 是否随机组卷
  paperType:
    type: string
    enum: [A, B, C]
    default: A
  description:
    type: string
    nullable: true
```

#### 11.1.2 向试卷添加题目

**`POST /api/v1/exam/paper/{paperId}/questions`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [questions]
properties:
  questions:
    type: array
    items:
      type: object
      required: [questionId, score]
      properties:
        questionId:  { type: integer, format: int64 }
        score:       { type: number, format: double }
        sortOrder:   { type: integer }
        paperGroup:  { type: string, enum: [A, B, C], default: A }
        section:     { type: string, nullable: true, description: "大题标题" }
```

#### 11.1.3 获取试卷详情（含题目）

**`GET /api/v1/exam/paper/{paperId}`** 👨‍🏫

题目含正确答案（仅教师可见）。

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "id": 301,
    "title": "数据结构期中测验",
    "totalScore": 100,
    "questionCount": 20,
    "questions": [
      {
        "id": 5001,
        "type": 1,
        "content": "<p>以下哪种数据结构适合实现递归？</p>",
        "score": 5,
        "options": [
          { "optionLabel": "A", "content": "队列", "isCorrect": false },
          { "optionLabel": "B", "content": "栈", "isCorrect": true }
        ],
        "section": "一、单选题",
        "sortOrder": 1
      }
    ]
  }
}
```

---

### 11.2 考试发布

#### 11.2.1 发布考试

**`POST /api/v1/exam/publish`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [paperId, classId, startTime, endTime, durationMin]
properties:
  paperId:
    type: integer
    format: int64
  classId:
    type: integer
    format: int64
  startTime:
    type: string
    format: date-time
  endTime:
    type: string
    format: date-time
  durationMin:
    type: integer
    description: 考试时长（分钟）
    minimum: 1
    maximum: 360
  password:
    type: string
    nullable: true
    description: 防泄题密码（明文，服务端 BCrypt 加密存储）
  enableMonitor:
    type: boolean
    default: false
  faceVerifyType:
    type: integer
    enum: [0, 1, 2]
    description: "0-不核验 1-证件照核验 2-现场拍照"
    default: 0
  answerShowAt:
    type: string
    format: date-time
    nullable: true
  shuffleQuestion:
    type: boolean
    default: false
  shuffleOption:
    type: boolean
    default: false
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": { "publishId": 401 }
}
```

#### 11.2.2 获取发布列表

**`GET /api/v1/exam/publish/list`** 👨‍🏫

**Query Parameters：** `classId` / `status` / `page` / `size`

---

### 11.3 学生考试流程

#### 11.3.1 获取我的考试列表

**`GET /api/v1/exam/publish/my`** 🔑

学生获取待参加/进行中/已完成的考试列表。

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    {
      "publishId": 401,
      "title": "数据结构期中测验",
      "className": "CS301-01班",
      "startTime": "2026-06-20T09:00:00+08:00",
      "endTime":   "2026-06-20T10:30:00+08:00",
      "durationMin": 90,
      "status": 0,
      "submitStatus": null,
      "myScore": null
    }
  ]
}
```

#### 11.3.2 进入考试（获取试题）

**`POST /api/v1/exam/publish/{publishId}/enter`** 🔑

验证身份、人脸核验（若开启）、返回加密试题（题目分批下发，不含答案）。

**Request Body：**

```yaml
type: object
properties:
  password:
    type: string
    nullable: true
    description: 防泄题密码（若试卷设置了密码）
  faceImageBase64:
    type: string
    nullable: true
    description: 人脸核验照片（faceVerifyType > 0 时必填，Base64 编码）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "publishId": 401,
    "title": "数据结构期中测验",
    "durationMin": 90,
    "serverTime": "2026-06-20T09:00:05+08:00",
    "endTime": "2026-06-20T10:30:00+08:00",
    "faceVerifyPassed": true,
    "faceVerifyScore": 96.5,
    "watermark": "李明 2024001",
    "questions": [
      {
        "id": 5001,
        "type": 1,
        "content": "<p>以下哪种数据结构适合实现递归？</p>",
        "score": 5,
        "options": [
          { "optionLabel": "A", "content": "队列" },
          { "optionLabel": "B", "content": "栈" }
        ],
        "section": "一、单选题",
        "sortOrder": 1
      }
    ]
  }
}
```

**错误码：** `200502`（密码错误）、`200504`（人脸核验不通过）

#### 11.3.3 提交答卷

**`POST /api/v1/exam/publish/{publishId}/submit`** 🔑

> **高并发接口**：先写 Redis 幂等键 + 暂存答案，发 Kafka 消息，毫秒级响应。  
> 前端须在倒计时 30 秒内按学号取模打散提交时间（见 CLAUDE.md §8.2）。

**Request Body：**

```yaml
type: object
required: [answers]
properties:
  answers:
    type: array
    items:
      type: object
      required: [questionId, answerContent]
      properties:
        questionId:
          type: integer
          format: int64
        answerContent:
          type: string
          nullable: true
          description: "客观题：选项标签如 'A' 或 'A,C'；主观题：文本或图片路径JSON"
  submitType:
    type: string
    enum: [MANUAL, AUTO, FORCE]
    default: MANUAL
    description: "MANUAL-主动交卷 AUTO-倒计时自动 FORCE-教师强制"
  clientSubmitAt:
    type: string
    format: date-time
    description: 客户端提交时间（用于记录打散延迟）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "status": "QUEUED",
    "message": "答卷已提交，系统正在处理中",
    "submitTime": "2026-06-20T10:28:45+08:00"
  }
}
```

**错误码：** `200503`（已提交，勿重复操作）

---

### 11.4 监考接口

#### 11.4.1 上报监考异常事件

**`POST /api/v1/exam/monitor/event`** 🔑

**Request Body：**

```yaml
type: object
required: [publishId, eventType]
properties:
  publishId:
    type: integer
    format: int64
  eventType:
    type: string
    enum: [TAB_SWITCH, SCREENSHOT, COPY, FACE_FAIL]
    description: 异常类型
  snapshotBase64:
    type: string
    nullable: true
    description: 截图存证（Base64，自动上传MinIO）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "tabSwitchCount": 3,
    "warningLevel": "MEDIUM",
    "message": null
  }
}
```

#### 11.4.2 心跳上报

**`POST /api/v1/exam/monitor/heartbeat`** 🔑

前端每 30 秒调用，维持 `session_status = ANSWERING`。

**Request Body：**

```yaml
type: object
required: [publishId]
properties:
  publishId: { type: integer, format: int64 }
```

#### 11.4.3 获取监考状态列表（教师）

**`GET /api/v1/exam/monitor/list`** 👨‍🏫

**Query Parameters：** `publishId` (required) / `sessionStatus` / `abnormalFlag`

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    {
      "studentId": 20001,
      "realName": "李明",
      "studentNo": "2024001",
      "sessionStatus": "ANSWERING",
      "lastHeartbeatAt": "2026-06-20T09:35:20+08:00",
      "faceVerifyPassed": true,
      "tabSwitchCount": 1,
      "abnormalFlag": false,
      "submitTime": null
    }
  ]
}
```

---

### 11.5 阅卷接口

#### 11.5.1 获取待批改答卷列表（教师）

**`GET /api/v1/exam/review/list`** 👨‍🏫

**Query Parameters：** `publishId` (required) / `reviewStatus` (0-未批改 1-AI已批 2-教师已批) / `questionId` / `page` / `size`

#### 11.5.2 教师人工批改

**`PUT /api/v1/exam/review/{answerId}`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [score]
properties:
  score:
    type: number
    format: double
    description: 最终得分
  comment:
    type: string
    nullable: true
    description: 批注内容
```

---

## 12. 成绩管理模块 — /api/v1/grade

> 下游服务：edu-grade（:8086）

### 12.1 配置评分规则

**`PUT /api/v1/grade/rule/{classId}`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [attendWeight, quizWeight, interactionWeight, examWeight, offlineWeight]
properties:
  attendWeight:
    type: number
    format: double
    description: 考勤权重（%）
  quizWeight:
    type: number
    format: double
  interactionWeight:
    type: number
    format: double
  examWeight:
    type: number
    format: double
  offlineWeight:
    type: number
    format: double
```

> 应用层校验：五项权重之和必须等于 100.00

**Response 200：** `{ "code": 200, "msg": "ok", "data": null }`

---

### 12.2 获取班级成绩列表

**`GET /api/v1/grade/class/{classId}`** 👨‍🏫

**Query Parameters：** `sortBy` (totalScore/attendScore) / `sortDir` / `page` / `size`

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "classId": 501,
    "className": "CS301-01班",
    "gradeRule": {
      "attendWeight": 20,
      "quizWeight": 20,
      "interactionWeight": 10,
      "examWeight": 40,
      "offlineWeight": 10
    },
    "list": [
      {
        "studentId": 20001,
        "realName": "李明",
        "studentNo": "2024001",
        "totalScore": 88.50,
        "attendScore": 18.00,
        "quizScore": 19.50,
        "interactionScore": 9.00,
        "examScore": 35.00,
        "offlineScore": 7.00,
        "calcStatus": 1
      }
    ],
    "total": 45
  }
}
```

---

### 12.3 获取学生个人成绩

**`GET /api/v1/grade/student/{classId}`** 🔑

学生查看自己的成绩（需成绩发布后可见）；教师可查任意学生。

---

### 12.4 导入线下成绩

**`POST /api/v1/grade/offline/import`** 👨‍🏫

**Request Body（multipart/form-data）：**

```
classId   integer  required
file      file     required  .xlsx 文件（含学号+分数列）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "total": 45,
    "success": 44,
    "failed": 1,
    "failedReasons": ["学号 2024099 在本班级未找到"]
  }
}
```

---

### 12.5 触发成绩计算

**`POST /api/v1/grade/calculate/{classId}`** 👨‍🏫

手动触发成绩重新计算（通常由定时任务自动执行）。

---

### 12.6 导出成绩单

**`GET /api/v1/grade/export/{classId}`** 👨‍🏫

**Query Parameters：**

```
format  string  导出格式：xlsx（默认）/ zhengfang / qiangzhi
```

**Response：** `application/octet-stream`，文件下载

---

### 12.7 成绩回传教务系统

**`POST /api/v1/grade/push/{classId}`** 👨‍💼

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "pushCount": 45,
    "status": "SUCCESS",
    "jwxtResponse": "回传成功"
  }
}
```

---

## 13. AI 能力模块 — /api/v1/ai

> 下游服务：edu-ai（:8087）  
> **所有请求强制经过 PromptSecurityFilter**  
> **所有生成任务异步化：发 Kafka → Kafka Consumer 处理 → WebSocket 通知完成**  
> SSE 接口：网关关闭全缓冲，流式转发

### 13.1 AI 对话任务

#### 13.1.1 创建对话任务

**`POST /api/v1/ai/dialogue/task`** 👨‍🏫

**Request Body：**

```yaml
type: object
required: [lessonId, topic]
properties:
  lessonId:
    type: integer
    format: int64
  topic:
    type: string
    description: 对话主题
    maxLength: 200
  opening:
    type: string
    description: 开场白（AI对学生说的第一句话）
    maxLength: 500
  maxTurns:
    type: integer
    default: 5
    description: 最大对话轮次（0=不限）
  modelType:
    type: string
    enum: [ANALYSIS, GENERATION, REVIEW]
    default: ANALYSIS
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "sessionId": "sess-uuid-xxxx",
    "topic": "面向对象三大特性的理解",
    "opening": "请用自己的话解释什么是多态，并举一个生活中的例子...",
    "maxTurns": 5
  }
}
```

#### 13.1.2 学生发送消息（SSE 流式）

**`POST /api/v1/ai/dialogue/{sessionId}/message`** 🔑

返回 `text/event-stream`，SSE 流式输出 AI 回复。

**Request Body：**

```yaml
type: object
required: [content]
properties:
  content:
    type: string
    description: 学生输入内容
    maxLength: 2000
```

**Response（SSE 流）：**

```
Content-Type: text/event-stream

data: {"type":"chunk","content":"多态是指"}

data: {"type":"chunk","content":"同一接口在不同对象上"}

data: {"type":"chunk","content":"表现出不同的行为..."}

data: {"type":"done","content":"","totalTokens":128,"turnNo":2}
```

**错误码：** `200702`（内容被安全层过滤）

#### 13.1.3 获取对话历史

**`GET /api/v1/ai/dialogue/{sessionId}/history`** 🔑

返回指定学生与 AI 的全部对话记录（教师可查任意学生）。

---

### 13.2 AI 智能批改

#### 13.2.1 提交 AI 批改任务

**`POST /api/v1/ai/review/submit`** 👨‍🏫

批量提交主观题待批改队列（Kafka 异步处理）。

**Request Body：**

```yaml
type: object
required: [publishId]
properties:
  publishId:
    type: integer
    format: int64
  questionIds:
    type: array
    items: { type: integer, format: int64 }
    nullable: true
    description: 指定题目（null=批改所有主观题）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "taskId": "ai-review-uuid-xxxx",
    "pendingCount": 45,
    "message": "AI批改任务已提交，预计3~5分钟完成"
  }
}
```

#### 13.2.2 获取 AI 批改结果

**`GET /api/v1/ai/review/{answerId}`** 👨‍🏫

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "answerId": 60001,
    "score": 7.5,
    "comment": "基本概念理解正确，但举例不够具体",
    "errorReason": "未能区分多态的两种实现方式（重载与覆盖）",
    "improvement": "建议复习《设计模式》第3章，重点关注里氏替换原则",
    "rawLlmResponse": "...",
    "modelName": "ernie-4.0",
    "reviewDurationMs": 2340
  }
}
```

---

### 13.3 思维导图

**`GET /api/v1/ai/mindmap/{lessonId}`** 🔑

获取课堂 AI 生成的思维导图。

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "genStatus": "DONE",
    "markmapJson": {
      "title": "树与二叉树",
      "children": [
        {
          "content": "树的基本概念",
          "children": [
            { "content": "节点" },
            { "content": "度" },
            { "content": "深度/高度" }
          ]
        },
        {
          "content": "二叉树",
          "children": [
            { "content": "定义" },
            { "content": "遍历：前/中/后序" },
            { "content": "完全二叉树" }
          ]
        }
      ]
    },
    "studentVisible": true
  }
}
```

**`POST /api/v1/ai/mindmap/{lessonId}/regenerate`** 👨‍🏫

重新生成思维导图（异步任务）。

**`PUT /api/v1/ai/mindmap/{lessonId}`** 👨‍🏫

教师编辑思维导图内容。

---

### 13.4 分组讨论 AI 汇总

**`GET /api/v1/ai/discussion/{lessonId}/group/{groupId}`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "groupId": 201,
    "topic": "分析快速排序的时间复杂度",
    "genStatus": "DONE",
    "aiAnalysis": {
      "summary": "本组讨论较为活跃，集中围绕最坏情况O(n²)展开分析...",
      "activeStudents": [20001, 20003],
      "effectiveParticipants": [20001, 20002, 20003],
      "keyViewpoints": [
        "平均情况O(nlogn)成立的前提是随机选取基准",
        "最坏情况发生在序列已有序时"
      ],
      "qualityScore": 8.5
    }
  }
}
```

---

### 13.5 汇报点评

**`GET /api/v1/ai/presentation/{lessonId}/{groupId}`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "presenterIds": [20001, 20002],
    "durationSec": 480,
    "transcript": "同学们好，我们组今天汇报的主题是...",
    "aiReview": {
      "overallScore": 82,
      "dimensions": {
        "contentQuality": 8.5,
        "expressionClarity": 7.8,
        "logicStructure": 8.0,
        "timeManagement": 8.2
      },
      "strengths": ["内容充实，覆盖了核心知识点", "语速适中，表达清晰"],
      "suggestions": ["建议增加图表辅助说明", "结论部分可再精炼"],
      "detailedComment": "整体汇报质量较高，逻辑结构清晰..."
    }
  }
}
```

---

### 13.6 课堂 ASR 转写

**`GET /api/v1/ai/transcript/{lessonId}`** 👨‍🏫

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "genStatus": "DONE",
    "fullTranscript": "同学们好，今天我们来学习树这种数据结构...",
    "aiSummary": "本次课堂主要介绍了树的基本概念、二叉树定义...",
    "keyPoints": ["树的定义与术语", "二叉树遍历算法", "完全二叉树性质"],
    "chunks": [
      { "seq": 1, "startMs": 0, "endMs": 300000, "text": "同学们好...", "confidence": 0.96 }
    ]
  }
}
```

---

## 14. 统计分析模块 — /api/v1/stat

> 下游服务：edu-stat（:8088）  
> 实时数据读 Redis（TTL 5min 刷新），历史数据查 ClickHouse

### 14.1 大屏实时数据

**`GET /api/v1/stat/realtime/overview`** 👨‍💼

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "refreshedAt": "2026-06-16T10:00:05+08:00",
    "activeLessonCount": 23,
    "onlineStudentCount": 1580,
    "onlineTeacherCount": 23,
    "todayLessonCount": 48,
    "todayAttendCount": 2180,
    "todayQuizCount": 156,
    "deptRanking": [
      { "deptId": 2, "deptName": "计算机学院", "activeCount": 8 }
    ]
  }
}
```

---

### 14.2 课堂实时统计

**`GET /api/v1/stat/realtime/lesson/{lessonId}`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "attendedCount": 42,
    "totalStudents": 45,
    "attendRate": 93.33,
    "barrageCount": 28,
    "feedbackPages": [5, 12, 18],
    "quizPublished": 2,
    "durationMin": 35
  }
}
```

---

### 14.3 历史统计查询

#### 14.3.1 班级历史统计

**`GET /api/v1/stat/history/class/{classId}`** 👨‍🏫

**Query Parameters：**

```
startDate  string  date，开始日期
endDate    string  date，结束日期
```

**Response 200（data 含时间序列数组，适合前端折线图）：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "classId": 501,
    "className": "CS301-01班",
    "dateRange": ["2026-04-01", "2026-06-16"],
    "summary": {
      "totalLessons": 32,
      "avgAttendRate": 91.5,
      "totalInteractions": 2048
    },
    "series": [
      {
        "date": "2026-06-16",
        "lessonCount": 1,
        "attendRate": 93.33,
        "interactCount": 156
      }
    ]
  }
}
```

#### 14.3.2 院系统计

**`GET /api/v1/stat/history/dept/{deptId}`** 👨‍💼

**Query Parameters：** `startDate` / `endDate` / `granularity`（day/week/month）

---

### 14.4 预警列表

**`GET /api/v1/stat/warn/list`** 👨‍💼

**Query Parameters：**

```
warnType  string  ATTENDANCE_LOW/ZERO_ACTIVE/STUDENT_ABSENCE
status    integer 0-未处理 1-已处理
page      integer
size      integer
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "list": [
      {
        "id": 9001,
        "warnType": "ATTENDANCE_LOW",
        "targetName": "CS301-01班",
        "description": "近7日平均到课率 58%，低于阈值 75%",
        "occurredAt": "2026-06-16T06:00:00+08:00",
        "status": 0
      }
    ],
    "total": 5
  }
}
```

---

## 15. 文件存储模块 — /api/v1/file

> 下游服务：edu-file（:8089）

### 15.1 申请上传预签名 URL

**`POST /api/v1/file/presign`** 🔑

**Request Body：**

```yaml
type: object
required: [fileName, fileType, fileSizeKb, bizType]
properties:
  fileName:
    type: string
  fileType:
    type: string
    description: MIME 类型
  fileSizeKb:
    type: integer
    description: 文件大小（KB）
  bizType:
    type: string
    enum: [SLIDE, VIDEO, AUDIO, EXAM_ATTACH, ARCHIVE_PHOTO]
    description: 业务类型（影响生命周期策略）
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "uploadId": "upload-uuid-xxxx",
    "presignedUrl": "https://minio.smu.edu.cn/edu-files/...?X-Amz-Signature=...",
    "objectPath": "exam-attach/2026/06/16/uuid.jpg",
    "expiresIn": 3600
  }
}
```

---

### 15.2 通知上传完成

**`POST /api/v1/file/upload/complete`** 🔑

**Request Body：**

```yaml
type: object
required: [uploadId]
properties:
  uploadId:  { type: string }
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "fileId": 70001,
    "accessUrl": "https://cdn.smu.edu.cn/exam-attach/2026/06/16/uuid.jpg",
    "auditStatus": 1
  }
}
```

---

### 15.3 获取文件访问 URL

**`GET /api/v1/file/{fileId}/url`** 🔑

返回带签名的临时访问 URL（有效期 1 小时），防止资源盗链。

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "fileId": 70001,
    "url": "https://cdn.smu.edu.cn/.../uuid.jpg?token=xxx&expires=1718507200",
    "expiresAt": "2026-06-16T15:00:00+08:00"
  }
}
```

---

## 16. 直播推流模块 — /api/v1/live

> 下游服务：edu-live（:8091）  
> **线下课堂默认 SLIDE_ONLY，禁止默认开启 WebRTC（CLAUDE.md §8.5）**

### 16.1 创建/配置直播

**`POST /api/v1/live/start`** 👨‍🏫

由 `POST /api/v1/course/lesson/start`（liveMode=ONLINE_CLASS）内部调用，也可直接调用。

**Request Body：**

```yaml
type: object
required: [lessonId, liveMode]
properties:
  lessonId:
    type: integer
    format: int64
  liveMode:
    type: string
    enum: [SLIDE_ONLY, ONLINE_CLASS]
```

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "liveMode": "ONLINE_CLASS",
    "webrtcEnabled": true,
    "rtmpEnabled": true,
    "rtmpPushUrl": "rtmp://rtmp.smu.edu.cn/live/lesson-8001?key=xxx",
    "hlsPlayUrl": "https://cdn.smu.edu.cn/hls/lesson-8001.m3u8",
    "webrtcOffer": "..."
  }
}
```

---

### 16.2 结束直播

**`POST /api/v1/live/{lessonId}/stop`** 👨‍🏫

停止推流并触发录制文件上传 MinIO，生成回放 URL。

---

### 16.3 获取直播回放

**`GET /api/v1/live/{lessonId}/replay`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "lessonId": 8001,
    "replayUrl": "https://cdn.smu.edu.cn/replay/lesson-8001.m3u8",
    "durationSec": 6300,
    "fileSize": "2.3GB",
    "storageClass": "HOT",
    "visible": true
  }
}
```

---

## 17. 消息通知模块 — /api/v1/notify

> 下游服务：edu-notify（:8090）

### 17.1 发送通知公告

**`POST /api/v1/notify/notice`** 👨‍💼

**Request Body：**

```yaml
type: object
required: [title, content, scope]
properties:
  title:
    type: string
    maxLength: 200
  content:
    type: string
    description: 富文本 HTML
  scope:
    type: string
    enum: [SCHOOL, DEPT, CLASS]
  deptId:
    type: integer
    format: int64
    nullable: true
    description: scope=DEPT 时必填
  targetRoles:
    type: string
    enum: [ALL, TEACHER, STUDENT]
    default: ALL
  needReview:
    type: boolean
    default: false
```

---

### 17.2 获取通知列表

**`GET /api/v1/notify/notice/list`** 🔑

**Query Parameters：** `scope` / `status` / `page` / `size`

---

### 17.3 获取我的未读消息数

**`GET /api/v1/notify/unread/count`** 🔑

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": { "unreadCount": 3 }
}
```

---

### 17.4 标记已读

**`PUT /api/v1/notify/read`** 🔑

**Request Body：**

```yaml
type: object
properties:
  noticeIds:
    type: array
    items: { type: integer, format: int64 }
    nullable: true
    description: null=标记全部已读
```

---

## 18. 教务对接模块 — /api/v1/jwxt

> 下游服务：edu-jwxt（:8093）  
> **仅 SUPER_ADMIN 或 DEPT_ADMIN 可调用**

### 18.1 触发教务数据同步

**`POST /api/v1/jwxt/sync/incremental`** 👨‍💼

手动触发增量同步（通常由 XXL-Job 自动执行）。

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "syncLogId": 1001,
    "syncType": "INCREMENTAL",
    "status": "RUNNING",
    "message": "增量同步已启动，完成后发送通知"
  }
}
```

**`POST /api/v1/jwxt/sync/full`** 🔐

全量同步（学期开学前使用，耗时较长）。

---

### 18.2 获取同步日志

**`GET /api/v1/jwxt/sync/log`** 👨‍💼

**Query Parameters：** `syncType` / `status` / `page` / `size`

**Response 200：**

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "list": [
      {
        "id": 1001,
        "syncType": "INCREMENTAL",
        "syncDate": "2026-06-16",
        "studentCnt": 12,
        "deptCnt": 0,
        "courseCnt": 3,
        "successCnt": 15,
        "failedCnt": 0,
        "status": 1,
        "costMs": 3420,
        "triggeredBy": "MANUAL",
        "createdAt": "2026-06-16T02:00:00+08:00",
        "finishedAt": "2026-06-16T02:00:03+08:00"
      }
    ],
    "total": 180
  }
}
```

---

### 18.3 成绩回传教务系统

见 §12.7（`POST /api/v1/grade/push/{classId}`），教务对接服务作为内部调用方。

---

## 19. WebSocket 事件规范

> 连接端点：`wss://api.smu.edu.cn/ws`（STOMP over SockJS）  
> 客户端依赖：`@stomp/stompjs` + `sockjs-client`

### 19.1 连接鉴权

```javascript
const stompClient = new Client({
  webSocketFactory: () => new SockJS('https://api.smu.edu.cn/ws'),
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`
  }
});
```

### 19.2 订阅主题（客户端 → 服务端订阅）

| Topic | 订阅方 | 说明 |
|-------|--------|------|
| `/topic/lesson/{lessonId}/slide` | 学生、教师 | 课件翻页同步 |
| `/topic/lesson/{lessonId}/barrage` | 学生、教师 | 弹幕推送 |
| `/topic/lesson/{lessonId}/attendance` | 教师 | 签到人数实时更新 |
| `/topic/lesson/{lessonId}/question` | 学生、教师 | 题目下发/收回 |
| `/topic/lesson/{lessonId}/random-call` | 学生、教师 | 随机点名结果 |
| `/topic/exam/{publishId}/monitor` | 教师 | 监考异常告警 |
| `/user/queue/ai-task` | 教师（个人队列） | AI任务完成通知 |
| `/user/queue/notify` | 所有用户（个人队列） | 个人通知推送 |

### 19.3 发送消息（客户端 → 服务端）

| Destination | 发送方 | 说明 |
|-------------|--------|------|
| `/app/lesson/{lessonId}/nextSlide` | 教师 | 翻到下一页 |
| `/app/lesson/{lessonId}/barrage` | 学生 | 发送弹幕 |
| `/app/exam/{publishId}/heartbeat` | 学生 | 考试心跳 |

### 19.4 事件消息格式

#### 课件翻页事件

```json
{
  "type": "SLIDE_CHANGE",
  "lessonId": 8001,
  "pageNum": 5,
  "slideUrl": "https://cdn.smu.edu.cn/slides/201/page-5.jpg",
  "timestamp": 1718500000000
}
```

#### 签到人数更新事件

```json
{
  "type": "ATTENDANCE_UPDATE",
  "lessonId": 8001,
  "attendedCount": 42,
  "totalStudents": 45,
  "attendRate": 93.33,
  "timestamp": 1718500000000
}
```

#### 题目下发事件

```json
{
  "type": "QUESTION_PUBLISH",
  "lessonId": 8001,
  "question": {
    "id": 5001,
    "type": 1,
    "content": "<p>以下哪种数据结构实现递归最高效？</p>",
    "options": [
      { "optionLabel": "A", "content": "队列" },
      { "optionLabel": "B", "content": "栈" }
    ],
    "timeLimit": 60
  },
  "timestamp": 1718500000000
}
```

#### 随机点名事件

```json
{
  "type": "RANDOM_CALL",
  "lessonId": 8001,
  "callId": 7001,
  "style": "spotlight",
  "selected": [
    { "id": 20003, "realName": "王芳", "avatarUrl": "..." }
  ],
  "timestamp": 1718500000000
}
```

#### AI 任务完成事件（个人队列）

```json
{
  "type": "AI_TASK_DONE",
  "taskType": "MINDMAP",
  "lessonId": 8001,
  "lessonTitle": "第3讲：树与二叉树",
  "message": "课堂思维导图已生成，点击查看",
  "link": "/lesson/8001/mindmap",
  "timestamp": 1718500000000
}
```

#### 监考异常告警事件（教师）

```json
{
  "type": "MONITOR_ALERT",
  "publishId": 401,
  "studentId": 20005,
  "realName": "陈某",
  "studentNo": "2024005",
  "eventType": "TAB_SWITCH",
  "totalCount": 5,
  "warningLevel": "HIGH",
  "timestamp": 1718500000000
}
```

---

## 20. 接口变更日志

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| V1.0 | 2026-06-16 | 初稿，覆盖全部 12 个模块共 76 个端点 |

---

**端点统计汇总：**

| 模块 | 端点数 | 关键约束 |
|------|--------|----------|
| 认证模块 | 6 | SMS 1次/min 限流 |
| 用户/组织 | 8 | 手机号脱敏输出 |
| 课程/课堂 | 12 | live_mode 分级 |
| 互动教学 | 10 | 签到 1500 TPS |
| 题库管理 | 8 | FULLTEXT ES 检索 |
| 在线测试 | 14 | 交卷 Kafka 异步 |
| 成绩管理 | 7 | 权重合计 = 100 |
| AI 能力 | 10 | SSE 流式 + PromptFilter |
| 统计分析 | 6 | ClickHouse + Redis |
| 文件存储 | 3 | 预签名 URL |
| 直播推流 | 3 | 线下默认关闭 |
| 消息通知 | 4 | Kafka 异步推送 |
| 教务对接 | 3 | 仅管理员可调 |
| **合计** | **94** | |

---

*API 接口规范文档 V1.0 | 对应技术方案 V2.0 | 对应数据库设计 V2.0*  
*本文档为接口契约，前后端开发须严格遵守；变更须走 PR 评审流程并更新版本号。*
DOCEOF
echo "文档写入完成，行数：$(wc -l < /home/claude/api-spec/part1.md)"