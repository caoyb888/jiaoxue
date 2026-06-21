import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  timeout: 30000,
  retries: 1,
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
