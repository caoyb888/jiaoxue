import { test, expect } from "@playwright/test";
import { GW, PHONES, bearer, login, okData } from "./api-helpers";

// S2 课程/课堂/课件（edu-course）
test.describe("S2 课程/课堂/课件", () => {
  let teacher: string;

  test.beforeAll(async ({ request }) => {
    teacher = await login(request, PHONES.teacher);
  });

  test("教师查询我的教学班", async ({ request }) => {
    const data = await okData(await request.get(`${GW}/v1/course/class/my`, { headers: bearer(teacher) }));
    expect(Array.isArray(data)).toBeTruthy();
    expect(data.length).toBeGreaterThan(0);
    expect(data[0]).toHaveProperty("className");
  });

  test("课程列表分页", async ({ request }) => {
    const data = await okData(
      await request.get(`${GW}/v1/course/list`, { headers: bearer(teacher), params: { page: 1, size: 5 } })
    );
    expect(Array.isArray(data.list)).toBeTruthy();
  });

  test("课堂生命周期：开课 → 课堂详情 → 下课", async ({ request }) => {
    const classes = await okData(await request.get(`${GW}/v1/course/class/my`, { headers: bearer(teacher) }));
    const classId = classes[0].id;

    const start = await okData(
      await request.post(`${GW}/v1/course/lesson/start`, {
        headers: bearer(teacher),
        data: { classId, title: "E2E课堂-S2", liveMode: "SLIDE_ONLY" },
      })
    );
    expect(start.lessonId).toBeGreaterThan(0);
    expect(start.status).toBe(1); // 进行中

    const detail = await okData(
      await request.get(`${GW}/v1/course/lesson/${start.lessonId}`, { headers: bearer(teacher) })
    );
    expect(detail.id).toBe(start.lessonId);
    expect(detail.classId).toBe(classId);

    const end = await okData(
      await request.post(`${GW}/v1/course/lesson/${start.lessonId}/end`, { headers: bearer(teacher) })
    );
    expect(end.lessonId).toBe(start.lessonId);
    expect(end.status).toBe(2); // 已结束
  });

  test("课件列表", async ({ request }) => {
    const data = await okData(
      await request.get(`${GW}/v1/course/material/list`, { headers: bearer(teacher), params: { page: 1 } })
    );
    expect(Array.isArray(data.list)).toBeTruthy();
  });
});
