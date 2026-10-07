# 园区业务事件中心 · 执行手册

面向实施、运维与演示人员，覆盖启动、登录、验证、常用操作与排障。命令默认在**仓库根目录**执行；
后端目录为 `backend/`，前端目录为 `frontend/`。

## 1. 前置条件

| 形态 | 依赖 |
| --- | --- |
| 生产（默认 profile） | JDK 17、Maven 3.9+、Docker（PostgreSQL 16 / Redis 7 / Kafka 3.7）、Node 20+ |
| 本地演示（`local` profile） | JDK 17、Maven 3.9+、Node 20+（**无需 Docker**） |

## 2. 形态一：生产部署（PostgreSQL + Redis + Kafka）

启动顺序：**基础设施 → 后端 → 前端**。

1. 启动基础设施
   ```bash
   docker compose up -d
   docker compose ps        # 确认 postgres / redis / kafka 均为健康
   ```
2. 构建后端
   ```bash
   cd backend
   mvn -q -DskipTests install
   ```
3. 启动后端（默认 `http://localhost:8080`）
   ```bash
   mvn -pl admin-api spring-boot:run
   ```
   或产出可执行 jar 独立部署：
   ```bash
   mvn -pl admin-api package
   java -jar admin-api/target/*.jar
   ```
4. 启动前端（`http://localhost:5173`，开发代理将 `/api` 转发到 8080）
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   生产构建：`npm run build`（产出静态资源，按 `frontend/README.md` 将 `/api` 反向代理到后端）。

数据库结构由 Flyway 启动时自动迁移（`backend/admin-api/src/main/resources/db/migration/postgresql`），
无需手工建表。

## 3. 形态二：本地演示（SQLite，无需 Docker）

通过 `local` profile 将 PostgreSQL/Redis/Kafka 切换为嵌入式 SQLite（基础设施端口/适配器，生产方案不变）。

一键启动：
```powershell
pwsh ./scripts/run-local.ps1          # Windows PowerShell
```
```bash
bash scripts/run-local.sh             # Linux / macOS
```

手动启动：
```powershell
$env:BEC_SQLITE_PATH = "$PWD/backend/admin-api/data/eventcenter.db"
cd backend
mvn -q -DskipTests install
mvn -pl admin-api spring-boot:run "-Dspring-boot.run.profiles=local"
```

- 数据文件：`backend/admin-api/data/eventcenter.db`（`BEC_SQLITE_PATH` 可覆盖任意路径）
- 迁移目录：`db/migration/sqlite`
- 对外 REST API 与生产一致；详见 `docs/local-demo.md`

## 4. 登录

### 4.1 管理前端

浏览器打开 `http://localhost:5173`，默认展示登录页；输入账号后进入事件列表。

| 用户名 | 密码 | 园区 | 角色 |
| --- | --- | --- | --- |
| `admin` | `admin123` | PARK-A | 管理员 |
| `ops` | `ops123` | PARK-B | 运营 |

### 4.2 后端 API

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
返回 `token`；后续请求带请求头 `Authorization: Bearer <token>`。

交互式 API 文档：`http://localhost:8080/swagger-ui.html`（OpenAPI：`/v3/api-docs`）。

## 5. 健康检查与可观测性

- `GET /actuator/health`（期望 `status=UP`）
- `GET /actuator/prometheus`、`GET /actuator/metrics`

## 6. 冒烟验证

```bash
# 后端：格式化 + 静态检查 + 单元测试
cd backend && mvn -q verify

# local profile 契约测试（嵌入式 SQLite，无需外部中间件）
mvn -q -pl admin-api -am test -Dtest=LocalProfileContractTest

# 生产 profile 契约 + 端到端（需 docker compose 基础设施，且设置 BEC_IT=true）
BEC_IT=true mvn -pl admin-api -am test -Dtest=EventCenterIntegrationTest

# 仅用 HTTP API 的前后台解耦脚本（需后端已启动）
bash scripts/api-e2e.sh

# 前端
cd frontend && npm run lint && npm run build
```

## 7. 常用操作

- 重置本地数据：停后端 → 删除 `backend/admin-api/data/eventcenter.db`（及 `-wal` / `-shm`）→ 重启（Flyway 自动重建）。
- 切换形态：不激活 `local` 即生产（PostgreSQL/Redis/Kafka）；激活 `local` 为本地演示（SQLite）。
- 停止服务：前台运行按 `Ctrl+C`；后台运行结束对应进程（占用 8080 的 java、5173 的 node）。

## 8. 回滚

本变更无线上存量：停止服务 → 还原仓库与部署编排（`docker-compose.yml`、`application*.yml`、迁移目录）即可；
本地演示删除 SQLite 数据文件即回到初始空库。

## 9. 排障

- **端口被占用**：确认 8080 / 5173 空闲，或修改 `server.port`（后端）与 `frontend/vite.config.ts` 的 `server.port`。
- **SQLite 无法创建**：确认 `BEC_SQLITE_PATH` 的父目录可写（应用会在数据源初始化前自动创建缺失目录）。
- **生产集成测试被跳过**：需设置 `BEC_IT=true` 且已 `docker compose up -d` 启动 PostgreSQL/Redis（Kafka 由测试内嵌）。
- **Windows 禁止运行脚本**（`npm.ps1` / `openspec.ps1` 报 UnauthorizedAccess）：改用 `npm.cmd` / `openspec.cmd`，或以管理员执行 `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`。
