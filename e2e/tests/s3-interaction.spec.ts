import { test, expect } from "@playwright/test";
import { GW, PHONES, STUDENT01_ID, bearer, login, okData, mysql } from "./api-helpers";

// S3 互动教学（edu-interaction，含 C1 签到削峰 + 幂等）
test.describe("S3 互动教学", () => {
  let teacher: string;
  let student: string;
  let lessonId: number;

  test.beforeAll(async ({ request }) => {
    teacher = await login(request, PHONES.teacher);
    student = await login(request, PHONES.student);
    const classes = await okData(await request.get(`${GW}/v1/course/class/my`, { headers: bearer(teacher) }));
    const start = await okData(
      await request.post(`${GW}/v1/course/lesson/start`, {
        headers: bearer(teacher),
        data: { classId: classes[0].id, title: "E2E互动课堂-S3", liveMode: "SLIDE_ONLY" },
      })
    );
    lessonId = start.lessonId;
  });

  test("生成签到码 → 学生签到 → 重复签到幂等", async ({ request }) => {
    const code = await okData(
      await request.post(`${GW}/v1/interaction/lesson/${lessonId}/attend/code`, { headers: bearer(teacher) })
    );
    expect(code.code).toMatch(/^[A-Z0-9]+$/);

    const first = await okData(
      await request.post(`${GW}/v1/interaction/lesson/${lessonId}/attend`, {
        headers: bearer(student),
        data: { code: code.code },
      })
    );
    expect(first.firstAttend).toBe(true);

    const again = await okData(
      await request.post(`${GW}/v1/interaction/lesson/${lessonId}/attend`, {
        headers: bearer(student),
        data: { code: code.code },
      })
    );
    expect(again.firstAttend, "重复签到应不再是首签").toBe(false);
  });

  test("考勤列表反映已签到 + DB 落库（削峰队列已排干）", async ({ request }) => {
    await expect
      .poll(
        async () => {
          const list = await okData(
            await request.get(`${GW}/v1/interaction/lesson/${lessonId}/attendance`, { headers: bearer(teacher) })
          );
          return list.attendedCount;
        },
        { timeout: 12_000, intervals: [500, 1000, 2000] }
      )
      .toBeGreaterThan(0);

    // C1：签到经 Redis 队列异步落库，最终应在 attendance 表出现
    expect(mysql(`SELECT status FROM attendance WHERE lesson_id=${lessonId} AND student_id=${STUDENT01_ID}`)).toBe("1");
  });

  test("教师随机点名", async ({ request }) => {
    const rc = await okData(
      await request.post(`${GW}/v1/interaction/lesson/${lessonId}/roll-call`, {
        headers: bearer(teacher),
        data: { count: 1, style: "random" },
      })
    );
    expect(Array.isArray(rc.studentIds)).toBeTruthy();
    expect(rc.studentIds.length).toBeGreaterThan(0);
  });

  test("学生发送弹幕", async ({ request }) => {
    const res = await request.post(`${GW}/v1/interaction/lesson/${lessonId}/barrage`, {
      headers: bearer(student),
      data: { content: "E2E弹幕-S3", style: "roll" },
    });
    await okData(res);
  });
});
