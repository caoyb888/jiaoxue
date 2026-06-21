import { test, expect } from "@playwright/test";
import { clearSmsRateLimit, getSmsCode } from "./helpers";

const ADMIN_PHONE = "13800000001";
const TEACHER_PHONE = "13800000002";
const STUDENT_PHONE = "13800000011";

async function loginAs(page: any, phone: string) {
  clearSmsRateLimit(phone);
  await page.goto("/login");
  await expect(page.locator("h1")).toContainText("山东管理学院");

  await page.fill('input[type="tel"]', phone);
  await page.click('button:has-text("获取验证码")');
  await page.waitForTimeout(800);

  const code = await getSmsCode(phone);
  expect(code).toMatch(/^\d{6}$/);

  await page.fill('input[placeholder="6位验证码"]', code);
  await page.click('button[type="submit"]');
  await page.waitForURL("**/dashboard", { timeout: 8000 });
  // Wait for lazy-loaded DashboardPage to finish mounting
  await page.waitForSelector('header', { timeout: 10000 });
}

test.describe("认证模块", () => {
  test("管理员登录 → 进入仪表盘", async ({ page }) => {
    await loginAs(page, ADMIN_PHONE);
    await expect(page.locator("header")).toContainText("智慧教学平台");
    await expect(page.locator("main")).toContainText("欢迎使用山东管理学院智慧教学系统");
  });

  test("教师账号登录成功", async ({ page }) => {
    await loginAs(page, TEACHER_PHONE);
    await expect(page.locator("main")).toContainText("欢迎使用山东管理学院智慧教学系统");
  });

  test("学生账号登录成功", async ({ page }) => {
    await loginAs(page, STUDENT_PHONE);
    await expect(page.locator("main")).toContainText("欢迎使用山东管理学院智慧教学系统");
  });

  test("手机号格式校验", async ({ page }) => {
    await page.goto("/login");
    await page.fill('input[type="tel"]', "12345");
    await page.click('button:has-text("获取验证码")');
    await expect(page.locator("text=请输入正确的手机号")).toBeVisible();
  });

  test("未登录访问受保护路由 → 重定向到登录页", async ({ page }) => {
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login/);
  });

  test("登出功能", async ({ page }) => {
    await loginAs(page, ADMIN_PHONE);
    await page.click('button:has-text("退出登录")');
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 });
  });
});
