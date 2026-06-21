import { test, expect } from "@playwright/test";
import { GW, PHONES, bearer, login, okData } from "./api-helpers";

// S4 题库 / 题目 / 试卷（edu-exam）
test.describe("S4 题库与试卷", () => {
  let teacher: string;

  test.beforeAll(async ({ request }) => {
    teacher = await login(request, PHONES.teacher);
  });

  test("题库创建 → 题目创建 → 题目检索 → 组卷（含分值核对）", async ({ request }) => {
    const bank = await okData(
      await request.post(`${GW}/v1/exam/banks`, {
        headers: bearer(teacher),
        data: { bankName: "E2E题库-S4", description: "e2e", isPublic: 0 },
      })
    );
    expect(bank.id).toBeGreaterThan(0);

    const q = await okData(
      await request.post(`${GW}/v1/exam/questions`, {
        headers: bearer(teacher),
        data: {
          bankId: bank.id,
          type: 1, // 单选
          content: "E2E-S4：2+2=?",
          answer: "B",
          score: 5,
          options: [
            { optionLabel: "A", content: "3", isCorrect: 0 },
            { optionLabel: "B", content: "4", isCorrect: 1 },
          ],
        },
      })
    );
    expect(q.id).toBeGreaterThan(0);

    const listed = await okData(
      await request.get(`${GW}/v1/exam/questions`, { headers: bearer(teacher), params: { bankId: bank.id } })
    );
    expect(listed.list.some((x: { id: number }) => x.id === q.id)).toBeTruthy();

    const paper = await okData(
      await request.post(`${GW}/v1/exam/papers`, {
        headers: bearer(teacher),
        data: { title: "E2E试卷-S4", totalScore: 5, isRandom: 0, paperType: "A" },
      })
    );
    expect(paper.id).toBeGreaterThan(0);

    const detail = await okData(
      await request.post(`${GW}/v1/exam/papers/${paper.id}/questions/batch`, {
        headers: bearer(teacher),
        data: { questions: [{ questionId: q.id, score: 5, sortOrder: 1 }] },
      })
    );
    expect(detail.questions.length).toBe(1);
    expect(detail.questions[0].questionId).toBe(q.id);
    expect(Number(detail.actualScore)).toBe(5); // 各题分值之和与 totalScore 对齐
  });
});
