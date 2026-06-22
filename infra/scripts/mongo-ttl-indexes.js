// ─────────────────────────────────────────────────────────────────────────────
// S6-15 MongoDB TTL 索引（幂等）
//
// 为 AI 模块的过期数据配置 TTL 索引，Mongo 后台每 ~60s 自动清理过期文档。
//   ai_dialogue_message   对话消息   保留 30 天
//   ai_lecture_transcript 课堂转写   保留 90 天
//
// 对已存在集合执行：若 createdAt 上已有非目标 TTL 的索引则先删除再重建（幂等）。
// 运行：mongosh edu_ai < mongo-ttl-indexes.js （或旧版 mongo 4.x: mongo edu_ai < ...）
// 兼容 mongosh 与 mongo 4.4 legacy shell（不使用 ?? 等新语法）。
// ─────────────────────────────────────────────────────────────────────────────
const DAY = 24 * 3600;
const targets = [
  { coll: 'ai_dialogue_message', seconds: 30 * DAY },
  { coll: 'ai_lecture_transcript', seconds: 90 * DAY },
];

targets.forEach(({ coll, seconds }) => {
  const c = db.getCollection(coll);
  c.getIndexes().forEach((ix) => {
    if (ix.key && ix.key.createdAt === 1 && ix.expireAfterSeconds !== seconds) {
      print(`[${coll}] 删除旧 createdAt 索引: ${ix.name} (expireAfterSeconds=${ix.expireAfterSeconds})`);
      c.dropIndex(ix.name);
    }
  });
  c.createIndex({ createdAt: 1 }, { expireAfterSeconds: seconds, name: 'ttl_created_at' });
  print(`[${coll}] 已确保 TTL 索引 ttl_created_at: expireAfterSeconds=${seconds}`);
});

print('──────── 当前索引 ────────');
targets.forEach(({ coll }) => {
  print(`# ${coll}`);
  db.getCollection(coll).getIndexes().forEach((ix) => {
    var ttl = (ix.expireAfterSeconds === undefined) ? '-' : ix.expireAfterSeconds;
    print('  ' + ix.name + ': key=' + JSON.stringify(ix.key) + ' ttl=' + ttl);
  });
});
