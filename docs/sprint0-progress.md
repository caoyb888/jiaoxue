# Sprint 0 执行进度

> 分支: feature/sprint5
> 开始时间: 2026-06-19
> 执行机器: onlyserver (100.84.68.115, xintong)

## 任务状态

| ID | 描述 | 状态 | 完成时间 | 备注 |
|----|------|------|---------|------|
| S0-01 | 内网机 Maven 安装与 PATH 配置 | ✅ 完成 | 2026-06-19 | Maven 3.9.9，/opt/maven，PATH 已配置在 /etc/profile.d/maven.sh |
| S0-02 | SSH 免密登录 | ✅ 完成 | 已有 | ssh onlyserver 无密码可用 |
| S0-03 | Git 仓库同步机制 | ✅ 完成 | 已有 | receive.denyCurrentBranch=updateInstead，422文件已同步 |
| S0-04 | Docker Compose 中间件部署 | ✅ 完成 | 已有 | 9容器运行中（含zookeeper），Kafka需修复 |
| S0-05 | 中间件健康验证+修复 | ✅ 完成 | 2026-06-19 | 修复 KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092；9/9 全部 healthy/200 |
| S0-06 | MySQL 业务库初始化 | ✅ 完成 | 2026-06-19 | 41张表+审计库2张表，3个测试账号(admin/teacher01/student01)，5个院系 |
| S0-07 | Nacos dev 配置导入 | ✅ 完成 | 2026-06-19 | dev命名空间+14个服务配置（DB/Redis/Kafka/MongoDB/MinIO地址） |
| S0-08 | MinIO Bucket 初始化 | ✅ 完成 | 2026-06-19 | 4个Bucket：edu-files/edu-exam-attach/edu-live-replay/edu-archive |
| S0-09 | Elasticsearch 索引初始化 | ✅ 完成 | 2026-06-19 | 安装IK分词插件(v8.11.4)，创建edu_question+edu_courseware索引 |
| S0-10 | ClickHouse 统计表初始化 | ✅ 完成 | 2026-06-19 | 新增clickhouse_schema.sql，3张统计表：lesson_event_log/lesson_stat_daily/dept_teaching_stat |
| S0-11 | 本机开发工具确认 | ✅ 完成 | 已有 | Java 21(Temurin) / Node 20.20 / pnpm 10.33 / Docker 29 |
| S0-12 | CI/CD 配置 | ⏭️ 适配 | — | 项目用 GitHub，.gitlab-ci.yml 已存在，跳过 GitLab Runner 注册 |

## 变更记录

### S0-01（2026-06-19）
- Maven 3.9.9 已预装于 /opt/maven
- /etc/profile.d/maven.sh 已配置 MAVEN_HOME 和 PATH
- ~/.bashrc 第121行已 source /etc/profile.d/maven.sh
- 验证：`ssh onlyserver 'source /etc/profile.d/maven.sh && mvn --version'` → Apache Maven 3.9.9

### S0-05 问题记录
- Kafka unhealthy：KAFKA_ADVERTISED_LISTENERS 设为外部IP（100.84.68.115:19092），
  但未设置 KAFKA_LISTENERS 绑定 0.0.0.0，导致健康检查连 localhost:9092 失败
  → 待 S0-05 修复 docker-compose.dev.yml
- ClickHouse：密码 edu_dev_2026（docker-compose 中配置），curl 需加 --user 参数
