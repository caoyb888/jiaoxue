# E2E 测试（edu-e2e）

经网关 `:18080` 打**真实运行后端**、断言后端状态的端到端测试。
完整测试体系见 [`../docs/测试说明.md`](../docs/测试说明.md)。

> ⚠️ **必须在内网机（onlyserver）运行**：用例依赖 `docker exec` 读 Redis（dev 短信验证码）、查 MySQL（落库核对），本机跑不了。

## 快速开始

```bash
ssh onlyserver 'cd ~/smart-edu/e2e && npm install'   # 首次（chromium 已预装）

# API 集成测试（S1–S5，无需浏览器）
npm run test:api          # 整套，串行
npm run test:s1           # 单 Sprint：s1 / s2 / s3 / s4 / s5

# UI / 健康检查（01–05，浏览器，需 web :5173 在跑）
npx playwright test tests/0*.spec.ts

# 报告
npm run report
```

## 目录

| 文件 | 说明 |
|------|------|
| `tests/api-helpers.ts` | 公共工具：dev 短信登录、`Result` 解包、本地 ISO 时间、MySQL 直查 |
| `tests/s1-s5-*.spec.ts` | 按 Sprint 的 API 集成测试（断言后端状态） |
| `tests/01-05-*.spec.ts` | 旧 UI/健康检查（页面可达 + 服务健康） |
| `playwright.config.ts` | `workers: 1` 串行（共用后端 + 同批手机号，并行会抢验证码） |

## 约定

- **串行**：勿改 `workers`，并行会抢短信验证码导致 flaky。
- **数据保留**：每次运行创建真实数据并保留，用例自带独立数据、可重复跑。
- **测试账号**：admin `13800000001` / teacher01 `13800000002` / student01 `13800000011`；
  重置验证码 `ssh onlyserver 'bash ~/smart-edu/infra/scripts/reset-sms-codes.sh'`。
