import { test, expect } from "@playwright/test";
import { GW, PHONES, STUDENT01_ID, bearer, login, okData, mysql } from "./api-helpers";

/**
 * S8-16 全功能端到端：模拟一节课完整流程（收官验收）。
 *
 * 签到 → 弹幕 → 出题 → 答题 → 课堂报告生成 → AI 思维导图可见。
 * 跨 edu-course / edu-interaction / edu-exam / edu-ai，经网关 :18080 打真实后端。
 * AI 为 mock-mode（默认），思维导图秒级生成。串行执行（workers=1）。
 */
test.describe("S8 全功能一节课", () => {
  let teacher: string;
  let student: string;
  let lessonId: number;
  let questionId: number;
  let lessonQuestionId: number;

  test.beforeAll(async ({ request }) => {
    teacher = await login(request, PHONES.teacher);
    student = await login(request, PHONES.student);

    const classes = await okData(
      await request.get(`${GW}/v1/course/class/my`, { headers: bearer(teacher) })
    );
    expect(classes.length, "教师应至少有一个教学班").toBeGreaterThan(0);

    const start = await okData(
      await request.post(`${GW}/v1/course/lesson/start`, {
        headers: bearer(teacher),
        data: { classId: classes[0].id, title: "E2E全流程课堂-S8", liveMode: "SLIDE_ONLY" },
      })
    );
    lessonId = start.lessonId;
    expect(lessonId).toBeGreaterThan(0);

    // 预置一道单选题（供课堂发题）
    const bank = await okData(
      await request.post(`${GW}/v1/exam/banks`, {
        headers: bearer(teacher),
        data: { bankName: "E2E题库-S8", description: "e2e full lesson", isPublic: 0 },
      })
    );
    const q = await okData(
      await request.post(`${GW}/v1/exam/questions`, {
        headers: bearer(teacher),
        data: {
          bankId: bank.id,
          type: 1,
          content: "E2E-S8：中国的首都是？",
          answer: "B",
          score: 5,
          options: [
            { optionLabel: "A", content: "上海", isCorrect: 0 },
            { optionLabel: "B", content: "北京", isCorrect: 1 },
          ],
        },
      })
    );
    questionId = q.id;
  });

  test("① 签到：生成签到码 → 学生签到 → 落库", async ({ request }) => {
    const code = await okData(
      await request.post(`${GW}/v1/interaction/lesson/${lessonId}/attend/code`, {
        headers: bearer(teacher),
      })
    );
    expect(code.code).toMatch(/^[A-Z0-9]+$/);

    const first = await okData(
      await request.post(`${GW}/v1/interaction/lesson/${lessonId}/attend`, {
        headers: bearer(student),
        data: { code: code.code },
      })
    );
    expect(first.firstAttend).toBe(true);

    // C1：签到经 Redis 队列异步落库，最终应出现在 attendance 表
    await expect
      .poll(() => mysql(`SELECT status FROM attendance WHERE lesson_id=${lessonId} AND student_id=${STUDENT01_ID}`), {
        timeout: 12_000,
        intervals: [500, 1000, 2000],
      })
      .toBe("1");
  });

  test("② 弹幕：学生发送弹幕", async ({ request }) => {
    await okData(
      await request.post(`${GW}/v1/interaction/lesson/${lessonId}/barrage`, {
        headers: bearer(student),
        data: { content: "E2E-S8 弹幕：懂了！", style: "roll" },
      })
    );
  });

  test("③ 出题：教师向课堂发布题目 → 学生可见当前题", async ({ request }) => {
    const published = await okData(
      await request.post(`${GW}/v1/exam/lessons/${lessonId}/questions`, {
        headers: bearer(teacher),
        data: { questionId },
      })
    );
    lessonQuestionId = published.id;
    expect(lessonQuestionId).toBeGreaterThan(0);

    const current = await okData(
      await request.get(`${GW}/v1/exam/lessons/${lessonId}/questions/current`, {
        headers: bearer(student),
      })
    );
    expect(current.id, "学生轮询到进行中的题目").toBe(lessonQuestionId);
  });

  test("④ 答题：学生提交答案 → 客观题即时判对", async ({ request }) => {
    const result = await okData(
      await request.post(`${GW}/v1/exam/lessons/${lessonId}/answers`, {
        headers: bearer(student),
        data: { lessonQuestionId, answer: "B" },
      })
    );
    expect(result.isCorrect, "答案 B 应判正确").toBe(true);
  });

  test("⑤ 课堂报告 + AI 思维导图：下课 → 生成 → 可见", async ({ request }) => {
    // 下课（触发课堂报告 AI 任务）
    await okData(
      await request.post(`${GW}/v1/course/lesson/${lessonId}/end`, { headers: bearer(teacher) })
    );

    // 显式触发思维导图生成（edu.ai.tasks，mock-mode 秒级完成）
    await okData(
      await request.post(`${GW}/v1/ai/mindmap/${lessonId}/regenerate`, { headers: bearer(teacher) })
    );

    // 轮询直到生成完成
    await expect
      .poll(
        async () => {
          const mm = await okData(
            await request.get(`${GW}/v1/ai/mindmap/${lessonId}`, { headers: bearer(teacher) })
          );
          return mm.genStatus;
        },
        { timeout: 30_000, intervals: [1000, 2000, 3000] }
      )
      .toBe("DONE");

    // 教师设为学生可见
    await okData(
      await request.put(`${GW}/v1/ai/mindmap/${lessonId}`, {
        headers: bearer(teacher),
        data: { studentVisible: true },
      })
    );

    // 学生侧：思维导图已完成且可见
    const studentView = await okData(
      await request.get(`${GW}/v1/ai/mindmap/${lessonId}`, { headers: bearer(student) })
    );
    expect(studentView.genStatus).toBe("DONE");
    expect(studentView.studentVisible, "学生应能看到思维导图").toBe(true);
    expect(studentView.markmapJson, "思维导图 JSON 非空").toBeTruthy();
  });
});
