# 管理前端（Vue 3 + TypeScript + Vite）

管理台仅消费后端公开 REST API，可独立部署与替换，不与后端共享会话或私有接口。

## 运行

```bash
npm install
npm run dev      # http://localhost:5173 ，开发代理将 /api 转发到 http://localhost:8080
```

先启动后端（默认端口 8080），再启动前端。默认账号：

- `admin / admin123`（园区 PARK-A，管理员）
- `ops / ops123`（园区 PARK-B，运营）

## 构建与校验

```bash
npm run build       # 生产构建
npm run lint        # ESLint
npm run type-check  # vue-tsc 类型检查
```

## 功能与页面

- 登录与路由守卫：未登录访问受限页面自动跳转登录；顶部展示当前园区，园区隔离由后端按 JWT 强制。
- 事件管理：列表（按类型/严重级别/来源/位置过滤）、详情与处理轨迹、手工创建事件、分发/重投。
- 来源管理：注册来源并一次性展示凭证。
- 订阅与规则：订阅增删改查、启停、过滤表达式与转换规则配置。
- 通知管理：通道、模板与投递记录（含失败重发）。
- 运营大屏：统计图表（ECharts）+ 实时态势（SSE `/api/dashboard/stream` 推送）。

## API 契约

后端 OpenAPI 契约位于 `backend/admin-api/src/main/resources/openapi/openapi.yaml`，前端基于同一契约对接。
