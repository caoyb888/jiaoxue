import { test, expect } from "@playwright/test";
import { GW, PHONES, STUDENT01_ID, bearer, login, okData } from "./api-helpers";

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

  test("角色列表返回固定角色（回归 /v1/roles 404，roleCode 与实际数据一致）", async ({ request }) => {
    const token = await login(request, PHONES.admin);
    const data = await okData(await request.get(`${GW}/v1/roles`, { headers: bearer(token) }));
    const codes = data.map((r: { roleCode: string }) => r.roleCode);
    // 与 user_role.role_code / JWT roles 一致（带 ROLE_ 前缀）
    expect(codes).toEqual(expect.arrayContaining(["ROLE_ADMIN", "ROLE_TEACHER", "ROLE_STUDENT"]));
  });

  test("设置用户角色：全量替换 + 还原（assignRoles 前后端契约）", async ({ request }) => {
    const token = await login(request, PHONES.admin);
    const h = bearer(token);

    // /v1/roles → roleCode→id 映射（前端多选回显/分配即依赖此映射）
    const roles = await okData(await request.get(`${GW}/v1/roles`, { headers: h }));
    const idByCode: Record<string, number> = {};
    for (const r of roles) idByCode[r.roleCode] = r.id;
    expect(idByCode["ROLE_TEACHER"]).toBeTruthy();

    // 选一个非 student01 的学生用户，避免影响其它用例依赖的登录账号
    const page = await okData(
      await request.get(`${GW}/v1/users`, { headers: h, params: { userType: 1, pageNum: 1, pageSize: 20 } })
    );
    const target = page.records.find(
      (u: { id: number; username: string }) => u.username !== "student01" && u.id !== STUDENT01_ID
    );
    expect(target, "应能找到一个非 student01 的学生用户").toBeTruthy();
    const userId: number = target.id;
    const originalCodes: string[] = target.roles;
    const originalIds = originalCodes.map((c) => idByCode[c]);
    expect(originalIds.every((x) => !!x), "用户现有角色均应在 /v1/roles 中").toBeTruthy();

    // 全量替换为 [原有 + 教师]
    const newIds = Array.from(new Set([...originalIds, idByCode["ROLE_TEACHER"]]));
    await okData(await request.put(`${GW}/v1/users/${userId}/roles`, { headers: h, data: { roleIds: newIds } }));
    let after = await okData(await request.get(`${GW}/v1/users/${userId}`, { headers: h }));
    expect(after.roles).toContain("ROLE_TEACHER");
    expect(after.roles.length).toBe(newIds.length);

    // 还原，确保不污染数据
    await okData(await request.put(`${GW}/v1/users/${userId}/roles`, { headers: h, data: { roleIds: originalIds } }));
    after = await okData(await request.get(`${GW}/v1/users/${userId}`, { headers: h }));
    expect([...after.roles].sort()).toEqual([...originalCodes].sort());
  });

  test("设置角色拒绝未知 roleId（400）", async ({ request }) => {
    const token = await login(request, PHONES.admin);
    const res = await request.put(`${GW}/v1/users/${STUDENT01_ID}/roles`, {
      headers: bearer(token),
      data: { roleIds: [99999] },
    });
    const body = await res.json();
    expect(body.code, "未知角色 id 应返回参数错误").toBe(400);
  });
});
