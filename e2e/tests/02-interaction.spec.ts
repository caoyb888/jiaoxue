import { test, expect } from "@playwright/test";
import { clearSmsRateLimit, getSmsCode, getFirstLessonId } from "./helpers";

const TEACHER_PHONE = "13800000002";

async function loginAsTeacher(page: any) {
  clearSmsRateLimit(TEACHER_PHONE);
  await page.goto("/login");
  await page.fill('input[type="tel"]', TEACHER_PHONE);
  await page.click('button:has-text("获取验证码")');
  await page.waitForTimeout(800);
  const code = await getSmsCode(TEACHER_PHONE);
  await page.fill('input[placeholder="6位验证码"]', code);
  await page.click('button[type="submit"]');
  await page.waitForURL("**/dashboard", { timeout: 8000 });
}

test.describe("互动教学模块", () => {
  test("签到管理页面可访问", async ({ page }) => {
    const lessonId = getFirstLessonId();
    await loginAsTeacher(page);
    await page.goto(`/lesson/${lessonId}/attendance`);
    await page.waitForTimeout(1500);
    await expect(page).not.toHaveURL(/\/login/);
    await expect(page.locator("body")).not.toContainText("404");
  });

  test("弹幕页面可访问", async ({ page }) => {
    const lessonId = getFirstLessonId();
    await loginAsTeacher(page);
    await page.goto(`/lesson/${lessonId}/barrage`);
    await page.waitForTimeout(1500);
    await expect(page).not.toHaveURL(/\/login/);
  });

  test("随机点名页面可访问", async ({ page }) => {
    const lessonId = getFirstLessonId();
    await loginAsTeacher(page);
    await page.goto(`/lesson/${lessonId}/roll-call`);
    await page.waitForTimeout(1500);
    await expect(page).not.toHaveURL(/\/login/);
  });

  test("学生签到页面可访问", async ({ page }) => {
    const lessonId = getFirstLessonId();
    clearSmsRateLimit("13800000011");
    await page.goto("/login");
    await page.fill('input[type="tel"]', "13800000011");
    await page.click('button:has-text("获取验证码")');
    await page.waitForTimeout(800);
    const code = await getSmsCode("13800000011");
    await page.fill('input[placeholder="6位验证码"]', code);
    await page.click('button[type="submit"]');
    await page.waitForURL("**/dashboard", { timeout: 8000 });
    await page.goto(`/lesson/${lessonId}/attend`);
    await page.waitForTimeout(1500);
    await expect(page).not.toHaveURL(/\/login/);
  });
});
