# 园区业务事件中心（Park Business Event Center）

统一接入、标准化、分发、通知与监控园区内多来源异构事件的平台。后端以 REST API 为一等交付物
（OpenAPI 契约见 `backend/admin-api/src/main/resources/openapi/openapi.yaml`），管理前端仅作为可替换的 API 客户端。

## 架构

模块化单体（Maven 多模块）`backend/` + 独立前端 `frontend/`：

| 模块 | 职责 |
| --- | --- |
| `event-model` | 规范事件模型与 JSON Schema（跨能力契约） |
| `common` | 安全（JWT/园区隔离）、错误处理、幂等存储、基础设施配置 |
| `ingestion` | 多源接入、来源鉴权、适配器、校验、去重、API 创建事件 |
| `store-query` | 事件持久化、条件检索、处理轨迹、审计 |
| `subscription` | 订阅生命周期、过滤表达式、转换规则 |
| `notification` | 通知通道、模板渲染、投递结果跟踪与重发 |
| `dispatch` | Kafka 事件总线路由、重试/死信、幂等与有序投递、API 触发分发/重投 |
| `dashboard` | 统计聚合、实时态势、与客户端无关的看板 API 与 SSE 推送 |
| `admin-api` | 可运行应用、OpenAPI、Flyway 迁移、可观测性配置 |

主链路：**接入 → 校验/去重 → 标准化 → 持久化 → Kafka → 订阅匹配 → 分发 → 通知/看板**。

## 前置条件

- JDK 17、Maven 3.9+
- Node.js 20+ 与 npm
- Docker（用于 PostgreSQL、Redis、Kafka）

## 从零启动

启动顺序：**基础设施 → 后端 → 前端**。

```bash
# 1. 基础设施（PostgreSQL + Redis + Kafka）
docker compose up -d

# 2. 后端（默认 http://localhost:8080）
cd backend
mvn -q -DskipTests install
mvn -pl admin-api spring-boot:run

# 3. 前端（默认 http://localhost:5173，代理 /api 到后端）
cd frontend
npm install
npm run dev
```

浏览器打开 http://localhost:5173 ，默认账号 `admin / admin123`（园区 PARK-A）。

## 本地演示（SQLite，无需 Docker）

若只想在本地 PC 快速跑通主链路，可用 `local` profile 将 PostgreSQL、Redis、Kafka 一键切换为嵌入式
SQLite（通过基础设施端口/适配器实现，生产方案不变）：

```bash
# Windows: pwsh ./scripts/run-local.ps1 ；Linux/macOS: bash scripts/run-local.sh
bash scripts/run-local.sh
```

默认数据文件为 `backend/admin-api/data/eventcenter.db`，可用 `BEC_SQLITE_PATH` 覆盖。启动后同样是
`http://localhost:8080`。详见 `docs/local-demo.md`。

## 配置项

后端 `backend/admin-api/src/main/resources/application.yml`：

| 键 | 说明 | 默认 |
| --- | --- | --- |
| `spring.datasource.url` | PostgreSQL 连接 | `jdbc:postgresql://localhost:5432/eventcenter` |
| `spring.data.redis.host/port` | Redis 连接 | `localhost:6379` |
| `spring.kafka.bootstrap-servers` | Kafka 地址 | `localhost:9092` |
| `event-center.security.jwt-secret` | JWT 签名密钥（生产必须通过 `JWT_SECRET` 覆盖） | 开发默认值 |
| `event-center.security.users` | 内置账号（用户名/密码/园区/角色） | admin、ops |

`docker-compose.yml` 的镜像可通过根目录 `.env` 覆盖（如镜像镜像源）。数据库结构由 Flyway
（生产 `backend/admin-api/src/main/resources/db/migration/postgresql`，本地
`.../db/migration/sqlite`）管理，应用启动时自动迁移。

## API 与文档

- OpenAPI：运行后访问 `/swagger-ui.html` 与 `/v3/api-docs`。
- 模块接口说明：`docs/ingestion-api.md`、`docs/query-api.md`、`docs/subscription-rules.md`、
  `docs/dispatch-operations.md`、`docs/notification-config.md`、`docs/dashboard-api.md`。
- 前端说明：`frontend/README.md`。

鉴权：管理接口使用 `Authorization: Bearer <JWT>`（`POST /api/auth/login` 获取）；来源接入使用
`X-Source-Id` / `X-Source-Secret`。所有事件、订阅、通知资源按 `parkId` 强制隔离。

## 测试与校验

```bash
# 后端：格式化 + 静态检查 + 单元测试
cd backend && mvn -q verify

# local profile 契约测试（嵌入式 SQLite，无需外部中间件）
mvn -q -pl admin-api -am test -Dtest=LocalProfileContractTest

# 端到端集成测试（需 docker compose 基础设施已启动）
BEC_IT=true mvn -pl admin-api -am test -Dtest=EventCenterIntegrationTest

# 前后端解耦脚本（需后端已启动，仅用 HTTP API，不启动前端）
bash scripts/api-e2e.sh

# 前端
cd frontend && npm run lint && npm run build
```

## 部署

Docker Compose 起 PostgreSQL/Redis/Kafka，后端以 `mvn -pl admin-api package` 产出可执行 jar 独立部署；
前端 `npm run build` 产出静态资源，宿主任意静态服务器，按 `frontend/README.md` 将 `/api` 反向代理到后端。
