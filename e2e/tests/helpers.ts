import { execSync } from "child_process";

export function clearSmsRateLimit(phone: string) {
  try {
    execSync(
      `docker exec edu-redis redis-cli -p 6379 DEL "sms:rate:${phone}" "sms:code:${phone}" 2>/dev/null`,
      { encoding: "utf8" }
    );
  } catch {}
}

export async function getSmsCode(phone: string): Promise<string> {
  const key = `sms:code:${phone}`;
  let code = execSync(
    `docker exec edu-redis redis-cli -p 6379 GET "${key}" 2>/dev/null`,
    { encoding: "utf8" }
  ).trim();

  if (!code || code === "(nil)" || code === "nil") {
    execSync(
      `curl -s -X POST "http://localhost:8081/api/v1/auth/sms/send?phone=${phone}" > /dev/null 2>&1`,
      { encoding: "utf8" }
    );
    await new Promise((r) => setTimeout(r, 600));
    code = execSync(
      `docker exec edu-redis redis-cli -p 6379 GET "${key}" 2>/dev/null`,
      { encoding: "utf8" }
    ).trim();
  }

  if (!code || code === "(nil)") {
    throw new Error(`Could not get SMS code for ${phone}`);
  }
  return code;
}

export function getFirstLessonId(): string {
  try {
    const result = execSync(
      `docker exec edu-mysql mysql -u root -pedu_dev_2026 edu_db -se "SELECT id FROM lesson ORDER BY id LIMIT 1;" 2>/dev/null`,
      { encoding: "utf8" }
    ).trim();
    return result || "1";
  } catch {
    return "1";
  }
}
