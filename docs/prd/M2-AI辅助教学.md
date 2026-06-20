# M2 AI 辅助教学（edu-ai）

> 对应 Sprint 6 —— **尚未开发**。本章为占位骨架，待 S6 启动时按 README 模板详写。

## 章节范围与功能清单（规划）

- F2-01 AI 任务异步框架（Kafka edu.ai.tasks，并发=3）— C3
- F2-02 AI 智能批改（主观题 → LLM → score/comment/errorReason）
- F2-03 课堂 ASR 转写（科大讯飞流式）
- F2-04 课堂摘要生成（≤500 字 + key_points）
- F2-05 AI 思维导图（LLM → Markmap）
- F2-06 一键 AI 出题
- F2-07 AI 对话任务（SSE 流式）
- F2-08 Prompt 安全过滤（C4，PromptSecurityFilter）

## 当前状态
- 前端：无 ai api 模块、无页面
- 后端：edu-ai 骨架（S2 已起，AI 网关 POC）

## 本章关联红线
- **C3** AI 任务全异步，禁止同步等待
- **C4** 所有 LLM 调用必须过 PromptSecurityFilter

> 待 S6 开发时，按 README 模板将 F2-01 ~ F2-08 展开为完整 PRD + AC。
