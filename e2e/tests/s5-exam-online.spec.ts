import { test, expect, APIRequestContext } from "@playwright/test";
import { GW, PHONES, bearer, login, okData, isoLocal } from "./api-helpers";

// S5 在线考试（edu-exam）：C2 交卷容灾/幂等 + 防泄题 + XXL-Job 异步批改
test.describe("S5 在线考试", () => {
  let teacher: string;
  let student: string;
  let classId: number;
  let paperId: number;
  let questionId: number;

  test.beforeAll(async ({ request }) => {
    teacher = await login(request, PHONES.teacher);
    student = await login(request, PHONES.student);
    const classes = await okData(await request.get(`${GW}/v1/course/class/my`, { headers: bearer(teacher) }));
    classId = classes[0].id;

    const bank = await okData(
      await request.post(`${GW}/v1/exam/banks`, {
        headers: bearer(teacher),
        data: { bankName: "E2E考试题库-S5", isPublic: 0 },
      })
    );
    const q = await okData(
      await request.post(`${GW}/v1/exam/questions`, {
        headers: bearer(teacher),
        data: {
          bankId: bank.id,
          type: 1,
          content: "E2E-S5：1+1=?",
          answer: "B",
          score: 5,
          options: [
            { optionLabel: "A", content: "1", isCorrect: 0 },
            { optionLabel: "B", content: "2", isCorrect: 1 },
          ],
        },
      })
    );
    questionId = q.id;
    const paper = await okData(
      await request.post(`${GW}/v1/exam/papers`, {
        headers: bearer(teacher),
        data: { title: "E2E在线试卷-S5", totalScore: 5, isRandom: 0, paperType: "A" },
      })
    );
    paperId = paper.id;
    await okData(
      await request.post(`${GW}/v1/exam/papers/${paperId}/questions/batch`, {
        headers: bearer(teacher),
        data: { questions: [{ questionId, score: 5, sortOrder: 1 }] },
      })
    );
  });

  /** 新发布一场即时开始的考试，返回 publishId。 */
  async function publish(request: APIRequestContext): Promise<number> {
    const pub = await okData(
      await request.post(`${GW}/v1/exam/publishes`, {
        headers: bearer(teacher),
        data: {
          paperId,
          classId,
          startTime: isoLocal(-1),
          endTime: isoLocal(120),
          durationMin: 60,
          enableMonitor: 0,
          faceVerifyType: 0,
          allowCopy: 1,
          shuffleQuestion: 0,
          shuffleOption: 0,
        },
      })
    );
    expect(pub.status).toBe(1); // 进行中
    return pub.id;
  }

  /** 轮询 my-score 直到批改完成（XXL-Job examSubmitExpandHandler 每 30s）。 */
  async function waitGraded(request: APIRequestContext, publishId: number) {
    await expect
      .poll(
        async () => {
          const s = await okData(
            await request.get(`${GW}/v1/exam/publishes/${publishId}/my-score`, { headers: bearer(student) })
          );
          return s.gradedQuestions;
        },
        { timeout: 75_000, intervals: [2000, 3000, 5000] }
      )
      .toBeGreaterThan(0);
    return okData(
      await request.get(`${GW}/v1/exam/publishes/${publishId}/my-score`, { headers: bearer(student) })
    );
  }

  test("进入考试时题目不含标准答案（防泄题）", async ({ request }) => {
    const publishId = await publish(request);
    const enter = await okData(
      await request.post(`${GW}/v1/exam/publishes/${publishId}/enter`, { headers: bearer(student), data: {} })
    );
    expect(enter.questions.length).toBeGreaterThan(0);
    expect(enter.questions[0].question.answer, "学生端不应返回标准答案").toBeNull();
  });

  test("交卷幂等：重复交卷被拒（100505）", async ({ request }) => {
    const publishId = await publish(request);
    await okData(
      await request.post(`${GW}/v1/exam/publishes/${publishId}/enter`, { headers: bearer(student), data: {} })
    );
    await okData(
      await request.post(`${GW}/v1/exam/publishes/${publishId}/submit`, {
        headers: bearer(student),
        data: { answers: [{ questionId, answerContent: "B" }], submitType: "MANUAL" },
      })
    );
    const r2 = await request.post(`${GW}/v1/exam/publishes/${publishId}/submit`, {
      headers: bearer(student),
      data: { answers: [{ questionId, answerContent: "A" }], submitType: "MANUAL" },
    });
    const body = await r2.json();
    expect(body.code, "重复交卷应返回已提交错误码").toBe(100505);
  });

  test("交卷 → XXL-Job 异步展开 + 客观题自动批改 → 正确答案得满分", async ({ request }) => {
    test.setTimeout(90_000);
    const publishId = await publish(request);
    await okData(
      await request.post(`${GW}/v1/exam/publishes/${publishId}/enter`, { headers: bearer(student), data: {} })
    );
    await okData(
      await request.post(`${GW}/v1/exam/publishes/${publishId}/submit`, {
        headers: bearer(student),
        data: { answers: [{ questionId, answerContent: "B" }], submitType: "MANUAL" },
      })
    );
    const score = await waitGraded(request, publishId);
    expect(Number(score.totalScore)).toBe(5);
    expect(score.correctCount).toBe(1);
    expect(score.answers[0].isCorrect).toBe(1);
  });

  test("错误答案自动批改为 0 分", async ({ request }) => {
    test.setTimeout(90_000);
    const publishId = await publish(request);
    await okData(
      await request.post(`${GW}/v1/exam/publishes/${publishId}/enter`, { headers: bearer(student), data: {} })
    );
    await okData(
      await request.post(`${GW}/v1/exam/publishes/${publishId}/submit`, {
        headers: bearer(student),
        data: { answers: [{ questionId, answerContent: "A" }], submitType: "MANUAL" },
      })
    );
    const score = await waitGraded(request, publishId);
    expect(Number(score.totalScore)).toBe(0);
    expect(score.correctCount).toBe(0);
    expect(score.answers[0].isCorrect).toBe(0);
  });
});
