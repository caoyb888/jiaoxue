import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  timeout: 30000,
  retries: 1,
  // 全套测试共用同一套运行中后端 + 同一批 dev 测试手机号；并行会抢短信验证码、
  // 产生登录竞态与共享状态时序问题。统一串行执行以保证稳定。
  workers: 1,
  reporter: [["list"], ["html", { open: "never", outputFolder: "/tmp/playwright-report" }]],
  use: {
    baseURL: "http://localhost:5173",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], headless: true },
    },
  ],
});
