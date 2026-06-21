import { APIRequestContext, APIResponse, expect } from "@playwright/test";
import { execSync } from "child_process";

/**
 * S1–S5 API 集成测试公共工具。
 *
 * 经网关 :18080 打真实后端，断言真实后端状态。需在内网机上运行
 * （依赖 docker exec 访问 Redis 取 dev 短信验证码、访问 MySQL 核对落库）。
 */

export const GW = "http://localhost:18080/api";

export const PHONES = {
  admin: "13800000001", // student_id 无；管理员
  teacher: "13800000002", // teacher01
  student: "13800000011", // student01 → sys_user.id = 3
} as const;

/** student01 的用户 id（seed 固定），用于 DB 落库核对。 */
export const STUDENT01_ID = 3;

export function bearer(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function sh(cmd: string): string {
  try {
    return execSync(cmd, { encoding: "utf8" }).trim();
  } catch {
    return "";
  }
}

/** dev 短信登录：清限流 → 发码 → 从 Redis 读码 → 登录，返回 accessToken。 */
export async function login(request: APIRequestContext, phone: string): Promise<string> {
  sh(`docker exec edu-redis redis-cli -p 6379 DEL "sms:rate:${phone}" "sms:code:${phone}"`);
  await request.post(`${GW}/v1/auth/sms/send`, { params: { phone } });
  await new Promise((r) => setTimeout(r, 600));
  const code = sh(`docker exec edu-redis redis-cli -p 6379 GET "sms:code:${phone}"`);
  expect(code, "应能从 Redis 取到 dev 验证码").toMatch(/^\d{6}$/);
  const res = await request.post(`${GW}/v1/auth/login/phone`, { data: { phone, code } });
  const body = await res.json();
  expect(body.code, JSON.stringify(body)).toBe(200);
  return body.data.accessToken;
}

/** 解包 Result 信封：断言 HTTP ok 且 code=200，返回 data。 */
export async function okData(res: APIResponse): Promise<any> {
  expect(res.ok(), `HTTP ${res.status()}`).toBeTruthy();
  const body = await res.json();
  expect(body.code, JSON.stringify(body)).toBe(200);
  return body.data;
}

/** 服务器本地时间（无时区后缀），偏移分钟。用于考试开始/结束时间。 */
export function isoLocal(offsetMin = 0): string {
  const d = new Date(Date.now() + offsetMin * 60_000);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

/** 直查 edu_db，返回首行首列（用于落库核对）。 */
export function mysql(sql: string): string {
  return sh(`docker exec edu-mysql mysql -u root -pedu_dev_2026 edu_db -se "${sql}"`);
}
