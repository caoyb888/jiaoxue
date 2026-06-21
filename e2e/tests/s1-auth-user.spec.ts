import { test, expect } from "@playwright/test";
import { GW, PHONES, bearer, login, okData } from "./api-helpers";

// S1 认证 + 用户/角色（edu-auth / edu-user）
test.describe("S1 认证 + 用户/角色", () => {
  test("三种角色均可短信登录并取得 JWT", async ({ request }) => {
    for (const [role, phone] of Object.entries(PHONES)) {
      const token = await login(request, phone);
      expect(token.length, `${role} token 长度`).toBeGreaterThan(100);
    }
  });

  test("管理员分页查询用户列表", async ({ request }) => {
    const token = await login(request, PHONES.admin);
    const data = await okData(
      await request.get(`${GW}/v1/users`, { headers: bearer(token), params: { pageNum: 1, pageSize: 5 } })
    );
    expect(Array.isArray(data.records)).toBeTruthy();
    expect(data.records.length).toBeGreaterThan(0);
    expect(data.total).toBeGreaterThan(0);
    expect(data.records[0]).toHaveProperty("username");
  });

  test("院系树返回根节点（山东管理学院）", async ({ request }) => {
    const token = await login(request, PHONES.admin);
    const data = await okData(await request.get(`${GW}/v1/depts/tree`, { headers: bearer(token) }));
    expect(Array.isArray(data)).toBeTruthy();
    expect(data[0].deptName).toContain("山东管理学院");
    expect(Array.isArray(data[0].children)).toBeTruthy();
  });

  test("角色列表返回四个固定角色（回归 /v1/roles 404）", async ({ request }) => {
    const token = await login(request, PHONES.admin);
    const data = await okData(await request.get(`${GW}/v1/roles`, { headers: bearer(token) }));
    const codes = data.map((r: { roleCode: string }) => r.roleCode);
    expect(codes).toEqual(
      expect.arrayContaining(["SUPER_ADMIN", "DEPT_ADMIN", "TEACHER", "STUDENT"])
    );
  });
});
