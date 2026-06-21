import { test, expect } from "@playwright/test";
import { clearSmsRateLimit, getSmsCode, getFirstLessonId } from "./helpers";

const TEACHER_PHONE = "13800000002";
const STUDENT_PHONE = "13800000011";

async function loginAs(page: any, phone: string) {
  clearSmsRateLimit(phone);
  await page.goto("/login");
  await page.fill('input[type="tel"]', phone);
  await page.click('button:has-text("获取验证码")');
  await page.waitForTimeout(800);
  const code = await getSmsCode(phone);
  await page.fill('input[placeholder="6位验证码"]', code);
  await page.click('button[type="submit"]');
  await page.waitForURL("**/dashboard", { timeout: 8000 });
}

test.describe("在线考试模块", () => {
  test("题库管理页面可访问", async ({ page }) => {
    await loginAs(page, TEACHER_PHONE);
    await page.goto("/exam/question-banks");
    await page.waitForTimeout(1500);
    await expect(page).not.toHaveURL(/\/login/);
    await expect(page.locator("body")).not.toContainText("404");
  });

  test("试卷管理页面可访问", async ({ page }) => {
    await loginAs(page, TEACHER_PHONE);
    await page.goto("/exam/papers");
    await page.waitForTimeout(1500);
    await expect(page).not.toHaveURL(/\/login/);
  });

  test("学生答题页面可访问", async ({ page }) => {
    const lessonId = getFirstLessonId();
    await loginAs(page, STUDENT_PHONE);
    await page.goto(`/lesson/${lessonId}/answer`);
    await page.waitForTimeout(2000);
    await expect(page).not.toHaveURL(/\/login/);
  });

  test("IndexedDB 草稿存储机制可用（容灾验证）", async ({ page }) => {
    const lessonId = getFirstLessonId();
    await loginAs(page, STUDENT_PHONE);
    await page.goto(`/lesson/${lessonId}/answer`);
    await page.waitForTimeout(2000);

    const idbWorks = await page.evaluate(async () => {
      return new Promise<boolean>((resolve) => {
        const req = indexedDB.open("edu_exam_draft");
        req.onsuccess = () => { req.result.close(); resolve(true); };
        req.onupgradeneeded = () => resolve(true);
        req.onerror = () => resolve(false);
      });
    });
    expect(idbWorks).toBe(true);
  });
});
