import { test, expect } from "@playwright/test";
import { clearSmsRateLimit, getSmsCode } from "./helpers";

const ADMIN_PHONE = "13800000001";

async function loginAsAdmin(page: any) {
  clearSmsRateLimit(ADMIN_PHONE);
  await page.goto("/login");
  await page.fill('input[type="tel"]', ADMIN_PHONE);
  await page.click('button:has-text("获取验证码")');
  await page.waitForTimeout(800);
  const code = await getSmsCode(ADMIN_PHONE);
  await page.fill('input[placeholder="6位验证码"]', code);
  await page.click('button[type="submit"]');
  await page.waitForURL("**/dashboard", { timeout: 8000 });
}

test.describe("教务管理模块", () => {
  test("用户管理页面可访问", async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto("/admin/users");
    await page.waitForTimeout(2000);
    await expect(page).not.toHaveURL(/\/login/);
    await expect(page.locator("body")).not.toContainText("404");
  });

  test("课程列表页面可访问", async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto("/courses");
    await page.waitForTimeout(2000);
    await expect(page).not.toHaveURL(/\/login/);
  });

  test("课件管理页面可访问", async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto("/materials");
    await page.waitForTimeout(2000);
    await expect(page).not.toHaveURL(/\/login/);
  });
});
