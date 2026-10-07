# 本地演示运行（SQLite，无需外部中间件）

在生产方案不变的前提下，本项目通过**基础设施端口/适配器**支持一个 `local` profile：
关系型数据库、事件总线与幂等存储分别切换到嵌入式 SQLite，无需 Docker、PostgreSQL、Redis、Kafka，
即可在本地 PC 上跑通"接入 → 存储 → 订阅匹配 → 分发 → 通知/看板"主链路。

## 适配层一览

| 能力端口 | 生产适配器（默认 profile） | 本地适配器（`local` profile） |
| --- | --- | --- |
| 事件总线 `EventBus` | `EventPublisher`（Kafka，`!local`） | `SqliteOutboxEventBus` + `OutboxDispatchPoller`（SQLite 发件箱 + 定时轮询） |
| 幂等 `IdempotencyStore` | `RedisIdempotencyStore`（`!local`） | `SqliteIdempotencyStore`（SQLite 表 + 过期清理） |
| 关系型持久化 | PostgreSQL（Hibernate + Flyway `db/migration/postgresql`） | SQLite（`SQLiteDialect` + Flyway `db/migration/sqlite`） |

业务模块只依赖端口；`local` profile 不改动任何对外 REST API 契约。

## 前置条件

- JDK 17、Maven 3.9+
- 无需 Docker

## 一键启动

```powershell
# Windows PowerShell
pwsh ./scripts/run-local.ps1
```

```bash
# Linux / macOS
bash scripts/run-local.sh
```

脚本会创建数据目录、导出 `BEC_SQLITE_PATH`，然后以 `local` profile 启动后端
（默认 `http://localhost:8080`）。

## 手动启动

```powershell
# Windows PowerShell
$env:BEC_SQLITE_PATH = "$PWD/backend/admin-api/data/eventcenter.db"
cd backend
mvn -q -DskipTests install
mvn -pl admin-api spring-boot:run "-Dspring-boot.run.profiles=local"
```

```bash
# Bash
export BEC_SQLITE_PATH="$PWD/backend/admin-api/data/eventcenter.db"
cd backend
mvn -q -DskipTests install
mvn -pl admin-api spring-boot:run -Dspring-boot.run.profiles=local
```

启动后可用内置账号 `admin / admin123`（园区 PARK-A）登录：`POST /api/auth/login`。

## 配置项

本地配置位于 `backend/admin-api/src/main/resources/application-local.yml`：

| 键 | 说明 | 默认 |
| --- | --- | --- |
| `spring.datasource.url` | SQLite 文件路径（可用 `BEC_SQLITE_PATH` 覆盖） | `jdbc:sqlite:./data/eventcenter.db?journal_mode=WAL&busy_timeout=10000` |
| `spring.flyway.locations` | 本地迁移目录 | `classpath:db/migration/sqlite` |
| `spring.jpa.properties.hibernate.dialect` | SQLite 方言 | `org.hibernate.community.dialect.SQLiteDialect` |
| `event-center.local.outbox.poll-interval-ms` | 发件箱轮询间隔 | `500` |
| `event-center.local.outbox.max-attempts` | 发件箱投递最大尝试次数 | `3` |
| `event-center.local.outbox.backoff-millis` | 发件箱退避基数 | `1000` |
| `event-center.local.idempotency.cleanup-interval-ms` | 幂等键过期清理间隔 | `600000` |

`local` profile 排除了 `KafkaAutoConfiguration` 与 Redis 自动配置，因此不会尝试连接外部中间件。
若数据库路径的父目录不存在，`SqliteDataDirectoryEnvironmentPostProcessor` 会在数据源初始化前自动创建。

## 数据文件位置

默认写入 `backend/admin-api/data/eventcenter.db`（`BEC_SQLITE_PATH` 可改为任意路径）。
删除该文件即可重置本地数据；Flyway 在下次启动时按 `db/migration/sqlite` 重新建表。

本地专有表（仅 SQLite 迁移创建）：

- `idempotency_key`：幂等去重键与过期时间（替代 Redis 键）。
- `event_outbox`：事件发件箱（`PENDING` / `DISPATCHED` / `DEAD_LETTER`），提供持久化缓冲、重试与死信。

## 与生产方案的差异

- 本地使用单文件 SQLite（单写者、WAL 并发），仅用于演示与开发；容量/并发基线只在 PostgreSQL 下评估。
- 生产默认 profile 仍是 **PostgreSQL + Redis + Kafka**，迁移位于 `db/migration/postgresql`，行为不变。
- 对外 REST API 契约在两种 profile 下完全一致，由共享的契约测试保证。

## 切回生产方案

不激活 `local` 即可（默认 profile）：

```bash
docker compose up -d          # PostgreSQL + Redis + Kafka
cd backend && mvn -pl admin-api spring-boot:run
```

## 验证

```bash
# 单元测试 + 格式化/静态检查
cd backend && mvn -q verify

# local profile 契约测试（嵌入式 SQLite，无需外部中间件）
mvn -q -pl admin-api -am test -Dtest=LocalProfileContractTest

# 生产 profile 契约测试 + 端到端（需 docker compose 基础设施）
BEC_IT=true mvn -pl admin-api -am test -Dtest=EventCenterIntegrationTest
```

`LocalProfileContractTest` 与 `EventCenterIntegrationTest` 共享同一组 API 契约场景
（见 `AbstractApiContractTest`），从而验证两种后端下可观察行为一致。
