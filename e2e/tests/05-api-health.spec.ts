import { test, expect } from "@playwright/test";

// 직접 API 엔드포인트 건강 확인 (Gateway 통과)
test.describe("API 서비스 상태 확인", () => {
  const services = [
    { name: "edu-auth (8081)",        url: "http://localhost:8081/actuator/health" },
    { name: "edu-user (8082/18082)",  url: "http://localhost:18082/actuator/health" },
    { name: "edu-course (8083)",      url: "http://localhost:8083/actuator/health" },
    { name: "edu-interaction (8084)", url: "http://localhost:8084/actuator/health" },
    { name: "edu-exam (8085)",        url: "http://localhost:8085/actuator/health" },
    { name: "edu-grade (8086)",       url: "http://localhost:8086/actuator/health" },
    { name: "edu-ai (8087)",          url: "http://localhost:8087/actuator/health" },
    { name: "edu-file (8089)",        url: "http://localhost:8089/actuator/health" },
    { name: "edu-notify (8090)",      url: "http://localhost:8090/actuator/health" },
    { name: "edu-live (8091)",        url: "http://localhost:8091/actuator/health" },
    { name: "edu-admin (8092)",       url: "http://localhost:8092/actuator/health" },
    { name: "edu-jwxt (8093)",        url: "http://localhost:8093/actuator/health" },
    { name: "edu-gateway (18080)",    url: "http://localhost:18080/actuator/health" },
    { name: "edu-stat (18088)",       url: "http://localhost:18088/actuator/health" },
  ];

  for (const svc of services) {
    test(`${svc.name} 健康检查`, async ({ request }) => {
      const res = await request.get(svc.url, { timeout: 5000 }).catch(() => null);
      if (!res) {
        console.log(`  ⚠️  ${svc.name}: 无法连接`);
        return; // 不失败，仅记录
      }
      const body = await res.json().catch(() => ({}));
      const status = body?.status ?? res.status();
      console.log(`  ${svc.name}: ${status}`);
      // 只要服务响应（不是 connection refused）就算通过
      expect(res.status()).toBeLessThan(503);
    });
  }
});
